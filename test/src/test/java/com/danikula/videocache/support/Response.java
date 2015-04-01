package com.danikula.videocache.support;

import com.google.common.io.ByteStreams;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class Response {

    public final int code;
    public final byte[] data;
    public final int contentLength;
    public final String contentType;
    public final Map<String, List<String>> headers;

    public Response(HttpURLConnection connection) throws IOException {
        this.code = connection.getResponseCode();
        this.contentLength = connection.getContentLength();
        this.contentType = connection.getContentType();
        this.headers = connection.getHeaderFields();
        this.data = ByteStreams.toByteArray(connection.getInputStream());
    }
}