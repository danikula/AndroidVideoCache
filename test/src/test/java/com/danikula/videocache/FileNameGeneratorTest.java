package com.danikula.videocache;

import com.danikula.videocache.file.FileNameGenerator;
import com.danikula.videocache.file.Md5FileNameGenerator;

import org.junit.Test;

import java.io.File;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.fail;

/**
 * Tests for {@link FileNameGenerator} and implementations.
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class FileNameGeneratorTest extends BaseTest {

    @Test
    public void testMd5SimpleName() throws Exception {
        String url = "http://host.com/videos/video.mpeg";
        String path = generateMd5Name("/home", url);
        String expected = "/home/" + ProxyCacheUtils.computeMD5(url) + ".mpeg";
        assertThat(path).isEqualTo(expected);
    }

    @Test
    public void testMd5NoExtension() throws Exception {
        String url = "http://host.com/video";
        String path = generateMd5Name("/home", url);
        String expected = "/home/" + ProxyCacheUtils.computeMD5(url);
        assertThat(path).isEqualTo(expected);
    }

    @Test
    public void testMd5TooLongExtension() throws Exception {
        String url = "http://host.com/videos/video-with-dot-.12345";
        String path = generateMd5Name("/home", url);
        String expected = "/home/" + ProxyCacheUtils.computeMD5(url);
        assertThat(path).isEqualTo(expected);
    }

    @Test
    public void testMd5InvalidExtension() throws Exception {
        String url = "http://host.com/videos/video.mp4?token=-648729473536183645";
        String path = generateMd5Name("/home", url);
        String expected = "/home/" + ProxyCacheUtils.computeMD5(url);
        assertThat(path).isEqualTo(expected);
    }

    @Test
    public void testMd5ExtraLongExtension() throws Exception {
        // https://github.com/danikula/AndroidVideoCache/issues/14
        String url = "https://d1wst0behutosd.cloudfront.net/videos/4367900/10807247.480p.mp4?Expires=1442849176&Signature=JXV~3AoI0rWcGuZBywg3-ukf6Ycw2X8v7Htog3lyvuFwp8o6VUEDFUsTC9-XtIGu-ULxCd7dP3fvB306lRyGFxdvf-sXLX~ar~HCQ7lullNyeLtp8BJOT5Y~W5rJE7X-AZaueNcycGtLFRhRtr5ySTguwtmJNaO3T1apX~-oVrFh1dWStEKbuPoXY04RgkmhMHoFgtwgXMC1ctIDeQHxZeXLi6LLyZnQsgzlUDffCx4P16iiW0uh2-Z~HUOi9BLBwHMQ5k5lYwZqdQ6DhhYoWlniRfQz6mp1IEiMgr4L3Z1ijgGITV4cYeF31CmFzCxaJTE7IIAC5tMDQSTt7M9Q4A__&Key-Pair-Id=APKAJJ6WELAPEP47UKWQ";
        String path = generateMd5Name("/home", url);
        String expected = "/home/" + ProxyCacheUtils.computeMD5(url);
        assertThat(path).isEqualTo(expected);
    }

    @Test(expected = NullPointerException.class)
    public void testAssertNullUrl() throws Exception {
        FileNameGenerator nameGenerator = new Md5FileNameGenerator();
        nameGenerator.generate(null);
        fail("Url should be not null");
    }

    private String generateMd5Name(String rootFolder, String url) {
        FileNameGenerator nameGenerator = new Md5FileNameGenerator();
        String name = nameGenerator.generate(url);
        return new File(rootFolder, name).getAbsolutePath();
    }
}
