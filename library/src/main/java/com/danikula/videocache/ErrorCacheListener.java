package com.danikula.videocache;

import java.io.File;

/**
 * Listener for cache error.
 *
 * @author Sasha Karamyshev (sheckspir88@gmail.com)
 */
public interface ErrorCacheListener {

    void onCacheUnavailable(Throwable throwable);
}
