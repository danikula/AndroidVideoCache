package com.danikula.videocache;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@link ProxyCache} that uses local server to handle requests and cache data.
 * Typical usage:
 * <pre><code>
 * HttpProxyCache proxy;
 * public onCreate(Bundle state) {
 *      super.onCreate(state);
 *      ...
 *      try{
 *          HttpUrlSource source = new HttpUrlSource(YOUR_VIDEO_URI);
 *          Cache cache = new FileCache(new File(context.getCacheDir(), "video.mp4"));
 *          proxy = new HttpProxyCache(source, cache);
 *          videoView.setVideoPath(proxy.getUrl());
 *      } catch(ProxyCacheException e) {
 *          Log.e(LOG_TAG, "Error playing video", e);
 *      }
 * }
 * public onDestroy(){
 *     super.onDestroy();
 *
 *     if (proxy != null) {
 *         proxy.shutdown();
 *     }
 * }
 * <code/></pre>
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpProxyCache extends ProxyCache {

    private static final int CLIENT_COUNT = 3;
    private static final Pattern RANGE_HEADER_PATTERN = Pattern.compile("[R,r]ange:[ ]?bytes=(\\d*)-");
    private static final String PROXY_HOST = "127.0.0.1";

    private final HttpUrlSource httpUrlSource;
    private final Cache cache;
    private final ServerSocket serverSocket;
    private final int port;
    private final Thread waitConnectionThread;
    private final ExecutorService executorService;

    public HttpProxyCache(HttpUrlSource source, Cache cache, boolean logEnabled) throws ProxyCacheException {
        super(source, cache, logEnabled);

        this.httpUrlSource = source;
        this.cache = cache;
        this.executorService = Executors.newFixedThreadPool(CLIENT_COUNT);
        try {
            InetAddress inetAddress = InetAddress.getByName(PROXY_HOST);
            this.serverSocket = new ServerSocket(0, CLIENT_COUNT, inetAddress);
            this.port = serverSocket.getLocalPort();
            CountDownLatch startSignal = new CountDownLatch(1);
            this.waitConnectionThread = new Thread(new WaitRequestsRunnable(startSignal));
            this.waitConnectionThread.start();
            startSignal.await(); // freeze thread, wait for server starts
        } catch (IOException | InterruptedException e) {
            executorService.shutdown();
            throw new ProxyCacheException("Error starting local server", e);
        }
    }

    public HttpProxyCache(HttpUrlSource source, Cache cache) throws ProxyCacheException {
        this(source, cache, false);
    }

    public String getUrl() {
        return "http://" + PROXY_HOST + ":" + port + Uri.parse(httpUrlSource.url).getPath();
    }

    @Override
    public void shutdown() {
        super.shutdown();

        Log.i(ProxyCacheUtils.LOG_TAG, "Shutdown proxy");
        waitConnectionThread.interrupt();
        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error shutting down local server", e));
        }
    }

    private void waitForRequest() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                Log.d(ProxyCacheUtils.LOG_TAG, "Accept new socket " + socket);
                processSocketInBackground(socket);
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error during waiting connection", e));
        }
    }

    private void processSocketInBackground(final Socket socket) throws IOException {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    processSocket(socket);
                } catch (Throwable e) {
                    onError(e);
                }
            }
        });
    }

    private void processSocket(Socket socket) {
        try {
            InputStream inputStream = socket.getInputStream();
            String request = readRequest(inputStream);
            Log.i(ProxyCacheUtils.LOG_TAG, "Request to cache proxy:\n" + request);
            long rangeOffset = getRangeOffset(request);
            writeResponse(socket, rangeOffset);
        } catch (ProxyCacheException | IOException e) {
            onError(new ProxyCacheException("Error processing request", e));
        } finally {
            releaseSocket(socket);
        }
    }

    private void writeResponse(Socket socket, long rangeOffset) throws ProxyCacheException, IOException {
        OutputStream out = new BufferedOutputStream(socket.getOutputStream());
        byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
        int readBytes;
        long offset = Math.max(rangeOffset, 0);
        boolean headersWrote = false;
        while ((readBytes = read(buffer, offset, buffer.length)) != -1) {
            // tiny optimization: to prevent HEAD request in source for content-length. content-length 'll available after reading source
            if (!headersWrote) {
                writeResponseHeaders(out, rangeOffset);
                headersWrote = true;
            }
            out.write(buffer, 0, readBytes);
            if (isLogEnabled()) {
                Log.d(ProxyCacheUtils.LOG_TAG, "Write data[" + readBytes + " bytes] to socket " + socket + " with offset " + offset + ": " + ProxyCacheUtils.preview(buffer, readBytes));
            }
            offset += readBytes;
        }
        out.flush();
    }

    private void writeResponseHeaders(OutputStream out, long rangeOffset) throws IOException, ProxyCacheException {
        String responseHeaders = newResponseHeaders(rangeOffset);
        out.write(responseHeaders.getBytes("UTF-8"));
        Log.i(ProxyCacheUtils.LOG_TAG, "Response headers:\n" + responseHeaders);
    }

    private String newResponseHeaders(long offset) throws IOException, ProxyCacheException {
        boolean partial = offset >= 0;
        String mime = httpUrlSource.getMime();
        boolean mimeKnown = !TextUtils.isEmpty(mime);
        int length = cache.isCompleted() ? cache.available() : httpUrlSource.available();
        boolean lengthKnown = length >= 0;
        long contentLength = partial ? length - offset : length;
        return new StringBuilder()
                .append(partial ? "HTTP/1.1 206 PARTIAL CONTENT\n" : "HTTP/1.1 200 OK\n")
                .append("Accept-Ranges: bytes\n")
                .append(lengthKnown ? String.format("Content-Length: %d\n", contentLength) : "")
                .append(lengthKnown && partial ? String.format("Content-Range: bytes %d-%d/%d\n", offset, length, length) : "")
                .append(mimeKnown ? String.format("Content-Type: %s\n", mime) : "")
                .append("\n") // headers end
                .toString();
    }

    private String readRequest(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        StringBuilder str = new StringBuilder();
        String line;
        while (!TextUtils.isEmpty(line = reader.readLine())) { // until new line (headers ending)
            str.append(line).append('\n');
        }
        return str.toString();
    }

    private long getRangeOffset(String request) {
        Matcher matcher = RANGE_HEADER_PATTERN.matcher(request);
        if (matcher.find()) {
            String rangeValue = matcher.group(1);
            return Long.parseLong(rangeValue);
        }
        return -1;
    }

    private void releaseSocket(Socket socket) {
        try {
            socket.shutdownInput();
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket input stream", e));
        }
        try {
            socket.shutdownOutput();
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket output stream", e));
        }
        try {
            socket.close();
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket", e));
        }
    }

    private final class WaitRequestsRunnable implements Runnable {

        private final CountDownLatch startSignal;

        public WaitRequestsRunnable(CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        @Override
        public void run() {
            startSignal.countDown();
            waitForRequest();
        }
    }
}
