package com.danikula.videocache;

import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.danikula.videocache.Preconditions.checkAllNotNull;
import static com.danikula.videocache.Preconditions.checkNotNull;
import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;

/**
 * Simple lightweight proxy server with file caching support that handles HTTP requests.
 * Typical usage:
 * <pre><code>
 * public onCreate(Bundle state) {
 *      super.onCreate(state);
 * <p/>
 *      HttpProxyCacheServer proxy = getProxy();
 *      String proxyUrl = proxy.getProxyUrl(VIDEO_URL);
 *      videoView.setVideoPath(proxyUrl);
 * }
 * <p/>
 * private HttpProxyCacheServer getProxy() {
 * // should return single instance of HttpProxyCacheServer shared for whole app.
 * }
 * <code/></pre>
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class HttpProxyCacheServer {

    private static final String PROXY_HOST = "127.0.0.1";

    private final Object clientsLock = new Object();
    private final ExecutorService socketProcessor = Executors.newFixedThreadPool(8);
    private final Map<String, HttpProxyCacheServerClients> clientsMap = new ConcurrentHashMap<>();
    private final ServerSocket serverSocket;
    private final int port;
    private final Thread waitConnectionThread;
    private final FileNameGenerator fileNameGenerator;

    public HttpProxyCacheServer(FileNameGenerator fileNameGenerator) {
        this.fileNameGenerator = checkNotNull(fileNameGenerator);
        try {
            InetAddress inetAddress = InetAddress.getByName(PROXY_HOST);
            this.serverSocket = new ServerSocket(0, 8, inetAddress);
            this.port = serverSocket.getLocalPort();
            CountDownLatch startSignal = new CountDownLatch(1);
            this.waitConnectionThread = new Thread(new WaitRequestsRunnable(startSignal));
            this.waitConnectionThread.start();
            startSignal.await(); // freeze thread, wait for server starts
        } catch (IOException | InterruptedException e) {
            socketProcessor.shutdown();
            throw new IllegalStateException("Error starting local proxy server", e);
        }
    }

    public String getProxyUrl(String url) {
        return String.format("http://%s:%d/%s", PROXY_HOST, port, ProxyCacheUtils.encode(url));
    }

    public void registerCacheListener(CacheListener cacheListener, String url) {
        checkAllNotNull(cacheListener, url);
        synchronized (clientsLock) {
            try {
                getClients(url).registerCacheListener(cacheListener);
            } catch (ProxyCacheException e) {
                Log.d(LOG_TAG, "Error registering cache listener", e);
            }
        }
    }

    public void unregisterCacheListener(CacheListener cacheListener, String url) {
        checkAllNotNull(cacheListener, url);
        synchronized (clientsLock) {
            try {
                getClients(url).unregisterCacheListener(cacheListener);
            } catch (ProxyCacheException e) {
                Log.d(LOG_TAG, "Error registering cache listener", e);
            }
        }
    }

    public void unregisterCacheListener(CacheListener cacheListener) {
        checkNotNull(cacheListener);
        synchronized (clientsLock) {
            for (HttpProxyCacheServerClients clients : clientsMap.values()) {
                clients.unregisterCacheListener(cacheListener);
            }
        }
    }

    public void shutdown() {
        Log.i(LOG_TAG, "Shutdown proxy server");

        shutdownClients();

        waitConnectionThread.interrupt();
        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error shutting down proxy server", e));
        }
    }

    private void shutdownClients() {
        synchronized (clientsLock) {
            for (HttpProxyCacheServerClients clients : clientsMap.values()) {
                clients.shutdown();
            }
            clientsMap.clear();
        }
    }

    private void waitForRequest() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                Log.d(LOG_TAG, "Accept new socket " + socket);
                socketProcessor.submit(new SocketProcessorRunnable(socket));
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error during waiting connection", e));
        }
    }

    private void processSocket(Socket socket) {
        try {
            GetRequest request = GetRequest.read(socket.getInputStream());
            Log.i(LOG_TAG, "Request to cache proxy:" + request);
            String url = ProxyCacheUtils.decode(request.uri);
            HttpProxyCacheServerClients clients = getClients(url);
            clients.processRequest(request, socket);
        } catch (SocketException e) {
            // There is no way to determine that client closed connection http://stackoverflow.com/a/10241044/999458
            // So just to prevent log flooding don't log stacktrace
            Log.d(LOG_TAG, "Client communication problem. It seems client closed connection");
        } catch (ProxyCacheException | IOException e) {
            onError(new ProxyCacheException("Error processing request", e));
        } finally {
            releaseSocket(socket);
            Log.d(LOG_TAG, "Opened connections: " + getClientsCount());
        }
    }

    private HttpProxyCacheServerClients getClients(String url) throws ProxyCacheException {
        synchronized (clientsLock) {
            HttpProxyCacheServerClients clients = clientsMap.get(url);
            if (clients == null) {
                clients = new HttpProxyCacheServerClients(url, fileNameGenerator);
                clientsMap.put(url, clients);
            }
            return clients;
        }
    }

    private int getClientsCount() {
        synchronized (clientsLock) {
            int count = 0;
            for (HttpProxyCacheServerClients clients : clientsMap.values()) {
                count += clients.getClientsCount();
            }
            return count;
        }
    }

    private void releaseSocket(Socket socket) {
        closeSocketInput(socket);
        closeSocketOutput(socket);
        closeSocket(socket);
    }

    private void closeSocketInput(Socket socket) {
        try {
            if (!socket.isInputShutdown()) {
                socket.shutdownInput();
            }
        } catch (SocketException e) {
            // There is no way to determine that client closed connection http://stackoverflow.com/a/10241044/999458
            // So just to prevent log flooding don't log stacktrace
            Log.d(LOG_TAG, "Error closing client's input stream: it seems client closed connection");
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket input stream", e));
        }
    }

    private void closeSocketOutput(Socket socket) {
        try {
            if (socket.isOutputShutdown()) {
                socket.shutdownOutput();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket output stream", e));
        }
    }

    private void closeSocket(Socket socket) {
        try {
            if (!socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            onError(new ProxyCacheException("Error closing socket", e));
        }
    }

    private void onError(Throwable e) {
        Log.e(LOG_TAG, "HttpProxyCacheServer error", e);
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

    private final class SocketProcessorRunnable implements Runnable {

        private final Socket socket;

        public SocketProcessorRunnable(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            processSocket(socket);
        }
    }

}
