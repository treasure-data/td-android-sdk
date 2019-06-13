package com.treasuredata.android.cdp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

abstract class JSONFetchUserSegmentsResult extends FetchUserSegmentsResult {
    final int statusCode;

    JSONFetchUserSegmentsResult(int statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * @throws JSONException            if the provided body is not json
     * @throws IllegalArgumentException if the parsed json is neither object or array, or has unexpected scheme
     */
    static FetchUserSegmentsResult createJSONResult(int status, String body) throws JSONException, IllegalArgumentException {
        Object json = new JSONTokener(body).nextValue();
        if (status != 200) {
            // Immediate consider this is an error for non-200 status code,
            // try to extract for "error" and "message" in the response body
            try {
                return new JSONObjectFetchUserSegmentsResult(status, (JSONObject) json);
            } catch (ClassCastException e) {
                // If this is not a JSON object then just throw for the caller to handle
                throw new IllegalArgumentException(e);
            }
        } else if (json instanceof JSONObject) {
            return new JSONObjectFetchUserSegmentsResult(status, (JSONObject) json);
        } else if (json instanceof JSONArray) {
            return new JSONArrayFetchUserSegmentsResult(status, (JSONArray) json);
        } else {
            throw new IllegalArgumentException(
                    "Expect either an JSON Object or Array while received: " +
                            (json != null ? json.getClass() : "null"));
        }
    }

    /**
     * JSON Array responses are considered success, expect to be an array of Profiles
     */
    private static class JSONArrayFetchUserSegmentsResult extends JSONFetchUserSegmentsResult {
        private final JSONArray json;

        JSONArrayFetchUserSegmentsResult(int responseCode, JSONArray json) {
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
     * Error response, expect to be in the form of <code>{"error":..., "message":..., "status":...}</code>
     */
    private static class JSONObjectFetchUserSegmentsResult extends JSONFetchUserSegmentsResult {
        private final JSONObject json;

        JSONObjectFetchUserSegmentsResult(int httpStatusCode, JSONObject json) {
            super(httpStatusCode);
            this.json = json;
        }

        @Override
        void invoke(FetchUserSegmentsCallback callback) {
            callback.onError(CDPAPIException.from(statusCode, json));
        }
    }
}
