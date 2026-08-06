package com.treasuredata.android.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.treasuredata.android.engage.LandingPageView;
import com.treasuredata.android.engage.TDBridgeDelegate;

import java.util.HashMap;
import java.util.Map;

/**
 * Manual smoke test for the TDBridge (v2) channel. Builds a {@link LandingPageView} in code
 * with inline landing-page HTML that exercises every bridge method
 * ({@code close}/{@code openUrl}/{@code track}/{@code invoke}) plus {@code window.TDContext},
 * then displays it full-screen.
 *
 * <p>Launch directly, e.g.
 * {@code adb shell am start -n com.treasuredata.android.demo/.PopupDemoActivity}.
 */
public class PopupDemoActivity extends Activity {
    private static final String TAG = "PopupDemoActivity";

    private static final String SAMPLE_HTML =
            "<!doctype html><html><head><meta charset='utf-8'>"
            + "<style>body{font:16px sans-serif;padding:20px}pre{background:#eee;padding:8px}</style>"
            + "</head><body><h2>TDBridge demo</h2>"
            + "<pre id='log'></pre>"
            + "<button onclick=\"TDBridge.track('click',{content_id:'42'})\">track</button> "
            + "<button onclick=\"TDBridge.openUrl('https://www.treasuredata.com/')\">openUrl</button> "
            + "<button onclick=\"TDBridge.invoke('submitRaffleEntries',{entries:3})\">invoke</button> "
            + "<button onclick='TDBridge.close()'>close</button>"
            + "<script>"
            + "document.getElementById('log').textContent="
            + "'TDContext: '+JSON.stringify(window.TDContext);"
            + "</script></body></html>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LandingPageView lp = new LandingPageView(this);

        Map<String, Object> context = new HashMap<>();
        context.put("location", "store-42");
        Map<String, Object> profile = new HashMap<>();
        profile.put("tier", "gold");
        context.put("user_profile", profile);
        lp.setContext(context);

        lp.setTrackTable("event_db", "event_table");

        lp.setOnClose(() -> {
            Toast.makeText(this, "close -> onClose fired", Toast.LENGTH_SHORT).show();
            finish();
        });

        lp.setDelegate(new TDBridgeDelegate() {
            @Override
            public void handleTDBridgeInvoke(@NonNull String name, @NonNull Map<String, Object> params) {
                Log.i(TAG, "invoke: " + name + " params=" + params);
                Toast.makeText(PopupDemoActivity.this, "invoke: " + name, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void handleTDBridgeOpenURL(@NonNull String url) {
                Log.i(TAG, "openUrl: " + url);
                Toast.makeText(PopupDemoActivity.this, "openUrl: " + url, Toast.LENGTH_SHORT).show();
            }
        });

        setContentView(lp);
        lp.loadHtml(SAMPLE_HTML, "https://appassets.treasuredata.example/");
    }
}
