package com.treasuredata.android.cdp;

import org.json.JSONException;
import org.json.JSONObject;

public class CdpApiException extends Exception {

    private int statusCode;
    private String error;

    /**
     * @param statusCode HTTP status code
     * @param error is nullable, in case the HTTP response body is not a JSON
     * @param message, either the `message` property in response JSON, or the entire body
     */
    CdpApiException(int statusCode, String error, String message) {
        super(message);
        this.statusCode = statusCode;
        this.error = error;
    }

    /**
     * Original "error" property from the responded error JSON from server
     * Will be null if the response body is not a JSON
     * */
    public String getError() {
        return error;
    }

    /**
     * HTTP Status Code responded from server
     */
    public int getStatusCode() {
        return statusCode;
    }

    static CdpApiException from(int statusCode, String body) {
        return new CdpApiException(statusCode, null, body);
    }

    /**
     * @param json Error body response from CDP API
     */
    static CdpApiException from(int statusCode, JSONObject json) {
        if (json == null) {
            return new CdpApiException(statusCode, null, null);
        }
        String error = "";
        String message = "";
        try {
            if (json.has("error")) error = json.getString("error");
            if (json.has("message")) message = json.getString("message");
        } catch (JSONException e) {
            // Since we checked,
        }
        if (error.isEmpty() && message.isEmpty()) {
            // Hopefully doesn't happen, but if the received JSON Object contains unexpected schema,
            // then use the entire JSON as the exception message.
            return new CdpApiException(statusCode, null, json.toString());
        } else {
            return new CdpApiException(statusCode, error, message);
        }
    }

}
