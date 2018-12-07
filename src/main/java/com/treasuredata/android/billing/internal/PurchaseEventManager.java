package com.treasuredata.android.billing.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.treasuredata.android.TreasureData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.treasuredata.android.billing.internal.PurchaseConstants.INAPP;
import static com.treasuredata.android.billing.internal.PurchaseConstants.SUBSCRIPTION;

class PurchaseEventManager {
    private static final String TAG = PurchaseEventManager.class.getSimpleName();

    private static final String SKU_DETAILS_SHARED_PREF_NAME =
            "td_sdk_sku_details";
    private static final String PURCHASE_INAPP_SHARED_PREF_NAME =
            "td_sdk_purchase_inapp";
    private static final String PURCHASE_SUBS_SHARED_PREF_NAME =
            "td_sdk_purchase_subs";
    private static final SharedPreferences skuDetailSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(SKU_DETAILS_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    private static final SharedPreferences purchaseInappSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(PURCHASE_INAPP_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    private static final SharedPreferences purchaseSubsSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(PURCHASE_SUBS_SHARED_PREF_NAME, Context.MODE_PRIVATE);

    private static final int PURCHASE_EXPIRE_DURATION_SEC = 24 * 60 * 60; // 24 h

    // SKU detail cache setting
    private static final int SKU_DETAIL_EXPIRE_DURATION_SEC = 24 * 60 * 60; // 24 h
    private static final int SKU_DETAIL_CACHE_CLEAR_DURATION_SEC = 7 * 24 * 60 * 60; // 7 days

    private static final String SKU_DETAIL_LAST_CLEARED_TIME = "SKU_DETAIL_LAST_CLEARED_TIME";

    private PurchaseEventManager() {

    }

    public static List<Purchase> getPurchasesInapp(Context context, Object inAppBillingObj) {

        return filterAndCachePurchasesInapp(BillingDelegate.getPurchases(context, inAppBillingObj, INAPP));
    }

    public static List<Purchase> getPurchaseHistoryInapp(Context context, Object inAppBillingObj) {
        return filterAndCachePurchasesInapp(BillingDelegate.getPurchaseHistory(context, inAppBillingObj, INAPP));
    }

    public static List<Purchase> getPurchasesSubs(Context context, Object inAppBillingObj) {

        return resolveAndCachePurchasesSubs(BillingDelegate.getPurchases(context, inAppBillingObj, SUBSCRIPTION));
    }

    private static List<Purchase> filterAndCachePurchasesInapp(List<String> purchases) {
        List<Purchase> filteredPurchases = new ArrayList<>();
        SharedPreferences.Editor editor = purchaseInappSharedPrefs.edit();
        long nowSec = System.currentTimeMillis() / 1000L;
        for (String purchase : purchases) {
            try {
                JSONObject purchaseJson = new JSONObject(purchase);
                String sku = purchaseJson.getString("productId");
                long purchaseTimeMillis = purchaseJson.getLong("purchaseTime");
                String purchaseToken = purchaseJson.getString("purchaseToken");
                if (nowSec - purchaseTimeMillis / 1000L > PURCHASE_EXPIRE_DURATION_SEC) {
                    continue;
                }

                String oldPurchaseToken = purchaseInappSharedPrefs.getString(sku, "");

                if (oldPurchaseToken.equals(purchaseToken)) {
                    continue;
                }

                // Write new purchase into cache
                editor.putString(sku, purchaseToken);
                filteredPurchases.add(new Purchase(purchase));
            } catch (JSONException e) {
                Log.e(TAG, "Unable to parse purchase, not a json object: ", e);
            }
        }

        editor.apply();

        return filteredPurchases;
    }

    private static List<Purchase> resolveAndCachePurchasesSubs(List<String> purchases) {
        List<Purchase> resolvedPurchases = new ArrayList<>();
        for (String purchase : purchases) {
            try {
                JSONObject purchaseJson = new JSONObject(purchase);
                String sku = purchaseJson.getString("productId");
                String purchaseToken = purchaseJson.getString("purchaseToken");

                String oldPurchase = purchaseSubsSharedPrefs.getString(sku, "");
                JSONObject oldPurchaseJson = oldPurchase.isEmpty()
                        ? new JSONObject() : new JSONObject(oldPurchase);
                String oldPurchaseToken = oldPurchaseJson.optString("purchaseToken");

                Purchase.SubscriptionStatus subscriptionStatus = null;

                if (!oldPurchaseToken.equals(purchaseToken)) {
                    // New purchase is always true for autoRenewing
                    if (!purchaseJson.getBoolean("autoRenewing")) {
                        continue;
                    }
                    subscriptionStatus = Purchase.SubscriptionStatus.New;
                }else if (!oldPurchase.isEmpty()) {
                    boolean oldAutoRenewing = oldPurchaseJson.getBoolean("autoRenewing");
                    boolean newAutoRenewing = purchaseJson.getBoolean("autoRenewing");

                    if (!newAutoRenewing && oldAutoRenewing) {
                        subscriptionStatus = Purchase.SubscriptionStatus.Cancelled;
                    } else if (!oldAutoRenewing && newAutoRenewing) {
                        subscriptionStatus = Purchase.SubscriptionStatus.Restored;
                    } else { // newAutoRenewing == oldAutoRenewing, tracked already
                        continue;
                    }
                }

                resolvedPurchases.add(new Purchase(purchase, subscriptionStatus));

                purchaseSubsSharedPrefs.edit().putString(sku, purchase).apply();
            } catch (JSONException e) {
                Log.e(TAG, "Unable to parse purchase, not a json object: ", e);
            }
        }

        // SubscriptionStatus.Expired
        resolvedPurchases.addAll(getExpiredPurchaseSubs(purchases));
        return resolvedPurchases;
    }

    private static List<Purchase> getExpiredPurchaseSubs(List<String> currentPurchases) {
        List<Purchase> expiredPurchases = new ArrayList<>();
        Map<String,?> keys = purchaseSubsSharedPrefs.getAll();

        if (keys.isEmpty()) {
            return expiredPurchases;
        }

        Set<String> currSkuSet = new HashSet<>();
        for (String purchase : currentPurchases) {
            try {
                JSONObject purchaseJson = new JSONObject(purchase);
                currSkuSet.add(purchaseJson.getString("productId"));
            } catch (JSONException e) {
                Log.e(TAG, "Unable to parse purchase, not a json object:", e);
            }
        }

        Set<String> expiredSkus = new HashSet<>();
        for (Map.Entry<String,?> entry : keys.entrySet()){
            String sku = entry.getKey();
            if (!currSkuSet.contains(sku)) {
                expiredSkus.add(sku);
            }
        }

        SharedPreferences.Editor editor = purchaseSubsSharedPrefs.edit();
        for (String expiredSku : expiredSkus) {
            String expiredPurchase = purchaseSubsSharedPrefs.getString(expiredSku, "");

            // Do not need to cache expired purchase any more
            editor.remove(expiredSku);

            if (!expiredPurchase.isEmpty()) {
                expiredPurchases.add(new Purchase(expiredPurchase, Purchase.SubscriptionStatus.Expired));
            }
        }
        editor.apply();

        return expiredPurchases;
    }

    public static Map<String, String> getAndCacheSkuDetails(
            Context context, Object inAppBillingObj, List<String> skuList, String type) {

        Map<String, String> skuDetailsMap = readSkuDetailsFromCache(skuList);

        ArrayList<String> newSkuList = new ArrayList<>();
        for (String sku : skuList) {
            if (!skuDetailsMap.containsKey(sku)) {
                newSkuList.add(sku);
            }
        }

        skuDetailsMap.putAll(BillingDelegate.getSkuDetails(
                context, inAppBillingObj, newSkuList, type));
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
                if (nowSec - timeSec < SKU_DETAIL_EXPIRE_DURATION_SEC) {
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

    public static void clearAllSkuDetailsCache() {
        long nowSec = System.currentTimeMillis() / 1000L;

        // Sku details cache
        long lastClearedTimeSec = skuDetailSharedPrefs.getLong(SKU_DETAIL_LAST_CLEARED_TIME, 0);
        if (lastClearedTimeSec == 0) {
            skuDetailSharedPrefs.edit()
                    .putLong(SKU_DETAIL_LAST_CLEARED_TIME, nowSec)
                    .apply();
        } else if ((nowSec - lastClearedTimeSec) > SKU_DETAIL_CACHE_CLEAR_DURATION_SEC) {
            skuDetailSharedPrefs.edit()
                    .clear()
                    .putLong(SKU_DETAIL_LAST_CLEARED_TIME, nowSec)
                    .apply();
        }
    }
}
