package com.danikula.videocache.support;

import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.ByteArraySource;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.HttpUrlSource;
import com.danikula.videocache.ProxyCacheException;
import com.danikula.videocache.Source;
import com.danikula.videocache.sourcestorage.SourceInfoStorage;
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import org.apache.tools.ant.util.ReflectUtil;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URL;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class ProxyCacheTestUtils {

    public static final String HTTP_DATA_URL = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/android.jpg";
    public static final String HTTP_DATA_URL_ONE_REDIRECT = "http://bit.ly/1LAJKAy";
    public static final String HTTP_DATA_URL_3_REDIRECTS = "http://bit.ly/1QtKJiB";
    public static final String HTTP_DATA_URL_6_REDIRECTS = "http://ow.ly/Z17wz";
    public static final String HTTP_DATA_BIG_URL = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/phones.jpg";
    public static final String HTTP_DATA_BIG_URL_ONE_REDIRECT = "http://bit.ly/24DdZ06";
    public static final String ASSETS_DATA_NAME = "android.jpg";
    public static final String ASSETS_DATA_BIG_NAME = "phones.jpg";
    public static final int HTTP_DATA_SIZE = 4768;
    public static final int HTTP_DATA_BIG_SIZE = 94363;

    public static byte[] getFileContent(File file) throws IOException {
        return Files.asByteSource(file).read();
    }

    public static Response readProxyResponse(HttpProxyCacheServer proxy, String url) throws IOException {
        return readProxyResponse(proxy, url, -1);
    }

    public static Response readProxyResponse(HttpProxyCacheServer proxy, String url, int offset) throws IOException {
        String proxyUrl = proxy.getProxyUrl(url, false);
        if (!proxyUrl.startsWith("http://127.0.0.1")) {
            throw new IllegalStateException("Proxy url " + proxyUrl + " is not proxied! Original url is " + url);
        }
        URL proxiedUrl = new URL(proxyUrl);
        HttpURLConnection connection = (HttpURLConnection) proxiedUrl.openConnection();
        try {
            if (offset >= 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            return new Response(connection);
        } finally {
            connection.disconnect();
        }
    }

    public static byte[] loadTestData() throws IOException {
        return loadAssetFile(ASSETS_DATA_NAME);
    }

    public static byte[] loadAssetFile(String name) throws IOException {
        InputStream in = RuntimeEnvironment.application.getResources().getAssets().open(name);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IoUtils.copy(in, out);
        IoUtils.closeSilently(in);
        IoUtils.closeSilently(out);
        return out.toByteArray();
    }

    public static File getTempFile(File file) {
        return new File(file.getParentFile(), file.getName() + ".download");
    }

    public static File newCacheFile() {
        return new File(RuntimeEnvironment.application.getCacheDir(), UUID.randomUUID().toString());
    }

    public static byte[] generate(int capacity) {
        Random random = new Random(System.currentTimeMillis());
        byte[] result = new byte[capacity];
        random.nextBytes(result);
        return result;
    }

    public static HttpUrlSource newAngryHttpUrlSource() throws ProxyCacheException {
        HttpUrlSource source = mock(HttpUrlSource.class);
        doThrow(new RuntimeException()).when(source).getMime();
        doThrow(new RuntimeException()).when(source).read(any(byte[].class));
        doThrow(new RuntimeException()).when(source).open(anyInt());
        doThrow(new RuntimeException()).when(source).length();
        doThrow(new RuntimeException()).when(source).getUrl();
        doThrow(new RuntimeException()).when(source).close();
        return source;
    }

    public static HttpUrlSource newNotOpenableHttpUrlSource(String url, SourceInfoStorage sourceInfoStorage) throws ProxyCacheException {
        HttpUrlSource httpUrlSource = new HttpUrlSource(url, sourceInfoStorage);
        HttpUrlSource source = spy(httpUrlSource);
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                System.out.print("Can't open!!!");
                throw new RuntimeException();
            }
        }).when(source).open(anyInt());
        return source;
    }

    public static Source newPhlegmaticSource(byte[] data, final int maxDelayMs) throws ProxyCacheException {
        Source spySource = spy(new ByteArraySource(data));
        final Random delayGenerator = new Random(System.currentTimeMillis());
        doAnswer(new Answer() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(delayGenerator.nextInt(maxDelayMs));
                return null;
            }
        }).doCallRealMethod().when(spySource).read(any(byte[].class));
        return spySource;
    }

    public static int getPort(HttpProxyCacheServer server) {
        String proxyUrl = server.getProxyUrl("test");
        Pattern pattern = Pattern.compile("http://127.0.0.1:(\\d*)/test");
        Matcher matcher = pattern.matcher(proxyUrl);
        assertThat(matcher.find()).isTrue();
        String portAsString = matcher.group(1);
        return Integer.parseInt(portAsString);
    }

    public static int getPortWithoutPing(HttpProxyCacheServer server) {
        return (Integer) ReflectUtil.getField(server, "port");
    }

    public static void installExternalSystemProxy() {
        // see proxies list at http://proxylist.hidemyass.com/
        Proxy systemProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("162.8.230.7", 11180));
        ProxySelector mockedProxySelector = Mockito.mock(ProxySelector.class);
        when(mockedProxySelector.select(Mockito.<URI>any())).thenReturn(Lists.newArrayList(systemProxy));
        ProxySelector.setDefault(mockedProxySelector);
    }

    public static void resetSystemProxy() {
        ProxySelector mockedProxySelector = Mockito.mock(ProxySelector.class);
        when(mockedProxySelector.select(Mockito.<URI>any())).thenReturn(Lists.newArrayList(Proxy.NO_PROXY));
        ProxySelector.setDefault(mockedProxySelector);
    }
}
