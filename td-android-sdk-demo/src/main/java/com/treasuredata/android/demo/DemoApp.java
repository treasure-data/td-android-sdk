package com.treasuredata.android.demo;

import android.app.Application;
import android.os.Looper;

import com.treasuredata.android.TreasureData;
import com.treasuredata.android.cdp.CDPClient;
import com.treasuredata.android.cdp.FetchUserSegmentCallback;

import java.io.IOException;
import java.util.Map;

/**
 * Created by vinhvd on 2/6/18.
 */

public class DemoApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // TreasureData.initializeApiEndpoint("https://specify-other-endpoint-if-needed.com");
        TreasureData.enableLogging();
        TreasureData.initializeEncryptionKey("hello world");
        TreasureData.setSessionTimeoutMilli(30 * 1000);

        TreasureData.initializeSharedInstance(this, "your_write_api_key");
        TreasureData.sharedInstance().enableAutoAppendUniqId();
        TreasureData.sharedInstance().enableAutoAppendModelInformation();
        TreasureData.sharedInstance().enableAutoAppendAppInformation();
        TreasureData.sharedInstance().enableAutoAppendLocaleInformation();
        TreasureData.sharedInstance().enableServerSideUploadTimestamp("server_upload_time");
        TreasureData.sharedInstance().enableAutoAppendRecordUUID();
        TreasureData.sharedInstance().setDefaultDatabase("test_db");
        TreasureData.sharedInstance().setDefaultTable("test_tbl");

        try {
            new CDPClient().justTrying(Looper.myLooper(), new FetchUserSegmentCallback() {
                @Override
                public void onSuccess(Map<String, ?> map) {
                    System.out.println(">> Success");
                }

                @Override
                public void onError(String s, String s1) {
                    System.out.println(">> Error");
                }
            });
        } catch (IOException e) {
            System.out.println(">> IO Error");
            e.printStackTrace();
        }
    }
}
