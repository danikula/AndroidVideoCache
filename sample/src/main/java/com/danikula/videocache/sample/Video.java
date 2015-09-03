package com.danikula.videocache.sample;

import android.content.Context;

import java.io.File;

public enum Video {

    ORANGE_1("https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/orange1.mp4"),
    ORANGE_2("https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/orange2.mp4"),
    ORANGE_3("https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/orange3.mp4"),
    ORANGE_4("https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/orange4.mp4"),
    ORANGE_5("https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/orange5.mp4");

    public final String url;

    Video(String url) {
        this.url = url;
    }

    public File getCacheFile(Context context) {
        return new File(context.getExternalCacheDir(), name());
    }
}
