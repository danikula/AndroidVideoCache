# Video cache support for Android
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-AndroidVideoCache-brightgreen.svg?style=flat)](http://android-arsenal.com/details/1/1751)

## Why AndroidVideoCache?
Because there is no sense to download video a lot of times while streaming!
`AndroidVideoCache` allows to add caching support to your `VideoView/MediaPlayer`, [ExoPlayer](https://github.com/danikula/ExoPlayer/commit/6110be8559f003f98020ada8c5e09691b67aaff4) or any another player with help of single line!

## How to use?
Just add link to repository and dependency:
```
repositories {
    maven { url 'https://dl.bintray.com/alexeydanilov/maven' }
}
dependencies {
    compile 'com.danikula:videocache:2.1.2'
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
For example you can store shared proxy on your `Application`:

```java
public class App extends Application {

    private HttpProxyCacheServer proxy;

    public static HttpProxyCacheServer getProxy(Context context) {
        App app = (App) context.getApplicationContext();
        return app.proxy == null ? (app.proxy = app.newProxy()) : app.proxy;
    }

    private HttpProxyCacheServer newProxy() {
        FileNameGenerator nameGenerator = new Md5FileNameGenerator(getExternalCacheDir());
        return new HttpProxyCacheServer(nameGenerator);
    }
}
```

or use [simple factory](http://pastebin.com/38uNkgBT).
More preferable way is use some dependency injector like [Dagger](http://square.github.io/dagger/).

See `sample` app for details.

## Whats new
### 2.1.2
- fix offline work
### 2.1.1
- fix for too long cache file name
- url redirects support (thanks [ongakuer](https://github.com/ongakuer) for [PR](https://github.com/danikula/AndroidVideoCache/pull/12))

### 2.0
- simpler api
- single cache for multiple clients
- cache file name policy
- more powerful listener
- more samples
- less log flood

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