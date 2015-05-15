package com.danikula.videocache;

import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.support.AngryHttpUrlSource;
import com.danikula.videocache.support.Response;
import com.danikula.videocache.test.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;
import java.util.Arrays;

import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_SIZE;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.generate;
import static com.danikula.videocache.support.ProxyCacheTestUtils.getFileContent;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadAssetFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.newCacheFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.readProxyResponse;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = BuildConfig.MIN_SDK_VERSION)
public class HttpProxyCacheTest {

    @Test
    public void testHttpProxyCache() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL);
        File file = newCacheFile();
        HttpProxyCache proxy = new HttpProxyCache(source, new FileCache(file));
        Response response = readProxyResponse(proxy);
        assertThat(response.code).isEqualTo(200);
        assertThat(response.data).isEqualTo(getFileContent(file));
        assertThat(response.data).isEqualTo(loadAssetFile(ASSETS_DATA_NAME));
        proxy.shutdown();
    }

    @Test
    public void testProxyContentWithPartialCache() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL);
        int cacheSize = 1000;
        HttpProxyCache proxy = new HttpProxyCache(source, new ByteArrayCache(new byte[cacheSize]));

        Response proxyResponse = readProxyResponse(proxy);
        byte[] expected = loadAssetFile(ASSETS_DATA_NAME);
        Arrays.fill(expected, 0, cacheSize, (byte) 0);
        assertThat(proxyResponse.data).isEqualTo(expected);
        proxy.shutdown();
    }

    @Test
    public void testMimeFromResponse() throws Exception {
        HttpUrlSource source = new HttpUrlSource("https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/android");
        HttpProxyCache proxy = new HttpProxyCache(source, new ByteArrayCache(new byte[0]));
        proxy.read(new byte[1], 0, 1);
        assertThat(source.getMime()).isEqualTo("application/octet-stream");
        proxy.shutdown();
    }

    @Test
    public void testProxyFullResponse() throws Exception {
        File file = newCacheFile();
        HttpProxyCache proxy = new HttpProxyCache(new HttpUrlSource(HTTP_DATA_BIG_URL), new FileCache(file));
        Response response = readProxyResponse(proxy);

        assertThat(response.code).isEqualTo(200);
        assertThat(response.contentLength).isEqualTo(HTTP_DATA_BIG_SIZE);
        assertThat(response.contentType).isEqualTo("image/jpeg");
        assertThat(response.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.headers.containsKey("Content-Range")).isFalse();
        assertThat(response.data).isEqualTo(getFileContent(file));
        assertThat(response.data).isEqualTo(loadAssetFile(ASSETS_DATA_BIG_NAME));
        proxy.shutdown();
    }

    @Test
    public void testProxyPartialResponse() throws Exception {
        int offset = 42000;
        File file = newCacheFile();
        HttpProxyCache proxy = new HttpProxyCache(new HttpUrlSource(HTTP_DATA_BIG_URL), new FileCache(file));
        Response response = readProxyResponse(proxy, offset);

        assertThat(response.code).isEqualTo(206);
        assertThat(response.contentLength).isEqualTo(HTTP_DATA_BIG_SIZE - offset);
        assertThat(response.contentType).isEqualTo("image/jpeg");
        assertThat(response.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.headers.containsKey("Content-Range")).isTrue();
        String rangeHeader = String.format("bytes %d-%d/%d", offset, HTTP_DATA_BIG_SIZE, HTTP_DATA_BIG_SIZE);
        assertThat(response.headers.get("Content-Range").get(0)).isEqualTo(rangeHeader);
        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_BIG_NAME), offset, HTTP_DATA_BIG_SIZE);
        assertThat(response.data).isEqualTo(expectedData);
        assertThat(getFileContent(file)).isEqualTo(loadAssetFile(ASSETS_DATA_BIG_NAME));
        proxy.shutdown();
    }

    @Test
    public void testAppendCache() throws Exception {
        byte[] cachedPortion = generate(1200);
        File file = newCacheFile();
        File partialFile = new File(file.getParentFile(), file.getName() + ".download");
        IoUtils.saveToFile(cachedPortion, partialFile);
        Cache cache = new FileCache(partialFile);
        assertThat(cache.isCompleted()).isFalse();

        HttpProxyCache proxy = new HttpProxyCache(new HttpUrlSource(HTTP_DATA_BIG_URL), cache);
        readProxyResponse(proxy);
        proxy.shutdown();

        assertThat(cache.isCompleted()).isTrue();

        byte[] expectedData = loadAssetFile(ASSETS_DATA_BIG_NAME);
        System.arraycopy(cachedPortion, 0, expectedData, 0, cachedPortion.length);
        assertThat(file.length()).isEqualTo(HTTP_DATA_BIG_SIZE);
        assertThat(expectedData).isEqualTo(getFileContent(file));
    }

    @Test
    public void testNoTouchSource() throws Exception {
        File file = newCacheFile();
        IoUtils.saveToFile(loadAssetFile(ASSETS_DATA_BIG_NAME), file);
        FileCache cache = new FileCache(file);
        HttpProxyCache proxy = new HttpProxyCache(new HttpUrlSource(HTTP_DATA_BIG_URL), cache);
        Response response = readProxyResponse(proxy);
        proxy.shutdown();
        assertThat(response.code).isEqualTo(200);

        proxy = new HttpProxyCache(new AngryHttpUrlSource(HTTP_DATA_BIG_URL, "image/jpeg"), new FileCache(file));
        readProxyResponse(proxy);
        assertThat(response.code).isEqualTo(200);
    }
}
