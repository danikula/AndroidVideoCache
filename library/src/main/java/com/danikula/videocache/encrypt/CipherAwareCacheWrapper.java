package com.danikula.videocache.encrypt;

import com.danikula.videocache.Cache;
import com.danikula.videocache.ProxyCacheException;

import static com.danikula.videocache.Preconditions.checkNotNull;

/**
 * {@link Cache} that take into account encryption/decryption.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class CipherAwareCacheWrapper implements Cache {

    private final Cache cache;
    private final Cipher cipher;

    public CipherAwareCacheWrapper(Cache cache, Cipher cipher) {
        this.cache = checkNotNull(cache);
        this.cipher = checkNotNull(cipher);
    }

    @Override
    public int read(byte[] buffer, long offset, int length) throws ProxyCacheException {
        cache.read(buffer, offset, length);
        cipher.decrypt(buffer, offset, length);
        return 0;
    }

    @Override
    public void append(byte[] data, int length) throws ProxyCacheException {
        cipher.encrypt(data, length);
        cache.append(data, length);
    }

    @Override
    public long available() throws ProxyCacheException {
        return cache.available();
    }

    @Override
    public void close() throws ProxyCacheException {
        cache.close();
    }

    @Override
    public void complete() throws ProxyCacheException {
        cache.complete();
    }

    @Override
    public boolean isCompleted() {
        return cache.isCompleted();
    }
}
