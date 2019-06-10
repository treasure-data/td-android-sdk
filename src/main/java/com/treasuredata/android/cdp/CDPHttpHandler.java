package com.treasuredata.android.cdp;

import io.keen.client.java.http.Request;
import io.keen.client.java.http.Response;
import io.keen.client.java.http.UrlConnectionHttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;

// TODO: consider make this an inner class if it turns out to be small
class CDPHttpHandler extends UrlConnectionHttpHandler {
    public CDPHttpHandler() { }

    @Override
    protected void sendRequest(HttpURLConnection connection, Request request) throws IOException {
        connection.setRequestMethod("GET");
        // connection.setRequestProperty("Content-Type", "application/json");
        // connection.setRequestProperty("User-Agent", String.format("TD-Android-SDK/%s (%s %s)", VERSION, Build.MODEL, Build.VERSION.RELEASE));
        // connection.setDoOutput(true);

        // try {
        //     if (isEventCompression) {
        //         connection.setRequestProperty("Content-Encoding", "deflate");
        //         ByteArrayOutputStream srcOutputStream = new ByteArrayOutputStream();
        //         request.body.writeTo(srcOutputStream);
        //         byte[] srcBytes = srcOutputStream.toByteArray();
        //
        //         BufferedInputStream compressedInputStream = new BufferedInputStream(new DeflaterInputStream(new ByteArrayInputStream(srcBytes)));
        //         int readLen;
        //         byte[] buf = new byte[256];
        //         while ((readLen = compressedInputStream.read(buf)) > 0) {
        //             connection.getOutputStream().write(buf, 0, readLen);
        //         }
        //     }
        //     else {
        //         request.body.writeTo(connection.getOutputStream());
        //     }
        // }
        // finally {
        //     connection.getOutputStream().close();
        // }
        // super.sendRequest(connection, request);
    }

    @Override
    protected Response readResponse(HttpURLConnection connection) throws IOException {
        // return super.readResponse(connection);
        return null;
    }
}
