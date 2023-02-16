package com.treasuredata.android;

import android.app.Application;
import android.content.Context;
import androidx.test.InstrumentationRegistry;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.keen.client.java.KeenCallback;
import io.keen.client.java.KeenProject;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class TreasureDataInstrumentTest extends TestCase {
    private static final String DUMMY_API_KEY = "dummy_api_key";

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

        public void clearAddedEvent() {
            addedEvent = new ArrayList<Event>();
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

    private Context context;
    private MockTDClient client;
    private TreasureData td;

    private TreasureData createTreasureData(Context context, TDClient client) {
        return new TreasureData(context, client);
    }

    public void setUp() throws IOException {
        Application application = mock(Application.class);
        context = InstrumentationRegistry.getTargetContext();

        client = new MockTDClient(DUMMY_API_KEY);
        td = spy(createTreasureData(context, client));
    }

    public void testEnableAutoAppendAdvertisingId() throws IOException {
        td.enableAutoAppendAdvertisingIdentifier();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail();
        }

        Map<String, Object> records = new HashMap<String, Object>();
        records.put("key", "val");
        td.addEvent("db_", "tbl", records);
        td.uploadEvents();
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(2, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
        assertTrue(client.addedEvent.get(0).event.containsKey("td_maid"));
    }

    public void testDisableAutoAppendAdvertisingId() throws IOException {
        td.enableAutoAppendAdvertisingIdentifier();
        td.disableAutoAppendAdvertisingIdentifier();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            fail();
        }

        Map<String, Object>records = new HashMap<String, Object>();
        records.put("key", "val");
        td.addEvent("db_", "tbl", records);
        td.uploadEvents();
        assertEquals(1, client.addedEvent.size());
        assertEquals("db_.tbl", client.addedEvent.get(0).tag);
        assertEquals(1, client.addedEvent.get(0).event.size());
        assertTrue(client.addedEvent.get(0).event.containsKey("key"));
        assertTrue(client.addedEvent.get(0).event.containsValue("val"));
    }
}
