package com.treasuredata.android.cdp;

import org.json.JSONException;

abstract class SegmentsResult {

    abstract void invoke(FetchUserSegmentsCallback callback);

    static SegmentsResult create(Exception exception) {
        return new SegmentResultsOfException(exception);
    }

    static SegmentsResult create(int status, String body) {
        try {
            // Assume body is a JSON, if it gets throw then fallback into into (error) raw string body.
            return SegmentsResultOfJSON.createJSONResult(status, body);
        } catch (JSONException | IllegalArgumentException e) {
            return SegmentResultsOfException.create(CDPAPIException.from(status, body));
        }
    }

}
