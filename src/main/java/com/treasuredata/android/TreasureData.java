package com.treasuredata.android;

import android.app.Activity;
import android.app.Application;
import android.app.UiModeManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import com.treasuredata.android.billing.internal.Purchase;
import com.treasuredata.android.billing.internal.PurchaseEventActivityLifecycleTracker;
import com.treasuredata.android.cdp.CDPClient;
import com.treasuredata.android.cdp.CDPClientImpl;
import com.treasuredata.android.cdp.FetchUserSegmentsCallback;
import io.keen.client.java.KeenClient;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import static android.content.Context.UI_MODE_SERVICE;

public class TreasureData implements CDPClient {
    private static final String TAG = TreasureData.class.getSimpleName();
    private static final String VERSION = "0.6.0";
    private static final String LABEL_ADD_EVENT = "addEvent";
    private static final String LABEL_UPLOAD_EVENTS = "uploadEvents";
    private static final Pattern DATABASE_NAME_PATTERN = Pattern.compile("^[0-9a-z_]{3,255}$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[0-9a-z_]{3,255}$");
    private static final String SHARED_PREF_NAME = "td_sdk_info";
    private static final String SHARED_PREF_KEY_UUID = "uuid";
    private static final String SHARED_PREF_VERSION_KEY = "version";
    private static final String SHARED_PREF_BUILD_KEY = "build";
    private static final String SHARED_PREF_KEY_FIRST_RUN = "first_run";
    private static final String SHARED_PREF_APP_LIFECYCLE_EVENT_ENABLED = "app_lifecycle_event_enabled";
    private static final String SHARED_PREF_IAP_EVENT_ENABLED = "iap_event_enabled";
    private static final String SHARED_PREF_CUSTOM_EVENT_ENABLED = "custom_event_enabled";
    private static final String SHARED_PREF_KEY_IS_UNITY = "TDIsUnity";
    private static final String SHARED_PREF_KEY_ADVERTISING_ID = "advertising_id";
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
    private static final String EVENT_KEY_PREV_APP_VER = "td_prev_app_ver";
    private static final String EVENT_KEY_PREV_APP_VER_NUM = "td_prev_app_ver_num";
    private static final String EVENT_KEY_LOCALE_COUNTRY = "td_locale_country";
    private static final String EVENT_KEY_LOCALE_LANG = "td_locale_lang";
    private static final String EVENT_KEY_ADVERTISING_IDENTIFIER = "td_maid";
    private static final String EVENT_KEY_EVENT = "td_android_event";
    private static final String EVENT_KEY_UNITY_EVENT = "td_unity_event";
    private static final String EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE = "__is_app_lifecycle_event";
    private static final String EVENT_KEY_RESET_UUID_EVENT_PRIVATE = "__is_reset_uuid_event";
    private static final String EVENT_KEY_IN_APP_PURCHASE_EVENT_PRIVATE = "__is_in_app_purchase_event";
    private static final String EVENT_KEY_SERVERSIDE_UPLOAD_TIMESTAMP = "#SSUT";
    private static final String EVENT_DEFAULT_KEY_RECORD_UUID = "record_uuid";
    private static final String EVENT_APP_INSTALL = "TD_ANDROID_APP_INSTALL";
    private static final String EVENT_APP_OPEN = "TD_ANDROID_APP_OPEN";
    private static final String EVENT_APP_UPDATE = "TD_ANDROID_APP_UPDATE";
    private static final String EVENT_IAP = "TD_ANDROID_IN_APP_PURCHASE";
    private static final String EVENT_RESET_UUID = "forget_device_uuid";
    private static final String TD_DEFAULT_DATABASE = "td";
    private static final String TD_DEFAULT_TABLE = "td_android";

    static {
        TDHttpHandler.VERSION = TreasureData.VERSION;
    }

    private static Context applicationContext;
    private static volatile Executor executor;
    private static volatile TreasureData sharedInstance;
    private final static WeakHashMap<Context, Session> sessions = new WeakHashMap<Context, Session>();

    private final Context context;
    private final TDClient client;
    private String osType;
    private String uuid;
    private volatile String defaultDatabase;
    private volatile String defaultTable;
    private volatile TDCallback addEventCallBack;
    private volatile TDCallback uploadEventsCallBack;
    private volatile boolean autoAppendUniqId;
    private volatile boolean autoAppendModelInformation;
    private volatile boolean autoAppendAppInformation;
    private volatile boolean autoAppendLocaleInformation;
    private volatile boolean autoTrackAppInstalledEvent = true;
    private volatile boolean autoTrackAppOpenEvent = true;
    private volatile boolean autoTrackAppUpdatedEvent = true;
    private volatile boolean customEventEnabled = true;
    private volatile boolean appLifecycleEventEnabled;
    private volatile boolean inAppPurchaseEventEnabled;
    private static volatile long sessionTimeoutMilli = Session.DEFAULT_SESSION_PENDING_MILLIS;
    private final String appVersion;
    private final int appVersionNumber;
    private volatile boolean serverSideUploadTimestamp;
    private volatile String serverSideUploadTimestampColumn;
    private Session session = new Session();
    private volatile String autoAppendRecordUUIDColumn;
    private volatile String autoAppendAdvertisingIdColumn;
    private volatile String advertisingId;
    private volatile GetAdvertisingIdAsyncTask getAdvertisingIdTask;
    private volatile Map<String, Map<String, Object>> defaultValues;

