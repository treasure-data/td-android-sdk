package com.treasuredata.android.internal;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import com.treasuredata.android.TreasureData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class InAppPurchaseEventActivityLifecycleTracker {
    private static final String TAG = InAppPurchaseEventActivityLifecycleTracker.class.getSimpleName();

    // Purchase types
    private static final String SUBSCRIPTION = "subs";
    private static final String INAPP = "inapp";

    private static final String BILLING_ACTIVITY_NAME =
            "com.android.billingclient.api.ProxyBillingActivity";

    private static Boolean hasBillingService = null;
    private static boolean hasBiillingActivity = false;
    private static ServiceConnection serviceConnection;
    private static Application.ActivityLifecycleCallbacks callbacks;
    private static Intent intent;
    private static Object inAppBillingObj;

    private static final AtomicBoolean isTracking = new AtomicBoolean(false);
    private static TreasureData treasureData;

    private InAppPurchaseEventActivityLifecycleTracker() {

    }

    public static void track(TreasureData treasureData) {
        initialize();
        if (!hasBillingService) {
            return;
        }

        if (!isTracking.compareAndSet(false, true)) {
            return;
        }

        InAppPurchaseEventActivityLifecycleTracker.treasureData = treasureData;

        final Context context = TreasureData.getApplicationContext();
        if (context instanceof Application) {
            Application application = (Application) context;
            application.registerActivityLifecycleCallbacks(callbacks);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private static void initialize() {
        if (isInitialized()) {
            return;
        }

        hasBillingService = InAppBillingDelegate.hasBillingService();

        if (!hasBillingService) {
            return;
        }

        try {
            Class.forName(BILLING_ACTIVITY_NAME);
            hasBiillingActivity = true;
        } catch (ClassNotFoundException ignored) {
            hasBiillingActivity = false;
        }

        intent = new Intent("com.android.vending.billing.InAppBillingService.BIND")
                .setPackage("com.android.vending");

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

                inAppBillingObj = InAppBillingDelegate.asInterface(TreasureData.getApplicationContext(), service);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        callbacks = new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {

            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                final Context context = TreasureData.getApplicationContext();
                ArrayList<String> purchasesInapp = InAppPurchaseEventManager
                        .getPurchasesInapp(context, inAppBillingObj);

                trackPurchaseInapp(context, purchasesInapp);
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        };
    }

    private static boolean isInitialized()
    {
        return hasBillingService != null;
    }

    private static void trackPurchaseInapp(final Context context, ArrayList<String> purchases) {
        if (purchases.isEmpty()) {
            return;
        }

        final Map<String, String> purchaseMap = new HashMap<>();
        ArrayList<String> skuList = new ArrayList<>();
        for (String purchase : purchases) {
            try {
                JSONObject purchaseJson = new JSONObject(purchase);
                String sku = purchaseJson.getString("productId");
                purchaseMap.put(sku, purchase);

                skuList.add(sku);
            }
            catch (JSONException e){
                Log.e(TAG, "Error parsing in-app purchase data.", e);
            }
        }

        final Map<String, String> skuDetailsMap = InAppPurchaseEventManager.getAndCacheSkuDetails(
                context, skuList, inAppBillingObj, INAPP);

        for (Map.Entry<String, String> entry : skuDetailsMap.entrySet()) {
            String purchase = purchaseMap.get(entry.getKey());
            String skuDetails = entry.getValue();
            trackPurchaseInapp(purchase, skuDetails);
        }
    }

    private static void trackPurchaseInapp(String purchase, String skuDetails) {
        try {
            JSONObject purchaseJSON = new JSONObject(purchase);
            JSONObject skuDetailsJSON = new JSONObject(skuDetails);
            Map<String, Object> record = new HashMap<>();
            record.put(InAppPurchaseConstants.EVENT_KEY, InAppPurchaseConstants.IAP_EVENT_NAME);

            String productId = purchaseJSON.getString("productId");
            String orderId = purchaseJSON.getString("orderId");
            String title = skuDetailsJSON.optString("title");
            String price = skuDetailsJSON.getString("price_amount_micros");
            Long priceAmountMicros = skuDetailsJSON.getLong("price_amount_micros");
            String currency = skuDetailsJSON.getString("price_currency_code");
            String description = skuDetailsJSON.optString("description");
            String type = skuDetailsJSON.optString("type");
            Long purchaseTime = purchaseJSON.getLong("purchaseTime");
            String purchaseToken = purchaseJSON.getString("purchaseToken");
            String packageName = purchaseJSON.optString("packageName");

            record.put(InAppPurchaseConstants.IAP_PRODUCT_ID, productId);
            record.put(InAppPurchaseConstants.IAP_ORDER_ID, orderId);
            record.put(InAppPurchaseConstants.IAP_PRODUCT_TITLE, title);
            record.put(InAppPurchaseConstants.IAP_PRODUCT_PRICE, price);
            record.put(InAppPurchaseConstants.IAP_PRODUCT_PRICE_AMOUNT_MICROS, priceAmountMicros);
            record.put(InAppPurchaseConstants.IAP_PRODUCT_CURRENCY, currency);
            record.put(InAppPurchaseConstants.IAP_PRODUCT_DESCRIPTION, description);
            record.put(InAppPurchaseConstants.IAP_PRODUCT_TYPE, type);
            record.put(InAppPurchaseConstants.IAP_PURCHASE_TIME, purchaseTime);
            record.put(InAppPurchaseConstants.IAP_PURCHASE_TOKEN, purchaseToken);
            record.put(InAppPurchaseConstants.IAP_PACKAGE_NAME, packageName);

            if (type.equals(SUBSCRIPTION)) {
                Boolean autoRenewing = purchaseJSON.optBoolean("autoRenewing",
                                false);
                String subscriptionPeriod = skuDetailsJSON.optString("subscriptionPeriod");
                String freeTrialPeriod = skuDetailsJSON.optString("freeTrialPeriod");
                String introductoryPriceCycles = skuDetailsJSON.optString("introductoryPriceCycles");

                record.put(InAppPurchaseConstants.IAP_SUBSCRIPTION_AUTORENEWING, autoRenewing);
                record.put(InAppPurchaseConstants.IAP_SUBSCRIPTION_PERIOD, subscriptionPeriod);
                record.put(InAppPurchaseConstants.IAP_FREE_TRIAL_PERIOD, freeTrialPeriod);
                record.put(InAppPurchaseConstants.IAP_INTRO_PRICE_CYCLES, introductoryPriceCycles);
                if (!introductoryPriceCycles.isEmpty()) {
                    Long introductoryPriceAmountMicros = skuDetailsJSON.getLong("introductoryPriceAmountMicros");
                    record.put(InAppPurchaseConstants.IAP_INTRO_PRICE_AMOUNT_MICROS, introductoryPriceAmountMicros);
                }
            }

            // TODO: Get default data base and table from TreasureData
            treasureData.addEvent("vv_td_android", "td_android", record);
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing in-app subscription data.", e);
        }
    }
}
