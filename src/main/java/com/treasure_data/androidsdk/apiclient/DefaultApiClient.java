package com.treasure_data.androidsdk.apiclient;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.komamitsu.android.util.Log;

public class DefaultApiClient implements ApiClient {
    private static final String TAG = DefaultApiClient.class.getSimpleName();
    private String apikey;
    private String host;
    private int port;

    public static class ApiError extends Exception {
        private static final long serialVersionUID = 1L;

        public ApiError(String message) {
            super(message);
        }
    }

    /* (non-Javadoc)
     * @see com.treasure_data.td_logger.android.ApiClient#init(java.lang.String, java.lang.String, int)
     */
    @Override
    public void init(String apikey, String host, int port) {
        this.apikey = apikey;
        this.host = host;
        this.port = port;
    }

    private void setupClient(HttpURLConnection conn) {
        conn.setRequestProperty("Authorization", "TD1 " + apikey);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setRequestProperty("Connection", "close");
    }

    private String post(String path, Map<String, String> properties) throws IOException, ApiError {
        URL url = new URL("https", host, port, path);
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            setupClient(conn);
            conn.setRequestMethod("POST");
            if (properties != null && properties.size() > 0) {
                StringBuffer buf = new StringBuffer();
                for(Entry<String, String> property : properties.entrySet()) {
                    if (buf.length() != 0)
                        buf.append("&");
                    buf.append(property.getKey())
                       .append("=")
                       .append(property.getValue());
                }
                OutputStreamWriter writer = null;
                try {
                    writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(buf.toString());
                    writer.flush();
                }
                catch (IOException ioe) {
                    Log.e(TAG, "Cannot create OutputStreamWriter with this connection object.");
                    throw ioe;
                }
                finally {
                    IOUtils.closeQuietly(writer);
                }
            } else {
                conn.connect();
            }
            Log.d(TAG, "post: url=" + url + ", conn=" + conn.hashCode());

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "status code=" + responseCode +
                    " (" + conn.getResponseMessage() + "), conn=" + conn.hashCode());
            if (responseCode == HttpURLConnection.HTTP_CONFLICT) {
                Log.d(TAG, "Got conflict doing a POST request to url=" + path);
            } else if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new ApiError("post: url=" + url +
                        ", status code=" + responseCode +
                        " (" + conn.getResponseMessage() + ")" +
                        ", conn=" + conn.hashCode());
            }
            return IOUtils.toString(conn.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
            return IOUtils.toString(conn.getInputStream());
        } finally {
            IOUtils.closeQuietly(conn.getInputStream());
        }
    }

    // TODO Javadoc
    @Override
    public String createDatabase(String database)
                    throws IOException, ApiError {
        String path = String.format("/v3/database/create/%s", database);
        return post(path, null);
    }

    /* (non-Javadoc)
     * @see com.treasure_data.td_logger.android.ApiClient#createTable(java.lang.String, java.lang.String)
     */
    @Override
    public String createTable(String database, String table)
                    throws IOException, ApiError {
        String path = String.format("/v3/table/create/%s/%s/log", database, table);
        return post(path, null);
    }

    // TODO Javadoc
    @Override
    public String createItemTable(String database, String table,
            String pkName, String pkType)
                    throws IOException, ApiError {
        String path = String.format("/v3/table/create/%s/%s/item", database, table);
        Map<String, String> postProps = new HashMap<String, String>();
        // example properties for creating an item table:
        //  primary_key=dev_id&primary_key_type=string
        postProps.put("primary_key", pkName);
        postProps.put("primary_key_type", pkType);
        return post(path, postProps);
    }

    /* (non-Javadoc)
     * @see com.treasure_data.td_logger.android.ApiClient#importTable(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public String importTable(String database, String table, byte[] data) throws IOException, ApiError {
        HttpURLConnection conn = null;
        try {
            String path = String.format("/v3/table/import/%s/%s/msgpack.gz", database, table);
            URL url = new URL("https", host, port, path);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("PUT");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            setupClient(conn);
            Log.d(TAG, "importTable: url=" + url + ", data.len=" + data.length +
                    " B, conn=" + conn.hashCode());

            long duration = System.currentTimeMillis();
            BufferedOutputStream out = null;
            try {
                out = new BufferedOutputStream(conn.getOutputStream());
                out.write(data);
                out.flush();
            }
            finally {
                IOUtils.closeQuietly(out);
            }
            duration = System.currentTimeMillis() - duration;

            int responseCode = conn.getResponseCode();
            Log.d(TAG, "status code=" + responseCode +
                    " (" + conn.getResponseMessage() + "), " +
                    "duration=" + ((float)duration / 1000.f) + " secs, " +
                    "conn=" + conn.hashCode());
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new ApiError("importTable: url=" + url +
                        ", status code=" + responseCode +
                        " (" + conn.getResponseMessage() +")" +
                        ", conn=" + conn.hashCode());
            }
            return IOUtils.toString(conn.getInputStream());
        }
        finally {
            IOUtils.closeQuietly(conn.getInputStream());
        }
    }
}
