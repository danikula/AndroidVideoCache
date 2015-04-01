Video cache support for Android
==============

Why AndroidVideoCache?
----
Because android MediaPlayer doesn't cache video while streaming.

How to use?
----
Just add link to repository and dependency:
```
repositories {
    maven { url 'https://github.com/danikula/AndroidVideoCache/raw/mvn-repo' }
}
...
compile 'com.danikula:videocache:1.0'
```

and use proxy for caching video:

```
@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        try {
            Cache cache = new FileCache(new File(getExternalCacheDir(), VIDEO_CACHE_NAME));
            HttpUrlSource source = new HttpUrlSource(VIDEO_URL);
            proxyCache = new HttpProxyCache(source, cache);
            videoView.setVideoPath(proxyCache.getUrl());
            videoView.start();
        } catch (ProxyCacheException e) {
            Log.e(LOG_TAG, "Error playing video", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (proxyCache != null) {
            proxyCache.shutdown();
        }
    }

```
See `sample` app for details.
