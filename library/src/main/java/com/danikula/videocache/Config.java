package com.danikula.videocache;

import android.database.sqlite.SQLiteDatabase;

import com.danikula.videocache.file.DiskUsage;
import com.danikula.videocache.file.FileNameGenerator;

import java.io.File;

/**
 * Configuration for proxy cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
class Config {

    public final File cacheRoot;
    public final FileNameGenerator fileNameGenerator;
    public final DiskUsage diskUsage;
    public final SQLiteDatabase contentInfoDb;

    Config(File cacheRoot, FileNameGenerator fileNameGenerator, DiskUsage diskUsage, SQLiteDatabase contentInfoDb) {
        this.cacheRoot = cacheRoot;
        this.fileNameGenerator = fileNameGenerator;
        this.diskUsage = diskUsage;
        this.contentInfoDb = contentInfoDb;
    }

    File generateCacheFile(String url) {
        String name = fileNameGenerator.generate(url);
        return new File(cacheRoot, name);
    }

}
