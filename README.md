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
    <version>0.1.1</version>
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

            td.addEvent("testdb", "testtbl", event);
        }
    });
```

or


```
    View v = findViewById(R.id.image);
    v.setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent ev) {
            td.addEvent("testdb", "testtbl", "event", ev.getAction());
            return false;
        }
    });
```

Specify the database and table to which you want to import the events.

### Upload Events to TreasureData


```
    findViewById(R.id.upload).setOnTouchListener(new OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            td.uploadEvents();
            return false;
        }
    });
```

The sent events is going to be buffered for a few minutes before they get imported into TreasureData storage.


### Get success/error callback

You can know the result of addEvent() and uploadEvents() with TDCallback.


```
    td.setUploadEventsCallBack(new TDCallback() {
        @Override
        public void onSuccess() {
            Log.i("Example", "success!");
        }

        @Override
        public void onError(Exception e) {
            Log.w("Example", "error: " + e.toString());
        }
    });

```

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


