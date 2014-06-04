package com.treasuredata.android;

import android.content.Context;
import io.keen.client.android.AndroidKeenClientBuilder;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenLogging;
import io.keen.client.java.KeenProject;
import org.komamitsu.android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TreasureData {
    private static final String TAG = TreasureData.class.getSimpleName();
    private final KeenClient client;

    public TreasureData(Context context, String apiKey) {
        AndroidKeenClientBuilder builder = new AndroidKeenClientBuilder(context);
        builder.setHttpHandler(new TDHttpHandler(apiKey));
        client = builder.build();
        client.setDebugMode(true);
        KeenProject project = new KeenProject("_treasure data_", "dummy_write_key", "dummy_read_key");
        client.setDefaultProject(project);
    }

    public void enableLogging() {
        KeenLogging.enableLogging();
    }

    public void disableLogging() {
        KeenLogging.disableLogging();
    }

    public void event(String database, String table, String key, String value) {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(key, value);
        event(database, table, record);
    }

    public void event(String database, String table, Map<String, Object> record) {
        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        client.addEvent(sb.toString(), record, null);
    }

    public void upload() {
        Log.i(TAG, "upload, active?:" + client.isActive());
        client.sendQueuedEvents();
    }
}
