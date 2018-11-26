package com.treasuredata.android.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.treasuredata.android.TreasureData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.treasuredata.android.internal.PurchaseConstants.INAPP;
import static com.treasuredata.android.internal.PurchaseConstants.SUBSCRIPTION;

public class PurchaseEventManager {
    private static final String TAG = PurchaseEventManager.class.getSimpleName();

    private static final String SKU_DETAILS_SHARED_PREF_NAME =
            "td_sdk_sku_details";
    private static final String PURCHASE_INAPP_SHARED_PREF_NAME =
            "td_sdk_sku_purchase_inapp";
    private static final SharedPreferences skuDetailSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(SKU_DETAILS_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    private static final SharedPreferences purchaseInappSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(PURCHASE_INAPP_SHARED_PREF_NAME, Context.MODE_PRIVATE);

    private static final int PURCHASE_EXPIRE_TIME_SEC = 12 * 60 * 60; // 12 h

    // SKU detail cache setting
    private static final int SKU_DETAIL_EXPIRE_TIME_SEC = 12 * 60 * 60; // 12 h

    private static final int SKU_DETAIL_CACHE_CLEAR_TIME_LIMIT_SEC = 7 * 24 * 60 * 60; // 7 days
    private static final String SKU_DETAIL_LAST_CLEARED_TIME = "SKU_DETAIL_LAST_CLEARED_TIME";

    private PurchaseEventManager() {

    }

    public static List<String> getPurchasesInapp(Context context, Object inAppBillingObj) {

        return filterAndCachePurchases(BillingDelegate.getPurchases(context, inAppBillingObj, INAPP));
    }

    public static List<String> getPurchaseHistoryInapp(Context context, Object inAppBillingObj) {
        return filterAndCachePurchases(BillingDelegate.getPurchaseHistory(context, inAppBillingObj, INAPP));
    }

    public static List<String> getPurchasesSubs(Context context, Object inAppBillingObj) {

        return filterAndCachePurchases(BillingDelegate.getPurchases(context, inAppBillingObj, SUBSCRIPTION));
    }

    private static List<String> filterAndCachePurchases(List<String> purchases) {
        List<String> filteredPurchases = new ArrayList<>();
        SharedPreferences.Editor editor = purchaseInappSharedPrefs.edit();
        long nowSec = System.currentTimeMillis() / 1000L;
        for (String purchase : purchases) {
            try {
                JSONObject purchaseJson = new JSONObject(purchase);
                String sku = purchaseJson.getString("productId");
                long purchaseTimeMillis = purchaseJson.getLong("purchaseTime");
                String purchaseToken = purchaseJson.getString("purchaseToken");
                if (nowSec - purchaseTimeMillis / 1000L > PURCHASE_EXPIRE_TIME_SEC) {
                    continue;
                }

                String historyPurchaseToken = purchaseInappSharedPrefs.getString(sku, "");

                if (historyPurchaseToken.equals(purchaseToken)) {
                    continue;
                }

                // Write new purchase into cache
                editor.putString(sku, purchaseToken);
                filteredPurchases.add(purchase);
            } catch (JSONException e) {
                Log.e(TAG, "Unable to parse purchase, not a json object: ", e);
            }
        }

        editor.apply();

        return filteredPurchases;
    }

    public static Map<String, String> getAndCacheSkuDetails(
            Context context, Object inAppBillingObj, List<String> skuList, String type) {

        Map<String, String> skuDetailsMap = readSkuDetailsFromCache(skuList);

        ArrayList<String> unresolvedSkuList = new ArrayList<>();
        for (String sku : skuList) {
            if (!skuDetailsMap.containsKey(sku)) {
                unresolvedSkuList.add(sku);
            }
        }

        skuDetailsMap.putAll(BillingDelegate.getSkuDetails(
                context, inAppBillingObj, unresolvedSkuList, type));
        writeSkuDetailsToCache(skuDetailsMap);

        return skuDetailsMap;
    }

    private static Map<String, String> readSkuDetailsFromCache(
            List<String> skuList) {

        Map<String, String> skuDetailsMap = new HashMap<>();
        long nowSec = System.currentTimeMillis() / 1000L;

        for (String sku : skuList) {
            String rawString = skuDetailSharedPrefs.getString(sku, null);
            if (rawString != null) {
                String[] splitted = rawString.split(";", 2);
                long timeSec = Long.parseLong(splitted[0]);
                if (nowSec - timeSec < SKU_DETAIL_EXPIRE_TIME_SEC) {
                    skuDetailsMap.put(sku, splitted[1]);
                }
            }
        }

        return skuDetailsMap;
    }

    private static void writeSkuDetailsToCache(Map<String, String> skuDetailsMap) {
        long nowSec = System.currentTimeMillis() / 1000L;

        SharedPreferences.Editor editor = skuDetailSharedPrefs.edit();
        for (Map.Entry<String, String> entry : skuDetailsMap.entrySet()) {
            editor.putString(entry.getKey(), nowSec + ";" + entry.getValue());
        }

        editor.apply();
    }

    public static void clearSkuDetailsCache() {
        long nowSec = System.currentTimeMillis() / 1000L;

        // Sku details cache
        long lastClearedTimeSec = skuDetailSharedPrefs.getLong(SKU_DETAIL_LAST_CLEARED_TIME, 0);
        if (lastClearedTimeSec == 0) {
            skuDetailSharedPrefs.edit()
                    .putLong(SKU_DETAIL_LAST_CLEARED_TIME, nowSec)
                    .apply();
        } else if ((nowSec - lastClearedTimeSec) > SKU_DETAIL_CACHE_CLEAR_TIME_LIMIT_SEC) {
            skuDetailSharedPrefs.edit()
                    .clear()
                    .putLong(SKU_DETAIL_LAST_CLEARED_TIME, nowSec)
                    .apply();
        }
    }
}
