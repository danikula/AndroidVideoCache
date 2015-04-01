package com.danikula.videocache;

/**
 * @author Egor Makovsky (yahor.makouski@gmail.com).
 */
public interface CacheListener {
    void onError(ProxyCacheException e);

    void onCacheDataAvailable(int cachePercentage);
}
