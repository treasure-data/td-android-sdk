package com.treasuredata.android;

import io.keen.client.java.http.Request;
import io.keen.client.java.http.UrlConnectionHttpHandler;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class TDHttpHandler extends UrlConnectionHttpHandler {
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final String TAG = TDHttpHandler.class.getSimpleName();
    private static final String DEFAULT_API_ENDPOINT = "http://in-staging.treasuredata.com/android/v3/event";

    private final String apiKey;
    private final String apiEndpoint;

    public TDHttpHandler(String apiKey, String apiEndpoint) {
        if (apiKey == null) {
            throw new IllegalArgumentException("apiKey is null");
        }
        if (apiEndpoint == null) {
            apiEndpoint = DEFAULT_API_ENDPOINT;
        }
        this.apiKey = apiKey;
        this.apiEndpoint = apiEndpoint;
    }

    protected HttpURLConnection openConnection(Request request) throws IOException {
        // URL url = new URL("https://in.treasuredata.com/android/v3/event");
        URL url = new URL(this.apiEndpoint);
        HttpURLConnection result = (HttpURLConnection) url.openConnection();
        result.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        result.setReadTimeout(DEFAULT_READ_TIMEOUT);
        Log.d(TAG, "openConnection, request:" + request + ", result:" + result);
        return result;
    }

    protected void sendRequest(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-TD-Data-Type", "k");
        connection.setRequestProperty("X-TD-Write-Key", apiKey);
        /*
        for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
            boolean needDelimiter = false;
            StringBuilder buf = new StringBuilder();
            for (String v : header.getValue()) {
                if (needDelimiter) {
                    buf.append(", ");
                }
                needDelimiter = true;
                buf.append(v);
            }
            Log.d(TAG, "sendRequest(header): k=[" + header.getKey() + "], v=[" + buf.toString() + "]");
        }
        */
        Log.d(TAG, "sendRequest: apiKey=" + apiKey);
        connection.setDoOutput(true);
        request.body.writeTo(connection.getOutputStream());
        Log.d(TAG, "sendRequest, connection:" + connection + ", request:" + request);
    }
}
