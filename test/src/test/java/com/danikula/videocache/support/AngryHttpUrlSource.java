package com.danikula.videocache.support;

import com.danikula.videocache.ProxyCacheException;
import com.danikula.videocache.Source;

/**
 * {@link Source} that throws exception in all methods.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
@Deprecated // use Mockito to throw error
public class AngryHttpUrlSource implements Source {

    @Override
    public int length() throws ProxyCacheException {
        throw new IllegalStateException();
    }

    @Override
    public void open(int offset) throws ProxyCacheException {
        throw new IllegalStateException();
    }

    @Override
    public void close() throws ProxyCacheException {
        throw new IllegalStateException();
    }

    @Override
    public int read(byte[] buffer) throws ProxyCacheException {
        throw new IllegalStateException();
    }
}
