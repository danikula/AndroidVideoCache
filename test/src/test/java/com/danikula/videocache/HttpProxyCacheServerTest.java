package com.danikula.videocache;

import android.util.Pair;

import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.file.FileNameGenerator;
import com.danikula.videocache.file.Md5FileNameGenerator;
import com.danikula.videocache.support.ProxyCacheTestUtils;
import com.danikula.videocache.support.Response;
import com.danikula.videocache.test.BuildConfig;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static com.danikula.android.garden.io.Files.cleanDirectory;
import static com.danikula.android.garden.io.Files.createDirectory;
import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_SIZE;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_URL_ONE_REDIRECT;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_SIZE;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL_3_REDIRECTS;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL_6_REDIRECTS;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL_ONE_REDIRECT;
import static com.danikula.videocache.support.ProxyCacheTestUtils.getFileContent;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadAssetFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.readProxyResponse;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = BuildConfig.MIN_SDK_VERSION)
public class HttpProxyCacheServerTest {

    private File cacheFolder;

    @Before
    public void setup() throws Exception {
        cacheFolder = ProxyCacheTestUtils.newCacheFile();
        createDirectory(cacheFolder);
        cleanDirectory(cacheFolder);
    }

    @Test
    public void testHttpProxyCache() throws Exception {
        Pair<File, Response> response = readProxyData(HTTP_DATA_URL);

        assertThat(response.second.code).isEqualTo(200);
        assertThat(response.second.data).isEqualTo(getFileContent(response.first));
        assertThat(response.second.data).isEqualTo(loadAssetFile(ASSETS_DATA_NAME));
    }

    @Test
    public void testProxyContentWithPartialCache() throws Exception {
        File cacheDir = RuntimeEnvironment.application.getExternalCacheDir();
        File file = new File(cacheDir, new Md5FileNameGenerator().generate(HTTP_DATA_URL));
        int partialCacheSize = 1000;
        byte[] partialData = ProxyCacheTestUtils.generate(partialCacheSize);
        File partialCacheFile = ProxyCacheTestUtils.getTempFile(file);
        IoUtils.saveToFile(partialData, partialCacheFile);

        HttpProxyCacheServer proxy = newProxy(cacheDir);
        Response response = readProxyResponse(proxy, HTTP_DATA_URL);
        proxy.shutdown();

        byte[] expected = loadAssetFile(ASSETS_DATA_NAME);
        System.arraycopy(partialData, 0, expected, 0, partialCacheSize);
        assertThat(response.data).isEqualTo(expected);
    }

    @Test
    public void testMimeFromResponse() throws Exception {
        Pair<File, Response> response = readProxyData("https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/android");
        assertThat(response.second.contentType).isEqualTo("application/octet-stream");
    }

    @Test
    public void testProxyFullResponse() throws Exception {
        Pair<File, Response> response = readProxyData(HTTP_DATA_BIG_URL);

        assertThat(response.second.code).isEqualTo(200);
        assertThat(response.second.contentLength).isEqualTo(HTTP_DATA_BIG_SIZE);
        assertThat(response.second.contentType).isEqualTo("image/jpeg");
        assertThat(response.second.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.second.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.second.headers.containsKey("Content-Range")).isFalse();
        assertThat(response.second.data).isEqualTo(getFileContent(response.first));
        assertThat(response.second.data).isEqualTo(loadAssetFile(ASSETS_DATA_BIG_NAME));
    }

    @Test
    public void testProxyFullResponseWithRedirect() throws Exception {
        Pair<File, Response> response = readProxyData(HTTP_DATA_BIG_URL_ONE_REDIRECT);

        assertThat(response.second.code).isEqualTo(200);
        assertThat(response.second.contentLength).isEqualTo(HTTP_DATA_BIG_SIZE);
        assertThat(response.second.contentType).isEqualTo("image/jpeg");
        assertThat(response.second.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.second.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.second.headers.containsKey("Content-Range")).isFalse();
        assertThat(response.second.data).isEqualTo(getFileContent(response.first));
        assertThat(response.second.data).isEqualTo(loadAssetFile(ASSETS_DATA_BIG_NAME));
    }

