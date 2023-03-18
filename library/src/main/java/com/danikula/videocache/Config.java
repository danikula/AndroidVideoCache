package com.danikula.videocache;

import com.danikula.videocache.file.DiskUsage;
import com.danikula.videocache.file.FileNameGenerator;
import com.danikula.videocache.headers.HeaderInjector;
import com.danikula.videocache.sourcestorage.SourceInfoStorage;

import java.io.File;
import java.net.Proxy;

/**
 * Configuration for proxy cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class Config {

    public final File cacheRoot;
    public final FileNameGenerator fileNameGenerator;
    public final DiskUsage diskUsage;
    public final SourceInfoStorage sourceInfoStorage;
    public final HeaderInjector headerInjector;
    public final Proxy proxy;

    Config(File cacheRoot, FileNameGenerator fileNameGenerator, DiskUsage diskUsage, SourceInfoStorage sourceInfoStorage, HeaderInjector headerInjector, Proxy proxy) {
        this.cacheRoot = cacheRoot;
        this.fileNameGenerator = fileNameGenerator;
        this.diskUsage = diskUsage;
        this.sourceInfoStorage = sourceInfoStorage;
        this.headerInjector = headerInjector;
        this.proxy = proxy;
    }

    File generateCacheFile(String url) {
        String name = fileNameGenerator.generate(url);
        return new File(cacheRoot, name);
    }

}
