package com.treasure_data.td_logger.android;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;

public class ApiClient {
    private static final String TAG = ApiClient.class.getSimpleName();
    private final String apikey;
    private final String host;
    private final int port;

    public static class ApiError extends Exception {
        private static final long serialVersionUID = 1L;

        public ApiError(String message) {
            super(message);
        }
    }

    public ApiClient(String apikey, String host, int port) {
        this.apikey = apikey;
        this.host = host;
        this.port = port;
    }

    public String importTable(String database, String table, byte [] data) throws IOException, ApiError {
        String path = String.format("/v3/table/import/%s/%s/msgpack.gz", database, table);
        URL url = new URL("http", host, port, path);
        Log.d(TAG, "importTable: url=" + url + ", data.len=" + data.length);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestProperty("Authorization", "TD1 " + apikey);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("Content-Length", String.valueOf(data.length));
        conn.setDoOutput(true);
        conn.setUseCaches (false);

        BufferedOutputStream out = null;
        try {
            out = new BufferedOutputStream(conn.getOutputStream());
            out.write(data);
            out.flush();
        }
        finally {
            IOUtils.closeQuietly(out);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_OK) {
            throw new ApiError(conn.getResponseMessage());
        }
        return IOUtils.toString(conn.getInputStream());
    }
}
