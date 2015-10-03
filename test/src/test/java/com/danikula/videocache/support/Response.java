package com.danikula.videocache.support;

import android.text.TextUtils;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Response {

    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final Pattern STATUS_CODE_PATTERN = Pattern.compile("HTTP/1.1 (\\d{3}) ");

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

    public Response(byte[] responseData) throws IOException {
        int read = 0;
        BufferedReader reader = new BufferedReader(new StringReader(new String(responseData, "ascii")));
        String statusLine = reader.readLine();
        read += statusLine.length() + 1;
        Matcher matcher = STATUS_CODE_PATTERN.matcher(statusLine);
        boolean hasCode = matcher.find();
        Preconditions.checkArgument(hasCode, "Status code not found in `" + statusLine + "`");
        this.code = Integer.parseInt(matcher.group(1));

        String header;
        this.headers = new HashMap<>();
        while (!TextUtils.isEmpty(header = reader.readLine())) {
            read += header.length() + 1;
            String[] keyValue = header.split(":");
            String headerName = keyValue[0].trim();
            String headerValue = keyValue[1].trim();
            headers.put(headerName, Collections.singletonList(headerValue));
        }
        read++;

        this.contentType = headers.containsKey(CONTENT_TYPE_HEADER) ? headers.get(CONTENT_TYPE_HEADER).get(0) : null;
        this.contentLength = headers.containsKey(CONTENT_LENGTH_HEADER) ? Integer.parseInt(headers.get(CONTENT_LENGTH_HEADER).get(0)) : -1;

        int bodySize = responseData.length - read;
        this.data = new byte[bodySize];
        System.arraycopy(responseData, read, data, 0, bodySize);
    }
}