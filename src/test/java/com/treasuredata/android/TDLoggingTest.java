package com.treasuredata.android;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TDLoggingTest {
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