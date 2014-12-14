package com.treasuredata.android;

import io.keen.client.java.KeenCallback;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenProject;
import junit.framework.TestCase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

public class TreasureDataTest extends TestCase {
    private static final String DUMMY_API_KEY = "dummy_api_key";
    boolean onSuccessCalledForAddEvent;
    boolean onSuccessCalledForUploadEvents;
    Exception exceptionOnFailedCalledForAddEvent;
    Exception exceptionOnFailedCalledForUploadEvents;
    String errorCodeForAddEvent;
    String errorCodeForUploadEvents;

    class MockTDClient extends TDClient {
        Exception exceptionOnQueueEventCalled;
        Exception exceptionOnSendQueuedEventsCalled;
        String errorCodeOnQueueEventCalled;
        String errorCodeOnSendQueuedEventsCalled;

        MockTDClient(String apiKey) throws IOException {
            super(apiKey);
        }

        @Override
        public void queueEvent(KeenProject project, String eventCollection, Map<String, Object> event, Map<String, Object> keenProperties, KeenCallback callback) {
            if (exceptionOnQueueEventCalled == null) {
                callback.onSuccess();
            }
            else {
                if (callback instanceof KeenCallbackWithErrorCode) {
                    KeenCallbackWithErrorCode callbackWithErrorCode = (KeenCallbackWithErrorCode) callback;
                    callbackWithErrorCode.setErrorCode(errorCodeOnQueueEventCalled);
                    callbackWithErrorCode.onFailure(exceptionOnQueueEventCalled);
                }
                else {
                    callback.onFailure(exceptionOnQueueEventCalled);
                }
            }
        }

        @Override
        public void sendQueuedEventsAsync(KeenProject project, KeenCallback callback) {
            if (exceptionOnSendQueuedEventsCalled == null) {
                callback.onSuccess();
            }
            else {
                if (callback instanceof KeenCallbackWithErrorCode) {
                    ((KeenCallbackWithErrorCode) callback).setErrorCode(errorCodeOnSendQueuedEventsCalled);
                }
                callback.onFailure(exceptionOnSendQueuedEventsCalled);
            }
        }
    }

    private TreasureData td;

    public void setUp() throws IOException {
        td = new TreasureData();
        init();
    }

    private void init() {
        onSuccessCalledForAddEvent = false;
        onSuccessCalledForUploadEvents = false;
        exceptionOnFailedCalledForAddEvent = null;
        exceptionOnFailedCalledForUploadEvents = null;
        errorCodeForAddEvent = null;
        errorCodeForUploadEvents = null;
    }

    private void enableCallbackForAddEvent() {
        TDCallback callback = new TDCallback() {
            @Override
            public void onSuccess() {
                onSuccessCalledForAddEvent = true;
                exceptionOnFailedCalledForAddEvent = null;
                errorCodeForAddEvent = null;
            }

            @Override
            public void onError(String errorCode, Exception e) {
                onSuccessCalledForAddEvent = false;
                exceptionOnFailedCalledForAddEvent = e;
                errorCodeForAddEvent = errorCode;
            }
        };
        td.setAddEventCallBack(callback);
    }

    private void enableCallbackForUploadEvents() {
        TDCallback callback = new TDCallback() {
            @Override
            public void onSuccess() {
                onSuccessCalledForUploadEvents = true;
                exceptionOnFailedCalledForUploadEvents = null;
                errorCodeForUploadEvents = null;
            }

            @Override
            public void onError(String errorCode, Exception e) {
                onSuccessCalledForUploadEvents = false;
                exceptionOnFailedCalledForUploadEvents = e;
                errorCodeForUploadEvents = errorCode;
            }
        };
        td.setUploadEventsCallBack(callback);
    }

    public void testApiKey() throws IOException, NoSuchAlgorithmException {
        String apikey1 = DUMMY_API_KEY + "1";
        MockTDClient client1 = new MockTDClient(apikey1);
        // System.out.println("client1.getDefaultProject().getProjectId()=" + client1.getDefaultProject().getProjectId());

        String apikey2 = DUMMY_API_KEY + "2";
        MockTDClient client2 = new MockTDClient(apikey2);
        // System.out.println("client2.getDefaultProject().getProjectId()=" + client2.getDefaultProject().getProjectId());

        assertNotSame(client1.getDefaultProject().getProjectId(), client2.getDefaultProject().getProjectId());
    }

    public void testAddEventAndUploadEventsWithoutCallBack() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);
        td.addEvent("db_", "tbl", "key", "val");
        td.uploadEvents();
    }

    public void testAddEventWithSuccess() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db_", "tbl", "key", "val");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
    }

    public void testAddEventWithError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        client.exceptionOnQueueEventCalled = new IOException("hello world");
        client.errorCodeOnQueueEventCalled = KeenClient.ERROR_CODE_STORAGE_ERROR;
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db_", "tbl", "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IOException);
        assertEquals("hello world", exceptionOnFailedCalledForAddEvent.getMessage());
        assertEquals(KeenClient.ERROR_CODE_STORAGE_ERROR, errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
    }

    public void testAddEventWithDatabaseNameError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        for (String db : Arrays.asList("db", "Db_", "db$")) {
            td.addEvent(db, "tbl", "key", "val");
            assertFalse(onSuccessCalledForAddEvent);
            assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
            assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
            assertFalse(onSuccessCalledForUploadEvents);
            assertNull(exceptionOnFailedCalledForUploadEvents);
            assertNull(errorCodeForUploadEvents);
        }
    }

    public void testAddEventWithTableNameError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        for (String tbl : Arrays.asList("tb", "tBl", "tbl$")) {
            td.addEvent(tbl, "tbl", "key", "val");
            assertFalse(onSuccessCalledForAddEvent);
            assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
            assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
            assertFalse(onSuccessCalledForUploadEvents);
            assertNull(exceptionOnFailedCalledForUploadEvents);
            assertNull(errorCodeForUploadEvents);
        }
    }

    public void testAddEventWithNullDatabaseName() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent(null, "tbl", "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
        assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
    }

    public void testAddEventWithNullTableName() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db_", null, "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
        assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
    }

    public void testUploadEventsWithSuccess() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.uploadEvents();
        assertFalse(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertTrue(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
    }

    public void testUploadEventsWithError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        client.exceptionOnSendQueuedEventsCalled = new IllegalArgumentException("foo bar");
        client.errorCodeOnSendQueuedEventsCalled = KeenClient.ERROR_CODE_NETWORK_ERROR;
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.uploadEvents();
        assertFalse(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(errorCodeForAddEvent);
        assertTrue(exceptionOnFailedCalledForUploadEvents instanceof IllegalArgumentException);
        assertEquals("foo bar", exceptionOnFailedCalledForUploadEvents.getMessage());
        assertEquals(KeenClient.ERROR_CODE_NETWORK_ERROR, errorCodeForUploadEvents);
    }
}