package com.danikula.videocache.extend;

import com.danikula.videocache.HttpUrlSource;
import com.danikula.videocache.Source;
import com.danikula.videocache.headers.HeaderInjector;
import com.danikula.videocache.sourcestorage.SourceInfoStorage;

public interface IHttpUrlSourceMaker
{
    HttpUrlSource createHttpUrlSource(String url, SourceInfoStorage sourceInfoStorage, HeaderInjector headerInjector);
}
