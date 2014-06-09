package com.treasuredata.android;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TDLoggingTest {
    @After
    public void tearDown() {
        TDLogging.disableLogging();
    }

    @Test
    public void testEnableLoggingDisableLogging() {
        TDLogging.enableLogging();
        assertTrue(TDLogging.isInitialized());
        assertTrue(TDLogging.isEnabled());

        TDLogging.disableLogging();
        assertTrue(TDLogging.isInitialized());
        assertFalse(TDLogging.isEnabled());

        TDLogging.enableLogging();
        assertTrue(TDLogging.isInitialized());
        assertTrue(TDLogging.isEnabled());
    }
}