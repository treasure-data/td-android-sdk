package com.treasuredata.android.demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.treasuredata.android.TreasureData;

import java.io.IOException;
import java.util.HashMap;

/*
 * For installation event test.
 * $ adb shell
 * # am broadcast -a com.android.vending.INSTALL_REFERRER -n com.treasuredata.android.demo/.InstallationReceiver \
 *     --es "referrer" "utm_source=test_source&utm_medium=test_medium&utm_term=test_term&utm_content=test_content&utm_campaign=test_name"
 */
public class InstallationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            TreasureData.enableLogging();
            final TreasureData td = new TreasureData(context, "your_default_api_key");
            HashMap<String, Object> referrer = new HashMap<String, Object>();
            referrer.put("type", "install_referrer");
            for (String kv : intent.getStringExtra("referrer").split("&")) {
                String[] kAndV = kv.split("=", 2);
                if (kAndV.length >= 2) {
                    referrer.put(kAndV[0], kAndV[1]);
                }
            }
            td.addEvent("testdb", "demotbl", referrer);
            td.uploadEvents();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
