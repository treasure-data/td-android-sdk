package com.treasuredata.android.cdp;

import java.io.IOException;
import java.io.InputStream;

import static io.keen.client.java.KeenUtils.convertStreamToString;

abstract class SegmentsResult {

    abstract void invoke(FetchUserSegmentsCallback callback);

    static SegmentsResult create(Exception exception) {
        return new SegmentsExceptionResult(exception);
    }

    static SegmentsResult create(int status, InputStream bodyStream) {
        String body = convertStreamToString(bodyStream);
        if (status != 200) {
            return new SegmentsExceptionResult(
                    new IOException("Unexpected response: " + status + "\n" + body));
        }
        return SegmentsJSONResult.createJSONResult(status, body);
    }

}
