package com.treasuredata.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Toast;
import com.treasure_data.td_logger.android.demo.R;
import com.treasuredata.android.TDException;
import com.treasuredata.android.TreasureData;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {
    private TreasureData td;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            td = new TreasureData(this, "1qaz2wsx3edc4rfv5tgb6yhn");
        } catch (TDException e) {
            e.printStackTrace();
            return;
        }

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
            final TreasureData finalTd = td;
            v.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Map event = new HashMap<String, Object>(1);
                    event.put(label, v.toString());
                    try {
                        td.event("testdb", "demotbl", event);
                        Toast.makeText(MainActivity.this, "TreasureData.event(testdb, testtbl, " + label + ", " + v.toString() + ")", Toast.LENGTH_SHORT).show();
                    } catch (TDException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        findViewById(R.id.image).setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent ev) {
                try {
                    td.event("testdb", "demotbl", "image", ev.toString());
                    Toast.makeText(MainActivity.this, "TreasureData.event(testdb, testtbl, image, " + ev.toString() + ")", Toast.LENGTH_SHORT).show();
                } catch (TDException e) {
                    e.printStackTrace();
                }
                return false;
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (td != null) {
            /*
            UploadFinishedCallback callback = new UploadFinishedCallback() {
                @Override
                public void callback() {
                    Toast.makeText(MainActivity.this, "TresureData.upload()", Toast.LENGTH_SHORT).show();
                }
            };
            td.upload(callback);
            */
            td.upload(null);
        }
        super.onDestroy();
    }
}
