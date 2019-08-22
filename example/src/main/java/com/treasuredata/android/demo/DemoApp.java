package com.treasuredata.android.demo;

import android.app.Application;
import com.treasuredata.android.TreasureData;

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
        TreasureData.sharedInstance().enableAutoAppendAdvertisingIdentifier("custom_td_maid");
        TreasureData.sharedInstance().setDefaultDatabase("test_db");
        TreasureData.sharedInstance().setDefaultTable("test_tbl");
    }
}
