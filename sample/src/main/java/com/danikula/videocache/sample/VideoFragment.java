package com.danikula.videocache.sample;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.danikula.videocache.Cache;
import com.danikula.videocache.CacheListener;
import com.danikula.videocache.FileCache;
import com.danikula.videocache.HttpProxyCache;
import com.danikula.videocache.HttpUrlSource;
import com.danikula.videocache.ProxyCacheException;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.SeekBarTouchStop;
import org.androidannotations.annotations.ViewById;

import java.io.File;

@EFragment(R.layout.fragment_video)
public class VideoFragment extends Fragment implements CacheListener {

    private static final String LOG_TAG = "VideoFragment";

    @FragmentArg String url;
    @FragmentArg String cachePath;

    @ViewById VideoView videoView;
    @ViewById ProgressBar progressBar;

    private HttpProxyCache proxyCache;
    private final VideoProgressUpdater updater = new VideoProgressUpdater();

    public static Fragment build(Context context, Video video) {
        return build(video.url, video.getCacheFile(context).getAbsolutePath());
    }

    public static Fragment build(String url, String cachePath) {
        return VideoFragment_.builder()
                .url(url)
                .cachePath(cachePath)
                .build();
    }

    @AfterViews
    void afterViewInjected() {
        startVideo();
    }

    private void startVideo() {
        try {
            Cache cache = new FileCache(new File(cachePath));
            HttpUrlSource source = new HttpUrlSource(url);
            proxyCache = new HttpProxyCache(source, cache);
            proxyCache.setCacheListener(this);
            videoView.setVideoPath(proxyCache.getUrl());
            videoView.start();
        } catch (ProxyCacheException e) {
            // do nothing. onError() handles all errors
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updater.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        updater.stop();
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
        progressBar.setSecondaryProgress(cachePercentage);
    }

    private void updateVideoProgress() {
        int videoProgress = videoView.getCurrentPosition() * 100 / videoView.getDuration();
        progressBar.setProgress(videoProgress);
    }

    @SeekBarTouchStop(R.id.progressBar)
    void seekVideo() {
        int videoPosition = videoView.getDuration() * progressBar.getProgress() / 100;
        videoView.seekTo(videoPosition);
    }

    private final class VideoProgressUpdater extends Handler {

        public void start() {
            sendEmptyMessage(0);
        }

        public void stop() {
            removeMessages(0);
        }

        @Override
        public void handleMessage(Message msg) {
            updateVideoProgress();
            sendEmptyMessageDelayed(0, 200);
        }
    }
}
