package com.danikula.videocache.sample;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.danikula.videocache.Cache;
import com.danikula.videocache.CacheListener;
import com.danikula.videocache.FileCache;
import com.danikula.videocache.HttpProxyCache;
import com.danikula.videocache.HttpUrlSource;
import com.danikula.videocache.ProxyCacheException;

import java.io.File;

public class VideoActivity extends Activity implements CacheListener {

    private static final String LOG_TAG = "VideoActivity";
    private static final String VIDEO_CACHE_NAME = "devbytes.mp4";
    private static final String VIDEO_URL = "https://dl.dropboxusercontent.com/u/15506779/persistent/proxycache/devbytes.mp4";

    private VideoView videoView;
    private ProgressBar progressBar;
    private HttpProxyCache proxyCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setUpUi();
        playWithCache();
    }

    private void setUpUi() {
        setContentView(R.layout.activity_video);
        videoView = (VideoView) findViewById(R.id.videoView);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setMax(100);
    }

    private void playWithCache() {
        try {
            Cache cache = new FileCache(new File(getExternalCacheDir(), VIDEO_CACHE_NAME));
            HttpUrlSource source = new HttpUrlSource(VIDEO_URL);
            proxyCache = new HttpProxyCache(source, cache);
            proxyCache.setCacheListener(this);
            videoView.setVideoPath(proxyCache.getUrl());
            videoView.start();
        } catch (ProxyCacheException e) {
            // do nothing. onError() handles all errors
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (proxyCache != null) {
            proxyCache.shutdown();
        }
    }

    @Override
    public void onError(ProxyCacheException e) {
        Log.e(LOG_TAG, "Error playing video", e);
    }

    @Override
    public void onCacheDataAvailable(int cachePercentage) {
        progressBar.setProgress(cachePercentage);
    }

}
