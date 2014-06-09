package com.treasuredata.android;

import io.keen.client.java.KeenCallback;
import io.keen.client.java.KeenProject;
import junit.framework.TestCase;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

public class TreasureDataTest extends TestCase {
    private static final String DUMMY_API_KEY = "dummy_api_key";
    boolean onSuccessCalledForAddEvent;
    boolean onSuccessCalledForUploadEvents;
    Exception exceptionOnFailedCalledForAddEvent;
    Exception exceptionOnFailedCalledForUploadEvents;

    class MockTDClient extends TDClient {
        Exception exceptionOnQueueEventCalled;
        Exception exceptionOnSendQueuedEventsCalled;

        MockTDClient(String apiKey) throws IOException {
            super(apiKey);
        }

        @Override
        public void queueEvent(KeenProject project, String eventCollection, Map<String, Object> event, Map<String, Object> keenProperties, KeenCallback callback) {
            if (exceptionOnQueueEventCalled == null) {
                callback.onSuccess();
            }
            else {
                callback.onFailure(exceptionOnQueueEventCalled);
            }
        }

        @Override
        public void sendQueuedEventsAsync(KeenProject project, KeenCallback callback) {
            if (exceptionOnSendQueuedEventsCalled == null) {
                callback.onSuccess();
            }
            else {
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
    }

    private void enableCallbackForAddEvent() {
        TDCallback callback = new TDCallback() {
            @Override
            public void onSuccess() {
                onSuccessCalledForAddEvent = true;
            }

            @Override
            public void onError(Exception e) {
                exceptionOnFailedCalledForAddEvent = e;
            }
        };
        td.setAddEventCallBack(callback);
    }

    private void enableCallbackForUploadEvents() {
        TDCallback callback = new TDCallback() {
            @Override
            public void onSuccess() {
                onSuccessCalledForUploadEvents = true;
            }

            @Override
            public void onError(Exception e) {
                exceptionOnFailedCalledForUploadEvents = e;
            }
        };
        td.setUploadEventsCallBack(callback);
    }

    public void testApiKey() throws IOException, NoSuchAlgorithmException {
        String apikey1 = DUMMY_API_KEY + "1";
        MockTDClient client1 = new MockTDClient(apikey1);
        System.out.println("client1.getDefaultProject().getProjectId()=" + client1.getDefaultProject().getProjectId());

        String apikey2 = DUMMY_API_KEY + "2";
        MockTDClient client2 = new MockTDClient(apikey2);
        System.out.println("client2.getDefaultProject().getProjectId()=" + client2.getDefaultProject().getProjectId());

        assertNotSame(client1.getDefaultProject().getProjectId(), client2.getDefaultProject().getProjectId());
    }

    public void testAddEventAndUploadEventsWithoutCallBack() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);
        td.addEvent("db", "tbl", "key", "val");
        td.uploadEvents();
    }

    public void testAddEventWithSuccess() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db", "tbl", "key", "val");
        assertTrue(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
    }

    public void testAddEventWithError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        client.exceptionOnQueueEventCalled = new IOException("hello world");
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.addEvent("db", "tbl", "key", "val");
        assertFalse(onSuccessCalledForAddEvent);
        assertTrue(exceptionOnFailedCalledForAddEvent instanceof IOException);
        assertEquals("hello world", exceptionOnFailedCalledForAddEvent.getMessage());
        assertFalse(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
    }

    public void testUploadEventsWithSuccess() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.uploadEvents();
        assertFalse(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertTrue(onSuccessCalledForUploadEvents);
        assertNull(exceptionOnFailedCalledForUploadEvents);
    }

    public void testUploadEventsWithError() throws IOException {
        MockTDClient client = new MockTDClient(DUMMY_API_KEY);
        client.exceptionOnSendQueuedEventsCalled = new IllegalArgumentException("foo bar");
        td.setClient(client);

        enableCallbackForAddEvent();
        enableCallbackForUploadEvents();

        td.uploadEvents();
        assertFalse(onSuccessCalledForAddEvent);
        assertNull(exceptionOnFailedCalledForAddEvent);
        assertFalse(onSuccessCalledForUploadEvents);
        assertTrue(exceptionOnFailedCalledForUploadEvents instanceof IllegalArgumentException);
        assertEquals("foo bar", exceptionOnFailedCalledForUploadEvents.getMessage());
    }
}