    private final AtomicBoolean isInAppPurchaseEventTracking = new AtomicBoolean(false);
    private CDPClientImpl cdpClientDelegate;
    private Debouncer debouncer;

    /**
     * Initialize shared instance with Treasure Data API key
     *
     * @param apiKey Treasure Data API key
     * @param context Context for Treasure Data shared instance
     * @return {@link TreasureData#sharedInstance()}
     */
    public static TreasureData initializeSharedInstance(Context context, String apiKey) {
        synchronized (TreasureData.class) {
            if (sharedInstance == null) {
                sharedInstance = new TreasureData(context, apiKey);
            }
        }
        return sharedInstance;
    }

    public static TreasureData initializeSharedInstance(Context context) {
        return initializeSharedInstance(context, null);
    }

    /**
     * The default singleton SDK instance.
     *
     * @return the shared instance
     */
    public static TreasureData sharedInstance() {
        if (sharedInstance == null) {
            synchronized (TreasureData.class) {
                if (sharedInstance == null) {
                    Log.w(TAG, "sharedInstance is initialized properly for testing only.");
                    return new NullTreasureData();
                }
            }
        }
        return sharedInstance;
    }

    public static Context getApplicationContext() {
        return applicationContext;
    }

    public static Executor getExecutor() {
        if (executor == null) { //Check for the first time
            synchronized (TreasureData.class) {
                if (executor == null) { //Check for the second time
                    //If there is no instance available. Create new one
                    executor = AsyncTask.SERIAL_EXECUTOR;
                }
            }
        }
        return executor;
    }

