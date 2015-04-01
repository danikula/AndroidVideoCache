package com.danikula.videocache.support;

import android.text.TextUtils;

import com.danikula.videocache.HttpUrlSource;
import com.danikula.videocache.ProxyCacheException;

/**
 * {@link HttpUrlSource} that throws exception in all methods.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class AngryHttpUrlSource extends HttpUrlSource {

    public AngryHttpUrlSource(String url, String mime) {
        super(url, mime);
    }

    @Override
    public int available() throws ProxyCacheException {
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

    public String getMime() throws ProxyCacheException {
        String mime = super.getMime();
        if (!TextUtils.isEmpty(mime)) {
            return mime;
        }
        throw new IllegalStateException();
    }
}
