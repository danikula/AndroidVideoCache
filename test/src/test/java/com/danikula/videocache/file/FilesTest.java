package com.danikula.videocache.file;

import com.danikula.videocache.BaseTest;
import com.danikula.videocache.support.ProxyCacheTestUtils;

import org.junit.Test;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Tests for {@link Files}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class FilesTest extends BaseTest {

    @Test
    public void testModify() throws Exception {
        byte[] data = ProxyCacheTestUtils.generate(999);
        File file = ProxyCacheTestUtils.newCacheFile();
        com.google.common.io.Files.write(data, file);
        long lastModified = file.lastModified();

        Thread.sleep(1100); // file can store modification date in seconds. so wait for ~ 1 sec
        Files.modify(file);

        assertThat(file).hasBinaryContent(data);
        assertThat(file.lastModified()).isGreaterThan(lastModified);
    }

    @Test
    public void testSetModifiedNow() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        com.google.common.io.Files.write(ProxyCacheTestUtils.generate(22), file);

        Thread.sleep(1100); // file can store modification date in seconds. so wait for ~ 1 sec
        long nowRoundedToSecond = System.currentTimeMillis() / 1000 * 1000;
        Files.setLastModifiedNow(file);

        assertThat(file.lastModified()).isGreaterThanOrEqualTo(nowRoundedToSecond);
    }

    @Test
    public void testModifyZeroSizeFile() throws Exception {
        File file = ProxyCacheTestUtils.newCacheFile();
        boolean created = file.createNewFile();
        assertThat(created).isTrue();

        Thread.sleep(1100); // file can store modification date in seconds. so wait for ~ 2 sec
        long nowRoundedToSecond = System.currentTimeMillis() / 1000 * 1000;
        Files.modify(file);

        assertThat(file.lastModified()).isGreaterThanOrEqualTo(nowRoundedToSecond);
    }
}
