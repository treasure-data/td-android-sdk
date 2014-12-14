package com.treasuredata.android;

import io.keen.client.java.KeenCallback;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenProject;
import junit.framework.TestCase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class TreasureDataTest extends TestCase {
    private static final String DUMMY_API_KEY = "dummy_api_key";
    boolean onSuccessCalledForAddEvent;
    boolean onSuccessCalledForUploadEvents;
    Exception exceptionOnFailedCalledForAddEvent;
    Exception exceptionOnFailedCalledForUploadEvents;
    String errorCodeForAddEvent;
    String errorCodeForUploadEvents;

    class Event {
        String tag;
        Map<String, Object> event;
        Event(String tag, Map<String, Object>event) {
            this.tag = tag;
            this.event = event;
        }
    }

    class MockTDClient extends TDClient {
        Exception exceptionOnQueueEventCalled;
        Exception exceptionOnSendQueuedEventsCalled;
        String errorCodeOnQueueEventCalled;
        String errorCodeOnSendQueuedEventsCalled;
        List<Event> addedEvent = new ArrayList<Event>();

        MockTDClient(String apiKey) throws IOException {
            super(apiKey);
        }

        @Override
        public void queueEvent(KeenProject project, String eventCollection, Map<String, Object> event, Map<String, Object> keenProperties, KeenCallback callback) {
            if (exceptionOnQueueEventCalled == null) {
                addedEvent.add(new Event(eventCollection, event));
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
        td = new TreasureData(client, null);
        td.addEvent("db_", "tbl", "key", "val");
        td.uploadEvents();
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }

    public void testAddEventAndUploadEventsWithDefaultDatabaseWithoutCallBack() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);
        td.setDefaultDatabase("db_");
        td.addEvent("tbl", "key", "val");
        td.uploadEvents();
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }

    public void testAddEventWithMapAndUploadEventsWithoutCallBack() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);
        Map<String, Object> records = new HashMap<String, Object>();
        records.put("key", "val");
        td.addEvent("db_", "tbl", records);
        td.uploadEvents();
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }

    public void testAddEventWithMapAndUploadEventsWithDefaultDatabaseWithoutCallBack() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);
        td.setDefaultDatabase("db_");
        Map<String, Object> records = new HashMap<String, Object>();
        records.put("key", "val");
        td.addEvent("tbl", records);
        td.uploadEvents();
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }

    public void testAddEventWithSuccess() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db_", "tbl", "key", "val");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }

    public void testStartSession() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);
        td.setDefaultDatabase("db_");

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.startSession("tbl");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(2, client.addedEvent.get(0).event.size());
        assertTrue(((String) client.addedEvent.get(0).event.get("td_session_id")).length() > 0);
        assertEquals("start", client.addedEvent.get(0).event.get("td_session_event"));
    }

    public void testEndSessionWithoutStartSession() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.endSession("db_", "tbl");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertEquals("end", client.addedEvent.get(0).event.get("td_session_event"));
    }

    public void testStartSessionAndEndSession() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.startSession("db_", "tbl");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(2, client.addedEvent.get(0).event.size());
        String sessionId = (String) client.addedEvent.get(0).event.get("td_session_id");
        assertTrue(sessionId.length() > 0);
        assertEquals("start", client.addedEvent.get(0).event.get("td_session_event"));

        td.setDefaultDatabase("db_");
        td.endSession("tbl");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(2, client.addedEvent.size());

        Event endEvent = client.addedEvent.get(1);
        assertEquals("db_.tbl", endEvent.tag);
        assertEquals(2, endEvent.event.size());
        assertEquals(sessionId, endEvent.event.get("td_session_id"));
        assertEquals("end", endEvent.event.get("td_session_event"));
    }

    public void testAddEventWithSuccessWithDefaultDatabase() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);
        td.setDefaultDatabase("db_");

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("tbl", "key", "val");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }

    public void testAddEventWithErrorWithoutDefaultDatabase() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("tbl", "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
        assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(0, client.addedEvent.size());
    }

    public void testAddEventWithError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        client.exceptionOnQueueEventCalled = new IOException("hello world");
        client.errorCodeOnQueueEventCalled = KeenClient.ERROR_CODE_STORAGE_ERROR;
        td = new TreasureData(client, null);

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
        assertEquals(0, client.addedEvent.size());
    }

    public void testAddEventWithDatabaseNameError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

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
        assertEquals(0, client.addedEvent.size());
    }

    public void testAddEventWithTableNameError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

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
        assertEquals(0, client.addedEvent.size());
    }

    public void testAddEventWithNullDatabaseName() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent(null, "tbl", "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
        assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(0, client.addedEvent.size());
    }

    public void testAddEventWithNullTableName() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db_", null, "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IllegalArgumentException);
        assertEquals(KeenClient.ERROR_CODE_INVALID_PARAM, errorCodeForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(0, client.addedEvent.size());
    }

    public void testUploadEventsWithSuccess() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td = new TreasureData(client, null);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.uploadEvents();
        assertFalse(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertNull(errorCodeForAddEvent);
        assertTrue(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
        assertNull(errorCodeForUploadEvents);
        assertEquals(0, client.addedEvent.size());
    }

    public void testUploadEventsWithError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        client.exceptionOnSendQueuedEventsCalled = new IllegalArgumentException("foo bar");
        client.errorCodeOnSendQueuedEventsCalled = KeenClient.ERROR_CODE_NETWORK_ERROR;
        td = new TreasureData(client, null);

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
        assertEquals(0, client.addedEvent.size());
    }
}