package com.treasuredata.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import io.keen.client.java.KeenClient;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

public class TreasureData {
    private static final String TAG = TreasureData.class.getSimpleName();
    private static final String VERSION = "0.1.11";
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
    private static final String EVENT_KEY_APP_VER = "td_app_ver";
    private static final String EVENT_KEY_APP_VER_NUM = "td_app_ver_num";
    private static final String EVENT_KEY_LOCALE_COUNTRY = "td_locale_country";
    private static final String EVENT_KEY_LOCALE_LANG = "td_locale_lang";
    private static final String EVENT_KEY_SERVERSIDE_UPLOAD_TIMESTAMP = "#SSUT";
    private static final String OS_TYPE = "Android";

    static {
        TDHttpHandler.VERSION = TreasureData.VERSION;
    }

    private static TreasureData sharedInstance;
    private final static WeakHashMap<Context, Session> sessions = new WeakHashMap<Context, Session>();

    private final Context context;
    private final TDClient client;
    private final String uuid;
    private volatile String defaultDatabase;
    private volatile TDCallback addEventCallBack;
    private volatile TDCallback uploadEventsCallBack;
    private volatile boolean autoAppendUniqId;
    private volatile boolean autoAppendModelInformation;
    private volatile boolean autoAppendAppInformation;
    private volatile boolean autoAppendLocaleInformation;
    private final String appVersion;
    private final int appVersionNumber;
    private volatile boolean serverSideUploadTimestamp;
    private Session session = new Session();

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
        this.context = applicationContext;
        uuid = getUUID(applicationContext);

        TDClient client = null;
        if (apiKey == null && TDClient.getDefaultApiKey() == null) {
            Log.e(TAG, "initializeApiKey() hasn't called yet");
        }
        else {
            try {
                client = new TDClient(applicationContext, apiKey);
            } catch (IOException e) {
                Log.e(TAG, "Failed to construct TreasureData object", e);
            }
        }

        String appVersion = "";
        int appVersionNumber = 0;
        try {
            PackageInfo pkgInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            appVersion = pkgInfo.versionName;
            appVersionNumber = pkgInfo.versionCode;
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to get package information", e);
        }
        this.appVersion = appVersion;
        this.appVersionNumber = appVersionNumber;

