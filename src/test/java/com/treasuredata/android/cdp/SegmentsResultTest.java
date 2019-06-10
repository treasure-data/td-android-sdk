package com.treasuredata.android.cdp;

import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;

public class SegmentsResultTest {

    @Test
    public void test_IOException_should_return_CONNECTION_ERROR_CODE() {
        SegmentsResult result = new SegmentsExceptionResult(new IOException());
        // assertEquals(result.getErrorCode(), CONNECTION_ERROR);
    }

    @Test
    public void test_responses_success() throws Exception {
        // JSON with absence of "error" attribute is considered success,
        // we may have to change this later.
        JSONObject responseJson = new JSONObject();

        // IUserSegmentResult result = new SegmentsExceptionResult(responseJson);

        // assertTrue(result.isSuccess());
        // assertNull(result.getErrorCode());
        // assertNull(result.getErrorDescription());
    }

    @Test
    public void test_responses_with_error_should_return_DATA_ERROR() throws Exception {
        JSONObject responseJson = new JSONObject()
                        .append("error", "Bad Request")
                        .append("message", "Blah blah blah");

        // IUserSegmentResult result = new SegmentsResult(responseJson);

        // assertFalse(result.isSuccess());
        // assertEquals(result.getErrorCode(), DATA_ERROR);
        // assertEquals(result.getErrorDescription(), responseJson.getString("message"));
    }

    @Test
    public void test_responses_with_error_should_return_DATA_ERROR__with_empty_message() throws Exception {
        JSONObject responseJson = new JSONObject()
                .append("error", "Bad Request");

        // IUserSegmentResult result = new SegmentsExceptionResult(responseJson);

        // assertFalse(result.isSuccess());
        // assertEquals(result.getErrorCode(), DATA_ERROR);
        // assertNull(result.getErrorDescription(), responseJson.getString("message"));
    }

    @Test
    public void test_unrecognizable_response_should_return_DATA_ERROR_CODE() throws Exception{
        String jsonErrorMessage = "Any JSON error message!";
        // IUserSegmentResult result = new SegmentsResult(new JSONException(jsonErrorMessage));
        // assertEquals(result.getErrorCode(), DATA_ERROR);
        // // Shouldjust forward whatever the message
        // assertEquals(result.getErrorDescription(), jsonErrorMessage);
    }

}