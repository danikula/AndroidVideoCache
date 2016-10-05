package com.danikula.videocache;

import android.net.Uri;
import android.util.Pair;

import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.file.FileNameGenerator;
import com.danikula.videocache.file.Md5FileNameGenerator;
import com.danikula.videocache.headers.HeaderInjector;
import com.danikula.videocache.support.ProxyCacheTestUtils;
import com.danikula.videocache.support.Response;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

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
import static com.danikula.videocache.support.ProxyCacheTestUtils.getPort;
import static com.danikula.videocache.support.ProxyCacheTestUtils.installExternalSystemProxy;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadAssetFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.readProxyResponse;
import static com.danikula.videocache.support.ProxyCacheTestUtils.resetSystemProxy;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpProxyCacheServerTest extends BaseTest {

    private File cacheFolder;

    @Before
    public void setup() throws Exception {
        cacheFolder = ProxyCacheTestUtils.newCacheFile();
        createDirectory(cacheFolder);
        cleanDirectory(cacheFolder);
        resetSystemProxy();
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

    @Test
    public void testCheckFileExistForNotCachedUrl() throws Exception {
        HttpProxyCacheServer proxy = newProxy(cacheFolder);
        proxy.shutdown();
        assertThat(proxy.isCached(HTTP_DATA_URL)).isFalse();
    }

    @Test
    public void testCheckFileExistForFullyCachedUrl() throws Exception {
        HttpProxyCacheServer proxy = newProxy(cacheFolder);
        readProxyResponse(proxy, HTTP_DATA_URL, 0);
        proxy.shutdown();

        assertThat(proxy.isCached(HTTP_DATA_URL)).isTrue();
    }

    @Test
    public void testCheckFileExistForPartiallyCachedUrl() throws Exception {
        File cacheDir = RuntimeEnvironment.application.getExternalCacheDir();
        File file = file(cacheDir, HTTP_DATA_URL);
        int partialCacheSize = 1000;
        byte[] partialData = ProxyCacheTestUtils.generate(partialCacheSize);
        File partialCacheFile = ProxyCacheTestUtils.getTempFile(file);
        IoUtils.saveToFile(partialData, partialCacheFile);

        HttpProxyCacheServer proxy = newProxy(cacheDir);
        assertThat(proxy.isCached(HTTP_DATA_URL)).isFalse();

        readProxyResponse(proxy, HTTP_DATA_URL);
        proxy.shutdown();

        assertThat(proxy.isCached(HTTP_DATA_URL)).isTrue();
    }

    @Test
    public void testCheckFileExistForDeletedCacheFile() throws Exception {
        HttpProxyCacheServer proxy = newProxy(cacheFolder);
        readProxyResponse(proxy, HTTP_DATA_URL, 0);
        proxy.shutdown();
        File cacheFile = file(cacheFolder, HTTP_DATA_URL);
        boolean deleted = cacheFile.delete();

        assertThat(deleted).isTrue();
        assertThat(proxy.isCached(HTTP_DATA_URL)).isFalse();
    }

    @Test
    public void testGetProxiedUrlForEmptyCache() throws Exception {
        HttpProxyCacheServer proxy = newProxy(cacheFolder);
        String expectedUrl = "http://127.0.0.1:" + getPort(proxy) + "/" + ProxyCacheUtils.encode(HTTP_DATA_URL);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL)).isEqualTo(expectedUrl);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL, true)).isEqualTo(expectedUrl);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL, false)).isEqualTo(expectedUrl);
        proxy.shutdown();
    }

    @Test
    public void testGetProxiedUrlForPartialCache() throws Exception {
        File cacheDir = RuntimeEnvironment.application.getExternalCacheDir();
        File file = new File(cacheDir, new Md5FileNameGenerator().generate(HTTP_DATA_URL));
        int partialCacheSize = 1000;
        byte[] partialData = ProxyCacheTestUtils.generate(partialCacheSize);
        File partialCacheFile = ProxyCacheTestUtils.getTempFile(file);
        IoUtils.saveToFile(partialData, partialCacheFile);

        HttpProxyCacheServer proxy = newProxy(cacheFolder);
        String expectedUrl = "http://127.0.0.1:" + getPort(proxy) + "/" + ProxyCacheUtils.encode(HTTP_DATA_URL);

        assertThat(proxy.getProxyUrl(HTTP_DATA_URL)).isEqualTo(expectedUrl);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL, true)).isEqualTo(expectedUrl);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL, false)).isEqualTo(expectedUrl);

        proxy.shutdown();
    }

    @Test
    public void testGetProxiedUrlForExistedCache() throws Exception {
        HttpProxyCacheServer proxy = newProxy(cacheFolder);
        readProxyResponse(proxy, HTTP_DATA_URL, 0);
        String proxiedUrl = "http://127.0.0.1:" + getPort(proxy) + "/" + ProxyCacheUtils.encode(HTTP_DATA_URL);

        File cachedFile = file(cacheFolder, HTTP_DATA_URL);
        String cachedFileUri = Uri.fromFile(cachedFile).toString();
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL)).isEqualTo(cachedFileUri);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL, true)).isEqualTo(cachedFileUri);
        assertThat(proxy.getProxyUrl(HTTP_DATA_URL, false)).isEqualTo(proxiedUrl);

        proxy.shutdown();
    }

    @Test
    public void testTrimFileCacheForTotalCountLru() throws Exception {
        FileNameGenerator fileNameGenerator = new Md5FileNameGenerator();
        HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .cacheDirectory(cacheFolder)
                .fileNameGenerator(fileNameGenerator)
                .maxCacheFilesCount(2)
                .build();
        readProxyResponse(proxy, proxy.getProxyUrl(HTTP_DATA_URL), 0);
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL))).exists();

        readProxyResponse(proxy, proxy.getProxyUrl(HTTP_DATA_URL_ONE_REDIRECT), 0);
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL_ONE_REDIRECT))).exists();

        readProxyResponse(proxy, proxy.getProxyUrl(HTTP_DATA_URL_3_REDIRECTS), 0);
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL_3_REDIRECTS))).exists();

        waitForAsyncTrimming();
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL))).doesNotExist();
    }

    @Test
    public void testTrimFileCacheForTotalSizeLru() throws Exception {
        FileNameGenerator fileNameGenerator = new Md5FileNameGenerator();
        HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .cacheDirectory(cacheFolder)
                .fileNameGenerator(fileNameGenerator)
                .maxCacheSize(HTTP_DATA_SIZE * 3 - 1)
                .build();
        readProxyResponse(proxy, proxy.getProxyUrl(HTTP_DATA_URL), 0);
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL))).exists();

        readProxyResponse(proxy, proxy.getProxyUrl(HTTP_DATA_URL_ONE_REDIRECT), 0);
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL_ONE_REDIRECT))).exists();

        readProxyResponse(proxy, proxy.getProxyUrl(HTTP_DATA_URL_3_REDIRECTS), 0);
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL_3_REDIRECTS))).exists();

        waitForAsyncTrimming();
        assertThat(new File(cacheFolder, fileNameGenerator.generate(HTTP_DATA_URL))).doesNotExist();
    }

    @Test // https://github.com/danikula/AndroidVideoCache/issues/28
    public void testWorkWithExternalProxy() throws Exception {
        installExternalSystemProxy();

        Pair<File, Response> response = readProxyData(HTTP_DATA_URL, 0);
        assertThat(response.second.data).isEqualTo(loadAssetFile(ASSETS_DATA_NAME));
    }

    @Test // https://github.com/danikula/AndroidVideoCache/issues/28
    public void testDoesNotWorkWithoutCustomProxySelector() throws Exception {
        HttpProxyCacheServer httpProxyCacheServer = new HttpProxyCacheServer(RuntimeEnvironment.application);
        // IgnoreHostProxySelector is set in HttpProxyCacheServer constructor. So let reset it by custom.
        installExternalSystemProxy();

        String proxiedUrl = httpProxyCacheServer.getProxyUrl(HTTP_DATA_URL);
        // server can't proxy this url due to it is not alive (can't ping itself), so it returns original url
        assertThat(proxiedUrl).isEqualTo(HTTP_DATA_URL);
    }

    @Test
    public void testHeadersInjectorIsInvoked() throws Exception {
        HeaderInjector mockedHeaderInjector = Mockito.mock(HeaderInjector.class);

        HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(RuntimeEnvironment.application)
                .headerInjector(mockedHeaderInjector)
                .build();

        readProxyResponse(proxy, HTTP_DATA_URL);
        proxy.shutdown();

        verify(mockedHeaderInjector, times(2)).addHeaders(HTTP_DATA_URL);   // content info & fetch data requests
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

    private void waitForAsyncTrimming() throws InterruptedException {
        Thread.sleep(500);
    }
}
