# Video cache support for Android
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-AndroidVideoCache-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1751)

## Why AndroidVideoCache?
Because android MediaPlayer doesn't cache video while streaming.

## How to use?
Just add link to repository and dependency:
```
repositories {
    maven { url 'https://dl.bintray.com/alexeydanilov/maven' }
}
...
compile 'com.danikula:videocache:1.0.1'
```

and use proxy for caching video:

```java
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

## Where published?
[Here](https://bintray.com/alexeydanilov/maven/videocache/view)

## License

    Copyright 2014-2015 Alexey Danilov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.