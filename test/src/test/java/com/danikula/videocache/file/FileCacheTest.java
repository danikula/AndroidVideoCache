package com.danikula.videocache.file;

import com.danikula.android.garden.io.Files;
import com.danikula.android.garden.io.IoUtils;
import com.danikula.videocache.BaseTest;
import com.danikula.videocache.Cache;
import com.danikula.videocache.ProxyCacheException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;

import static com.danikula.videocache.support.ProxyCacheTestUtils.ASSETS_DATA_NAME;
import static com.danikula.videocache.support.ProxyCacheTestUtils.generate;
import static com.danikula.videocache.support.ProxyCacheTestUtils.getFileContent;
import static com.danikula.videocache.support.ProxyCacheTestUtils.getTempFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.loadAssetFile;
import static com.danikula.videocache.support.ProxyCacheTestUtils.newCacheFile;
import static org.fest.assertions.api.Assertions.assertThat;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class FileCacheTest extends BaseTest {

    @Test
    public void testWriteReadDiscCache() throws Exception {
        int firstPortionLength = 10000;
        byte[] firstDataPortion = generate(firstPortionLength);
        File file = newCacheFile();
        Cache fileCache = new FileCache(file);

        fileCache.append(firstDataPortion, firstDataPortion.length);
        byte[] readData = new byte[firstPortionLength];
        fileCache.read(readData, 0, firstPortionLength);
        assertThat(readData).isEqualTo(firstDataPortion);
        byte[] fileContent = getFileContent(getTempFile(file));
        assertThat(readData).isEqualTo(fileContent);
    }

    @Test
    public void testFileCacheCompletion() throws Exception {
        File file = newCacheFile();
        File tempFile = getTempFile(file);
        Cache fileCache = new FileCache(file);
        assertThat(file.exists()).isFalse();
        assertThat(tempFile.exists()).isTrue();

        int dataSize = 345;
        fileCache.append(generate(dataSize), dataSize);
        fileCache.complete();

        assertThat(file.exists()).isTrue();
        assertThat(tempFile.exists()).isFalse();
        assertThat(file.length()).isEqualTo(dataSize);
    }

    @Test(expected = ProxyCacheException.class)
    public void testErrorAppendFileCacheAfterCompletion() throws Exception {
        Cache fileCache = new FileCache(newCacheFile());
        fileCache.append(generate(20), 10);
        fileCache.complete();
        fileCache.append(generate(20), 10);
        Assert.fail();
    }

    @Test
    public void testAppendDiscCache() throws Exception {
        File file = newCacheFile();
        Cache fileCache = new FileCache(file);

        int firstPortionLength = 10000;
        byte[] firstDataPortion = generate(firstPortionLength);
        fileCache.append(firstDataPortion, firstDataPortion.length);

        int secondPortionLength = 30000;
        byte[] secondDataPortion = generate(secondPortionLength * 2);
        fileCache.append(secondDataPortion, secondPortionLength);

        byte[] wroteSecondPortion = Arrays.copyOfRange(secondDataPortion, 0, secondPortionLength);
        byte[] readData = new byte[secondPortionLength];
        fileCache.read(readData, firstPortionLength, secondPortionLength);
        assertThat(readData).isEqualTo(wroteSecondPortion);

        readData = new byte[fileCache.available()];
        fileCache.read(readData, 0, readData.length);
        byte[] fileContent = getFileContent(getTempFile(file));
        assertThat(readData).isEqualTo(fileContent);
    }

    @Test
    public void testIsFileCacheCompleted() throws Exception {
        File file = newCacheFile();
        File partialFile = new File(file.getParentFile(), file.getName() + ".download");
        IoUtils.saveToFile(loadAssetFile(ASSETS_DATA_NAME), partialFile);
        Cache fileCache = new FileCache(partialFile);

        assertThat(file.exists()).isFalse();
        assertThat(partialFile.exists()).isTrue();
        assertThat(fileCache.isCompleted()).isFalse();

        fileCache.complete();

        assertThat(file.exists()).isTrue();
        assertThat(partialFile.exists()).isFalse();
        assertThat(fileCache.isCompleted()).isTrue();
        assertThat(partialFile.exists()).isFalse();
        assertThat(new FileCache(file).isCompleted()).isTrue();
    }

    @Test(expected = ProxyCacheException.class)
    public void testErrorWritingCompletedCache() throws Exception {
        File file = newCacheFile();
        IoUtils.saveToFile(loadAssetFile(ASSETS_DATA_NAME), file);
        FileCache fileCache = new FileCache(file);
        fileCache.append(generate(100), 20);
        Assert.fail();
    }

    @Test(expected = ProxyCacheException.class)
    public void testErrorWritingAfterCompletion() throws Exception {
        File file = newCacheFile();
        File partialFile = new File(file.getParentFile(), file.getName() + ".download");
        IoUtils.saveToFile(loadAssetFile(ASSETS_DATA_NAME), partialFile);
        FileCache fileCache = new FileCache(partialFile);
        fileCache.complete();
        fileCache.append(generate(100), 20);
        Assert.fail();
    }

    @Ignore("How to emulate file error?")
    @Test(expected = ProxyCacheException.class)
    public void testFileErrorForDiscCache() throws Exception {
        File file = new File("/system/data.bin");
        FileCache fileCache = new FileCache(file);
        Files.delete(file);
        fileCache.available();
        Assert.fail();
    }
}
