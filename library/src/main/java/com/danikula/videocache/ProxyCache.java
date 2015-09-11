package com.danikula.videocache;

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
class ProxyCache {

    private static final int MAX_READ_SOURCE_ATTEMPTS = 1;

    private final Source source;
    private final Cache cache;
    private final Object wc = new Object();
    private final Object stopLock = new Object();
    private volatile Thread sourceReaderThread;
    private volatile boolean stopped;
    private final AtomicInteger readSourceErrorsCount;

    public ProxyCache(Source source, Cache cache) {
        this.source = checkNotNull(source);
        this.cache = checkNotNull(cache);
        this.readSourceErrorsCount = new AtomicInteger();
    }

    public int read(byte[] buffer, long offset, int length) throws ProxyCacheException {
        ProxyCacheUtils.assertBuffer(buffer, offset, length);

        while (!cache.isCompleted() && cache.available() < (offset + length) && !stopped) {
            readSourceAsync();
            waitForSourceData();
            checkReadSourceErrorsCount();
        }
        return cache.read(buffer, offset, length);
    }

    private void checkReadSourceErrorsCount() throws ProxyCacheException {
        int errorsCount = readSourceErrorsCount.get();
        if (errorsCount >= MAX_READ_SOURCE_ATTEMPTS) {
            readSourceErrorsCount.set(0);
            throw new ProxyCacheException("Error reading source " + errorsCount + " times");
        }
    }

    public void shutdown() {
        synchronized (stopLock) {
            Log.d(LOG_TAG, "Shutdown proxy for " + source);
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
    }

    private synchronized void readSourceAsync() throws ProxyCacheException {
        boolean readingInProgress = sourceReaderThread != null && sourceReaderThread.getState() != Thread.State.TERMINATED;
        if (!stopped && !cache.isCompleted() && !readingInProgress) {
            sourceReaderThread = new Thread(new SourceReaderRunnable(), "Source reader for " + source);
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
        onCacheAvailable(cachePercentage);

        synchronized (wc) {
            wc.notifyAll();
        }
    }

    protected void onCacheAvailable(int percents) {
    }

    private void readSource() {
        int cachePercentage = 0;
        try {
            int offset = cache.available();
            source.open(offset);
            byte[] buffer = new byte[ProxyCacheUtils.DEFAULT_BUFFER_SIZE];
            int readBytes;
            while ((readBytes = source.read(buffer)) != -1) {
                synchronized (stopLock) {
                    if (isStopped()) {
                        return;
                    }
                    cache.append(buffer, readBytes);
                }
                offset += readBytes;
                cachePercentage = offset * 100 / source.available();

                notifyNewCacheDataAvailable(cachePercentage);
            }
            tryComplete();
        } catch (Throwable e) {
            readSourceErrorsCount.incrementAndGet();
            onError(e);
        } finally {
            closeSource();
            notifyNewCacheDataAvailable(cachePercentage);
        }
    }

    private void tryComplete() throws ProxyCacheException {
        synchronized (stopLock) {
            if (!isStopped() && cache.available() == source.available()) {
                cache.complete();
            }
        }
    }

    private boolean isStopped() {
        return Thread.currentThread().isInterrupted() || stopped;
    }

    private void closeSource() {
        try {
            source.close();
        } catch (ProxyCacheException e) {
            onError(new ProxyCacheException("Error closing source " + source, e));
        }
    }

    protected final void onError(final Throwable e) {
        boolean interruption = e instanceof InterruptedProxyCacheException;
        if (interruption) {
            Log.d(LOG_TAG, "ProxyCache is interrupted");
        } else {
            Log.e(LOG_TAG, "ProxyCache error", e);
        }
    }

    private class SourceReaderRunnable implements Runnable {

        @Override
        public void run() {
            readSource();
        }
    }
}
