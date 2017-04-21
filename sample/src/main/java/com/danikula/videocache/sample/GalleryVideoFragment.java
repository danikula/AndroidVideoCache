package com.danikula.videocache.sample;

import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.widget.ProgressBar;
import android.widget.VideoView;

import com.danikula.videocache.CacheListener;
import com.danikula.videocache.HttpProxyCacheServer;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.SeekBarTouchStop;
import org.androidannotations.annotations.ViewById;

import java.io.File;

@EFragment(R.layout.fragment_video)
public class GalleryVideoFragment extends Fragment implements CacheListener {

    @FragmentArg String url;

    @InstanceState int position;
    @InstanceState boolean playerStarted;

    @ViewById VideoView videoView;
    @ViewById ProgressBar progressBar;

    private boolean visibleForUser;

    private final VideoProgressUpdater updater = new VideoProgressUpdater();

    public static Fragment build(String url) {
        return GalleryVideoFragment_.builder()
                .url(url)
                .build();
    }

    @AfterViews
    void afterViewInjected() {
        startProxy();

        if (visibleForUser) {
            startPlayer();
        }
    }

    private void startPlayer() {
        videoView.seekTo(position);
        videoView.start();
        playerStarted = true;
    }

    private void startProxy() {
        HttpProxyCacheServer proxy = App.getProxy(getActivity());
        proxy.registerCacheListener(this, url);
        videoView.setVideoPath(proxy.getProxyUrl(url));
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        visibleForUser = isVisibleToUser;
        if (videoView != null) {
            if (visibleForUser) {
                startPlayer();
            } else if (playerStarted) {
                position = videoView.getCurrentPosition();
                videoView.pause();
            }
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

        videoView.stopPlayback();
        App.getProxy(getActivity()).unregisterCacheListener(this);
    }

    @Override
    public void onCacheAvailable(File file, String url, int percentsAvailable) {
        progressBar.setSecondaryProgress(percentsAvailable);
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
            sendEmptyMessageDelayed(0, 500);
        }
    }
}
