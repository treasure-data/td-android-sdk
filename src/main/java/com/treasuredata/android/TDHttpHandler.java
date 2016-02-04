package com.treasuredata.android;

import android.os.Build;
import io.keen.client.java.http.Request;
import io.keen.client.java.http.Response;
import io.keen.client.java.http.UrlConnectionHttpHandler;
import org.komamitsu.android.util.Log;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Arrays;
import java.util.zip.DeflaterInputStream;

class TDHttpHandler extends UrlConnectionHttpHandler {
    static volatile String VERSION = "0.0.0";
    private static final int DEFAULT_CONNECT_TIMEOUT = 30000;
    private static final int DEFAULT_READ_TIMEOUT = 30000;
    private static final String TAG = TDHttpHandler.class.getSimpleName();
    private static final String DEFAULT_API_ENDPOINT = "https://in.treasuredata.com";
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

    protected HttpURLConnection openConnection(Request request) throws IOException {
        URL url = new URL(String.format("%s/android/v3/event", this.apiEndpoint));
        HttpURLConnection result = (HttpURLConnection) url.openConnection();
        result.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT);
        result.setReadTimeout(DEFAULT_READ_TIMEOUT);
        return result;
    }

    protected void sendRequest(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-TD-Data-Type", "k");
        connection.setRequestProperty("X-TD-Write-Key", apiKey);
        connection.setRequestProperty("User-Agent", String.format("TD-Android-SDK/%s (%s %s)", VERSION, Build.MODEL, Build.VERSION.RELEASE));
        connection.setDoOutput(true);

        try {
            if (isEventCompression) {
                connection.setRequestProperty("Content-Encoding", "deflate");
                ByteArrayOutputStream srcOutputStream = new ByteArrayOutputStream();
                request.body.writeTo(srcOutputStream);
                byte[] srcBytes = srcOutputStream.toByteArray();

                BufferedInputStream compressedInputStream = new BufferedInputStream(new DeflaterInputStream(new ByteArrayInputStream(srcBytes)));
                int readLen;
                byte[] buf = new byte[256];
                while ((readLen = compressedInputStream.read(buf)) > 0) {
                    connection.getOutputStream().write(buf, 0, readLen);
                }
            }
            else {
                request.body.writeTo(connection.getOutputStream());
            }
        }
        finally {
            connection.getOutputStream().close();
        }
    }

    @Override
    protected Response readResponse(HttpURLConnection connection) throws IOException {
        Response response = super.readResponse(connection);
        return response;
    }
}