        this.client = client;
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
    }

    public TDCallback getAddEventCallBack() {
        return this.addEventCallBack;
    }

    public synchronized void setUploadEventsCallBack(TDCallback callBack) {
        this.uploadEventsCallBack = callBack;
    }

    public TDCallback getUploadEventsCallBack() {
        return this.uploadEventsCallBack;
    }

    public void addEvent(String database, String table, Map<String, Object> record) {
        addEventWithCallback(database, table, record, null);
    }

    private void handleParamError(TDCallback callback, String errMsg) {
        if (TDLogging.isEnabled())
            Log.e(TAG, errMsg);

        if (callback != null) {
            callback.onError(KeenClient.ERROR_CODE_INVALID_PARAM, new IllegalArgumentException(errMsg));
        }
    }

    public void addEventWithCallback(String database, String table, Map<String, Object> record, TDCallback callback) {
        if (client == null) {
            Log.w(TAG, "TDClient is null");
            return;
        }

        if (callback == null) {
            callback = addEventCallBack;
        }

        if (database == null) {
            handleParamError(callback, "database is null");
            return;
        }

        if (table == null) {
            handleParamError(callback, "table is null");
            return;
        }

        if (record == null) {
            record = new HashMap<String, Object>();
        }

        appendSessionId(record);

        if (autoAppendUniqId) {
            appendUniqId(record);
        }

        if (autoAppendModelInformation) {
            appendModelInformation(record);
        }

        if (autoAppendAppInformation) {
            appendAppInformation(record);
        }

        if (autoAppendLocaleInformation) {
            appendLocaleInformation(record);
        }

        if (!(DATABASE_NAME_PATTERN.matcher(database).find() && TABLE_NAME_PATTERN.matcher(table).find())) {
            String errMsg = String.format("database and table need to be consist of lower letters, numbers or '_': database=%s, table=%s", database, table);
            handleParamError(callback, errMsg);
            return;
        }

        if (serverSideUploadTimestamp) {
            record.put(EVENT_KEY_SERVERSIDE_UPLOAD_TIMESTAMP, true);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        client.queueEvent(null, sb.toString(), record, null, createKeenCallback(LABEL_ADD_EVENT, callback));
    }

    public void addEvent(String table, Map<String, Object> record) {
        addEvent(defaultDatabase, table, record);
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
        return new KeenClient.KeenCallbackWithErrorCode() {
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
    }

    public void appendSessionId(Map<String, Object> record) {
        String instanceSessionId = session.getId();
        String globalSessionId = null;
        Session globalSession = getSession(context);
        if (globalSession != null) {
            globalSessionId = globalSession.getId();
        }

        if (globalSession != null && instanceSessionId != null) {
            Log.w(TAG, "instance method TreasureData#startSession(String) and static method TreasureData.startSession(android.content.Context) are both enabled, but the instance method will be ignored.");
        }

        if (instanceSessionId != null) {
            record.put(EVENT_KEY_SESSION_ID, instanceSessionId);
        }

        if (globalSessionId != null) {
            record.put(EVENT_KEY_SESSION_ID, globalSessionId);
        }
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

    public void appendAppInformation(Map<String, Object> record) {
        record.put(EVENT_KEY_APP_VER, appVersion);
        record.put(EVENT_KEY_APP_VER_NUM, appVersionNumber);
    }

    public void appendLocaleInformation(Map<String, Object> record) {
        Locale locale = context.getResources().getConfiguration().locale;
        record.put(EVENT_KEY_LOCALE_COUNTRY, locale.getCountry());
        record.put(EVENT_KEY_LOCALE_LANG, locale.getLanguage());
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

    public void disableAutoAppendAppInformation() {
        this.autoAppendAppInformation = false;
    }

    public void enableAutoAppendAppInformation() {
        this.autoAppendAppInformation = true;
    }

    public void disableAutoAppendLocaleInformation() {
        this.autoAppendLocaleInformation = false;
    }

    public void enableAutoAppendLocaleInformation() {
        this.autoAppendLocaleInformation = true;
    }

    public void disableAutoRetryUploading() {
        client.disableAutoRetryUploading();
    }

    public void enableAutoRetryUploading() {
        client.enableAutoRetryUploading();
    }

    private static Session getSession(Context context) {
        if (context == null) {
            Log.w(TAG, "context is null. It's an unit test, right?");
            return null;
        }
        Context applicationContext = context.getApplicationContext();
        return sessions.get(applicationContext);
    }

    public void startSession(String table) {
        startSession(defaultDatabase, table);
    }

    public void startSession(String database, String table) {
        session.start();
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(EVENT_KEY_SESSION_EVENT, "start");
        addEvent(database, table, record);
    }

    public static void startSession(Context context) {
        Session session = getSession(context);
        if (session == null) {
            session = new Session();
            sessions.put(context.getApplicationContext(), session);
        }
        session.start();
    }

    public void endSession(String table) {
        endSession(defaultDatabase, table);
    }

    public void endSession(String database, String table) {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(EVENT_KEY_SESSION_EVENT, "end");
        addEvent(database, table, record);
        session.finish();
    }

    public static void endSession(Context context) {
        Session session = getSession(context);
        if (session != null) {
            session.finish();
        }
    }

    public void enableServerSideUploadTimestamp() {
        serverSideUploadTimestamp = true;
    }

    public void disableServerSideUploadTimestamp() {
        serverSideUploadTimestamp = false;
    }

    // Only for testing
    @Deprecated
    TreasureData(Context context, TDClient mockClient, String uuid) {
        this.context = context;
        this.client = mockClient;
        this.uuid = uuid;
        this.appVersion = "3.1.4";
        this.appVersionNumber = 42;
    }

    static class NullTreasureData extends TreasureData {
        public NullTreasureData() {
            super(null, null, null);
        }

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
        public void addEvent(String database, String table, Map<String, Object> record) {
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
        public void addEvent(String table, Map<String, Object> record) {
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

        @Override
        public void enableServerSideUploadTimestamp() {
        }

        @Override
        public void disableServerSideUploadTimestamp() {
        }
    }
}
