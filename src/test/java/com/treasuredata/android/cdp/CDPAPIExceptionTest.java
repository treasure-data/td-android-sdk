package com.treasuredata.android.cdp;

import org.junit.Test;

import static org.junit.Assert.*;

public class CDPAPIExceptionTest {

    @Test
    public void typical_error_json() {
        CDPAPIException e = CDPAPIException.from(
                200,
                "{\"error\": \"Bad Request\", \"message\": \"you did xyz wrong\", \"status\": 400}");

        // Actual HTTP status code (200) is ignored,
        // since CDP API server always response 200 except IO errors
        assertEquals(400, e.getStatus());
        assertEquals("Bad Request", e.getError());
        assertEquals("you did xyz wrong", e.getMessage());
    }

    @Test
    public void if_status_property_is_missing_then_use_http_status_code() {
        CDPAPIException e = CDPAPIException.from(
                200,
                "{\"error\": \"Bad Request\", \"message\": \"you did xyz wrong\"}");

        assertEquals(200, e.getStatus());
        assertEquals("Bad Request", e.getError());
        assertEquals("you did xyz wrong", e.getMessage());
    }

    @Test
    public void non_json_body() {
        CDPAPIException e = CDPAPIException.from(
                401,
                "<body>");

        assertEquals(401, e.getStatus());
        assertNull("Bad Request", e.getError());
        assertEquals("<body>", e.getMessage());
    }

}