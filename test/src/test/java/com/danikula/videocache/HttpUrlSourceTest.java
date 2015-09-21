package com.danikula.videocache;

import com.danikula.videocache.test.BuildConfig;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_SIZE;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_SIZE;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL_3_REDIRECTS;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL_6_REDIRECTS;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL_ONE_REDIRECT;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadAssetFile;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = BuildConfig.MIN_SDK_VERSION)
public class HttpUrlSourceTest {

    @Test
    public void testHttpUrlSourceRange() throws Exception {
        int offset = 1000;
        int length = 10;
        Source source = new HttpUrlSource(HTTP_DATA_URL);
        source.open(offset);
        byte[] readData = new byte[length];
        source.read(readData);
        source.close();
        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_NAME), offset, offset + length);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Test
    public void testHttpUrlSourceWithOffset() throws Exception {
        int offset = 30000;
        Source source = new HttpUrlSource(HTTP_DATA_BIG_URL);
        source.open(offset);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int read;
        byte[] buffer = new byte[3000];
        while ((read = (source.read(buffer))) != -1) {
            outputStream.write(buffer, 0, read);
        }
        source.close();
        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_BIG_NAME), offset, HTTP_DATA_BIG_SIZE);
        assertThat(outputStream.toByteArray()).isEqualTo(expectedData);
    }

    @Test
    public void testFetchContentLength() throws Exception {
        Source source = new HttpUrlSource(HTTP_DATA_URL);
        assertThat(source.available()).isEqualTo(loadAssetFile(ASSETS_DATA_NAME).length);
    }

    @Test
    public void testFetchInfoWithRedirect() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL_ONE_REDIRECT);
        source.open(0);
        int available = source.available();
        String mime = source.getMime();
        source.close();

        assertThat(available).isEqualTo(HTTP_DATA_SIZE);
        assertThat(mime).isEqualTo("image/jpeg");
    }

    @Test
    public void testFetchDataWithRedirect() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL_ONE_REDIRECT);
        source.open(0);
        byte[] readData = new byte[HTTP_DATA_SIZE];
        source.read(readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_NAME), 0, HTTP_DATA_SIZE);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Test
    public void testFetchPartialDataWithRedirect() throws Exception {
        int offset = 42;
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL_ONE_REDIRECT);
        source.open(offset);
        byte[] readData = new byte[HTTP_DATA_SIZE - offset];
        source.read(readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_NAME), offset, HTTP_DATA_SIZE);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Test
    public void testFetchPartialDataWithMultiRedirects() throws Exception {
        int offset = 42;
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL_3_REDIRECTS);
        source.open(offset);
        byte[] readData = new byte[HTTP_DATA_SIZE - offset];
        source.read(readData);
        source.close();

        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_NAME), offset, HTTP_DATA_SIZE);
        assertThat(readData).isEqualTo(expectedData);
    }

    @Ignore("To test it fairly we should disable caching connection.setUseCaches(false), but it will decrease performance")
    @Test(expected = ProxyCacheException.class)
    public void testExceedingRedirects() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL_6_REDIRECTS);
        source.open(0);
        fail("Too many redirects");
    }

    @Ignore("Seems Robolectric bug: MimeTypeMap.getFileExtensionFromUrl always returns null")
    @Test
    public void testMimeByUrl() throws Exception {
        assertThat(new HttpUrlSource("http://mysite.by/video.mp4").getMime()).isEqualTo("video/mp4");
        assertThat(new HttpUrlSource(HTTP_DATA_URL).getMime()).isEqualTo("image/jpeg");
    }
}
