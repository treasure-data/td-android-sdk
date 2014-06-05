package com.treasuredata.android;

import android.content.Context;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class TreasureData {
    private static final String TAG = TreasureData.class.getSimpleName();
    private final TDClient client;

    public TreasureData(Context context, String apiKey) throws IOException {
        client = new TDClient(context, apiKey);
    }

    public static void enableLogging() {
        TDClient.enableLogging();
    }

    public static void disableLogging() {
        TDClient.disableLogging();
    }

    public void event(String database, String table, String key, String value) {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(key, value);
        event(database, table, record);
    }

    public void event(String database, String table, Map<String, Object> record) {
        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        client.queueEvent(sb.toString(), record, null);
    }

    public void upload() {
        client.sendQueuedEventsAsync();
    }
}
