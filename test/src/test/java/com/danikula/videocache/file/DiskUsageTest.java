package com.danikula.videocache.file;

import com.danikula.videocache.BaseTest;
import com.danikula.videocache.support.ProxyCacheTestUtils;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.danikula.android.garden.io.Files.cleanDirectory;
import static com.danikula.android.garden.io.Files.createDirectory;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests for implementations of {@link DiskUsage}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class DiskUsageTest extends BaseTest {

    private File cacheFolder;

    @Before
    public void setup() throws Exception {
        cacheFolder = ProxyCacheTestUtils.newCacheFile();
        createDirectory(cacheFolder);
        cleanDirectory(cacheFolder);
    }

    @Test
    public void testMaxSizeCacheLimit() throws Exception {
        DiskUsage diskUsage = new TotalSizeLruDiskUsage(300);
        long now = System.currentTimeMillis();
        createFile(file("b"), 101, now - 10000);
        createFile(file("c"), 102, now - 8000);
        createFile(file("a"), 104, now - 4000); // exceeds

        diskUsage.touch(file("c"));
        waitForAsyncTrimming();

        assertThat(file("b")).doesNotExist();
        assertThat(file("c")).exists();
        assertThat(file("a")).exists();

        createFile(file("d"), 500, now); // exceeds all
        diskUsage.touch(file("d"));
        waitForAsyncTrimming();

        assertThat(file("a")).doesNotExist();
        assertThat(file("c")).doesNotExist();
        assertThat(file("d")).doesNotExist();
    }

    @Test
    public void testMaxFilesCount() throws Exception {
        DiskUsage diskUsage = new TotalCountLruDiskUsage(2);
        long now = System.currentTimeMillis();
        createFile(file("b"), 101, now - 10000);
        createFile(file("c"), 102, now - 8000);
        createFile(file("a"), 104, now - 4000);

        diskUsage.touch(file("c"));
        waitForAsyncTrimming();

        assertThat(file("b")).doesNotExist();
        assertThat(file("a")).exists();
        assertThat(file("c")).exists();

        createFile(file("d"), 500, now);
        diskUsage.touch(file("d"));
        waitForAsyncTrimming();

        assertThat(file("a")).doesNotExist();
        assertThat(file("c")).exists();
        assertThat(file("d")).exists();
    }

    @Test
    public void testTouch() throws Exception {
        DiskUsage diskUsage = new TotalCountLruDiskUsage(2);
        long now = System.currentTimeMillis();
        createFile(file("b"), 101, now - 10000);
        createFile(file("c"), 102, now - 8000);
        createFile(file("a"), 104, now - 4000);

        diskUsage.touch(file("b"));
        waitForAsyncTrimming();

        assertThat(file("b")).exists();
        assertThat(file("a")).exists();
        assertThat(file("c")).doesNotExist();

        Thread.sleep(1000);  // last modified is rounded to seconds, so wait for sec
        new TotalCountLruDiskUsage(1).touch(file("a"));
        waitForAsyncTrimming();

        assertThat(file("a")).exists();
        assertThat(file("b")).doesNotExist();
    }

    private void waitForAsyncTrimming() throws InterruptedException {
        Thread.sleep(200);
    }

    private File file(String name) {
        return new File(cacheFolder, name);
    }

    private void createFile(File file, int capacity, long lastModified) throws IOException {
        byte[] data = ProxyCacheTestUtils.generate(capacity);
        com.google.common.io.Files.write(data, file);
        boolean modified = file.setLastModified(lastModified);
        assertThat(modified).isTrue();
    }
}
