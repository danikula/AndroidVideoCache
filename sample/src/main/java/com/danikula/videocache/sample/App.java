package com.danikula.videocache.sample;

import android.app.Application;
import android.content.Context;

import com.danikula.videocache.FileNameGenerator;
import com.danikula.videocache.HttpProxyCacheServer;
import com.danikula.videocache.Md5FileNameGenerator;

/**
 * @author Alexey Danilov (danikula@gmail.com).
 */
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
