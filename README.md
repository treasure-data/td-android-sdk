TreasureData Android SDK
===============

Android SDK for [TreasureData](http://www.treasuredata.com/). With this SDK, you can import the events on your applications into TreasureData easily.

## Installation

You can install td-android-sdk into your Android project in the following ways.

### Maven

If you use maven, add the following directives to your pom.xml

```
  <dependency>
    <groupId>com.treasuredata</groupId>
    <artifactId>td-android-sdk</artifactId>
    <version>0.1.3</version>
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
          :
```

or

```
        TreasureData.initializeDefaultApiKey("your_default_api_key");
        TreasureData td = new TreasureData(this);

```

We recommend to use a write-only API key for the SDK. To obtain one, please:

1. Login into the Treasure Data Console at http://console.treasuredata.com;
2. Visit your Profile page at http://console.treasuredata.com/users/current;
3. Insert your password under the 'API Keys' panel;
4. In the bottom part of the panel, under 'Write-Only API keys', either copy the API key or click on 'Generate New' and copy the new API key.


### Add Events

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

            td.addEventWithCallback("testdb", "testtbl", event, new TDCallback() {
                @Override
                public void onSuccess() {
                    Log.i("ExampleApp", "success!");
                }
                @Override
                public void onError(String errorCode, Exception e) {
                    Log.w("ExampleApp", "errorCode: " + errorCode + ", detail: " + e.toString());
                }
            });
        }
    });
```
Or, simply

```
            final Map event = new HashMap<String, Object>();
            event.put("id", v.getId());
            event.put("left", v.getLeft());
            event.put("right", v.getRight());
            event.put("top", v.getTop());
            event.put("bottom", v.getBottom());

            td.addEvent("testdb", "testtbl", event);
```

Specify the database and table to which you want to import the events.

### Upload Events to TreasureData


```
    findViewById(R.id.upload).setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
        
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
            
            return false;
        }
    });
```
Or, simply

```
            td.uploadEvents();
```

The sent events is going to be buffered for a few minutes before they get imported into TreasureData storage.

## About Error Code

`TreasureData#addEventWithCallback()` and `uploadEventsWithCallback()` call back `TDCallback#onError()` method with `errorCode` argument. This argument is useful to know the cause type of the error. There are the following error codes.

- "init_error"
  - The initialization failed.
- "invalid_param"
  - The parameter passed to the API was invalid
- "invalid_event"
  - The event was invalid
- "data_conversion"
  - Failed to convert the data to/from JSON
- "storage_error"
  - Failed to read/write data in the storage
- "network_error"
  - Failed to communicate with the server due to network problem
- "server_response"
  - The server returned an error response


## Additioanl Configuration

### Endpoint

The API endpoint (default: https://in.treasuredata.com/android/v3) can be modified using the `initializeApiEndpoint` API after the TreasureData client constructor has been called and the underlying client initialized. For example:

```
        td = new TreasureData(this, "your_api_key");
        td.initializeApiEndpoint("https://in.treasuredata.com/android/v3");
```

or

```
        TreasureData.initializeDefaultApiKey("your_default_api_key");
        TreasureData td = new TreasureData(this);
        td.initializeApiEndpoint("https://in.treasuredata.com/android/v3");
```

### Encryption key

If you've set an encryption key via `TreasureData.initializeEncryptionKey()`, our SDK saves the event data as encrypted when called `addEvent` or `addEventWithCallback`.
