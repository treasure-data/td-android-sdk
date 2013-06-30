package com.treasure_data.td_logger.android.demo;

import java.util.Arrays;
import java.util.List;

import com.treasure_data.mobilesdk.TdAndroidLogger;

import android.os.Bundle;
import android.app.Activity;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TdAndroidLogger logger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logger = new TdAndroidLogger(this);

        List<Pair<Integer, String>> targets = Arrays.asList(
                new Pair<Integer, String>(R.id.navi_help, "navi_help"),
                new Pair<Integer, String>(R.id.navi_news, "navi_news"),
                new Pair<Integer, String>(R.id.navi_play, "navi_play"),
                new Pair<Integer, String>(R.id.navi_signup, "navi_signup")
        );

        for (Pair<Integer, String> target : targets) {
            int id = target.first;
            final String label = target.second;
            View v = findViewById(id);
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    logger.increment("testdb", "demotbl", label);
                    Toast.makeText(MainActivity.this, "logger.increment(testdb, testtbl, " + label + ", 1)", Toast.LENGTH_SHORT).show();
                }
            });
        }
        findViewById(R.id.image).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                logger.write("testdb", "demotbl", "image", ev.toString());
                Toast.makeText(MainActivity.this, "logger.write(testdb, testtbl, image, " + ev.toString() + ")", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        logger.flushAll();
        Toast.makeText(MainActivity.this, "logger.flush()", Toast.LENGTH_SHORT).show();
    }
}
