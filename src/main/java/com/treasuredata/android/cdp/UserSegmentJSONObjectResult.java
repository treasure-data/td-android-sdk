package com.treasuredata.android.cdp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

abstract class SegmentsJSONResult extends SegmentsResult {
    final int httpStatusCode;

    SegmentsJSONResult(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    static SegmentsResult createJSONResult(int status, String body) {
        Object json;
        try {
            json = new JSONTokener(body).nextValue();
        } catch (JSONException e) {
            return new SegmentsExceptionResult(e);
        }

        if (json instanceof JSONObject) {
            return new SegmentsJSONObjectResult(status, (JSONObject) json);
        } else if (json instanceof JSONArray) {
            return new SegmentsJSONArrayResult(status, (JSONArray) json);
        } else {
            // FIXME: null.getClass()?
            return new SegmentsExceptionResult(
                    new IllegalArgumentException(format("Unexpected JSON format. %s is not a JSON type", json.getClass())));
        }
    }

    private static class SegmentsJSONArrayResult extends SegmentsJSONResult {
        private final JSONArray json;

        SegmentsJSONArrayResult(int responseCode, JSONArray json) {
            super(responseCode);
            this.json = json;
        }

        @Override
        void invoke(FetchUserSegmentsCallback callback) {
            List<Audience> audiences = new ArrayList<>();
            try {
                for (int i = 0; i < json.length(); i++) {
                    audiences.add(AudienceImpl.fromJSONObject(json.getJSONObject(i)));
                }
            } catch (JSONException e) {
                callback.onError(e);
                return;
            }
            callback.onSuccess(audiences);
        }
    }

    private static class SegmentsJSONObjectResult extends SegmentsJSONResult {
        private final JSONObject json;

        SegmentsJSONObjectResult(int httpStatusCode, JSONObject json) {
            super(httpStatusCode);
            this.json = json;
        }

        @Override
        void invoke(FetchUserSegmentsCallback callback) {
            String error = "";
            String message = "";
            try {
                if (json.has("error")) error = json.getString("error");
                if (json.has("message")) message = json.getString("message");
            } catch (JSONException e) {
                // Should not happen, even if it is, just silence
            }
            // TODO: Consider making a better semantic exception
            callback.onError(new IllegalArgumentException(httpStatusCode + " - " + error + " - " + message));
        }
    }
}
