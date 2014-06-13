package com.treasuredata.android;

import android.content.Context;
import io.keen.client.java.KeenCallback;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class TreasureData {
    private static final String TAG = TreasureData.class.getSimpleName();
    private static final String VERSION = "0.1.0";
    private static final String LABEL_ADD_EVENT = "addEvent";
    private static final String LABEL_UPLOAD_EVENTS = "uploadEvents";
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("^[0-9a-z_]{3,255}$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[0-9a-z_]{3,255}$");
    static {
        TDHttpHandler.VERSION = TreasureData.VERSION;
    }

    private TDClient client;
    private volatile TDCallback addEventCallBack;
    private volatile TDCallback uploadEventsCallBack;
    private volatile KeenCallback addEventKeenCallBack = createKeenCallback(LABEL_ADD_EVENT, null);
    private volatile KeenCallback uploadEventsKeenCallBack = createKeenCallback(LABEL_UPLOAD_EVENTS, null);;

    public TreasureData(Context context, String apiKey) throws IOException {
        if (apiKey == null && TDClient.getDefaultApiKey() == null) {
            throw new IllegalStateException("initializeApiKey() hasn't called yet");
        }
        client = new TDClient(context, apiKey);
    }

    public TreasureData(Context context) throws IOException {
        this(context, null);
    }

    public static void enableLogging() {
        TDLogging.enableLogging();
    }

    public static void disableLogging() {
        TDLogging.disableLogging();
    }

    public static void initializeApiEndpoint(String apiEndpoint) {
        TDClient.setApiEndpoint(apiEndpoint);
    }

    public static void initializeDefaultApiKey(String defaultApiKey) {
        TDClient.setDefaultApiKey(defaultApiKey);
    }

    public static void enableEventCompression() {
        TDHttpHandler.enableEventCompression();
    }

    public static void disableEventCompression() {
        TDHttpHandler.disableEventCompression();
    }

    public synchronized void setAddEventCallBack(TDCallback callBack) {
        this.addEventCallBack = callBack;
        this.addEventKeenCallBack = createKeenCallback(LABEL_ADD_EVENT, callBack);
    }

    public TDCallback getAddEventCallBack() {
        return this.addEventCallBack;
    }

    public synchronized void setUploadEventsCallBack(TDCallback callBack) {
        this.uploadEventsCallBack = callBack;
        this.uploadEventsKeenCallBack = createKeenCallback(LABEL_UPLOAD_EVENTS, callBack);
    }

    public TDCallback getUploadEventsCallBack() {
        return this.uploadEventsCallBack;
    }

    public void addEvent(String database, String table, String key, Object value) {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(key, value);
        addEvent(database, table, record);
    }

    public void addEvent(String database, String table, Map<String, Object> record) {
        if (!(DATABASE_NAME_PATTERN.matcher(database).find() && TABLE_NAME_PATTERN.matcher(table).find())) {
            String errmsg = String.format("database and table need to be consist of lower letters, numbers or '_': database=%s, table=%s", database, table);
            if (TDLogging.isEnabled())
                Log.e(TAG, errmsg);
            addEventCallBack.onError(new IllegalArgumentException(errmsg));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        client.queueEvent(null, sb.toString(), record, null, this.addEventKeenCallBack);
    }

    public void uploadEvents() {
        client.sendQueuedEventsAsync(null, uploadEventsKeenCallBack);
    }

    private static KeenCallback createKeenCallback(final String methodName, final TDCallback callback) {
       KeenCallback keenCallback = new KeenCallback() {
            @Override
            public void onSuccess() {
                if (callback != null) {
                    callback.onSuccess();
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (TDLogging.isEnabled())
                    Log.e(TAG, methodName + " failed: " + e.getMessage());

                if (callback != null) {
                    callback.onError(e);
                }
            }
        };
        return keenCallback;
    }

    public void setDebugMode(boolean debug) {
        client.setDebugMode(debug);
    }

    // Only for testing
    @Deprecated
    TreasureData() {
    }

    @Deprecated
    void setClient(TDClient mockClient) {
        this.client = mockClient;
    }
}
