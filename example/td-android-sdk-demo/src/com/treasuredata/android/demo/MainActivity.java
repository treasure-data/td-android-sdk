package com.treasuredata.android.demo;

import android.app.Activity;
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

    class AddEventCallback implements TDCallback {
        String eventName;

        @Override
        public void onSuccess() {
            String message = "TreasureData.addEvent:onSuccess[" + eventName + "]";
            Log.d(TAG, message);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(Exception e) {
            String message = "TreasureData.addEvent:onError[" + eventName + ": " + e + "]";
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
        public void onError(Exception e) {
            String message = "TreasureData.uploadEvents:onError[" + e + "]";
            Log.d(TAG, message);
        }
    }

    private AddEventCallback addEventCallback = new AddEventCallback();

    private UploadEventsCallback uploadEventsCallback = new UploadEventsCallback();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            td = new TreasureData(this, "your_api_key");
            td.setAddEventCallBack(addEventCallback);
            td.setUploadEventsCallBack(uploadEventsCallback);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        TreasureData.enableLogging();

        List<Pair<Integer, String>> targets = Arrays.asList(
                new Pair<Integer, String>(R.id.navi_help, "navi_help"),
                new Pair<Integer, String>(R.id.navi_news, "navi_news"),
                new Pair<Integer, String>(R.id.navi_play, "navi_play")
//                new Pair<Integer, String>(R.id.navi_signup, "navi_signup")
        );

        for (Pair<Integer, String> target : targets) {
            int id = target.first;
            final String label = target.second;
            View v = findViewById(id);
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Map event = new HashMap<String, Object>(1);
                    event.put(label, v.toString());
                    td.addEvent("testdb", "demotbl", event);
                    addEventCallback.eventName = label;
                }
            });
        }
        findViewById(R.id.image).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                addEventCallback.eventName = "image";
                td.addEvent("testdb", "demotbl", "image", ev.toString());
                return false;
            }
        });
        findViewById(R.id.navi_signup).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                td.uploadEvents();
                return false;
            }
        });
    }
}
