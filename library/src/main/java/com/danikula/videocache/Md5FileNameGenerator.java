package com.danikula.videocache;

import android.text.TextUtils;

import java.io.File;

import static com.danikula.videocache.Preconditions.checkNotNull;

/**
 * Implementation of {@link FileNameGenerator} that uses MD5 of url as file name
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class Md5FileNameGenerator implements FileNameGenerator {

    private static final int MAX_EXTENSION_LENGTH = 4;
    private final File cacheDirectory;

    public Md5FileNameGenerator(File cacheDirectory) {
        this.cacheDirectory = checkNotNull(cacheDirectory);
    }

    @Override
    public File generate(String url) {
        checkNotNull(url);
        String extension = getExtension(url);
        String name = ProxyCacheUtils.computeMD5(url);
        name = TextUtils.isEmpty(extension) ? name : name + "." + extension;
        return new File(cacheDirectory, name);
    }

    private String getExtension(String url) {
        int dotIndex = url.lastIndexOf('.');
        int slashIndex = url.lastIndexOf('/');
        return dotIndex != -1 && dotIndex > slashIndex && dotIndex + 2 + MAX_EXTENSION_LENGTH > url.length() ?
                url.substring(dotIndex + 1, url.length()) : "";
    }
}
