# Video cache support for Android
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-AndroidVideoCache-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1751)

## Why AndroidVideoCache?
Because there is no sense to download video a lot of times while streaming!
`AndroidVideoCache` allows to add caching support to your `VideoView/MediaPlayer`, [ExoPlayer](https://github.com/danikula/ExoPlayer/commit/6110be8559f003f98020ada8c5e09691b67aaff4) or any another player with help of single line!

## Features
- caching to disk during streaming;
- offline work with cached resources;
- partial loading;
- cache limits (max cache size, max files count);
- multiple clients for same url.

Note `AndroidVideoCache` works only with **direct urls** to media file, it  [**doesn't support**](https://github.com/danikula/AndroidVideoCache/issues/19) any streaming technology like DASH, SmoothStreaming, HLS.  

## How to use?
Just add dependency (`AndroidVideoCache` is available in jcenter):
```
repositories {
    jcenter()
}
dependencies {
    compile 'com.danikula:videocache:2.3.2'
}
```

and use url from proxy instead of original url for adding caching:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
super.onCreate(savedInstanceState);

    HttpProxyCacheServer proxy = getProxy();
    String proxyUrl = proxy.getProxyUrl(VIDEO_URL);
    videoView.setVideoPath(proxyUrl);
}

private HttpProxyCacheServer getProxy() {
    // should return single instance of HttpProxyCacheServer shared for whole app.
}
```

To guarantee normal work you should use **single** instance of `HttpProxyCacheServer` for whole app.
For example you can store shared proxy in your `Application`:

```java
public class App extends Application {

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        return new HttpProxyCacheServer(this);
    }
}
```

or use [simple factory](http://pastebin.com/s2fafSYS).
More preferable way is use some dependency injector like [Dagger](http://square.github.io/dagger/).

By default `HttpProxyCacheServer` uses 512Mb for caching files. You can change this value:

```java
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .maxCacheSize(1024 * 1024 * 1024)       // 1 Gb for cache
            .build();
}
```    

or can limit total count of files in cache: 

```java
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .maxCacheFilesCount(20)
            .build();
}
``` 

See `sample` app for details.

## Whats new
See Release Notes [here](https://github.com/danikula/AndroidVideoCache/releases)

## Code contributions
If it's a feature that you think would need to be discussed please open an issue first, otherwise, you can follow this process:

1. [Fork the project](http://help.github.com/fork-a-repo/)
2. Create a feature branch (git checkout -b my_branch)
3. Push your changes to your new branch (git push origin my_branch)
4. Initiate a [pull request](http://help.github.com/send-pull-requests/) on github
5. Your pull request will be reviewed and hopefully merged :)

## Where published?
[Here](https://bintray.com/alexeydanilov/maven/videocache/view)

## Questions?
[danikula@gmail.com](mailto:danikula@gmail.com)

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
