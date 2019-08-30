package com.treasuredata.android;

import android.os.AsyncTask;
import android.content.Context;

import org.komamitsu.android.util.Log;

class GetAdvertisingIdAsyncTask extends AsyncTask<Context, Void, String> {
    private static final String TAG = GetAdvertisingIdAsyncTask.class.getSimpleName();
    private final GetAdvertisingIdAsyncTaskCallback callback;

    GetAdvertisingIdAsyncTask(GetAdvertisingIdAsyncTaskCallback callback) {
        this.callback = callback;
    }

    @Override
    protected String doInBackground(Context... params) {
        Context context = params[0];
        try {
            Object advertisingInfo = Class.forName("com.google.android.gms.ads.identifier.AdvertisingIdClient")
                    .getMethod("getAdvertisingIdInfo", Context.class)
                    .invoke(null, context);
            Boolean isLimitAdTrackingEnabled = (Boolean) advertisingInfo.getClass()
                    .getMethod("isLimitAdTrackingEnabled")
                    .invoke(advertisingInfo);
            if (!isLimitAdTrackingEnabled) {
                String advertisingId = (String) advertisingInfo.getClass()
                        .getMethod("getId")
                        .invoke(advertisingInfo);
                return advertisingId;
            } else {
                return null;
            }
        } catch (Exception e) {
            // Customer does not include google services ad library, indicate not wanting to track Advertising Id
            Log.w(TAG, "Exception getting advertising id: " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String advertisingId) {
        callback.onGetAdvertisingIdAsyncTaskCompleted(advertisingId);
    }
}
