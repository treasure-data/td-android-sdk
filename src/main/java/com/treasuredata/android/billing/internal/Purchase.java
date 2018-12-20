package com.treasuredata.android.billing.internal;

import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Purchase {
    private static final String TAG = Purchase.class.getSimpleName();
    private String originalJson;
    private String skuDetail;
    private SubscriptionStatus subscriptionStatus;

    enum SubscriptionStatus {
        New, Expired, Cancelled, Restored
    }

    public Purchase(String originalJson) {
        this(originalJson, null);
    }

    public Purchase(String originalJson, SubscriptionStatus subscriptionStatus) {
        this.originalJson = originalJson;
        this.subscriptionStatus = subscriptionStatus;
    }

    public Map<String, Object> toRecord() {
        Map<String, Object> record = new HashMap<>();
        try {
            JSONObject purchaseJSON = new JSONObject(originalJson);
            JSONObject skuDetailsJSON = new JSONObject(skuDetail);
            String productId = purchaseJSON.getString("productId");
            String orderId = purchaseJSON.optString("orderId");
            String title = skuDetailsJSON.optString("title");
            String price = skuDetailsJSON.getString("price");
            Long priceAmountMicros = skuDetailsJSON.getLong("price_amount_micros");
            String currency = skuDetailsJSON.getString("price_currency_code");
            String description = skuDetailsJSON.optString("description");
            String type = skuDetailsJSON.optString("type");
            Integer purchaseState = purchaseJSON.optInt("purchaseState");
            String developerPayload = purchaseJSON.optString("developerPayload");
            Long purchaseTime = purchaseJSON.getLong("purchaseTime");
            String purchaseToken = purchaseJSON.getString("purchaseToken");
            String packageName = purchaseJSON.optString("packageName");

            record.put(PurchaseConstants.IAP_PRODUCT_ID, productId);
            record.put(PurchaseConstants.IAP_ORDER_ID, orderId);
            record.put(PurchaseConstants.IAP_PRODUCT_TITLE, title);
            record.put(PurchaseConstants.IAP_PRODUCT_PRICE, price);
            record.put(PurchaseConstants.IAP_PRODUCT_PRICE_AMOUNT_MICROS, priceAmountMicros);
            record.put(PurchaseConstants.IAP_PRODUCT_CURRENCY, currency);

            // Quantity is always 1 for Android IAP purchase
            record.put(PurchaseConstants.IAP_QUANTITY, 1);
            record.put(PurchaseConstants.IAP_PRODUCT_TYPE, type);
            record.put(PurchaseConstants.IAP_PRODUCT_DESCRIPTION, description);
            record.put(PurchaseConstants.IAP_PURCHASE_STATE, purchaseState);
            record.put(PurchaseConstants.IAP_PURCHASE_DEVELOPER_PAYLOAD, developerPayload);
            record.put(PurchaseConstants.IAP_PURCHASE_TIME, purchaseTime);
            record.put(PurchaseConstants.IAP_PURCHASE_TOKEN, purchaseToken);
            record.put(PurchaseConstants.IAP_PACKAGE_NAME, packageName);

            if (type.equals(PurchaseConstants.SUBSCRIPTION)) {
                Boolean autoRenewing = purchaseJSON.optBoolean("autoRenewing",
                        false);

                if(subscriptionStatus == SubscriptionStatus.Expired) {
                    autoRenewing = false;
                }

                String subscriptionPeriod = skuDetailsJSON.optString("subscriptionPeriod");
                String freeTrialPeriod = skuDetailsJSON.optString("freeTrialPeriod");
                String introductoryPricePeriod = skuDetailsJSON.optString("introductoryPricePeriod");

                record.put(PurchaseConstants.IAP_SUBSCRIPTION_STATUS, subscriptionStatus);
                record.put(PurchaseConstants.IAP_SUBSCRIPTION_AUTORENEWING, autoRenewing);
                record.put(PurchaseConstants.IAP_SUBSCRIPTION_PERIOD, subscriptionPeriod);
                record.put(PurchaseConstants.IAP_FREE_TRIAL_PERIOD, freeTrialPeriod);

                if (!introductoryPricePeriod.isEmpty()) {
                    record.put(PurchaseConstants.IAP_INTRO_PRICE_PERIOD, introductoryPricePeriod);
                    Long introductoryPriceCycles = skuDetailsJSON.optLong("introductoryPriceCycles");
                    record.put(PurchaseConstants.IAP_INTRO_PRICE_CYCLES, introductoryPriceCycles);
                    Long introductoryPriceAmountMicros = skuDetailsJSON.getLong("introductoryPriceAmountMicros");
                    record.put(PurchaseConstants.IAP_INTRO_PRICE_AMOUNT_MICROS, introductoryPriceAmountMicros);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unable to parse purchase, not a json object:", e);
        }
        return record;
    }

    public void setSkuDetail(String skuDetail) {
        this.skuDetail = skuDetail;
    }

    public String getOriginalJson() {
        return originalJson;
    }
}
