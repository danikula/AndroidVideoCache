## Video cache support for Android
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-AndroidVideoCache-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1751) [![Build Status](https://api.travis-ci.org/danikula/AndroidVideoCache.svg?branch=master)](https://travis-ci.org/danikula/AndroidVideoCache/) [ ![Download](https://api.bintray.com/packages/alexeydanilov/maven/videocache/images/download.svg) ](https://bintray.com/alexeydanilov/maven/videocache/_latestVersion)

## Table of Content
- [Why AndroidVideoCache?](#why-androidvideocache)
- [Features](#features)
- [Get started](#get-started)
- [Recipes](#recipes)
  - [Disk cache limit](#disk-cache-limit)
  - [Listen caching progress](#listen-caching-progress)
  - [Providing names for cached files](#providing-names-for-cached-files)
  - [Adding custom http headers](#adding-custom-http-headers)
  - [Using exoPlayer](#using-exoplayer)
  - [Sample](#sample)
- [Known problems](#known-problems)
- [Whats new](#whats-new)
- [Code contributions](#code-contributions)
- [Where published?](#where-published)
- [Questions?](#questions)
- [License](#license)

## Why AndroidVideoCache?
Because there is no sense to download video a lot of times while streaming!
`AndroidVideoCache` allows to add caching support to your `VideoView/MediaPlayer`, [ExoPlayer](https://github.com/danikula/AndroidVideoCache/tree/exoPlayer) or any another player with help of single line!

## Features
- caching to disk during streaming;
- offline work with cached resources;
- partial loading;
- cache limits (max cache size, max files count);
- multiple clients for same url.

Note `AndroidVideoCache` works only with **direct urls** to media file, it  [**doesn't support**](https://github.com/danikula/AndroidVideoCache/issues/19) any streaming technology like DASH, SmoothStreaming, HLS.  

## Get started
Just add dependency (`AndroidVideoCache` is available in jcenter):
```
dependencies {
    compile 'com.danikula:videocache:2.7.1'
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

## Recipes
### Disk cache limit
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

or even implement your own `DiskUsage` strategy:
```java
private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .diskUsage(new MyCoolDiskUsageStrategy())
            .build();
}
```


### Listen caching progress
Use `HttpProxyCacheServer.registerCacheListener(CacheListener listener)` method to set listener with callback `onCacheAvailable(File cacheFile, String url, int percentsAvailable)` to be aware of caching progress. Do not forget to to unsubscribe listener with help of `HttpProxyCacheServer.unregisterCacheListener(CacheListener listener)` method to avoid memory leaks.

Use `HttpProxyCacheServer.isCached(String url)` method to check was url's content fully cached to file or not.

See `sample` app for more details.

### Providing names for cached files
By default `AndroidVideoCache` uses MD5 of video url as file name. But in some cases url is not stable and it can contain some generated parts (e.g. session token). In this case caching mechanism will be broken. To fix it you have to provide own `FileNameGenerator`:
``` java
public class MyFileNameGenerator implements FileNameGenerator {

    // Urls contain mutable parts (parameter 'sessionToken') and stable video's id (parameter 'videoId').
    // e. g. http://example.com?videoId=abcqaz&sessionToken=xyz987
    public String generate(String url) {
        Uri uri = Uri.parse(url);
        String videoId = uri.getQueryParameter("videoId");
        return videoId + ".mp4";
    }
}

...
HttpProxyCacheServer proxy = new HttpProxyCacheServer.Builder(context)
    .fileNameGenerator(new MyFileNameGenerator())
    .build()
```

### Adding custom http headers
You can add custom headers to requests with help of `HeadersInjector`:
``` java
public class UserAgentHeadersInjector implements HeaderInjector {

    @Override
    public Map<String, String> addHeaders(String url) {
        return Maps.newHashMap("User-Agent", "Cool app v1.1");
    }
}

private HttpProxyCacheServer newProxy() {
    return new HttpProxyCacheServer.Builder(this)
            .headerInjector(new UserAgentHeadersInjector())
            .build();
}

```

### Using exoPlayer
You can use [`exoPlayer`](https://google.github.io/ExoPlayer/) with `AndroidVideoCache`. See `sample` app in [`exoPlayer`](https://github.com/danikula/AndroidVideoCache/tree/exoPlayer) branch. Note [exoPlayer supports](https://github.com/google/ExoPlayer/commit/bd7be1b5e7cc41a59ebbc348d394820fc857db92) cache as well.  

### Sample
See `sample` app.

## Known problems
- In some cases clients [can't connect](https://github.com/danikula/AndroidVideoCache/issues/134) to local proxy server ('Error pinging server' error). May be it is result of previous error. Note in this case video will be played, but without caching.

## Whats new
See Release Notes [here](https://github.com/danikula/AndroidVideoCache/releases)

## Code contributions
If it's a feature that you think would need to be discussed please open an issue first, otherwise, you can follow this process:

1. [Fork the project](http://help.github.com/fork-a-repo/)
2. Create a feature branch (git checkout -b my_branch)
3. Fix a problem. Your code **must** contain test for reproducing problem. Your tests **must be passed** with help of your fix
4. Push your changes to your new branch (git push origin my_branch)
5. Initiate a [pull request](http://help.github.com/send-pull-requests/) on github
6. Rebase [master branch](https://github.com/danikula/AndroidVideoCache) if your local branch is not actual. Merging is not acceptable, only rebase
6. Your pull request will be reviewed and hopefully merged :)

## Where published?
[Here](https://bintray.com/alexeydanilov/maven/videocache/view)

## Questions?
[danikula@gmail.com](mailto:danikula@gmail.com)

## License

    Copyright 2014-2017 Alexey Danilov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