    private SharedPreferences getSharedPreference(Context context) {
        return context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Get UUID generated from TreasureData. The value will be set to `td_uuid` column for every events if `enableAutoAppendUniqId` is called.
     *
     * @return UUID value
     */
    public String getUUID() {
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

    /**
     * Reset UUID and send forget_device_uuid event with old uuid
     */
    public void resetUniqId() {
        String targetDatabase = TD_DEFAULT_DATABASE;
        if (defaultDatabase == null) {
            Log.w(TAG, "Default database is not set, forget_device_uuid event will be uploaded to " + TD_DEFAULT_DATABASE);
        } else {
            targetDatabase = defaultDatabase;
        }

        String targetTable = TD_DEFAULT_TABLE;
        if (defaultTable == null) {
            Log.w(TAG, "Default table is not set, forget_device_uuid event will be uploaded to " + TD_DEFAULT_TABLE);
        } else {
            targetTable = defaultTable;
        }

        // Send forget_device_uuid event
        Map record = new HashMap<String, Object>();
        uuid = getUUID();
        record.put(isOnUnity() ? EVENT_KEY_UNITY_EVENT : EVENT_KEY_EVENT, EVENT_RESET_UUID);
        record.put(EVENT_KEY_UUID, uuid);
        record.put(EVENT_KEY_RESET_UUID_EVENT_PRIVATE, true);
        addEvent(targetDatabase, targetTable, record);

        // Reset UUID
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            String uuid = UUID.randomUUID().toString();
            sharedPreferences.edit().putString(SHARED_PREF_KEY_UUID, uuid).commit();
        }
    }

    private String getAdvertisingIdFromSharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            return sharedPreferences.getString(SHARED_PREF_KEY_ADVERTISING_ID, null);
        }
    }

    private void setAdvertisingId(String advertisingId) {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            this.advertisingId = advertisingId;
            if (advertisingId == null) {
                sharedPreferences.edit().remove(SHARED_PREF_KEY_ADVERTISING_ID).commit();
            } else {
                sharedPreferences.edit().putString(SHARED_PREF_KEY_ADVERTISING_ID, advertisingId).commit();
            }
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
        applicationContext = context.getApplicationContext();
        this.context = context.getApplicationContext();
        this.uuid = getUUID();
        this.appLifecycleEventEnabled = getAppLifecycleEventEnabled();
        this.customEventEnabled = getCustomEventEnabled();
        this.inAppPurchaseEventEnabled = getInAppPurchaseEventEventEnabled();
        this.advertisingId = getAdvertisingIdFromSharedPreferences();

        UiModeManager uiModeManager = (UiModeManager) applicationContext.getSystemService(UI_MODE_SERVICE);
        if (uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION) {
            this.osType = "Android TV";
        } else {
            this.osType = "Android";
        }

        TDClient client = null;
        if (apiKey == null && TDClient.getDefaultApiKey() == null) {
            Log.e(TAG, "initializeApiKey() hasn't called yet");
        }
        else {
            try {
                client = new TDClient(apiKey, applicationContext.getCacheDir());
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            final Application application = (Application) applicationContext;
            application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                final AtomicBoolean trackedAppLifecycleEvents = new AtomicBoolean(false);

                @Override
                public void onActivityCreated(Activity activity, Bundle bundle) {
                    TreasureData.getExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            if (!trackedAppLifecycleEvents.getAndSet(true)
                                    && appLifecycleEventEnabled) {
                                trackApplicationLifecycleEvents();
                            }
                        }
                    });
                }

                @Override
                public void onActivityStarted(Activity activity) {

                }

                @Override
                public void onActivityResumed(Activity activity) {

                }

                @Override
                public void onActivityPaused(Activity activity) {

                }

                @Override
                public void onActivityStopped(Activity activity) {

                }

                @Override
                public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

                }

                @Override
                public void onActivityDestroyed(Activity activity) {

                }
            });
        }
    }

    private void trackApplicationLifecycleEvents() {

        String targetDatabase = TD_DEFAULT_DATABASE;
        if (defaultDatabase == null) {
            Log.w(TAG, "Default database is not set, app lifecycle events will be uploaded to " + TD_DEFAULT_DATABASE);
        } else {
            targetDatabase = defaultDatabase;
        }

        String targetTable = TD_DEFAULT_TABLE;
        if (defaultTable == null) {
            Log.w(TAG, "Default table is not set, auto app lifecycle events will be uploaded to " + TD_DEFAULT_TABLE);
        } else {
            targetTable = defaultTable;
        }

        String currentVersion = appVersion;
        int currentBuild = appVersionNumber;

        SharedPreferences sharedPreferences = getSharedPreference(context);
        String previousVersion = sharedPreferences.getString(SHARED_PREF_VERSION_KEY, null);
        int previousBuild = sharedPreferences.getInt(SHARED_PREF_BUILD_KEY, 0);

        Map<String, Object> record;
        if (autoTrackAppInstalledEvent && previousBuild == 0) {
            record = new HashMap<String, Object>();
            record.put(EVENT_KEY_EVENT, EVENT_APP_INSTALL);
            record.put(EVENT_KEY_APP_VER_NUM, currentBuild);
            record.put(EVENT_KEY_APP_VER, currentVersion);
            record.put(EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE, true);
            addEvent(targetDatabase, targetTable, record);
        }else if (autoTrackAppUpdatedEvent && currentBuild != previousBuild) {
            record = new HashMap<String, Object>();
            record.put(EVENT_KEY_EVENT, EVENT_APP_UPDATE);
            record.put(EVENT_KEY_APP_VER_NUM, currentBuild);
            record.put(EVENT_KEY_APP_VER, currentVersion);
            record.put(EVENT_KEY_PREV_APP_VER_NUM, previousBuild);
            record.put(EVENT_KEY_PREV_APP_VER, previousVersion);
            record.put(EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE, true);
            addEvent(targetDatabase, targetTable, record);
        }

        if (autoTrackAppOpenEvent) {
            record = new HashMap<String, Object>();
            record.put(EVENT_KEY_EVENT, EVENT_APP_OPEN);
            record.put(EVENT_KEY_APP_VER_NUM, currentBuild);
            record.put(EVENT_KEY_APP_VER, currentVersion);
            record.put(EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE, true);
            addEvent(targetDatabase, targetTable, record);
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(SHARED_PREF_BUILD_KEY, currentBuild);
        editor.putString(SHARED_PREF_VERSION_KEY, currentVersion);
        editor.apply();
    }

    private void trackPurchases(List<Purchase> purchases) {
        String targetDatabase = TD_DEFAULT_DATABASE;
        if (defaultDatabase == null) {
            Log.w(TAG, "Default database is not set, auto in app purchase events will be uploaded to " + TD_DEFAULT_DATABASE);
        } else {
            targetDatabase = defaultDatabase;
        }

        String targetTable = TD_DEFAULT_TABLE;
        if (defaultTable == null) {
            Log.w(TAG, "Default table is not set, auto in app purchase events will be uploaded to " + TD_DEFAULT_TABLE);
        } else {
            targetTable = defaultTable;
        }

        for (Purchase purchase : purchases) {
            Map<String, Object> record = new HashMap<>();
            record.put(EVENT_KEY_EVENT, EVENT_IAP);
            record.put(EVENT_KEY_IN_APP_PURCHASE_EVENT_PRIVATE, true);
            record.putAll(purchase.toRecord());
            addEvent(targetDatabase, targetTable, record);
        }
    }

    public TreasureData(Context context) {
        this(context, null);
    }

    /**
     * Enable client logging. Disabled by default.
     */
    public static void enableLogging() {
        TDLogging.enableLogging();
    }

    /**
     * Disable client's logging
     */
    public static void disableLogging() {
        TDLogging.disableLogging();
    }

    /**
     * Assign the target API endpoint, default is "https://us01.records.in.treasuredata.com".
     * Possible values:
     *    AWS East  https://https://us01.records.in.treasuredata.com
     * This have to be call before {@link TreasureData#initializeDefaultApiKey(String)}, otherwise it won't have effect.
     * @param apiEndpoint for the in effect endpoint.
     */
    public static void initializeApiEndpoint(String apiEndpoint) {
        TDClient.setApiEndpoint(apiEndpoint);
    }

    /**
     * Initialize `TreasureData.sharedInstance` with the current `apiEndpoint` configured via {@link TreasureData#initializeApiEndpoint(String)}
     *
     * @param defaultApiKey API Key (only requires `write-only`) for the in effect endpoint {@link TreasureData#initializeApiEndpoint(String)}
     */
    public static void initializeDefaultApiKey(String defaultApiKey) {
        TDClient.setDefaultApiKey(defaultApiKey);
    }

    /**
     * Encrypts the event data in the local persisted buffer.
     * This should be called only once and prior to any `addEvent...` call.
     *
     * @param encryptionKey encryption key to use
     */
    public static void initializeEncryptionKey(String encryptionKey) {
        TDClient.setEncryptionKey(encryptionKey);
    }

    /**
     * Event data will be compressed before uploading to server.
     */
    public static void enableEventCompression() {
        TDHttpHandler.enableEventCompression();
    }

    /**
     * Event data will be uploaded in it's full format.
     */
    public static void disableEventCompression() {
        TDHttpHandler.disableEventCompression();
    }

    /**
     * The destination database for events that doesn't specify one, default is "td".
     *
     * @param defaultDatabase name of the destination database
     */
    public void setDefaultDatabase(String defaultDatabase) {
        this.defaultDatabase = defaultDatabase;
    }

    /**
     * The destination table for events that doesn't specify one. Currently this also applied for automatically tracked events (if enabled): app lifecycle, IAP and audits, default is "td_android".
     *
     * @param defaultTable name of the destination table
     */
    public void setDefaultTable(String defaultTable) {
        this.defaultTable = defaultTable;
    }

    /**
     * Set callback for when add event either succeed or fail.
     *
     * @param callBack callback to be invoked
     */
    public synchronized void setAddEventCallBack(TDCallback callBack) {
        this.addEventCallBack = callBack;
    }

    /**
     * Get callback for when add event either succeed or fail if you had set one.
     *
     * @return callback to be invoked
     */
    public TDCallback getAddEventCallBack() {
        return this.addEventCallBack;
    }

    /**
     * Set callback for when upload events either succeed or fail.
     *
     * @param callBack callback to be invoked
     */
    public synchronized void setUploadEventsCallBack(TDCallback callBack) {
        this.uploadEventsCallBack = callBack;
    }

    /**
     * Get callback for when upload events either succeed or fail if you had set one.
     *
     * @return callback to be invoked
     */
    public TDCallback getUploadEventsCallBack() {
        return this.uploadEventsCallBack;
    }

    /**
     * Set max number of records can be sent per upload events call.
     *
     * @param maxUploadEventsAtOnce number of maximum events
     */
    public void setMaxUploadEventsAtOnce(int maxUploadEventsAtOnce) {
        client.setMaxUploadEventsAtOnce(maxUploadEventsAtOnce);
    }

    /**
     * Get max number of records can be sent per upload events call.
     *
     * @return number of maximum events
     */
    public int getMaxUploadEventsAtOnce() {
        return client.getMaxUploadEventsAtOnce();
    }

    /**
     * Track a new event
     *
     * @param database the event's destination database
     * @param table the event's destination table
     * @param record event data
     */
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

    /**
     * Track a new event
     *
     * @param database the event's destination database
     * @param table the event's destination table
     * @param origRecord event data
     * @param callback callback for when add event either succeed or fail. Default to {@link #setAddEventCallBack(TDCallback)}
     */
    public void addEventWithCallback(String database, String table, Map<String, Object> origRecord, TDCallback callback) {

        if(!isCustomEventEnabled() && isCustomEvent(origRecord)) {
            return;
        }

        if(!isAppLifecycleEventEnabled() && isAppLifecycleEvent(origRecord)) {
            return;
        }

        if(!isInAppPurchaseEventEnabled() && isInAppPurchaseEvent(origRecord)) {
            return;
        }

        // Remove private key
        origRecord.remove(EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE);
        origRecord.remove(EVENT_KEY_RESET_UUID_EVENT_PRIVATE);
        origRecord.remove(EVENT_KEY_IN_APP_PURCHASE_EVENT_PRIVATE);

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

        Map<String, Object> record = new HashMap<String, Object>();

        appendDefaultValues(database, table, record);

        if (origRecord != null) {
            record.putAll(origRecord);
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

        if (autoAppendRecordUUIDColumn != null) {
            appendRecordUUID(record);
        }

        if (autoAppendAdvertisingIdColumn != null) {
            appendAdvertisingIdentifier(record);
        }

        if (!(DATABASE_NAME_PATTERN.matcher(database).find() && TABLE_NAME_PATTERN.matcher(table).find())) {
            String errMsg = String.format("database and table need to be consist of lower letters, numbers or '_': database=%s, table=%s", database, table);
            handleParamError(callback, errMsg);
            return;
        }

        if (serverSideUploadTimestamp) {
            String columnName = serverSideUploadTimestampColumn;
            if (columnName != null) {
                record.put(EVENT_KEY_SERVERSIDE_UPLOAD_TIMESTAMP, columnName);
            }
            else {
                record.put(EVENT_KEY_SERVERSIDE_UPLOAD_TIMESTAMP, true);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(database).append(".").append(table);
        client.queueEvent(null, sb.toString(), record, null, createKeenCallback(LABEL_ADD_EVENT, callback));
    }

    /**
     * Track a new event to default database set by {@link #setDefaultDatabase(String)}
     *
     * @param table the event's destination table
     * @param record event data
     */
    public void addEvent(String table, Map<String, Object> record) {
        addEvent(defaultDatabase, table, record);
    }

    /**
     * Track a new event to default database set by {@link #setDefaultDatabase(String)}
     *
     * @param table the event's destination table
     * @param record event data
     * @param callback callback for when add event either succeed or fail. Default to {@link #setAddEventCallBack(TDCallback)}
     */
    public void addEventWithCallback(String table, Map<String, Object> record, TDCallback callback) {
        addEventWithCallback(defaultDatabase, table,  record, callback);
    }

    /**
     * Upload events with callback from {@link #setUploadEventsCallBack(TDCallback)}
     */
    public void uploadEvents() {
        uploadEventsWithCallback(null);
    }

    /**
     * Upload events with callback
     *
     * @param callback callback to be invoked
     */
    public void uploadEventsWithCallback(final TDCallback callback) {
        if (debouncer == null) {
            debouncer = new Debouncer(new Debouncer.Callback() {
                @Override
                public void call(Object key) {
                    if (client == null) {
                        Log.w(TAG, "TDClient is null");
                        return;
                    }

                    if (callback == null) {
                        client.sendQueuedEventsAsync(null, createKeenCallback(LABEL_UPLOAD_EVENTS, uploadEventsCallBack));
                    } else {
                        client.sendQueuedEventsAsync(null, createKeenCallback(LABEL_UPLOAD_EVENTS, callback));
                    }
                    debouncer = null;
                }
            }, 100);
        }
        debouncer.call("uploadEvents");
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

    private void appendDefaultValues(String database, String table, Map<String, Object> record) {
        if (defaultValues == null) return;

        String anyTableOrDatabaseKey = ".";
        if (defaultValues.containsKey(anyTableOrDatabaseKey)) record.putAll(defaultValues.get(anyTableOrDatabaseKey));
        String anyTableKey = String.format("%s.", database);
        if (defaultValues.containsKey(anyTableKey)) record.putAll(defaultValues.get(anyTableKey));
        String anyDatabaseKey = String.format(".%s", table);
        if (defaultValues.containsKey(anyDatabaseKey)) record.putAll(defaultValues.get(anyDatabaseKey));
        String tableKey = String.format("%s.%s", database, table);
        if (defaultValues.containsKey(tableKey)) record.putAll(defaultValues.get(tableKey));
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
        record.put(EVENT_KEY_OS_TYPE, this.osType);
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

    public void appendRecordUUID(Map<String, Object> record) {
        record.put(autoAppendRecordUUIDColumn, UUID.randomUUID().toString());
    }

    private void appendAdvertisingIdentifier(Map<String, Object> record) {
        updateAdvertisingId();

        if (advertisingId != null) {
            record.put(autoAppendAdvertisingIdColumn, advertisingId);
        }
    }

    /**
     * Enable app lifecycle tracking. This setting has no effect to custom tracking
     */
    public void enableAppLifecycleEvent() {
        enableAppLifecycleEvent(true);
    }

    /**
     * Disable app lifecycle tracking. This setting has no effect to custom tracking
     */
    public void disableAppLifecycleEvent() {
        enableAppLifecycleEvent(false);
    }

    /**
     * Toggle app lifecycle tracking. This setting has no effect to custom tracking
     *
     * @param enabled true : enabled, false : disabled
     */
    public void enableAppLifecycleEvent(boolean enabled) {
        this.appLifecycleEventEnabled = enabled;
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            sharedPreferences.edit().putBoolean(SHARED_PREF_APP_LIFECYCLE_EVENT_ENABLED, this.appLifecycleEventEnabled).commit();
        }
    }

    /**
     * Whether or not the app lifecycle tracking is enabled
     * @return true : enabled, false : disabled
     */
    public boolean isAppLifecycleEventEnabled() {
        return this.appLifecycleEventEnabled;
    }

    private boolean getAppLifecycleEventEnabled() {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            return sharedPreferences.getBoolean(SHARED_PREF_APP_LIFECYCLE_EVENT_ENABLED, false);
        }
    }

    /**
     * Enable tracking In App Purchase event automatically. This is disabled by default.
     */
    public void enableInAppPurchaseEvent() {
        enableInAppPurchaseEvent(true);
    }

    /**
     * Enable tracking In App Purchase event automatically. This is disabled by default.
     */
    public void disableInAppPurchaseEvent() {
        enableInAppPurchaseEvent(false);
    }

    /**
     * Toggle tracking In App Purchase event automatically. This is disabled by default.
     *
     * @param enabled true : enabled, false : disabled
     */
    public void enableInAppPurchaseEvent(boolean enabled) {
        this.inAppPurchaseEventEnabled = enabled;
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            sharedPreferences.edit().putBoolean(SHARED_PREF_IAP_EVENT_ENABLED, this.inAppPurchaseEventEnabled).commit();
        }

        if (inAppPurchaseEventEnabled && !isInAppPurchaseEventTracking.getAndSet(true)) {
            PurchaseEventActivityLifecycleTracker.track(
                    new PurchaseEventActivityLifecycleTracker.PurchaseEventListener() {
                        @Override
                        public void onTrack(List<Purchase> purchases) {
                            if (inAppPurchaseEventEnabled) {
                                trackPurchases(purchases);
                            }
                        }
                    });
        }
    }

    /**
     * Whether or not the In App Purchase tracking is enabled
     *
     * @return true : enabled, false : disabled
     */
    public boolean isInAppPurchaseEventEnabled() {
        return this.inAppPurchaseEventEnabled;
    }

    private boolean getInAppPurchaseEventEventEnabled() {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            return sharedPreferences.getBoolean(SHARED_PREF_IAP_EVENT_ENABLED, false);
        }
    }

    /**
     * Enable custom event tracking.
     */
    public void enableCustomEvent() {
        enableCustomEvent(true);
    }

    /**
     * Disable custom event tracking.
     */
    public void disableCustomEvent() {
        enableCustomEvent(false);
    }

    /**
     * Whether or not the custom event tracking is enable
     *
     * @return true : enable, false : disabled
     */
    public boolean isCustomEventEnabled() {
        return this.customEventEnabled;
    }

    /**
     * Toggle custom event availability
     *
     * @param enabled true : enabled, false : disabled
     */
    public void enableCustomEvent(boolean enabled) {
        this.customEventEnabled = enabled;
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            sharedPreferences.edit().putBoolean(SHARED_PREF_CUSTOM_EVENT_ENABLED,  this.customEventEnabled).commit();
        }
    }

    /**
     * This is required before calling {@link TreasureData#fetchUserSegments},
     * Note that this CDP Endpoint is independent and
     * not related to the API endpoint setup from {@link TreasureData#initializeApiEndpoint(String)}
     *
     * @param cdpEndpoint Known endpoints are:
     *                    AWS US                    https://cdp.in.treasuredata.com
     *                    AWS Tokyo                 https://cdp-tokyo.in.treasuredata.com
     *                    AWS EU                    https://cdp-eu01.in.treasuredata.com
     *                    AWS Asia Pacific (Seoul)  https://cdp-ap02.in.treasuredata.com
     *                    AWS Asia Pacific (Tokyo)  https://cdp-ap03.in.treasuredata.com
     */
    public void setCDPEndpoint(URI cdpEndpoint) {
        this.cdpClientDelegate = new CDPClientImpl(cdpEndpoint);
    }

    /**
     * This is required before calling {@link TreasureData#fetchUserSegments},
     * Note that this CDP Endpoint is independent and
     * not related to the API endpoint setup from {@link TreasureData#initializeApiEndpoint(String)}
     *
     * @param cdpEndpoint Known endpoints are:
     *                    AWS US:                   https://cdp.in.treasuredata.com
     *                    AWS Tokyo:                https://cdp-tokyo.in.treasuredata.com
     *                    AWS EU:                   https://cdp-eu01.in.treasuredata.com
     *                    AWS Asia Pacific (Seoul)  https://cdp-ap02.in.treasuredata.com
     *                    AWS Asia Pacific (Tokyo)  https://cdp-ap03.in.treasuredata.com
     * @throws URISyntaxException when the provided cdpEndpoint is not a valid URI
     */
    public void setCDPEndpoint(String cdpEndpoint) throws URISyntaxException {
        this.cdpClientDelegate = new CDPClientImpl(cdpEndpoint);
    }

    /**
     * @param profilesTokens list of Profile API Token that are defined on TreasureData
     * @param keys           lookup keyColumn values
     * @param callback       to receive the looked-up result
     */
    @Override
    public void fetchUserSegments(List<String> profilesTokens, Map<String, String> keys, FetchUserSegmentsCallback callback) {
        if (cdpClientDelegate != null) {
            cdpClientDelegate.fetchUserSegments(profilesTokens, keys, callback);
        } else {
            throw new IllegalStateException("`setCDPEndpoint()` is required before using `fetchUserSegments()`");
        }
    }

    private String defaultValueTableKey(String database, String table) {
        String _database = database == null ? "" : database;
        String _table = table == null ? "" : table;
        return String.format("%s.%s", _database, _table);
    }

    /**
     * Set default `value` for `key` in all new events targeting `database` and `table`.
     * When `database` and/or `table` parameters are null, the null parameter acts like a wild card that allows to set specified key value pair to new events added to any database (if `database` is null) and/or to any table (if `table` is null).
     * For example, if you pass null to both `database` and `table` parameters, all new events will have specified default value.
     * @param database the database to set default value to. If null, specified table of any database will have new events with the added default value.
     * @param table the table to set default value to. If null, any table of specified database will have new events with the added default value.
     * @param key the event's key that default value is set to, corresponding to column in table.
     * @param value default value for `key`
     */
    public void setDefaultValue(String database, String table, String key, Object value) {
        if (defaultValues == null) defaultValues = new HashMap();
        String tableKey = this.defaultValueTableKey(database, table);
        if (!defaultValues.containsKey(tableKey)) defaultValues.put(tableKey, new HashMap());
        Map tableMap = defaultValues.get(tableKey);
        tableMap.put(key, value);
    }

    /**
     * Get default value of `key` in all new events targeting `database` and `table`.
     * See setDefaultValue() for logic setting database and table.
     * @param database the database to get default value from. If null, get default value of specified table of any database.
     * @param table the table to get default value from. If null, get default value of any table of specified database.
     * @param key the event's key that default value is set to, corresponding to column in table.
     * @return default value for `key` for events targeting `database` and `table`.
     */
    public Object getDefaultValue(String database, String table, String key) {
        if (defaultValues == null) return null;
        String tableKey = this.defaultValueTableKey(database, table);
        if (!defaultValues.containsKey(tableKey)) return null;
        return defaultValues.get(tableKey).get(key);
    }

    /**
     * Remove default value of `key` in all new events targeting `database` and `table`.
     * See setDefaultValue() for logic setting database and table.
     * @param database the database to remove default value from. If null, specified table of any database will have new events without the default value.
     * @param table the table to remove default value from. If null, any table of specified database will have new events without the default value.
     * @param key the event's key that default value is set to, corresponding to column in table.
     */
    public void removeDefaultValue(String database, String table, String key) {
        if (defaultValues == null) return;
        String tableKey = this.defaultValueTableKey(database, table);
        if (!defaultValues.containsKey(tableKey)) return;
        defaultValues.get(tableKey).remove(key);
    }

    private boolean getCustomEventEnabled() {
        SharedPreferences sharedPreferences = getSharedPreference(context);
        synchronized (this) {
            this.customEventEnabled = sharedPreferences.getBoolean(SHARED_PREF_CUSTOM_EVENT_ENABLED, true);
            return this.customEventEnabled;
        }
    }

    private static boolean isCustomEvent(Map record) {
        return !record.containsKey(EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE)
                && !record.containsKey(EVENT_KEY_RESET_UUID_EVENT_PRIVATE)
                && !record.containsKey(EVENT_KEY_IN_APP_PURCHASE_EVENT_PRIVATE);
    }

    private static boolean isAppLifecycleEvent(Map record) {
        return record.containsKey(EVENT_KEY_APP_LIFECYCLE_EVENT_PRIVATE);
    }

    private static boolean isInAppPurchaseEvent(Map record) {
        return record.containsKey(EVENT_KEY_IN_APP_PURCHASE_EVENT_PRIVATE);
    }

    /**
     * Disable automatic tracking of event when app is installed
     */
    public void disableAppInstalledEvent() {
        this.autoTrackAppInstalledEvent = false;
    }

    /**
     * Disable automatic tracking of event when app is updated
     */
    public void disableAppUpdatedEvent() {
        this.autoTrackAppUpdatedEvent = false;
    }

    /**
     * Disable automatic tracking of event when app is opened
     */
    public void disableAppOpenEvent() {
        this.autoTrackAppOpenEvent = false;
    }

    /**
     * Disable the automatically appended `td_uuid` column.
     */
    public void disableAutoAppendUniqId() {
        this.autoAppendUniqId = false;
    }

    /**
     * Automatically append `td_uuid` column for every events. The value is randomly generated and persisted, it is shared across app launches and events. Basically, it is used to represent for a unique app installation instance.
     *
     * This is disabled by default.
     */
    public void enableAutoAppendUniqId() {
        this.autoAppendUniqId = true;
    }

    /**
     * Disable automatic tracking of model information
     */
    public void disableAutoAppendModelInformation() {
        this.autoAppendModelInformation = false;
    }

    /**
     * Enable automatic tracking of model information
     */
    public void enableAutoAppendModelInformation() {
        this.autoAppendModelInformation = true;
    }

    /**
     * Disable automatic tracking of app information
     */
    public void disableAutoAppendAppInformation() {
        this.autoAppendAppInformation = false;
    }

    /**
     * Enable automatic tracking of app information
     */
    public void enableAutoAppendAppInformation() {
        this.autoAppendAppInformation = true;
    }

    /**
     * Disable automatic tracking of locale information
     */
    public void disableAutoAppendLocaleInformation() {
        this.autoAppendLocaleInformation = false;
    }

    /**
     * Enable automatic tracking of locale information
     */
    public void enableAutoAppendLocaleInformation() {
        this.autoAppendLocaleInformation = true;
    }

    /**
     * Enable automatic tracking of advertising identifier
     */
    public void enableAutoAppendAdvertisingIdentifier() {
        enableAutoAppendAdvertisingIdentifier(EVENT_KEY_ADVERTISING_IDENTIFIER);
    }

    /**
     * Enable automatic tracking of advertising identifier with custom column name
     *
     * @param columnName column name for advertising id
     */
    public void enableAutoAppendAdvertisingIdentifier(String columnName) {
        if (columnName == null) {
            Log.w(TAG, "columnName must not be null");
            return;
        }
        autoAppendAdvertisingIdColumn = columnName;
        updateAdvertisingId();
    }

    /**
     * Disable automatic tracking of advertising identifier
     */
    public void disableAutoAppendAdvertisingIdentifier() {
        autoAppendAdvertisingIdColumn = null;
        setAdvertisingId(null);
        if (getAdvertisingIdTask != null) {
            getAdvertisingIdTask.cancel(true);
            getAdvertisingIdTask = null;
        }
    }

    private void updateAdvertisingId() {
        if (getAdvertisingIdTask != null) return;
        try {
            getAdvertisingIdTask = new GetAdvertisingIdAsyncTask(new GetAdvertisingIdAsyncTaskCallback() {
                @Override
                public void onGetAdvertisingIdAsyncTaskCompleted(String aid) {
                    getAdvertisingIdTask = null;
                    setAdvertisingId(aid);
                }
            });
            getAdvertisingIdTask.execute(context);
        } catch (Exception e) {
            Log.w(TAG, e.getMessage());
        }
    }

    /**
     * Disable automatic retry uploading. Once disabled, app will only attempt to upload events once per upload events call.
     */
    public void disableAutoRetryUploading() {
        client.disableAutoRetryUploading();
    }

    /**
     * Enable automatic retry uploading. Once enabled, app will keep retry to upload events until events are uploaded successfully.
     */
    public void enableAutoRetryUploading() {
        client.enableAutoRetryUploading();
    }

    /**
     * Set the timeout in milliseconds. If {@link TreasureData#startSession(Context)} is called during this timeout after {@link TreasureData#endSession(Context)} is called. App will start session with the same id as previous one.
     *
     * @param timeoutMilli timeout duration in milliseconds
     */
    public static void setSessionTimeoutMilli(long timeoutMilli)
    {
        sessionTimeoutMilli = timeoutMilli;
    }

    private static Session getSession(Context context) {
        if (context == null) {
            Log.w(TAG, "context is null. It's an unit test, right?");
            return null;
        }
        Context applicationContext = context.getApplicationContext();
        return sessions.get(applicationContext);
    }

    /**
     * Add new start session event to specified table set in the params and default database set in {@link #setDefaultDatabase(String)}
     *
     * @param table table to track session events
     */
    public void startSession(String table) {
        startSession(defaultDatabase, table);
    }

    /**
     * Add new start session event
     *
     * @param database database to track session events
     * @param table table to track session events
     */
    public void startSession(String database, String table) {
        session.start();
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(EVENT_KEY_SESSION_EVENT, "start");
        addEvent(database, table, record);
    }

    /**
     * Start tracking a global session
     *
     * @param context context of the global session
     */
    public static void startSession(Context context) {
        Session session = getSession(context);
        if (session == null) {
            session = new Session(sessionTimeoutMilli);
            sessions.put(context.getApplicationContext(), session);
        }
        session.start();
    }

    /**
     * Add new end session event to specified table set in the params and default database set in {@link #setDefaultDatabase(String)}
     *
     * @param table table to track session events
     */
    public void endSession(String table) {
        endSession(defaultDatabase, table);
    }

    /**
     * Add new end session event
     *
     * @param database database to track session events
     * @param table table to track session events
     */
    public void endSession(String database, String table) {
        HashMap<String, Object> record = new HashMap<String, Object>(1);
        record.put(EVENT_KEY_SESSION_EVENT, "end");
        addEvent(database, table, record);
        session.finish();
    }

    /**
     * End tracking global session
     * @param context context of the global session
     */
    public static void endSession(Context context) {
        Session session = getSession(context);
        if (session != null) {
            session.finish();
        }
    }

    /**
     * Get current session id
     * @return current session id
     */
    public String getSessionId() {
        return session.getId();
    }

    /**
     * Get current global session id
     * @return current global session id
     */
    public static String getSessionId(Context context) {
        Session session = getSession(context);
        if (session == null) {
            return null;
        }
        return session.getId();
    }

    /**
     * Reset global session id immediately
     */
    public static void resetSessionId(Context context) {
        Session session = getSession(context);
        if (session != null) {
            session.resetId();
        }
    }

    /**
     * Automatically append the time when the event is received on server.
     *
     * This is disabled by default.
     */
    public void enableServerSideUploadTimestamp() {
        serverSideUploadTimestamp = true;
        serverSideUploadTimestampColumn = null;
    }

    /**
     * Automatically append the time value when the event is received on server. Disabled by default.
     *
     * @param columnName The column to write the uploaded time value
     */
    public void enableServerSideUploadTimestamp(String columnName) {
        if (columnName == null) {
            Log.w(TAG, "columnName shouldn't be null");
            return;
        }

        serverSideUploadTimestamp = true;
        serverSideUploadTimestampColumn = columnName;
    }

    /**
     * Disable the uploading time column
     */
    public void disableServerSideUploadTimestamp() {
        serverSideUploadTimestamp = false;
        serverSideUploadTimestampColumn = null;
    }

    /**
     * Same as {@link #enableAutoAppendRecordUUID(String)}, using "record_uuid" as the column name.
     */
    public void enableAutoAppendRecordUUID() {
        autoAppendRecordUUIDColumn = EVENT_DEFAULT_KEY_RECORD_UUID;
    }

    /**
     * Automatically append a random and unique ID for each event. Disabled by default.
     *
     * @param columnName The column to write the ID
     */
    public void enableAutoAppendRecordUUID(String columnName) {
        if (columnName == null) {
            Log.w(TAG, "columnName shouldn't be null");
            return;
        }
        autoAppendRecordUUIDColumn = columnName;
    }

    /**
     * Disable appending ID for each event.
     */
    public void disableAutoAppendRecordUUID() {
        autoAppendRecordUUIDColumn = null;
    }

    private boolean isOnUnity() {
        return context
                // Default preference name of Unity's PlayerPrefs
                .getSharedPreferences(context.getPackageName() + ".v2.playerprefs", Context.MODE_PRIVATE)
                // Since Unity's PlayerPrefs doesn't support boolean
                .getInt(SHARED_PREF_KEY_IS_UNITY, 0) == 1;
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
        public String getUUID() {
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
