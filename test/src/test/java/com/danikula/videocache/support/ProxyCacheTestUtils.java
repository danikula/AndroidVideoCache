package com.danikula.videocache.support;

import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.HttpProxyCacheServer;
import com.google.common.io.Files;

import org.robolectric.RuntimeEnvironment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;
import java.util.UUID;

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
        String proxyUrl = proxy.getProxyUrl(url);
        if (!proxyUrl.startsWith("http://127.0.0.1")) {
            throw new IllegalStateException("Url " + url + " is not proxied!");
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
}
