package com.danikula.videocache.sourcestorage;

import com.danikula.videocache.BaseTest;
import com.danikula.videocache.SourceInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.robolectric.RuntimeEnvironment;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * Tests for {@link SourceInfoStorage}.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class SourceInfoStorageTest extends BaseTest {

    private SourceInfoStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = SourceInfoStorageFactory.newSourceInfoStorage(RuntimeEnvironment.application);
    }

    @After
    public void tearDown() throws Exception {
        storage.release();
    }

    @Test
    public void testGetAbsent() throws Exception {
        SourceInfo sourceInfo = storage.get(":-)");
        assertThat(sourceInfo).isNull();
    }

    @Test
    public void testSaving() throws Exception {
        storage.put(":-)", new SourceInfo(":-)", 42, "text/plain"));
        storage.put(":-(", new SourceInfo(":-(", 43, "video/mp4"));

        SourceInfo sourceInfo = storage.get(":-)");
        assertThat(sourceInfo.url).isEqualTo(":-)");
        assertThat(sourceInfo.length).isEqualTo(42);
        assertThat(sourceInfo.mime).isEqualTo("text/plain");

        SourceInfo sourceInfo2 = storage.get(":-(");
        assertThat(sourceInfo2.url).isEqualTo(":-(");
        assertThat(sourceInfo2.length).isEqualTo(43);
        assertThat(sourceInfo2.mime).isEqualTo("video/mp4");
    }

    @Test
    public void testUpdating() throws Exception {
        String url = ":-)";
        storage.put(url, new SourceInfo(url, 42, "text/plain"));
        storage.put(url, new SourceInfo(url, 43, "video/mp4"));

        SourceInfo sourceInfo = storage.get(url);
        assertThat(sourceInfo.url).isEqualTo(url);
        assertThat(sourceInfo.length).isEqualTo(43);
        assertThat(sourceInfo.mime).isEqualTo("video/mp4");
    }

    @Test(expected = NullPointerException.class)
    public void testNpeForGetting() throws Exception {
        storage.get(null);
        fail("null is not acceptable");
    }

    @Test(expected = NullPointerException.class)
    public void testNpeForPuttingUrl() throws Exception {
        storage.put(null, new SourceInfo("", 0, ""));
        fail("null is not acceptable");
    }

    @Test(expected = NullPointerException.class)
    public void testNpeForPuttingSource() throws Exception {
        storage.put("url", null);
        fail("null is not acceptable");
    }
}
