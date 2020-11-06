package com.treasuredata.android;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
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

public class TreasureData implements CDPClient {
    private static final String TAG = TreasureData.class.getSimpleName();
    private static final String VERSION = "0.4.0";
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
    private static final String OS_TYPE = "Android";
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

    public void setDefaultTable(String defaultTable) {
        this.defaultTable = defaultTable;
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

    public void setMaxUploadEventsAtOnce(int maxUploadEventsAtOnce) {
        client.setMaxUploadEventsAtOnce(maxUploadEventsAtOnce);
    }

    public int getMaxUploadEventsAtOnce() {
        return client.getMaxUploadEventsAtOnce();
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

    public void enableInAppPurchaseEvent() {
        enableInAppPurchaseEvent(true);
    }

    public void disableInAppPurchaseEvent() {
        enableInAppPurchaseEvent(false);
    }

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
     * Enable custom event tracking. This setting has no effect to auto tracking
     */
    public void enableCustomEvent() {
        enableCustomEvent(true);
    }

    /**
     * Disable custom event tracking. This setting has no effect to auto tracking
     */
    public void disableCustomEvent() {
        enableCustomEvent(false);
    }

    /**
     * Whether or not the custom event tracking is enable
     * @return true : enable, false : disabled
     */
    public boolean isCustomEventEnabled() {
        return this.customEventEnabled;
    }

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

    public void disableAppInstalledEvent() {
        this.autoTrackAppInstalledEvent = false;
    }

    public void disableAppUpdatedEvent() {
        this.autoTrackAppUpdatedEvent = false;
    }

    public void disableAppOpenEvent() {
        this.autoTrackAppOpenEvent = false;
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

    public void enableAutoAppendAdvertisingIdentifier() {
        enableAutoAppendAdvertisingIdentifier(EVENT_KEY_ADVERTISING_IDENTIFIER);
    }

    public void enableAutoAppendAdvertisingIdentifier(String columnName) {
        if (columnName == null) {
            Log.w(TAG, "columnName must not be null");
            return;
        }
        autoAppendAdvertisingIdColumn = columnName;
        updateAdvertisingId();
    }

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

    public void disableAutoRetryUploading() {
        client.disableAutoRetryUploading();
    }

    public void enableAutoRetryUploading() {
        client.enableAutoRetryUploading();
    }

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
            session = new Session(sessionTimeoutMilli);
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

    public String getSessionId() {
        return session.getId();
    }

    public static String getSessionId(Context context) {
        Session session = getSession(context);
        if (session == null) {
            return null;
        }
        return session.getId();
    }

    public static void resetSessionId(Context context) {
        Session session = getSession(context);
        if (session != null) {
            session.resetId();
        }
    }

    public void enableServerSideUploadTimestamp() {
        serverSideUploadTimestamp = true;
        serverSideUploadTimestampColumn = null;
    }

    public void enableServerSideUploadTimestamp(String columnName) {
        if (columnName == null) {
            Log.w(TAG, "columnName shouldn't be null");
            return;
        }

        serverSideUploadTimestamp = true;
        serverSideUploadTimestampColumn = columnName;
    }

    public void disableServerSideUploadTimestamp() {
        serverSideUploadTimestamp = false;
        serverSideUploadTimestampColumn = null;
    }

    public void enableAutoAppendRecordUUID() {
        autoAppendRecordUUIDColumn = EVENT_DEFAULT_KEY_RECORD_UUID;
    }

    public void enableAutoAppendRecordUUID(String columnName) {
        if (columnName == null) {
            Log.w(TAG, "columnName shouldn't be null");
            return;
        }
        autoAppendRecordUUIDColumn = columnName;
    }

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
