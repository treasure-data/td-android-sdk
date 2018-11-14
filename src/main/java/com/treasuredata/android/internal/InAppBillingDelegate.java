package com.treasuredata.android.internal;

import android.content.Context;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class InAppBillingDelegate {

    private static final String TAG = InAppBillingDelegate.class.getSimpleName();

    // Method and class cache
    private static final HashMap<String, Method> methodMap =
            new HashMap<>();
    private static final HashMap<String, Class<?>> classMap =
            new HashMap<>();

    // In App Billing Service class names
    private static final String IN_APP_BILLING_SERVICE_STUB =
            "com.android.vending.billing.IInAppBillingService$Stub";
    private static final String IN_APP_BILLING_SERVICE =
            "com.android.vending.billing.IInAppBillingService";

    // In App Billing Service method names
    private static final String AS_INTERFACE = "asInterface";
    private static final String GET_SKU_DETAILS = "getSkuDetails";
    private static final String GET_PURCHASES = "getPurchases";
    private static final String GET_PURCHASE_HISTORY = "getPurchaseHistory";
    private static final String IS_BILLING_SUPPORTED = "isBillingSupported";

    // In App Purchase key
    private static final String RESPONSE_CODE = "RESPONSE_CODE";
    private static final String ITEM_ID_LIST = "ITEM_ID_LIST";
    private static final String DETAILS_LIST = "DETAILS_LIST";
    private static final String INAPP_PURCHASE_DATA_LIST = "INAPP_PURCHASE_DATA_LIST";
    private static final String INAPP_CONTINUATION_TOKEN = "INAPP_CONTINUATION_TOKEN";

    private static final int MAX_QUERY_PURCHASE_NUM = 30;
    private static final int PURCHASE_STOP_QUERY_TIME_SEC = 30 * 60; // 30 minutes

    private InAppBillingDelegate() {

    }

    public static boolean hasBillingService()
    {
        try {
            Class.forName(IN_APP_BILLING_SERVICE_STUB);
           return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Nullable
    public static Object asInterface(Context context, IBinder service) {
        Object[] args = new Object[]{service};
        return invokeMethod(context, IN_APP_BILLING_SERVICE_STUB,
                AS_INTERFACE, null, args);
    }

    public static boolean isBillingSupported(Context context,
                                             Object inAppBillingObj, String type) {
        if (inAppBillingObj == null) {
            return false;
        }

        Object[] args = new Object[]{3, context.getApplicationContext().getPackageName(), type};
        Object result = invokeMethod(context, IS_BILLING_SUPPORTED, inAppBillingObj, args);

        return result != null && ((int) result) == 0;
    }

    /**
     * Returns the current SKUs owned by the user of the type and package name specified along with
     * purchase information and a signature of the data to be validated.
     * This will return all SKUs that have been purchased that have not been consumed.
     */
    public static ArrayList<String> getPurchases(Context context, Object inAppBillingObj, String type) {
        ArrayList<String> purchases = new ArrayList<>();

        if (inAppBillingObj == null) {
            return purchases;
        }

        if (!isBillingSupported(context, inAppBillingObj, type)) {
            return purchases;
        }

        String continuationToken = null;
        int queriedPurchaseCount = 0;

        do {
            Object[] args = new Object[]{3, context.getApplicationContext().getPackageName(), type, continuationToken};
            Object resultObject = invokeMethod(context, GET_PURCHASES, inAppBillingObj, args);

            continuationToken = null;

            if (resultObject != null) {
                Bundle purchaseBundle = (Bundle) resultObject;
                int response = purchaseBundle.getInt(RESPONSE_CODE);
                if (response == 0) {
                    ArrayList<String> purchaseDataList =
                            purchaseBundle.getStringArrayList(INAPP_PURCHASE_DATA_LIST);

                    if (purchaseDataList == null || purchaseDataList.isEmpty()) {
                        break;
                    }
                    queriedPurchaseCount += purchaseDataList.size();
                    purchases.addAll(purchaseDataList);
                    continuationToken = purchaseBundle.getString(INAPP_CONTINUATION_TOKEN);
                }
            }
        } while (queriedPurchaseCount < MAX_QUERY_PURCHASE_NUM
                && continuationToken != null);

        return purchases;
    }

    /**
     * Returns the most recent purchase made by the user for each SKU, even if that purchase is
     * expired, canceled, or consumed.
     */
    public static ArrayList<String> getPurchaseHistory(Context context, Object inAppBillingObj, String type) {
        ArrayList<String> purchases = new ArrayList<>();

        if (!isBillingSupported(context, inAppBillingObj, type)) {
            return purchases;
        }
        String continuationToken = null;
        int queriedPurchaseCount = 0;
        boolean reachTimeLimit = false;

        do {
            Object[] args = new Object[]{
                    6, context.getApplicationContext().getPackageName(), type, continuationToken, new Bundle()};
            continuationToken = null;

            Object resultObject = invokeMethod(context, IN_APP_BILLING_SERVICE,
                    GET_PURCHASE_HISTORY, inAppBillingObj, args);

            if (resultObject == null) {
                break;
            }

            long nowSec = System.currentTimeMillis() / 1000L;
            Bundle purchaseBundle = (Bundle) resultObject;
            int response = purchaseBundle.getInt(RESPONSE_CODE);
            if (response == 0) {
                ArrayList<String> purchaseDataList =
                        purchaseBundle.getStringArrayList(INAPP_PURCHASE_DATA_LIST);

                for (String purchaseData : purchaseDataList) {
                    try {
                        JSONObject purchaseJSON = new JSONObject(purchaseData);
                        long purchaseTimeSec =
                                purchaseJSON.getLong("purchaseTime") / 1000L;

                        if (nowSec - purchaseTimeSec > PURCHASE_STOP_QUERY_TIME_SEC) {
                            reachTimeLimit = true;
                            break;
                        } else {
                            purchases.add(purchaseData);
                            queriedPurchaseCount++;
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Unable to parse purchase, not a json object: ", e);
                    }
                }

                continuationToken = purchaseBundle.getString(INAPP_CONTINUATION_TOKEN);
            }

        } while (queriedPurchaseCount < MAX_QUERY_PURCHASE_NUM
                && continuationToken != null
                && !reachTimeLimit);

        return purchases;
    }

    /**
     * Provides details of a list of SKUs
     * Given a list of SKUs of a valid type, this returns a map with key is each SKU id
     * and value is JSON string containing the productId, price, title and description.
     */
    public static Map<String, String> getSkuDetails(
            Context context, Object inAppBillingObj, ArrayList<String> skuList, String type) {
        Map<String, String> skuDetailsMap = new HashMap<>();

        if (inAppBillingObj == null || skuList.isEmpty()) {
            return skuDetailsMap;
        }

        if (!isBillingSupported(context, inAppBillingObj, type)) {
            return skuDetailsMap;
        }

        Bundle querySkus = new Bundle();
        querySkus.putStringArrayList(ITEM_ID_LIST, skuList);
        Object[] args = new Object[]{
                3, context.getApplicationContext().getPackageName(), type, querySkus};

        Object result = invokeMethod(context, IN_APP_BILLING_SERVICE,
                GET_SKU_DETAILS, inAppBillingObj, args);

        if (result != null) {
            Bundle bundle = (Bundle) result;
            int response = bundle.getInt(RESPONSE_CODE);
            if (response == 0) {
                ArrayList<String> skuDetailsList = bundle.getStringArrayList(DETAILS_LIST);
                if (skuDetailsList != null && skuList.size() == skuDetailsList.size()) {
                    for (int i = 0; i < skuList.size(); i++) {
                        skuDetailsMap.put(skuList.get(i), skuDetailsList.get(i));
                    }
                }
            }
        }

        return skuDetailsMap;
    }

    @Nullable
    private static Object invokeMethod(Context context, String methodName, Object obj, Object[] args) {
        return invokeMethod(context, IN_APP_BILLING_SERVICE, methodName, obj, args);
    }

    @Nullable
    private static Object invokeMethod(Context context, String className, String methodName, Object obj, Object[] args) {
        Class<?> classObj = getClass(context, className);
        if (classObj == null) {
            return null;
        }

        Method methodObj = getMethod(classObj, methodName);
        if (methodObj == null) {
            return null;
        }

        if (obj != null) {
            obj = classObj.cast(obj);
        }

        try {
            return methodObj.invoke(obj, args);
        } catch (IllegalAccessException e) {
            Log.e(TAG,
                    "Illegal access to method "
                            + classObj.getName() + "." + methodObj.getName(), e);
        } catch (InvocationTargetException e) {
            Log.e(TAG,
                    "Invocation target exception in "
                            + classObj.getName() + "." + methodObj.getName(), e);
        }

        return null;
    }

    @Nullable
    private static Method getMethod(Class<?> classObj, String methodName) {
        Method method = methodMap.get(methodName);
        if (method != null) {
            return method;
        }

        try {
            Class<?>[] paramTypes = null;
            switch (methodName) {
                case AS_INTERFACE:
                    paramTypes = new Class[]{IBinder.class};
                    break;
                case GET_SKU_DETAILS:
                    paramTypes = new Class[]{
                            Integer.TYPE, String.class, String.class, Bundle.class};
                    break;
                case IS_BILLING_SUPPORTED:
                    paramTypes = new Class[]{
                            Integer.TYPE, String.class, String.class};
                    break;
                case GET_PURCHASES:
                    paramTypes = new Class[]{
                            Integer.TYPE, String.class, String.class, String.class};
                    break;
                case GET_PURCHASE_HISTORY:
                    paramTypes = new Class[]{
                            Integer.TYPE, String.class, String.class, String.class, Bundle.class};
                    break;
            }

            method = classObj.getDeclaredMethod(methodName, paramTypes);
            methodMap.put(methodName, method);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, classObj.getName() + "." + methodName + " method is not available", e);
        }

        return method;
    }

    @Nullable
    private static Class<?> getClass(Context context, String className) {
        Class<?> classObj = classMap.get(className);
        if (classObj != null) {
            return classObj;
        }

        try {
            classObj = context.getClassLoader().loadClass(className);
            classMap.put(className, classObj);
        } catch (ClassNotFoundException e) {
            Log.e(TAG, className + " is not available, please add "
                    + className + " to the project.", e);
        }

        return classObj;
    }
}
