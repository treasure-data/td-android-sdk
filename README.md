Treasure Data Android SDK
===============
[<img src="https://travis-ci.org/treasure-data/td-android-sdk.svg?branch=master"/>](https://travis-ci.org/treasure-data/td-android-sdk)

Android SDK for [Treasure Data](http://www.treasuredata.com/). With this SDK, you can import the events on your applications into Treasure Data easily.

## Installation

You can install td-android-sdk into your Android project in the following ways.

### Gradle

If you use gradle, add the following dependency to `dependencies` directive in the build.gradle

```
dependencies {
    compile 'com.treasuredata:td-android-sdk:0.1.16'
}
```

### Maven

If you use maven, add the following directives to your pom.xml

```
  <dependency>
    <groupId>com.treasuredata</groupId>
    <artifactId>td-android-sdk</artifactId>
    <version>0.1.16</version>
  </dependency>
```

This SDK has [an example Android application project](https://github.com/treasure-data/td-android-sdk/tree/master/example/td-android-sdk-demo). The pom.xml would be a good reference.

### Jar file

Or put td-android-sdk-x.x.x-shaded.jar (get the latest [here](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.treasuredata%22%20AND%20a%3A%22td-android-sdk%22)) into (YOUR_ANDROID_PROJECT)/libs.

## Usage

### Instantiate TreasureData object with your API key

```
public class ExampleActivity extends Activity {
	private TreasureData td;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
			:
		td = new TreasureData(this, "your_api_key");
```

or

```
    TreasureData.initializeDefaultApiKey("your_default_api_key");
    	:
    TreasureData td = new TreasureData(this);
```

We recommend to use a write-only API key for the SDK. To obtain one, please:

1. Login to the Treasure Data Console at http://console.treasuredata.com;
2. Visit your Profile page at http://console.treasuredata.com/users/current;
3. Insert your password under the 'API Keys' panel;
4. In the bottom part of the panel, under 'Write-Only API keys', either copy the API key or click on 'Generate New' and copy the new API key.

### Use a shared instance

Also, you can use a shared instance from anywhere with `TreasureData.sharedInstance` method after calling `TreasureData.initializeSharedInstance`.

```
public class MainActivity extends Activity {
		:
	TreasureData.initializeDefaultApiKey("your_write_apikey");
	TreasureData.initializeEncryptionKey("hello world");
		:
	TreasureData.initializeSharedInstance(this);
	TreasureData.sharedInstance().setDefaultDatabase("testdb");
		:
}

public class OtherActivity extends Activity {
		:
	Map<String, Object> event = new HashMap<String, Object>();
	event.put("event_name", "data_load");
	event.put("elapsed_time", elapsed_time);
	TreasureData.sharedInstance().addEvent("demotbl", event);
		:
```

### Add a event to local buffer

To add a event to local buffer, you can call `TreasureData#addEvent` or `TreasureData#addEventWithCallback` API.

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

Specify the database and table to which you want to import the events.

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

### Detect if it's the first running

You can detect if it's the first running or not easily using `TreasureData#isFirstRun` method and then clear the flag with `TreasureData#clearFirstRun`.

```
	if (TreasureData.sharedInstance().isFirstRun(this)) {
	    Map<String, Object> event = new HashMap<String, Object>();
	    event.put("first_run", true);
	    event.put("app_name", "td-android-sdk-demo");
	    TreasureData.sharedInstance().addEventWithCallback("demotbl", event, new TDCallback() {
			@Override
			public void onSuccess() {
				TreasureData.sharedInstance().clearFirstRun(MainActivity.this);
				TreasureData.sharedInstance().uploadEvents();
			}
			
			@Override
			public void onError(String errorCode, Exception e) {
				Log.w(TAG, "TreasureData.addEvent:onError errorCode=" + errorCode + ", ex=" + e);
			}
		});
	}
```


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

The API endpoint (default: https://in.treasuredata.com) can be modified using  `TreasureData.initializeApiEndpoint`. For example:

```
    TreasureData.initializeApiEndpoint("https://in.treasuredata.com");
    td = new TreasureData(this, "your_api_key");
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

### Adding UUID of the device to each event automatically

UUID of the device will be added to each event automatically if you call `TreasureData#enableAutoAppendUniqId()`. This value won't change until the application is uninstalled.

```
	td.enableAutoAppendUniqId();
		:
	td.addEvent(...);
```

It outputs the value as a column name `td_uuid`.

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

### Adding device model information to each event automatically

Device model infromation will be added to each event automatically if you call `TreasureData#enableAutoAppendModelInformation`.

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

Application package version infromation will be added to each event automatically if you call `TreasureData#enableAutoAppendAppInformation`.

```
	td.enableAutoAppendAppInformation();
		:
	td.addEvent(...);
```

It outputs the following column names and values:

- `td_app_ver` : android.content.pm.PackageInfo.versionName (from Context.getPackageManager().getPackageInfo())
- `td_app_ver_num` : android.content.pm.PackageInfo.versionCode (from Context.getPackageManager().getPackageInfo())

### Adding locale configuration information to each event automatically

Locale configuration infromation will be added to each event automatically if you call `TreasureData#enableAutoAppendLocaleInformation`.

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

### Enable/Disable debug log

```
	TreasureData.enableLogging();
```

```
	TreasureData.disableLogging();
```
