package com.treasuredata.android.cdp;

import java.util.List;

public interface FetchUserSegmentsCallback {
    /**
     * Receive looked up segments result
     */
    void onSuccess(List<Profile> profiles);

    /**
     * Could receive:
     * - {@link CDPAPIException},
     * - {@link org.json.JSONException},
     * or potentially an unexpected exception upstream.
     */
    void onError(Exception e);

}
