package com.treasuredata.android;

import android.os.Build;
import io.keen.client.java.http.Request;
import io.keen.client.java.http.Response;
import io.keen.client.java.http.UrlConnectionHttpHandler;

import java.io.*;
import java.net.HttpURLConnection;
import java.util.zip.GZIPOutputStream;

class TDHttpHandler extends UrlConnectionHttpHandler {
    static volatile String VERSION = "0.0.0";
    private static final String DEFAULT_API_ENDPOINT = "https://us01.records.in.treasuredata.com";
    private static volatile boolean isEventCompression = true;

    private final String apiKey;
    private final String apiEndpoint;

    public static void disableEventCompression() {
        isEventCompression = false;
    }

    public static void enableEventCompression() {
        isEventCompression = true;
    }

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

    protected void sendRequest(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "TD1 " + apiKey);
        connection.setRequestProperty("Content-Type", "application/vnd.treasuredata.v1+json");
        connection.setRequestProperty("Accept", "application/vnd.treasuredata.v1+json");
        connection.setRequestProperty("User-Agent", String.format("TD-Android-SDK/%s (%s %s)", VERSION, Build.MODEL, Build.VERSION.RELEASE));
        connection.setDoOutput(true);

        if (isEventCompression) {
            connection.setRequestProperty("Content-Encoding", "gzip");
            ByteArrayOutputStream srcOutputStream = new ByteArrayOutputStream();
            request.body.writeTo(srcOutputStream);
            byte[] srcBytes = srcOutputStream.toByteArray();

            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(connection.getOutputStream());
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(gzipOutputStream);
            bufferedOutputStream.write(srcBytes);
            bufferedOutputStream.close();
        }
        else {
            request.body.writeTo(connection.getOutputStream());
            connection.getOutputStream().close();
        }
    }

    @Override
    protected Response readResponse(HttpURLConnection connection) throws IOException {
        return super.readResponse(connection);
    }
}
