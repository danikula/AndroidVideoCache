package com.danikula.videocache;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_PARTIAL;

/**
 * {@link Source} that uses http resource as source for {@link ProxyCache}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpUrlSource implements Source {

    public final String url;
    private HttpURLConnection connection;
    private InputStream inputStream;
    private volatile int available = Integer.MIN_VALUE;
    private volatile String mime;

    public HttpUrlSource(String url) {
        this(url, ProxyCacheUtils.getSupposablyMime(url));
    }

    public HttpUrlSource(String url, String mime) {
        this.url = Preconditions.checkNotNull(url);
        this.mime = mime;
    }

    @Override
    public int available() throws ProxyCacheException {
        if (available == Integer.MIN_VALUE) {
            fetchContentInfo();
        }
        return available;
    }

    @Override
    public void open(int offset) throws ProxyCacheException {
        try {
            Log.d(LOG_TAG, "Open connection " + (offset > 0 ? " with offset " + offset : "") + " to " + url);
            connection = (HttpURLConnection) new URL(url).openConnection();
            if (offset > 0) {
                connection.setRequestProperty("Range", "bytes=" + offset + "-");
            }
            mime = connection.getContentType();
            inputStream = connection.getInputStream();
            available = readSourceAvailableBytes(connection, offset);
        } catch (IOException e) {
            throw new ProxyCacheException("Error opening connection for " + url + " with offset " + offset, e);
        }
    }

    private int readSourceAvailableBytes(HttpURLConnection connection, int offset) throws IOException {
        int contentLength = connection.getContentLength();
        int responseCode = connection.getResponseCode();
        return responseCode == HTTP_OK ? contentLength :
                responseCode == HTTP_PARTIAL ? contentLength + offset :
                        available;
    }

    @Override
    public void close() throws ProxyCacheException {
        if (connection != null) {
            connection.disconnect();
        }
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        if (inputStream == null) {
            throw new ProxyCacheException("Error reading data from " + url + ": connection is absent!");
        }
        try {
            return inputStream.read(buffer, 0, buffer.length);
        } catch (InterruptedIOException e) {
            throw new InterruptedProxyCacheException("Reading source " + url + " is interrupted", e);
        } catch (IOException e) {
            throw new ProxyCacheException("Error reading data from " + url, e);
        }
    }

    private void fetchContentInfo() throws ProxyCacheException {
        Log.d(LOG_TAG, "Read content info from " + url);
        HttpURLConnection urlConnection = null;
        try {
            urlConnection = (HttpURLConnection) new URL(url).openConnection();
            urlConnection.setRequestMethod("HEAD");
            available = urlConnection.getContentLength();
            mime = urlConnection.getContentType();
            Log.i(LOG_TAG, "Info read: " + this);
        } catch (IOException e) {
            throw new ProxyCacheException("Error fetching Content-Length from " + url);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }

    public String getMime() throws ProxyCacheException {
        if (TextUtils.isEmpty(mime)) {
            fetchContentInfo();
        }
        return mime;
    }

    @Override
    public String toString() {
        return "HttpUrlSource{" +
                "url='" + url + '\'' +
                ", available=" + available +
                ", mime='" + mime + '\'' +
                '}';
    }
}
