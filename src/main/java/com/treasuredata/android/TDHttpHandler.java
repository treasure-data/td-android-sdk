package com.treasuredata.android;

import io.keen.client.java.http.Request;
import io.keen.client.java.http.UrlConnectionHttpHandler;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class TDHttpHandler extends UrlConnectionHttpHandler {
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final String TAG = TDHttpHandler.class.getSimpleName();

    private final String apiKey;

    public TDHttpHandler(String apiKey) {
        Log.d(TAG, "init, apiKey:" + apiKey);
        if (apiKey == null) {
            throw new IllegalArgumentException("apiKey is null");
        }
        this.apiKey = apiKey;
    }

    protected HttpURLConnection openConnection(Request request) throws IOException {
        // TODO: Make it configurable
        // URL url = new URL("https://in.treasuredata.com/android/v3/event");
        URL url = new URL("http://in-staging.treasuredata.com/android/v3/event");
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
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        request.body.writeTo(outputStream);
        Log.d(TAG, "sendRequest, request.body:" + new String(outputStream.toByteArray()));
        */

        connection.setDoOutput(true);
        request.body.writeTo(connection.getOutputStream());
        Log.d(TAG, "sendRequest, connection:" + connection + ", request:" + request);
    }
}
