package com.treasuredata.android.demo;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import com.treasuredata.android.TDCallback;
import com.treasuredata.android.TreasureData;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private TreasureData td;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TreasureData.initializeApiEndpoint("https://anotherapi.treasuredata.com");
        TreasureData.initializeDefaultApiKey("your_default_api_key");
        TreasureData.enableLogging();
        TreasureData.initializeEncryptionKey("hello world");
        // TreasureData.disableEventCompression();

        try {
            td = new TreasureData(this);
            // td = new TreasureData(this, "your_api_key");
            // td.setDebugMode(true);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final SharedPreferences prefs = getSharedPreferences("my_application", MODE_PRIVATE);
        if (!prefs.getBoolean("hasLaunchedOnce", false)) {
            td.addEventWithCallback("testdb", "demotbl", "installed", true, new TDCallback() {
                @Override
                public void onSuccess() {
                    prefs.edit().putBoolean("hasLaunchedOnce", true).commit();
                    td.uploadEvents();
                }

                @Override
                public void onError(String errorCode, Exception e) {
                    Log.w(TAG, "TreasureData.addEvent:onError errorCode=" + errorCode + ", ex=" + e);
                }
            });
        }

        // For default callback, optional.
        td.setAddEventCallBack(addEventCallback);
        // td.setUploadEventsCallBack(uploadEventsCallback);

        List<Pair<Integer, String>> targets = Arrays.asList(
                new Pair<Integer, String>(R.id.navi_help, "navi_help"),
                new Pair<Integer, String>(R.id.navi_news, "navi_news"),
                new Pair<Integer, String>(R.id.navi_play, "navi_play")
        );

        for (Pair<Integer, String> target : targets) {
            int id = target.first;
            final String label = target.second;
            View v = findViewById(id);
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Map event = new HashMap<String, Object>();
                    event.put("label", label);
                    event.put("id", v.getId());
                    event.put("left", v.getLeft());
                    event.put("right", v.getRight());
                    event.put("top", v.getTop());
                    event.put("bottom", v.getBottom());

                    addEventCallback.eventName = label;
                    td.addEventWithCallback("testdb", "demotbl", event, addEventCallback);
                }
            });
        }

        findViewById(R.id.image).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                addEventCallback.eventName = "image";
                // Use default callback
                td.addEvent("testdb", "demotbl", "image", ev.toString());
                return false;
            }
        });

        findViewById(R.id.upload).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                td.uploadEventsWithCallback(uploadEventsCallback);
                return false;
            }
        });
    }

    /*
     * These are only for callback, Optional.
     */
    class AddEventCallback implements TDCallback {
        String eventName;

        @Override
        public void onSuccess() {
            String message = "TreasureData.addEvent:onSuccess[" + eventName + "]";
            Log.d(TAG, message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(String errorCode, Exception e) {
            String message = "TreasureData.addEvent:onError[" + eventName + ": errorCode=" + errorCode + ", ex=" + e + "]";
            Log.d(TAG, message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    }

    class UploadEventsCallback implements TDCallback {
        @Override
        public void onSuccess() {
            String message = "TreasureData.uploadEvents:onSuccess";
            Log.d(TAG, message);
        }

        @Override
        public void onError(String errorCode, Exception e) {
            String message = "TreasureData.uploadEvents:onError[" + errorCode + ": errorCode=" + errorCode + ", ex=" + e + "]";
            Log.d(TAG, message);
        }
    }

    private AddEventCallback addEventCallback = new AddEventCallback();

    private UploadEventsCallback uploadEventsCallback = new UploadEventsCallback();
}
