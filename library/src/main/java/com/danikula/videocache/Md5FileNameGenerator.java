package com.danikula.videocache;

import android.text.TextUtils;

import java.io.File;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.danikula.videocache.Preconditions.checkNotNull;

/**
 * Implementation of {@link FileNameGenerator} that uses MD5 of url as file name
 *
 * @author Alexey Danilov (danikula@gmail.com).
 */
public class Md5FileNameGenerator implements FileNameGenerator {

    private final File cacheDirectory;

    public Md5FileNameGenerator(File cacheDirectory) {
        this.cacheDirectory = checkNotNull(cacheDirectory);
    }

    @Override
    public File generate(String url) {
        checkNotNull(url);
        String extension = getExtension(url);
        String name = computeMD5(url);
        name = TextUtils.isEmpty(extension) ? name : name + "." + extension;
        return new File(cacheDirectory, name);
    }

    private String getExtension(String url) {
        int dotIndex = url.lastIndexOf('.');
        int slashIndex = url.lastIndexOf(File.separator);
        return dotIndex != -1 && dotIndex > slashIndex ? url.substring(dotIndex + 1, url.length()) : "";
    }

    private String computeMD5(String string) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] digestBytes = messageDigest.digest(string.getBytes());
            return bytesToHexString(digestBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
