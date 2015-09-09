package com.danikula.videocache;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

import static com.danikula.videocache.Preconditions.checkNotNull;
import static com.danikula.videocache.ProxyCacheUtils.LOG_TAG;

/**
 * Proxy for {@link Source} with caching support ({@link Cache}).
 * <p/>
 * Can be used only for sources with persistent data (that doesn't change with time).
 * Method {@link #read(byte[], long, int)} will be blocked while fetching data from source.
 * Useful for streaming something with caching e.g. streaming video/audio etc.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class ProxyCache {

    private static final int MAX_READ_SOURCE_ATTEMPTS = 1;

    private final Source source;
    private final Cache cache;
    private final Object wc;
    private final ListenerHandler handler;
    private volatile Thread sourceReaderThread;
    private volatile boolean stopped;
    private final AtomicInteger readSourceErrorsCount;
    private CacheListener cacheListener;
    private final boolean logEnabled;

    public ProxyCache(Source source, Cache cache, boolean logEnabled) {
        this.source = checkNotNull(source);
        this.cache = checkNotNull(cache);
        this.logEnabled = logEnabled;
        this.wc = new Object();
        this.handler = new ListenerHandler();
        this.readSourceErrorsCount = new AtomicInteger();
    }

    public ProxyCache(Source source, Cache cache) {
        this(source, cache, false);
    }

    public void setCacheListener(CacheListener cacheListener) {
        this.cacheListener = cacheListener;
    }

    public int read(byte[] buffer, long offset, int length) throws ProxyCacheException {
        ProxyCacheUtils.assertBuffer(buffer, offset, length);

        while (!cache.isCompleted() && cache.available() < (offset + length) && !stopped) {
            readSourceAsync();
            waitForSourceData();
            checkIsCacheValid();
            checkReadSourceErrorsCount();
        }
        int read = cache.read(buffer, offset, length);
        if (isLogEnabled()) {
            Log.d(LOG_TAG, "Read data[" + read + " bytes] from cache with offset " + offset + ": " + ProxyCacheUtils.preview(buffer, read));
        }
        return read;
    }

    private void checkIsCacheValid() throws ProxyCacheException {
        int sourceAvailable = source.available();
        if (sourceAvailable > 0 && cache.available() > sourceAvailable) {
            throw new ProxyCacheException("Unexpected cache: cache [" + cache.available() + " bytes] > source[" + sourceAvailable + " bytes]");
        }
    }

    private void checkReadSourceErrorsCount() throws ProxyCacheException {
        int errorsCount = readSourceErrorsCount.get();
        if (errorsCount >= MAX_READ_SOURCE_ATTEMPTS) {
            readSourceErrorsCount.set(0);
            throw new ProxyCacheException("Error reading source " + errorsCount + " times");
        }
    }

    public void shutdown() {
        try {
            stopped = true;
            if (sourceReaderThread != null) {
                sourceReaderThread.interrupt();
            }
            cache.close();
        } catch (ProxyCacheException e) {
            onError(e);
        }
    }

    private void readSourceAsync() throws ProxyCacheException {
        boolean readingInProgress = sourceReaderThread != null && sourceReaderThread.getState() != Thread.State.TERMINATED;
        if (!stopped && !cache.isCompleted() && !readingInProgress) {
            sourceReaderThread = new Thread(new SourceReaderRunnable(), "Source reader for ProxyCache");
            sourceReaderThread.start();
        }
    }

    private void waitForSourceData() throws ProxyCacheException {
        synchronized (wc) {
            try {
                wc.wait(1000);
            } catch (InterruptedException e) {
                throw new ProxyCacheException("Waiting source data is interrupted!", e);
            }
        }
    }

    private void notifyNewCacheDataAvailable(int cachePercentage) {
        handler.deliverCachePercentage(cachePercentage);

        synchronized (wc) {
            wc.notifyAll();
        }
    }

    private void readSource() {
        int cachePercentage = 0;
        try {
            int offset = cache.available();
            source.open(offset);
            byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = source.read(buffer)) != -1 && !Thread.currentThread().isInterrupted() && !stopped) {
                if (isLogEnabled()) {
                    Log.d(LOG_TAG, "Write data[" + readBytes + " bytes] to cache from source with offset " + offset + ": " + ProxyCacheUtils.preview(buffer, readBytes));
                }
                cache.append(buffer, readBytes);
                offset += readBytes;
                cachePercentage = offset * 100 / source.available();

                notifyNewCacheDataAvailable(cachePercentage);
            }
            if (cache.available() == source.available()) {
                cache.complete();
            }
        } catch (Throwable e) {
            readSourceErrorsCount.incrementAndGet();
            onError(e);
        } finally {
            closeSource();
            notifyNewCacheDataAvailable(cachePercentage);
        }
    }

    private void closeSource() {
        try {
            source.close();
        } catch (ProxyCacheException e) {
            onError(new ProxyCacheException("Error closing source " + source, e));
        }
    }

    protected final void onError(final Throwable e) {
        Log.e(LOG_TAG, "ProxyCache error", e);
        handler.deliverError(e);
    }

    protected boolean isLogEnabled() {
        return logEnabled;
    }

    private class SourceReaderRunnable implements Runnable {

        @Override
        public void run() {
            readSource();
        }
    }

    private final class ListenerHandler extends Handler {

        private static final int MSG_ERROR = 1;
        private static final int MSG_CACHE_PERCENTAGE = 2;

        public ListenerHandler() {
            super(Looper.getMainLooper());
        }

        public void deliverCachePercentage(int percents) {
            if (cacheListener != null) {
                send(MSG_CACHE_PERCENTAGE, percents, null);
            }
        }

        public void deliverError(Throwable error) {
            if (isFatalError(error) || cacheListener != null) {
                send(MSG_ERROR, 0, error);
            }
        }

        private boolean isFatalError(Throwable error) {
            return !(error instanceof ProxyCacheException);
        }

        private void send(int what, int arg1, Object data) {
            Message message = obtainMessage(what);
            message.arg1 = arg1;
            message.obj = data;
            sendMessage(message);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_CACHE_PERCENTAGE:
                    cacheListener.onCacheDataAvailable(msg.arg1);
                    break;
                case MSG_ERROR:
                    Throwable error = (Throwable) msg.obj;
                    if (isFatalError(error)) {
                        throw new RuntimeException("Unexpected error!", error);
                    }
                    cacheListener.onError((ProxyCacheException) error);
                    break;
                default:
                    throw new RuntimeException("Unknown message " + msg);
            }
        }
    }
}
