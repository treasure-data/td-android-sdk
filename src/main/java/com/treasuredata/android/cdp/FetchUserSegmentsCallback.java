package com.treasuredata.android.cdp;

import java.util.List;

public interface FetchUserSegmentsCallback {
    /**
     * Handle success looked up segments result
     *
     * @param profiles that matches with the specified query
     */
    void onSuccess(List<Profile> profiles);

    /**
     * Handle failure
     *
     * @param e the exception could be:
     * - {@link CDPAPIException},
     * - {@link org.json.JSONException},
     * or any unexpected upstream exception (IOException for example).
     */
    void onError(Exception e);

}
