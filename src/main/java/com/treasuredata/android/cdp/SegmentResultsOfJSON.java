package com.treasuredata.android.cdp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

abstract class SegmentsResultOfJSON extends SegmentsResult {
    final int statusCode;

    SegmentsResultOfJSON(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @throws JSONException            if the provided body is not json
     * @throws IllegalArgumentException if the parsed json is neither object or array, or has unexpected scheme
     */
    static SegmentsResult createJSONResult(int status, String body) throws JSONException, IllegalArgumentException {
        Object json = new JSONTokener(body).nextValue();
        if (status != 200) {
            // Immediate consider this is an error for non-200 status code,
            // try to extract for "error" and "message" in the response body
            try {
                return new SegmentsResultOfJSONObject(status, (JSONObject) json);
            } catch (ClassCastException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (json instanceof JSONObject) {
            return new SegmentsResultOfJSONObject(status, (JSONObject) json);
        } else if (json instanceof JSONArray) {
            return new SegmentsResultOfJSONArray(status, (JSONArray) json);
        } else {
            throw new IllegalArgumentException(
                    "Expect either an JSON Object or Array while received: " +
                            (json != null ? json.getClass() : "null"));
        }
    }

    /**
     * JSON Array responses are considered success, expect to be an array of Profiles
     */
    private static class SegmentsResultOfJSONArray extends SegmentsResultOfJSON {
        private final JSONArray json;

        SegmentsResultOfJSONArray(int responseCode, JSONArray json) {
            super(responseCode);
            this.json = json;
        }

        @Override
        void invoke(FetchUserSegmentsCallback callback) {
            List<Profile> profiles = new ArrayList<>();
            try {
                for (int i = 0; i < json.length(); i++) {
                    profiles.add(ProfileImpl.fromJSONObject(json.getJSONObject(i)));
                }

            } catch (JSONException e) {
                callback.onError(e);
                return;
            }
            callback.onSuccess(profiles);
        }
    }

    /**
     * Error response, expect to be in the form of <code>{"error":..., "message":... }</code>
     */
    private static class SegmentsResultOfJSONObject extends SegmentsResultOfJSON {
        private final JSONObject json;

        SegmentsResultOfJSONObject(int httpStatusCode, JSONObject json) {
            super(httpStatusCode);
            this.json = json;
        }

        @Override
        void invoke(FetchUserSegmentsCallback callback) {
            callback.onError(CDPAPIException.from(statusCode, json));
        }
    }
}
