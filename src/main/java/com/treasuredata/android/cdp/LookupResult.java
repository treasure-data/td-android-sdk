package com.treasuredata.android.cdp;

import org.json.JSONException;

abstract class LookupResult {

    abstract void invoke(FetchUserSegmentsCallback callback);

    static LookupResult create(Exception exception) {
        return new ErrorLookupResult(exception);
    }

    static LookupResult create(int status, String body) {
        try {
            // Assume body is a JSON, if it gets throw then fallback into into (error) raw string body.
            return JSONLookupResult.createJSONResult(status, body);
        } catch (JSONException | IllegalArgumentException e) {
            return ErrorLookupResult.create(CDPAPIException.from(status, body));
        }
    }

}