    @Test
    public void testProxyPartialResponse() throws Exception {
        int offset = 18000;
        Pair<File, Response> response = readProxyData(HTTP_DATA_BIG_URL, offset);

        assertThat(response.second.code).isEqualTo(206);
        assertThat(response.second.contentLength).isEqualTo(HTTP_DATA_BIG_SIZE - offset);
        assertThat(response.second.contentType).isEqualTo("image/jpeg");
        assertThat(response.second.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.second.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.second.headers.containsKey("Content-Range")).isTrue();
        String rangeHeader = String.format("bytes %d-%d/%d", offset, HTTP_DATA_BIG_SIZE - 1, HTTP_DATA_BIG_SIZE);
        assertThat(response.second.headers.get("Content-Range").get(0)).isEqualTo(rangeHeader);
        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_BIG_NAME), offset, HTTP_DATA_BIG_SIZE);
        assertThat(response.second.data).isEqualTo(expectedData);
        assertThat(getFileContent(response.first)).isEqualTo(loadAssetFile(ASSETS_DATA_BIG_NAME));
    }

    @Test
    public void testProxyPartialResponseWithRedirect() throws Exception {
        int offset = 18000;
        Pair<File, Response> response = readProxyData(HTTP_DATA_BIG_URL_ONE_REDIRECT, offset);

        assertThat(response.second.code).isEqualTo(206);
        assertThat(response.second.contentLength).isEqualTo(HTTP_DATA_BIG_SIZE - offset);
        assertThat(response.second.contentType).isEqualTo("image/jpeg");
        assertThat(response.second.headers.containsKey("Accept-Ranges")).isTrue();
        assertThat(response.second.headers.get("Accept-Ranges").get(0)).isEqualTo("bytes");
        assertThat(response.second.headers.containsKey("Content-Range")).isTrue();
        String rangeHeader = String.format("bytes %d-%d/%d", offset, HTTP_DATA_BIG_SIZE - 1, HTTP_DATA_BIG_SIZE);
        assertThat(response.second.headers.get("Content-Range").get(0)).isEqualTo(rangeHeader);
        byte[] expectedData = Arrays.copyOfRange(loadAssetFile(ASSETS_DATA_BIG_NAME), offset, HTTP_DATA_BIG_SIZE);
        assertThat(response.second.data).isEqualTo(expectedData);
        assertThat(getFileContent(response.first)).isEqualTo(loadAssetFile(ASSETS_DATA_BIG_NAME));
    }

    @Test
    public void testMaxSizeCacheLimit() throws Exception {
        HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .cacheDirectory(cacheFolder)
                .maxCacheSize(HTTP_DATA_SIZE * 3 - 1) // for 2 files
                .build();

        // use different url (doesn't matter than same content)
        readProxyResponse(proxy, HTTP_DATA_URL, 0);
        Thread.sleep(1050); // wait for new last modified date (file rounds time to second)

        readProxyResponse(proxy, HTTP_DATA_URL_ONE_REDIRECT, 0);
        Thread.sleep(1050);

        readProxyResponse(proxy, HTTP_DATA_URL_3_REDIRECTS, 0);
        Thread.sleep(1050);

        assertThat(file(cacheFolder, HTTP_DATA_URL)).doesNotExist();
        assertThat(file(cacheFolder, HTTP_DATA_URL_ONE_REDIRECT)).exists();
        assertThat(file(cacheFolder, HTTP_DATA_URL_3_REDIRECTS)).exists();

        readProxyResponse(proxy, HTTP_DATA_URL_ONE_REDIRECT, 0); // touch file
        readProxyResponse(proxy, HTTP_DATA_URL_6_REDIRECTS, 0);
        proxy.shutdown();

        assertThat(file(cacheFolder, HTTP_DATA_URL_3_REDIRECTS)).doesNotExist();
        assertThat(file(cacheFolder, HTTP_DATA_URL_ONE_REDIRECT)).exists();
        assertThat(file(cacheFolder, HTTP_DATA_URL_6_REDIRECTS)).exists();
    }

    @Test
    public void testMaxFileCacheLimit() throws Exception {
        HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .cacheDirectory(cacheFolder)
                .maxCacheFilesCount(2)
                .build();

        // use different url (doesn't matter than same content)
        readProxyResponse(proxy, HTTP_DATA_URL, 0);
        Thread.sleep(1050); // wait for new last modified date (file rounds time to second)

        readProxyResponse(proxy, HTTP_DATA_URL_ONE_REDIRECT, 0);
        Thread.sleep(1050);

        readProxyResponse(proxy, HTTP_DATA_URL_3_REDIRECTS, 0);
        Thread.sleep(1050);

        assertThat(file(cacheFolder, HTTP_DATA_URL)).doesNotExist();
        assertThat(file(cacheFolder, HTTP_DATA_URL_ONE_REDIRECT)).exists();
        assertThat(file(cacheFolder, HTTP_DATA_URL_3_REDIRECTS)).exists();

        readProxyResponse(proxy, HTTP_DATA_URL_ONE_REDIRECT, 0); // touch file
        readProxyResponse(proxy, HTTP_DATA_URL_6_REDIRECTS, 0);
        proxy.shutdown();

        assertThat(file(cacheFolder, HTTP_DATA_URL_3_REDIRECTS)).doesNotExist();
        assertThat(file(cacheFolder, HTTP_DATA_URL_ONE_REDIRECT)).exists();
        assertThat(file(cacheFolder, HTTP_DATA_URL_6_REDIRECTS)).exists();
    }

    private Pair<File, Response> readProxyData(String url, int offset) throws IOException {
        File file = file(cacheFolder, url);
        HttpProxyCacheServer proxy = newProxy(cacheFolder);

        Response response = readProxyResponse(proxy, url, offset);
        proxy.shutdown();

        return new Pair<>(file, response);
    }

    private File file(File parent, String url) {
        FileNameGenerator fileNameGenerator = new Md5FileNameGenerator();
        String name = fileNameGenerator.generate(url);
        return new File(parent, name);
    }

    private Pair<File, Response> readProxyData(String url) throws IOException {
        return readProxyData(url, -1);
    }

    private HttpProxyCacheServer newProxy(File cacheDir) {
        return new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .cacheDirectory(cacheDir)
                .build();
    }
}
