package com.danikula.videocache;

import android.text.TextUtils;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * {@link ProxyCache} that read http url and writes data to {@link Socket}
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class HttpProxyCache extends ProxyCache {

    private final HttpUrlSource source;
    private final FileCache cache;
    private CacheListener listener;

    public HttpProxyCache(HttpUrlSource source, FileCache cache) {
        super(source, cache);
        this.cache = cache;
        this.source = source;
    }

    public void registerCacheListener(CacheListener cacheListener) {
        this.listener = cacheListener;
    }

    public void processRequest(GetRequest request, Socket socket) throws IOException, ProxyCacheException {
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
        int readBytes;
        boolean headersWrote = false;
        long offset = request.rangeOffset;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            // tiny optimization: to prevent HEAD request in source for content-length. content-length 'll available after reading source
            if (!headersWrote) {
                String responseHeaders = newResponseHeaders(request);
                out.write(responseHeaders.getBytes("UTF-8"));
                headersWrote = true;
            }
            out.write(buffer, 0, readBytes);
            offset += readBytes;
            if (cache.isCompleted()) {
                onCacheAvailable(100);
            }
        }
        out.flush();
    }

    private String newResponseHeaders(GetRequest request) throws IOException, ProxyCacheException {
        String mime = source.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        int length = cache.isCompleted() ? cache.available() : source.available();
        boolean lengthKnown = length >= 0;
        long contentLength = request.partial ? length - request.rangeOffset : length;
        boolean addRange = lengthKnown && request.partial;
        return new StringBuilder()
                .append(request.partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? String.format("Content-Length: %d\n", contentLength) : "")
                .append(addRange ? String.format("Content-Range: bytes %d-%d/%d\n", request.rangeOffset, length, length) : "")
                .append(mimeKnown ? String.format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    @Override
    protected void onCacheAvailable(int percents) {
        if (listener != null) {
            listener.onCacheAvailable(cache.file, source.url, percents);
        }
    }
}
