package com.treasuredata.android;

import android.os.AsyncTask;
import android.content.Context;

import org.komamitsu.android.util.Log;

import java.lang.reflect.Method;

class GetAdvertisingIdAsyncTask extends AsyncTask<Context, Void, String> {
    private static final String TAG = GetAdvertisingIdAsyncTask.class.getSimpleName();
    private static Object advertisingInfo;
    private static Method isLimitAdTrackingEnabledMethod;
    private static Method getIdMethod;
    private final GetAdvertisingIdAsyncTaskCallback callback;

    GetAdvertisingIdAsyncTask(GetAdvertisingIdAsyncTaskCallback callback) {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Context... params) {
        Context context = params[0];
        try {
            if (advertisingInfo == null) {
                advertisingInfo = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                        .getMethod("getAdvertisingIdInfo", Context.class)
                        .invoke(null, context);
                isLimitAdTrackingEnabledMethod = advertisingInfo.getClass()
                        .getMethod("isLimitAdTrackingEnabled");
                getIdMethod = advertisingInfo.getClass().getMethod("getId");
            }
            if (!(Boolean) isLimitAdTrackingEnabledMethod.invoke(advertisingInfo)) {
                return (String) getIdMethod.invoke(advertisingInfo);
            } else {
                return null;
            }
        } catch (ClassNotFoundException e) {
            // Customer does not include google services ad library, indicate not wanting to track Advertising Id
            Log.w(TAG, "Exception getting advertising id: " + e.getMessage(), e);
            Log.w(TAG, "You are attempting to enable auto append Advertising Identifer but AdvertisingIdClient class is not detected. To use this feature, you must use Google Mobile Ads library");
            return null;
        } catch (Exception e) {
            Log.w(TAG, "Exception getting advertising id: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String advertisingId) {
        callback.onGetAdvertisingIdAsyncTaskCompleted(advertisingId);
    }
}
