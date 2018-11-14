package com.treasuredata.android.internal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.treasuredata.android.TreasureData;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class InAppPurchaseEventManager {
    private static final String TAG = InAppPurchaseEventManager.class.getSimpleName();

    // Purchase types
    private static final String SUBSCRIPTION = "subs";
    private static final String INAPP = "inapp";

    private static final String SKU_DETAILS_SHARED_PREF_NAME =
            "td_sdk_sku_details";
    private static final String PURCHASE_INAPP_SHARED_PREF_NAME =
            "td_sdk_sku_purchase_inapp";
    private static final String PURCHASE_SUBS_SHARED_PREF_NAME =
            "td_sdk_sku_purchase_subs";
    private static final SharedPreferences skuDetailSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(SKU_DETAILS_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    private static final SharedPreferences purchaseInappSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(PURCHASE_INAPP_SHARED_PREF_NAME, Context.MODE_PRIVATE);
    private static final SharedPreferences purchaseSubsSharedPrefs =
            TreasureData.getApplicationContext().getSharedPreferences(PURCHASE_SUBS_SHARED_PREF_NAME, Context.MODE_PRIVATE);

    private static final int PURCHASE_EXPIRE_TIME_SEC = 12 * 60 * 60; // 12 h

    private InAppPurchaseEventManager() {

    }

    public static ArrayList<String> getPurchasesInapp(Context context, Object inAppBillingObj) {

        return filterAndCachePurchasesInapp(InAppBillingDelegate.getPurchases(context, inAppBillingObj, INAPP));
    }

    private static ArrayList<String> filterAndCachePurchasesInapp(List<String> purchases) {
        ArrayList<String> filteredPurchases = new ArrayList<>();
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
                Log.e(TAG, "Unable to parse purchase, not json object: ", e);
            }
        }

        editor.apply();

        return filteredPurchases;
    }
}
