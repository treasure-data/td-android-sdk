package io.keen.client.android;

import android.content.Context;
import io.keen.client.android.exceptions.KeenInitializationException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class TDClient extends KeenClient {
    private final String apiKey;

    public TDClient(Context context, String apiKey) throws KeenInitializationException {
        super(context, "_treasure data_", "dummy_write_key", "dummy_read_key");
        this.apiKey = apiKey;
    }

    @Override
    HttpURLConnection sendEvents(Map<String, List<Map<String, Object>>> requestDict) throws IOException {
        // just using basic JDK HTTP library
        // TODO use apiEndpoint
        String urlString = String.format("%s/event", "https://in.treasuredata.com/android/v3");
        URL url = new URL(urlString);

        // set up the POST
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("X-TD-Write-Key", apiKey);
        connection.setRequestProperty("X-TD-Data-Type", "k");

        // we're writing
        connection.setDoOutput(true);
        OutputStream out = connection.getOutputStream();
        // write JSON to the output stream
        MAPPER.writeValue(out, requestDict);
        out.close();
        return connection;
    }
}
