package com.treasuredata.android;

import android.content.Context;
import io.keen.client.android.TDClient;
import io.keen.client.android.exceptions.KeenException;
import io.keen.client.android.exceptions.KeenInitializationException;

import java.util.HashMap;
import java.util.Map;

public class TreasureData {
    private final TDClient client;

    public TreasureData(Context context, String apiKey) throws TDException {
        try {
            client = new TDClient(context, apiKey);
        } catch (KeenInitializationException e) {
            throw new TDException(e);
        }
    }

    public void event(String database, String table, String key, String value) throws TDException {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(key, value);
        event(database, table, record);
    }

    public void event(String database, String table, Map<String, Object> record) throws TDException {
        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        try {
            client.addEvent(sb.toString(), record, null);
        } catch (KeenException e) {
            throw new TDException(e);
        }
    }

    public void upload(final UploadFinishedCallback callback) {
        client.upload(callback);
    }

}
