package com.danikula.videocache.sample;

import android.content.Context;

import java.io.File;

public enum Video {

    ORANGE_1(Config.ROOT + "orange1.mp4"),
    ORANGE_2(Config.ROOT + "orange2.mp4"),
    ORANGE_3(Config.ROOT + "orange3.mp4"),
    ORANGE_4(Config.ROOT + "orange4.mp4"),
    ORANGE_5(Config.ROOT + "orange5.mp4");

    public final String url;

    Video(String url) {
        this.url = url;
    }

    public File getCacheFile(Context context) {
        return new File(context.getExternalCacheDir(), name());
    }

    private class Config {
        private static final String ROOT = "https://raw.githubusercontent.com/danikula/AndroidVideoCache/master/files/";
    }
}
