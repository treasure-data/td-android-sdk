Treasure Data Android SDK
===============
[<img src="https://travis-ci.org/treasure-data/td-android-sdk.svg?branch=master"/>](https://travis-ci.org/treasure-data/td-android-sdk)

Android and Android TV SDK for [Treasure Data](http://www.treasuredata.com/). With this SDK, you can import the events on your Android and Android TV applications into Treasure Data easily.

## Migration to version 1

Version 1 has major changes that are not backward compatible with previous versions. If you are upgrading from version 0.6.0 or earlier, your code will not run correctly without doing these following steps:
- API endpoint has changed to Ingestion Endpoint. The default value is https://us01.records.in.treasuredata.com.
- `initializeApiEndpoint(String apiEndpoint)` API is no longer available, please use `initializeSharedInstance(Context context, String apiKey, String apiEndpoint)` instead.
- Server side upload timestamp feature is removed. If you need this feature, please contact our support team.
- `uuid` is now reserved column name. If you try to add value to event's `uuid` key, you won't see the column show up in the database.

## Installation

You can install td-android-sdk into your Android project in the following ways.

### Gradle

If you use gradle, add the following dependency to `dependencies` directive in the build.gradle

```
dependencies {
    implementation 'com.treasuredata:td-android-sdk:1.0.0'
}
```

### Maven

If you use maven, add the following directives to your pom.xml

```
  <dependency>
    <groupId>com.treasuredata</groupId>
    <artifactId>td-android-sdk</artifactId>
    <version>1.0.0</version>
  </dependency>
```

This SDK has [an example Android application project](https://github.com/treasure-data/td-android-sdk/tree/master/example/td-android-sdk-demo). The pom.xml would be a good reference.

### Jar file

Or put td-android-sdk-x.x.x-shaded.jar (get the latest [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.treasuredata%22%20AND%20a%3A%22td-android-sdk%22)) into (YOUR_ANDROID_PROJECT)/libs.

## Usage

### Initialize the Library at onCreate() in your Application subclass
For efficient API calls, we highly recommend initializing a `TreasureData` shared instance at the `onCreate()` method of your Application subclass.
```
public class ExampleApp extends Application {

  @Override
  public void onCreate() {

    // Initialize Treasure Data Android SDK
    TreasureData.initializeEncryptionKey("RANDOM_STRING_TO_ENCRYPT_DATA");
    TreasureData.disableLogging();
    TreasureData.initializeSharedInstance(this, "YOUR_WRITE_ONLY_API_KEY", "API_ENDPOINT");
    TreasureData.sharedInstance.setDefaultDatabase("your_application_name");
    TreasureData.sharedInstance.setDefaultTable("your_event_name");
    TreasureData.sharedInstance.enableAutoAppendUniqId();
    TreasureData.sharedInstance.enableAutoAppendModelInformation();
    TreasureData.sharedInstance.enableAutoAppendAppInformation();
    TreasureData.sharedInstance.enableAutoAppendLocaleInformation();
  }
}
```

We recommend to use a write-only API key for the SDK. To obtain one, please:

1. Login to the Treasure Data Console at http://console.treasuredata.com;
2. Visit your Profile page at http://console.treasuredata.com/users/current;
3. Insert your password under the 'API Keys' panel;
4. In the bottom part of the panel, under 'Write-Only API keys', either copy the API key or click on 'Generate New' and copy the new API key.

Then, you can use a shared instance from anywhere with the `TreasureData.sharedInstance()` method.

### Use the shared instance

```
public class ExampleActivity extends Activity {

  public void onDataLoadSomethingFinished(long elapsedTime) {
    Map<String, Object> event = new HashMap<String, Object>();
    event.put("data_type", "something");
    event.put("elapsed_time", elapsedTime);
    TreasureData.sharedInstance().addEvent("events", event);
  }
}
```

### Add an event to local buffer

To add an event to local buffer, you can call `TreasureData#addEvent` or `TreasureData#addEventWithCallback` API.

```
  View v = findViewById(R.id.button);
  v.setOnClickListener(new OnClickListener() {
    @Override
    public void onClick(View v) {

      final Map event = new HashMap<String, Object>();
      event.put("id", v.getId());
      event.put("left", v.getLeft());
      event.put("right", v.getRight());
      event.put("top", v.getTop());
      event.put("bottom", v.getBottom());

      td.addEventWithCallback("testdb", "demotbl", event, new TDCallback() {
        @Override
        public void onSuccess() {
          Log.i("ExampleApp", "success!");
        }

        @Override
        public void onError(String errorCode, Exception e) {
          Log.w("ExampleApp", "errorCode: " + errorCode + ", detail: " + e.toString());
        }
      });
      
      // Or, simply...
      //    td.addEvent("testdb", "demotbl", event);

    }
  });
```

Specify the database and table to which you want to import the events. The total length of database and table must be shorter than 256 chars. Each table will cache no more than 10000 events.

On top of that, the length of key in event must not exceed 256 chars and the length of value in event must not exceed 10000 chars.

### Upload buffered events to Treasure Data

To upload events buffered events to Treasure Data, you can call `TreasureData#uploadEvents` or `TreasureData#uploadEventsWithCallback` API.

```
  findViewById(R.id.upload).setOnTouchListener(new OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

      // You can call this API to uplaod buffered events whenever you want.
      td.uploadEventsWithCallback(new TDCallback() {
        @Override
        public void onSuccess() {
          Log.i("ExampleApp", "success!");
        }

        @Override
        public void onError(String errorCode, Exception e) {
          Log.w("ExampleApp", "errorCode: " + errorCode + ", detail: " + e.toString());
        }
      });
      
      // Or, simply...
      //   td.uploadEvents();
            
      return false;
    }
  });
```

It depends on the characteristic of your application when to upload and how often to upload buffered events. But we recommend the followings at least as good timings to upload.

- When the current screen is closing or moving to background
- When closing the application

The sent events is going to be buffered for a few minutes before they get imported into Treasure Data storage.

### Retry uploading and deduplication

This SDK imports events in exactly once style with the combination of these features.

- This SDK keeps buffered events with adding unique keys and retries to upload them until confirming the events are uploaded and stored on server side (at least once)
- The server side remembers the unique keys of all events within the past 1 hours by default and prevents duplicated imports (at most once)

As for the deduplication window is 1 hour by default, so it's important not to keep buffered events more than 1 hour to avoid duplicated events.

### Default values

Set a default value if you want an event added to a table, a database, or any table or database to automatically set value for a key.
If you have multiple default values set to the same key, newly added event will have the default value applied and override in following order:
1. Default value targeting all table and database will be applied first.
2. Default value targeting all table in a database will then be applied.
3. Default value targeting the table to which the event is added will then be applied.
4. Default value targeting the table and database to which the event is added will then be applied.
5. Finally, if the event has a value for the key, that value will override all default values.

To set default value:
```
TreasureData.sharedInstance().setDefaultValue(null, null, "key", "Value"); // Targeting all databases and tables
TreasureData.sharedInstance().setDefaultValue("database_name", null, "key", "Value"); // Targeting all tables of database "database_name"
TreasureData.sharedInstance().setDefaultValue(null, "table_name", "key", "Value"); // Targeting all tables with "table_name"
TreasureData.sharedInstance().setDefaultValue("database_name", "table_name", "key", "Value"); // Targeting table "table_name" of database "database_name"
```

To get default value:
```
String defaultValue = (String) TreasureData.sharedInstance().getDefaultValue("database_name", "table_name", "key"); // Get default value for key targeting database "database_name" and table "table_name".
```

To remove default value:
```
TreasureData.sharedInstance().removeDefaultValue("database_name", "table_name", "key"); // Only remove default value targeting database "database_name" and table "table_name".
```

### Start/End session

When you call `TreasureData#startSession` method, the SDK generates a session ID that's kept until `TreasureData#endSession` is called. The session id is outputs as a column name "td_session_id". Also, `TreasureData#startSession` and `TreasureData#endSession` method add an event that includes `{"td_session_event":"start" or "end"}`.

```
	@Override
	protected void onStart(Bundle savedInstanceState) {
			:
		TreasureData.sharedInstance().startSession("demotbl");
			:
	}

	@Override
	protected void onStop() {
			:
		TreasureData.sharedInstance().endSession("demotbl");
		TreasureData.sharedInstance().uploadEvents();
		// Outputs =>>
		//   [{"td_session_id":"cad88260-67b4-0242-1329-2650772a66b1",
		//		"td_session_event":"start", "time":1418880000},
		//
		//    {"td_session_id":"cad88260-67b4-0242-1329-2650772a66b1",
		//		"td_session_event":"end", "time":1418880123}
		//    ]
			:
	}
	
```

If you want to handle the following case, use a pair of class methods `TreasureData.startSession` and `TreasureData.endSession` for global session tracking

* User opens the application and starts session tracking. Let's call this session `session#0`
* User moves to home screen and destroys the Activity
* User reopens the application and restarts session tracking within default 10 seconds. But you want to deal with this new session as the same session as `session#0`

```
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
			:
		TreasureData.setSessionTimeoutMilli(30 * 1000);  // Default is 10 seconds
	}

	@Override
	protected void onStart() {
			:
		TreasureData.startSession(this);
			:
	}

	@Override
	protected void onStop() {
			:
		TreasureData.endSession(this);
		TreasureData.sharedInstance().uploadEvents();
			:
	}
```

In this case, you can get the current session ID using `TreasureData.getSessionId`

```
	@Override
	protected void onStart() {
			:
		TreasureData.startSession(this);
		Log.i(TAG, "onStart(): Session ID=" + TreasureData.getSessionId(this));
			:
	}
```

### Track app lifecycle events automatically

App lifecycle event tracking is optional and not enable by default. You can track app lifecycle events automatically using :
`TreasureData#enableAppLifecycleEvent()`

App lifecycle events include : Application Install, Application Open, Application Update. You can disable the individual core events as the following:

- Disable Application Install: `disableAppInstalledEvent()`
- Disable Application Open: `disableAppOpenEvent()`
- Disable Application Update: `disableAppUpdatedEvent()`

### Track in app purchase events automatically

In app purchase event tracking is optional and not enable by default. To track in app purchase events automatically, you only need to add a line of code :
`TreasureData#enableInAppPurchaseEvent()`

It outputs the following columns: 

- `td_android_event` : TD_ANDROID_IN_APP_PURCHASE
- `td_iap_product_id` : productId (Purchase)
- `td_iap_order_id` : orderId (Purchase)
- `td_iap_product_price` : price (SKU detail)
- `td_iap_quantity` : 1
- `td_iap_product_price_amount_micros` : price_amount_micros (SKU detail)
- `td_iap_product_currency` : price_currency_code (SKU detail)
- `td_iap_purchase_time` : purchaseTime (Purchase)
- `td_iap_purchase_token` : purchaseToken (Purchase)
- `td_iap_purchase_state` : purchaseState (Purchase)
- `td_iap_purchase_developer_payload` : developerPayload (Purchase)
- `td_iap_product_type` : type (SKU detail), inapp for one-time product and subs for subscription
- `td_iap_product_title` : title (SKU detail)
- `td_iap_product_description` : description (SKU detail)
- `td_iap_package_name` : packageName (Purchase)
- `td_iap_subs_auto_renewing` : autoRenewing (Purchase)
- `td_iap_subs_status` : Auto detection for subscription (New|Cancelled|Restored|Expired)
- `td_iap_subs_period` : subscriptionPeriod (SKU detail for subscription)
- `td_iap_free_trial_period` : freeTrialPeriod (SKU detail for subscription)
- `td_iap_intro_price_period` : introductoryPricePeriod (SKU detail for subscription)
- `td_iap_intro_price_cycless` : introductoryPriceCycles (SKU detail for subscription)
- `td_iap_intro_price_amount_micros` : introductoryPriceAmountMicro (SKU detail for subscription)

This SDK can track in app purchase events for Android application using both Google Play Billing Library and In-app Billing with AIDL . You must add the following ProGuard rule to keep AIDL classes using by the SDK to your project if your application is developed using In-app Billing with AIDL api.

```
-keep class com.android.vending.billing.** { *; }
```

### Opt out 

Depending on the countries where you sell your app (e.g. the EU), you may need to offer the ability for users to opt-out of tracking data inside your app.

- To turn off auto tracking application lifecycle events (when application lifecycle event tracking is enabled) : `TreasureData#disableAppLifecycleEvent()`.
- To turn off auto tracking in app purchase events(when in app purchase event is enabled) : `TreasureData#disableInAppPurchaseEvent()`.
- To turn off custom events (the events you are tracking by `TreasureData#addEvent`, `TreasureData#addEventWithCallback` ) : `TreasureData#disableCustomEvent`. To turn on it again :  `TreasureData#enableCustomEvent`

You can query the state of tracking events by using : `TreasureData#isAppLifecycleEventEnabled()`, `TreasureData#isInAppPurchaseEventEnabled()` and `TreasureData#isCustomEventEnabled()`.
The states have effects across device reboots, app updates, so you can simply call this once during your application.

## About error codes

`TreasureData#addEventWithCallback` and `TreasureData#uploadEventsWithCallback` call back `TDCallback#onError` method with `errorCode` argument. This argument is useful to know the cause type of the error. There are the following error codes.

- `init_error` :  The initialization failed.
- `invalid_param` : The parameter passed to the API was invalid
- `invalid_event` : The event was invalid
- `data_conversion` : Failed to convert the data to/from JSON
- `storage_error` : Failed to read/write data in the storage
- `network_error` : Failed to communicate with the server due to network problem 
- `server_response` : The server returned an error response


## Additional configuration

### Endpoint

The API endpoint (default: https://us01.records.in.treasuredata.com) can be changed. For example:

```
    td = new TreasureData(this, "your_api_key", "https://specifying-another-endpoint.com");
```

### Encryption key

If you've set an encryption key via `TreasureData.initializeEncryptionKey`, our SDK saves the event data as encrypted when called `TreasureData#addEvent` or `TreasureData.addEventWithCallback`.

```
    TreasureData.initializeEncryptionKey("hello world");
        :
    td.addEventWithCallback(...)
```

### Default database

```
	TreasureData.sharedInstance().setDefaultDatabase("default_db");
		:
	TreasureData.sharedInstance().addEvent("demotbl", …);
```

### Adding local timestamp to each event record automatically (enabled by default)

By default, local timestamp will be added to event's `time` key automatically. If you `disableAutoAppendLocalTimestamp` without adding `time` key to the event yourself, the server will add server side timestamp to `time` column. You can also auto track local time with custom column. If so, the `time` column will have server side timestamp.

```
    // Use local time as `time` column
    TreasureData.sharedInstance().enableAutoAppendLocalTimestamp();

    // Add local time as a customized column name
    TreasureData.sharedInstance().enableAutoAppendLocalTimestamp("custom_time");

    // Disable auto append local time
    TreasureData.sharedInstance().disableAutoAppendLocalTimestamp();
```

### Adding UUID of the device to each event automatically

UUID of the device will be added to each event automatically if you call `TreasureData#enableAutoAppendUniqId()`. This value won't change until the application is uninstalled.

```
	td.enableAutoAppendUniqId();
		:
	td.addEvent(...);
```

It outputs the value as a column name `td_uuid`.

You can reset the UUID and send `forget_device_uuid` event with old UUID using `TreasureData#resetUniqId()`. 

### Adding an UUID to each event record automatically

UUID will be added to each event record automatically if you call `enableAutoAppendRecordUUID`. Each event has different UUID.

```
	td.enableAutoAppendRecordUUID();
	// If you want to customize the column name, pass it to the API
	// td.enableAutoAppendRecordUUID("my_record_uuid");
		:
	td.addEvent(...);
```

It outputs the value as a column name `record_uuid` by default.

### Adding Advertising Id to each event record automatically
Advertising Id will be added to each event record automatically if you call `enableAutoAppendAdvertisingIdentifier`. You must install Google Play Service Ads (Gradle `com.google.android.gms:play-services-ads`) as a dependency for this feature to work. User must also not turn on Limit Ad Tracking feature in their device, otherwise, Treasure Data will not attach Advertising Id to the record.
Due to asynchronous nature of getting Advertising Id, after enableAutoAppendAdvertisingIdentifier method called, it may take some time for Advertising Id to be available to be added to the record. However, Treasure Data does cache the Advertising Id in order to add to the next event without having to wait for the fetch Advertising Id task to complete.

```
td.enableAutoAppendAdvertisingIdentifier();
// If you want to customize the column name, pass it to the API
// td.enableAutoAppendAdvertisingIdentifier("my_advertising_id_column");
:
td.addEvent(...);
```

It outputs the value as a column name `td_maid` by default.


### Adding device model information to each event automatically

Device model information will be added to each event automatically if you call `TreasureData#enableAutoAppendModelInformation`.

```
	td.enableAutoAppendModelInformation();
		:
	td.addEvent(...);
```

It outputs the following column names and values:

- `td_board` : android.os.Build#BOARD
- `td_brand` : android.os.Build#BRAND
- `td_device` : android.os.Build#DEVICE
- `td_display` : android.os.Build#DISPLAY
- `td_model` : android.os.Build#MODEL
- `td_os_ver` : android.os.Build.VERSION#SDK_INT
- `td_os_type` : "Android"

### Adding application package version information to each event automatically

Application package version information will be added to each event automatically if you call `TreasureData#enableAutoAppendAppInformation`.

```
	td.enableAutoAppendAppInformation();
		:
	td.addEvent(...);
```

It outputs the following column names and values:

- `td_app_ver` : android.content.pm.PackageInfo.versionName (from Context.getPackageManager().getPackageInfo())
- `td_app_ver_num` : android.content.pm.PackageInfo.versionCode (from Context.getPackageManager().getPackageInfo())

### Adding locale configuration information to each event automatically

Locale configuration information will be added to each event automatically if you call `TreasureData#enableAutoAppendLocaleInformation`.

```
	td.enableAutoAppendLocaleInformation();
		:
	td.addEvent(...);
```

It outputs the following column names and values:

- `td_locale_country` : java.util.Locale.getCountry() (from Context.getResources().getConfiguration().locale)
- `td_locale_lang` : java.util.Locale.getLanguage() (from Context.getResources().getConfiguration().locale)

### Use server side upload timestamp

If you want to use server side upload timestamp not only client device time that is recorded when your application calls `addEvent`, use `enableServerSideUploadTimestamp`.

```
	// Use server side upload time as `time` column
	td.enableServerSideUploadTimestamp(true);
	
	// Add server side upload time as a customized column name
	td.enableServerSideUploadTimestamp("server_upload_time");
```

### Profiles API

Lookup for profiles via [Profiles API](https://docs.treasuredata.com/display/public/PD/Working+with+Profiles+and+the+Profiles+API+Tokens)

```
// Set your CDP endpoint to either:
//   https://cdp.in.treasuredata.com        (US)
//   https://cdp-tokyo.in.treasuredata.com  (Tokyo)
//   https://cdp-eu01.in.treasuredata.com   (EU)
//   https://cdp-ap02.in.treasuredata.com   (Seoul)
//   https://cdp-ap03.in.treasuredata.com   (Tokyo)
TreasureData.sharedInstance().setCDPEndpoint("<your_cdp_endpoint>");

TreasureData.sharedInstance().fetchUserSegments(Arrays.asList("<your_profile_api_tokens>"),
                                                Collections.singletonMap("<your_key_column>", "<value>"),
                                                new FetchUserSegmentsCallback() {
                                                    @Override
                                                    public void onSuccess(List<Profile> profiles) {
                                                        System.out.println(profiles);
                                                    }
                                                    @Override
                                                    public void onError(Exception e) {
                                                        System.err.println(e);
                                                    }
                                                });
```

### Enable/Disable debug log

```
	TreasureData.enableLogging();
```

```
	TreasureData.disableLogging();
```

## Android version support

Android SDK for Arm Treasure Data only supports any Android device running API 15 (Android 4.0) and higher

|Codename          |Version | API| Tested?|
|------------------|-------:|---:|:------:|
|Android 11        |11.0    |30  |Yes     |
|Android 10        |10.0    |29  |Yes     |
|Pie               |9.0     |28  |Yes     |
|Oreo              |8.1     |27  |Yes     |
|Oreo              |8.0     |26  |Yes     |
|Nougat            |7.1     |25  |Yes     |
|Nougat            |7.0     |24  |Yes     |
|Marshmallow       |6.0     |23  |Yes     |
|Lollipop          |5.1     |22  |Yes     |
|Lollipop          |5.0     |21  |Yes     |
|KitKat            |4.4     |19  |Yes     |
|Jelly Bean        |4.3     |18  |Yes     |
|Jelly Bean        |4.2     |17  |No      |
|Jelly Bean        |4.1     |16  |No      |
|Ice Cream Sandwich|4.0     |15  |No      |
