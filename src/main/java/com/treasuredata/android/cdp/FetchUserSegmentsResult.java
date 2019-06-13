package com.treasuredata.android.cdp;

import org.json.JSONException;

abstract class FetchUserSegmentsResult {

    abstract void invoke(FetchUserSegmentsCallback callback);

    static FetchUserSegmentsResult create(Exception exception) {
        return new ErrorFetchUserSegmentsResult(exception);
    }

    static FetchUserSegmentsResult create(int status, String body) {
        try {
            // Assume body is a JSON, if it gets throw then fallback into into (error) raw string body.
            return JSONFetchUserSegmentsResult.createJSONResult(status, body);
        } catch (JSONException | IllegalArgumentException e) {
            return ErrorFetchUserSegmentsResult.create(CDPAPIException.from(status, body));
        }
    }

}
