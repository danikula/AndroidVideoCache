package com.danikula.videocache;

/**
 * Source for proxy.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public interface Source {

    int available() throws ProxyCacheException;

    void open(int offset) throws ProxyCacheException;

    void close() throws ProxyCacheException;

    int read(byte[] buffer) throws ProxyCacheException;
}
