package com.treasuredata.android.cdp;

import org.json.JSONException;

import java.io.InputStream;

import static io.keen.client.java.KeenUtils.convertStreamToString;

abstract class SegmentsResult {

    abstract void invoke(FetchUserSegmentsCallback callback);

    static SegmentsResult create(Exception exception) {
        return new SegmentsExceptionResult(exception);
    }

    // FIXME: stream or string?
    static SegmentsResult create(int status, InputStream bodyStream) {
        String body = convertStreamToString(bodyStream);
        try {
            // Assume body is a JSON, if it gets throw then fallback into into (error) raw string body.
            return SegmentsJSONResult.createJSONResult(status, body);
        } catch (JSONException | IllegalArgumentException e) {
            return SegmentsExceptionResult.create(CdpApiException.from(status, body));
        }
    }

}
