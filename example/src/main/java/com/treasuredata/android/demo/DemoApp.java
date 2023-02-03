package com.treasuredata.android.demo;

import android.app.Application;
import android.os.RemoteException;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.treasuredata.android.TreasureData;

import java.util.HashMap;

/**
 * Created by vinhvd on 2/6/18.
 */

public class DemoApp extends Application {
    InstallReferrerClient referrerClient;

    @Override
    public void onCreate() {
        super.onCreate();
        setupTreasureData();
        setupInstallReferrer();
    }

    private void setupTreasureData() {
        TreasureData.enableLogging();
        TreasureData.initializeEncryptionKey("hello world");
        TreasureData.setSessionTimeoutMilli(30 * 1000);

        TreasureData.initializeSharedInstance(this, "your_write_api_key", "https://api_endpoint");
        TreasureData.sharedInstance().enableAutoAppendUniqId();
        TreasureData.sharedInstance().enableAutoAppendModelInformation();
        TreasureData.sharedInstance().enableAutoAppendAppInformation();
        TreasureData.sharedInstance().enableAutoAppendLocaleInformation();
        TreasureData.sharedInstance().enableAutoAppendRecordUUID();
        TreasureData.sharedInstance().enableAutoAppendAdvertisingIdentifier("custom_td_maid");
        TreasureData.sharedInstance().setDefaultDatabase("default_db");
        TreasureData.sharedInstance().setDefaultTable("default_table");
        TreasureData.sharedInstance().setDefaultValue(null, null, "default_value", "Test default value");

    }

    private void setupInstallReferrer() {
        referrerClient = InstallReferrerClient.newBuilder(this).build();
        referrerClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        // Connection established.
                        try {
                            ReferrerDetails response = referrerClient.getInstallReferrer();
                            addReferrerEvent(response);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
                        // API not available on the current Play Store app.
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
                        // Connection couldn't be established.
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    private void addReferrerEvent(ReferrerDetails referrer) {
        String referrerUrl = referrer.getInstallReferrer();
        long referrerClickTime = referrer.getReferrerClickTimestampSeconds();
        long appInstallTime = referrer.getInstallBeginTimestampSeconds();
        boolean instantExperienceLaunched = referrer.getGooglePlayInstantParam();
        HashMap<String, Object> eventRecord = new HashMap<String, Object>();
        eventRecord.put("type", "install_referrer");
        eventRecord.put("url", referrerUrl);
        eventRecord.put("referrerClickTime", referrerClickTime);
        eventRecord.put("appInstallTime", appInstallTime);
        eventRecord.put("instantExperienceLaunched", instantExperienceLaunched);
        TreasureData.sharedInstance().addEvent("test_db", "demo_tbl", eventRecord);
        TreasureData.sharedInstance().uploadEvents();
    }

}
