package com.danikula.videocache;

import com.danikula.videocache.file.FileCache;
import com.danikula.videocache.support.ProxyCacheTestUtils;
import com.danikula.videocache.support.Response;
import com.danikula.videocache.test.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.Socket;

import static com.danikula.videocache.support.ProxyCacheTestUtils.HTTP_DATA_URL;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadTestData;
import static org.fest.assertions.api.Assertions.assertThat;
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
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, emulateSdk = BuildConfig.MIN_SDK_VERSION)
public class HttpProxyCacheTest {

    @Test
    public void testProcessRequestNoCache() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL);
        FileCache cache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        HttpProxyCache proxyCache = new HttpProxyCache(source, cache);
        GetRequest request = new GetRequest("GET /" + HTTP_DATA_URL + " HTTP/1.1");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(out);

        proxyCache.processRequest(request, socket);
        Response response = new Response(out.toByteArray());

        assertThat(response.data).isEqualTo(loadTestData());
        assertThat(response.code).isEqualTo(200);
        assertThat(response.contentLength).isEqualTo(ProxyCacheTestUtils.HTTP_DATA_SIZE);
        assertThat(response.contentType).isEqualTo("image/jpeg");
    }

    @Test
    public void testProcessPartialRequestWithoutCache() throws Exception {
        HttpUrlSource source = new HttpUrlSource(HTTP_DATA_URL);
        FileCache fileCache = new FileCache(ProxyCacheTestUtils.newCacheFile());
        FileCache spyFileCache = Mockito.spy(fileCache);
        doThrow(new RuntimeException()).when(spyFileCache).read(any(byte[].class), anyLong(), anyInt());
        HttpProxyCache proxyCache = new HttpProxyCache(source, spyFileCache);
        GetRequest request = new GetRequest("GET /" + HTTP_DATA_URL + " HTTP/1.1\nRange: bytes=2000-");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Socket socket = mock(Socket.class);
        when(socket.getOutputStream()).thenReturn(out);

        proxyCache.processRequest(request, socket);
        Response response = new Response(out.toByteArray());

        byte[] fullData = loadTestData();
        byte[] partialData = new byte[fullData.length - 2000];
        System.arraycopy(fullData, 2000, partialData, 0, partialData.length);
        assertThat(response.data).isEqualTo(partialData);
        assertThat(response.code).isEqualTo(206);
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
}
