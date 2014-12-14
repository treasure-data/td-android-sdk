package com.treasuredata.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import io.keen.client.java.KeenCallback;
import io.keen.client.java.KeenClient;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public class TreasureData {
    private static final String TAG = TreasureData.class.getSimpleName();
    private static final String VERSION = "0.1.6";
    private static final String LABEL_ADD_EVENT = "addEvent";
    private static final String LABEL_UPLOAD_EVENTS = "uploadEvents";
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("^[0-9a-z_]{3,255}$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[0-9a-z_]{3,255}$");
    private static final String SHARED_PREF_NAME = "td_sdk_info";
    private static final String SHARED_PREF_KEY_UUID = "uuid";
    private static final String SHARED_PREF_KEY_FIRST_RUN = "first_run";
    private static final String EVENT_KEY_UUID = "td_uuid";
    private static final String EVENT_KEY_SESSION_ID = "td_session_id";
    private static final String EVENT_KEY_SESSION_EVENT = "td_session_event";
    private static final String EVENT_KEY_BOARD = "td_board";
    private static final String EVENT_KEY_BRAND = "td_brand";
    private static final String EVENT_KEY_DEVICE = "td_device";
    private static final String EVENT_KEY_DISPLAY = "td_display";
    private static final String EVENT_KEY_MODEL = "td_model";
    private static final String EVENT_KEY_OS_VER = "td_os_ver";
    private static final String EVENT_KEY_OS_TYPE = "td_os_type";
    private static final String OS_TYPE = "Android";

    static {
        TDHttpHandler.VERSION = TreasureData.VERSION;
    }

    private static TreasureData sharedInstance;

    private TDClient client;    // This should be `final' but isn't because of testability...
    private volatile String defaultDatabase;
    private volatile TDCallback addEventCallBack;
    private volatile TDCallback uploadEventsCallBack;
    private volatile KeenCallback addEventKeenCallBack = createKeenCallback(LABEL_ADD_EVENT, null);
    private volatile KeenCallback uploadEventsKeenCallBack = createKeenCallback(LABEL_UPLOAD_EVENTS, null);
    private volatile boolean autoAppendUniqId;
    private volatile boolean autoAppendModelInformation;
    private String uuid;    // This should be `final' but isn't because of testability...
    private volatile String sessionId;

    public static TreasureData initializeSharedInstance(Context context, String apiKey) {
        sharedInstance = new TreasureData(context, apiKey);
        return sharedInstance;
    }

    public static TreasureData initializeSharedInstance(Context context) {
        return initializeSharedInstance(context, null);
    }

    public static TreasureData sharedInstance() {
        if (sharedInstance == null) {
            Log.w(TAG, "sharedInstance is initialized properly");
            return new NullTreasureData();
        }
        return sharedInstance;
    }

    private SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getUUID(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            String uuid = sharedPreferences.getString(SHARED_PREF_KEY_UUID, null);
            if (uuid == null) {
                uuid = UUID.randomUUID().toString();
                sharedPreferences.edit().putString(SHARED_PREF_KEY_UUID, uuid).commit();
            }
            return uuid;
        }
    }

    public boolean isFirstRun(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            return sharedPreferences.getBoolean(SHARED_PREF_KEY_FIRST_RUN, true);
        }
    }

    public void clearFirstRun(Context context) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            sharedPreferences.edit().putBoolean(SHARED_PREF_KEY_FIRST_RUN, false).commit();
        }
    }

    public TreasureData(Context context, String apiKey) {
        Context applicationContext = context.getApplicationContext();
        uuid = getUUID(applicationContext);

        if (apiKey == null && TDClient.getDefaultApiKey() == null) {
            Log.e(TAG, "initializeApiKey() hasn't called yet");
            return;
        }

        try {
            client = new TDClient(applicationContext.getApplicationContext(), apiKey);
        } catch (IOException e) {
            Log.e(TAG, "Failed to construct TreasureData object", e);
        }
    }

    public TreasureData(Context context) {
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

    public static void initializeEncryptionKey(String encryptionKey) {
        TDClient.setEncryptionKey(encryptionKey);
    }

    public static void enableEventCompression() {
        TDHttpHandler.enableEventCompression();
    }

    public static void disableEventCompression() {
        TDHttpHandler.disableEventCompression();
    }

    public void setDefaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
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
        addEventWithCallback(database, table, key, value, null);
    }

    public void addEvent(String database, String table, Map<String, Object> record) {
        addEventWithCallback(database, table, record, null);
    }

    public void addEventWithCallback(String database, String table, String key, Object value, TDCallback callback) {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(key, value);
        addEventWithCallback(database, table, record, callback);
    }

    public void addEventWithCallback(String database, String table, Map<String, Object> record, TDCallback callback) {
        if (client == null) {
            Log.w(TAG, "TDClient is null");
            return;
        }

        if (callback == null) {
            callback = addEventCallBack;
        }

        if (sessionId != null) {
            appendSessionId(record);
        }

        if (autoAppendUniqId) {
            appendUniqId(record);
        }

        if (autoAppendModelInformation) {
            appendModelInformation(record);
        }

        if (!(DATABASE_NAME_PATTERN.matcher(database).find() && TABLE_NAME_PATTERN.matcher(table).find())) {
            String errMsg = String.format("database and table need to be consist of lower letters, numbers or '_': database=%s, table=%s", database, table);
            if (TDLogging.isEnabled())
                Log.e(TAG, errMsg);
            callback.onError(KeenClient.ERROR_CODE_INVALID_PARAM, new IllegalArgumentException(errMsg));
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        client.queueEvent(null, sb.toString(), record, null, createKeenCallback(LABEL_ADD_EVENT, callback));
    }

    public void addEvent(String table, String key, Object value) {
        addEvent(defaultDatabase, table, key, value);
    }

    public void addEvent(String table, Map<String, Object> record) {
        addEvent(defaultDatabase, table, record);
    }

    public void addEventWithCallback(String table, String key, Object value, TDCallback callback) {
        addEventWithCallback(defaultDatabase, table, key, value, callback);
    }

    public void addEventWithCallback(String table, Map<String, Object> record, TDCallback callback) {
        addEventWithCallback(defaultDatabase, table,  record, callback);
    }

    public void uploadEvents() {
        uploadEventsWithCallback(null);
    }

    public void uploadEventsWithCallback(TDCallback callback) {
        if (client == null) {
            Log.w(TAG, "TDClient is null");
            return;
        }

        if (callback == null) {
            callback = uploadEventsCallBack;
        }
        client.sendQueuedEventsAsync(null, createKeenCallback(LABEL_UPLOAD_EVENTS, callback));
    }

    private static KeenClient.KeenCallbackWithErrorCode createKeenCallback(final String methodName, final TDCallback callback) {
        KeenClient.KeenCallbackWithErrorCode keenCallback = new KeenClient.KeenCallbackWithErrorCode() {
            private String currentErrorCode;

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
                    callback.onError(getErrorCode(), e);
                }
            }

           @Override
           public void setErrorCode(String errorCode) {
               this.currentErrorCode = errorCode;
           }

           @Override
           public String getErrorCode() {
               return this.currentErrorCode;
           }
        };
        return keenCallback;
    }

    public void setDebugMode(boolean debug) {
        if (client == null) {
            Log.w(TAG, "TDClient is null");
            return;
        }

        client.setDebugMode(debug);
    }

    public void appendSessionId(Map<String, Object> record) {
        record.put(EVENT_KEY_SESSION_ID, sessionId);
    }

    public void appendUniqId(Map<String, Object> record) {
        record.put(EVENT_KEY_UUID, uuid);
    }

    public void appendModelInformation(Map<String, Object> record) {
        record.put(EVENT_KEY_BOARD, Build.BOARD);
        record.put(EVENT_KEY_BRAND, Build.BRAND);
        record.put(EVENT_KEY_DEVICE, Build.DEVICE);
        record.put(EVENT_KEY_DISPLAY, Build.DISPLAY);
        record.put(EVENT_KEY_DEVICE, Build.DEVICE);
        record.put(EVENT_KEY_MODEL, Build.MODEL);
        record.put(EVENT_KEY_OS_VER, Build.VERSION.SDK_INT);
        record.put(EVENT_KEY_OS_TYPE, OS_TYPE);
    }

    public void disableAutoAppendUniqId() {
        this.autoAppendUniqId = false;
    }

    public void enableAutoAppendUniqId() {
        this.autoAppendUniqId = true;
    }

    public void disableAutoAppendModelInformation() {
        this.autoAppendModelInformation = false;
    }

    public void enableAutoAppendModelInformation() {
        this.autoAppendModelInformation = true;
    }

    public void disableAutoRetryUploading() {
        client.disableAutoRetryUploading();
    }

    public void enableAutoRetryUploading() {
        client.enableAutoRetryUploading();
    }

    public void startSession(String table) {
        startSession(defaultDatabase, table);
    }

    public void startSession(String database, String table) {
        sessionId = UUID.randomUUID().toString();
        addEvent(database, table, EVENT_KEY_SESSION_EVENT, "start");
    }

    public void endSession(String table) {
        endSession(defaultDatabase, table);
    }

    public void endSession(String database, String table) {
        addEvent(database, table, EVENT_KEY_SESSION_EVENT, "end");
        sessionId = null;
    }

    // Only for testing
    @Deprecated
    TreasureData() {
    }

    @Deprecated
    void setClient(TDClient mockClient) {
        this.client = mockClient;
    }

    static class NullTreasureData extends TreasureData {
        @Override
        public synchronized void setAddEventCallBack(TDCallback callBack) {
        }

        @Override
        public TDCallback getAddEventCallBack() {
            return null;
        }

        @Override
        public synchronized void setUploadEventsCallBack(TDCallback callBack) {
        }

        @Override
        public TDCallback getUploadEventsCallBack() {
            return null;
        }

        @Override
        public void addEvent(String database, String table, String key, Object value) {
        }

        @Override
        public void addEvent(String database, String table, Map<String, Object> record) {
        }

        @Override
        public void addEventWithCallback(String database, String table, String key, Object value, TDCallback callback) {
        }

        @Override
        public void addEventWithCallback(String database, String table, Map<String, Object> record, TDCallback callback) {
        }

        @Override
        public void uploadEvents() {
        }

        @Override
        public void uploadEventsWithCallback(TDCallback callback) {
        }

        @Override
        public void setDebugMode(boolean debug) {
        }

        @Override
        public void disableAutoAppendUniqId() {
        }

        @Override
        public void enableAutoAppendUniqId() {
        }

        @Override
        public void disableAutoAppendModelInformation() {
        }

        @Override
        public void enableAutoAppendModelInformation() {
        }

        @Override
        public void enableAutoRetryUploading() {
        }

        @Override
        public void disableAutoRetryUploading() {
        }

        @Override
        public void appendUniqId(Map<String, Object> record) {
        }

        @Override
        public void appendModelInformation(Map<String, Object> record) {
        }

        @Override
        public boolean isFirstRun(Context context) {
            return false;
        }

        @Override
        public void clearFirstRun(Context context) {
        }

        @Override
        public String getUUID(Context context) {
            return null;
        }

        @Override
        public void setDefaultDatabase(String defaultDatabase) {
        }

        @Override
        public void addEvent(String table, String key, Object value) {
        }

        @Override
        public void addEvent(String table, Map<String, Object> record) {
        }

        @Override
        public void addEventWithCallback(String table, String key, Object value, TDCallback callback) {
        }

        @Override
        public void addEventWithCallback(String table, Map<String, Object> record, TDCallback callback) {
        }

        @Override
        public void appendSessionId(Map<String, Object> record) {
        }

        @Override
        public void startSession(String table) {
        }

        @Override
        public void startSession(String database, String table) {
        }

        @Override
        public void endSession(String table) {
        }

        @Override
        public void endSession(String database, String table) {
        }
    }
}
