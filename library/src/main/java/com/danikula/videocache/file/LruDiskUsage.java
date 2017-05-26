package com.danikula.videocache.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * {@link DiskUsage} that uses LRU (Least Recently Used) strategy to trim cache.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public abstract class LruDiskUsage implements DiskUsage {

    private static final Logger LOG = LoggerFactory.getLogger("LruDiskUsage");
    private final ExecutorService workerThread = Executors.newSingleThreadExecutor();

    @Override
    public void trim(File folder){
        //folder passed in is the cache folder. Trim this if required
        workerThread.submit(new TrimCallable(folder));
    }

    @Override
    public void touch(File file) throws IOException {
        workerThread.submit(new TouchCallable(file));
    }

    private void trimInBackground(File folder) throws IOException {
        List<File> files = Files.getLruListFiles(folder);
        trim(files);
    }

    private void touchInBackground(File file) throws IOException {
        Files.setLastModifiedNow(file);
        List<File> files = Files.getLruListFiles(file.getParentFile());
        trim(files);
    }

    protected abstract boolean accept(File file, long totalSize, int totalCount);

    private void trim(List<File> files) {
        long totalSize = countTotalSize(files);
        int totalCount = files.size();
        for (File file : files) {
            boolean accepted = accept(file, totalSize, totalCount);
            if (!accepted) {
                long fileSize = file.length();
                boolean deleted = file.delete();
                if (deleted) {
                    totalCount--;
                    totalSize -= fileSize;
                    LOG.info("Cache file " + file + " is deleted because it exceeds cache limit");
                } else {
                    LOG.error("Error deleting file " + file + " for trimming cache");
                }
            }
        }
    }

    private long countTotalSize(List<File> files) {
        long totalSize = 0;
        for (File file : files) {
            totalSize += file.length();
        }
        return totalSize;
    }


    private class TouchCallable implements Callable<Void> {

        private final File file;

        public TouchCallable(File file) {
            this.file = file;
        }

        @Override
        public Void call() throws Exception {
            touchInBackground(file);
            return null;
        }
    }

    private class TrimCallable implements Callable<Void> {

        private final File folder;

        public TrimCallable(File folder) {
            this.folder = folder;
        }

        @Override
        public Void call() throws Exception {
            trimInBackground(folder);
            return null;
        }
    }
}
