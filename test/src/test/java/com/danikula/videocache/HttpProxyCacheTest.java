package com.danikula.videocache;

import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.file.FileCache;
import com.danikula.videocache.sourcestorage.SourceInfoStorage;
import com.danikula.videocache.sourcestorage.SourceInfoStorageFactory;
import com.danikula.videocache.support.ProxyCacheTestUtils;
import com.danikula.videocache.support.Response;

import org.junit.Test;
import org.mockito.Mockito;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_BIG_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_BIG_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_SIZE;
import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadAssetFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadTestData;
import static com.danikula.videocache.support.ProxyCacheTestUtils.newCacheFile;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test {@link HttpProxyCache}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpProxyCacheTest extends BaseTest {

    @Test
    public void testProcessRequestNoCache() throws Exception {
        Response response = processRequest(HTTP_DATA_URL, "GET /" + HTTP_DATA_URL + " HTTP/1.1");

        assertThat(response.data).isEqualTo(loadTestData());
        assertThat(response.code).isEqualTo(200);
        assertThat(response.contentLength).isEqualTo(HTTP_DATA_SIZE);
        assertThat(response.contentType).isEqualTo("image/jpeg");
    }

    @Test
    public void testProcessPartialRequestWithoutCache() throws Exception {
        FileCache fileCache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        FileCache spyFileCache = Mockito.spy(fileCache);
        doThrow(new RuntimeException()).when(spyFileCache).read(any(byte[].class), anyLong(), anyInt());

        String httpRequest = "GET /" + HTTP_DATA_URL + " HTTP/1.1\nRange: bytes=2000-";
        Response response = processRequest(HTTP_DATA_URL, httpRequest, spyFileCache);

        byte[] fullData = loadTestData();
        byte[] partialData = new byte[fullData.length - 2000];
        System.arraycopy(fullData, 2000, partialData, 0, partialData.length);
        assertThat(response.data).isEqualTo(partialData);
        assertThat(response.code).isEqualTo(206);
    }

    @Test   // https://github.com/danikula/AndroidVideoCache/issues/43
    public void testPreventClosingOriginalSourceForNewPartialRequestWithoutCache() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_BIG_URL);
        FileCache fileCache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        HttpProxyCache proxyCache = new HttpProxyCache(source, fileCache);
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Future<Response> firstRequestFeature = processAsync(executor, proxyCache, "GET /" + HTTP_DATA_URL + " HTTP/1.1");
        Thread.sleep(100);  // wait for first request started to process

        int offset = 30000;
        String partialRequest = "GET /" + HTTP_DATA_URL + " HTTP/1.1\nRange: bytes=" + offset + "-";
        Future<Response> secondRequestFeature = processAsync(executor, proxyCache, partialRequest);

        Response secondResponse = secondRequestFeature.get();
        Response firstResponse = firstRequestFeature.get();

        byte[] responseData = loadAssetFile(ASSETS_DATA_BIG_NAME);
        assertThat(firstResponse.data).isEqualTo(responseData);

        byte[] partialData = new byte[responseData.length - offset];
        System.arraycopy(responseData, offset, partialData, 0, partialData.length);
        assertThat(secondResponse.data).isEqualTo(partialData);
    }

    @Test
    public void testProcessManyThreads() throws Exception {
        final String url = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/space.jpg";
        HttpUrlSource source = new HttpUrlSource(url);
        FileCache fileCache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        final HttpProxyCache proxyCache = new HttpProxyCache(source, fileCache);
        final byte[] loadedData = loadAssetFile("space.jpg");
        final Random random = new Random(System.currentTimeMillis());
        int concurrentRequests = 10;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentRequests);
        Future[] results = new Future[concurrentRequests];
        int[] offsets = new int[concurrentRequests];
        final CountDownLatch finishLatch = new CountDownLatch(concurrentRequests);
        final CountDownLatch startLatch = new CountDownLatch(1);
        for (int i = 0; i < concurrentRequests; i++) {
            final int offset = random.nextInt(loadedData.length);
            offsets[i] = offset;
            results[i] = executor.submit(new Callable<Response>() {

                @Override
                public Response call() throws Exception {
                    try {
                        startLatch.await();
                        String partialRequest = "GET /" + url + " HTTP/1.1\nRange: bytes=" + offset + "-";
                        return processRequest(proxyCache, partialRequest);
                    } finally {
                        finishLatch.countDown();
                    }
                }
            });
        }
        startLatch.countDown();
        finishLatch.await();

        for (int i = 0; i < results.length; i++) {
            Response response = (Response) results[i].get();
            int offset = offsets[i];
            byte[] partialData = new byte[loadedData.length - offset];
            System.arraycopy(loadedData, offset, partialData, 0, partialData.length);
            assertThat(response.data).isEqualTo(partialData);
        }
    }

    @Test
    public void testLoadEmptyFile() throws Exception {
        String zeroSizeUrl = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/empty.txt";
        HttpUrlSource source = new HttpUrlSource(zeroSizeUrl);
        HttpProxyCache proxyCache = new HttpProxyCache(source, new FileCache(ProxyCacheTestUtils.newCacheFile()));
        GetRequest request = new GetRequest("GET /" + HTTP_DATA_URL + " HTTP/1.1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(out);

        CacheListener listener = Mockito.mock(CacheListener.class);
        proxyCache.registerCacheListener(listener);
        proxyCache.processRequest(request, socket);
        proxyCache.registerCacheListener(null);
        Response response = new Response(out.toByteArray());

        Mockito.verify(listener).onCacheAvailable(Mockito.<File>any(), eq(zeroSizeUrl), eq(100));
        assertThat(response.data).isEmpty();
    }

    @Test
    public void testCacheListenerCalledAtTheEnd() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        File tempFile = ProxyCacheTestUtils.getTempFile(file);
        HttpProxyCache proxyCache = new HttpProxyCache(new HttpUrlSource(HTTP_DATA_URL), new FileCache(file));
        CacheListener listener = Mockito.mock(CacheListener.class);
        proxyCache.registerCacheListener(listener);
        processRequest(proxyCache, "GET /" + HTTP_DATA_URL + " HTTP/1.1");

        Mockito.verify(listener).onCacheAvailable(tempFile, HTTP_DATA_URL, 100);    // must be called for temp file ...
        Mockito.verify(listener).onCacheAvailable(file, HTTP_DATA_URL, 100);        // .. and for original file too
    }

    @Test(expected = ProxyCacheException.class)
    public void testTouchSourceForAbsentSourceInfoAndCache() throws Exception {
        SourceInfoStorage sourceInfoStorage = SourceInfoStorageFactory.newEmptySourceInfoStorage();
        HttpUrlSource source = ProxyCacheTestUtils.newNotOpenableHttpUrlSource(HTTP_DATA_URL, sourceInfoStorage);
        HttpProxyCache proxyCache = new HttpProxyCache(source, new FileCache(newCacheFile()));
        processRequest(proxyCache, "GET /" + HTTP_DATA_URL + " HTTP/1.1");
        proxyCache.shutdown();
        fail("Angry source should throw error! There is no file and caches source info");
    }

    @Test(expected = ProxyCacheException.class)
    public void testTouchSourceForExistedSourceInfoAndAbsentCache() throws Exception {
        SourceInfoStorage sourceInfoStorage = SourceInfoStorageFactory.newSourceInfoStorage(RuntimeEnvironment.application);
        sourceInfoStorage.put(HTTP_DATA_URL, new SourceInfo(HTTP_DATA_URL, HTTP_DATA_SIZE, "image/jpg"));
        HttpUrlSource source = ProxyCacheTestUtils.newNotOpenableHttpUrlSource(HTTP_DATA_URL, sourceInfoStorage);
        HttpProxyCache proxyCache = new HttpProxyCache(source, new FileCache(newCacheFile()));
        processRequest(proxyCache, "GET /" + HTTP_DATA_URL + " HTTP/1.1");
        proxyCache.shutdown();
        fail("Angry source should throw error! There is no cache file");
    }

    @Test
    public void testTouchSourceForExistedSourceInfoAndCache() throws Exception {
        SourceInfoStorage sourceInfoStorage = SourceInfoStorageFactory.newSourceInfoStorage(RuntimeEnvironment.application);
        sourceInfoStorage.put(HTTP_DATA_URL, new SourceInfo(HTTP_DATA_URL, HTTP_DATA_SIZE, "cached/mime"));
        HttpUrlSource source = ProxyCacheTestUtils.newNotOpenableHttpUrlSource(HTTP_DATA_URL, sourceInfoStorage);
        File file = newCacheFile();
        IoUtils.saveToFile(loadAssetFile(ASSETS_DATA_NAME), file);
        HttpProxyCache proxyCache = new HttpProxyCache(source, new FileCache(file));
        Response response = processRequest(proxyCache, "GET /" + HTTP_DATA_URL + " HTTP/1.1");
        proxyCache.shutdown();
        assertThat(response.data).isEqualTo(loadAssetFile(ASSETS_DATA_NAME));
        assertThat(response.contentLength).isEqualTo(HTTP_DATA_SIZE);
        assertThat(response.contentType).isEqualTo("cached/mime");
    }

    @Test
    public void testReuseSourceInfo() throws Exception {
        SourceInfoStorage sourceInfoStorage = SourceInfoStorageFactory.newSourceInfoStorage(RuntimeEnvironment.application);
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL, sourceInfoStorage);
        File cacheFile = newCacheFile();
        HttpProxyCache proxyCache = new HttpProxyCache(source, new FileCache(cacheFile));
        processRequest(proxyCache, "GET /" + HTTP_DATA_URL + " HTTP/1.1");

        HttpUrlSource notOpenableSource = ProxyCacheTestUtils.newNotOpenableHttpUrlSource(HTTP_DATA_URL, sourceInfoStorage);
        HttpProxyCache proxyCache2 = new HttpProxyCache(notOpenableSource, new FileCache(cacheFile));
        Response response = processRequest(proxyCache2, "GET /" + HTTP_DATA_URL + " HTTP/1.1");
        proxyCache.shutdown();

        assertThat(response.data).isEqualTo(loadAssetFile(ASSETS_DATA_NAME));
        assertThat(response.contentLength).isEqualTo(HTTP_DATA_SIZE);
        assertThat(response.contentType).isEqualTo("image/jpeg");
    }

    private Response processRequest(String sourceUrl, String httpRequest) throws ProxyCacheException, IOException {
        FileCache fileCache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        return processRequest(sourceUrl, httpRequest, fileCache);
    }

    private Response processRequest(String sourceUrl, String httpRequest, FileCache fileCache) throws ProxyCacheException, IOException {
        HttpUrlSource source = new HttpUrlSource(sourceUrl);
        HttpProxyCache proxyCache = new HttpProxyCache(source, fileCache);
        return processRequest(proxyCache, httpRequest);
    }

    private Response processRequest(HttpProxyCache proxyCache, String httpRequest) throws ProxyCacheException, IOException {
        GetRequest request = new GetRequest(httpRequest);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(out);
        proxyCache.processRequest(request, socket);
        return new Response(out.toByteArray());
    }

    private Future<Response> processAsync(ExecutorService executor, final HttpProxyCache proxyCache, final String httpRequest) {
        return executor.submit(new Callable<Response>() {

            @Override
            public Response call() throws Exception {
                return processRequest(proxyCache, httpRequest);
            }
        });
    }
}
