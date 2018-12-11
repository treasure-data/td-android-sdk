package com.treasuredata.android;

import com.fasterxml.jackson.jr.ob.JSON;
import io.keen.client.java.KeenCallback;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class TDClientTest
{
    private final static String APIKEY = "9999/1qaz2wsx3edc4rfv5tgb6yhn";
    private final static JSON JSON = new JSON();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File cacheDir;
    private MockWebServer server;

    @Before
    public void setUp()
            throws IOException
    {
        cacheDir = temporaryFolder.getRoot();

        server = new MockWebServer();

        TDHttpHandler.disableEventCompression();
    }

    @After
    public void tearDown()
            throws IOException
    {
        TDHttpHandler.enableEventCompression();

        server.shutdown();
    }

    private void sendQueuedEventsAndAssert(TDClient client, final List<Map<String, List<Map<String, Object>>>> expects)
            throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(1);
        client.sendQueuedEventsAsync(null, new KeenCallback() {
            @Override
            public void onSuccess()
            {
                try {
                    assertThat(server.getRequestCount(), is(expects.size()));

                    for (Map<String, List<Map<String, Object>>> expected : expects) {
                        RecordedRequest recordedRequest = server.takeRequest();
                        assertThat(recordedRequest.getMethod(), is("POST"));
                        assertThat(recordedRequest.getHeader("X-TD-Write-Key"), is(APIKEY));
                        assertThat(recordedRequest.getHeader("X-TD-Data-Type"), is("k"));
                        Map<String, Object> requests = JSON.mapFrom(recordedRequest.getBody().inputStream());
                        assertThat(requests.size(), is(expected.size()));
                        System.out.println("Request : " + requests);
                        System.out.println("Expect :" + expected);
                        for (Map.Entry<String, List<Map<String, Object>>> exp : expected.entrySet()) {
                            List<Map<String, Object>> events = (List<Map<String, Object>>) requests.get(exp.getKey());
                            assertThat(events.size(), is(exp.getValue().size()));
                            Collections.sort(events, new Comparator<Map<String, Object>>() {
                                @Override
                                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                                    String name1 = (String) o1.get("name");
                                    String name2 = (String) o2.get("name");
                                    return name1.compareTo(name2);
                                }
                            });
                            int i = 0;
                            List<Map<String, Object>> expList = exp.getValue();
                            Collections.sort(expList, new Comparator<Map<String, Object>>() {
                                @Override
                                public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                                    String name1 = (String) o1.get("name");
                                    String name2 = (String) o2.get("name");
                                    return name1.compareTo(name2);
                                }
                            });
                            for (Map<String, Object> expectedEvent : expList) {
                                Map<String, Object> event = events.get(i);
                                for (Map.Entry<String, Object> keyAndValue : expectedEvent.entrySet()) {
                                    assertThat(event.get(keyAndValue.getKey()), is(keyAndValue.getValue()));
                                }
                                i++;
                            }
                        }
                    }
                    latch.countDown();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Exception e)
            {
                e.printStackTrace();
                assertTrue(false);
            }
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void sendToSingleTable()
            throws Exception
    {
        server.enqueue(new MockResponse().setBody("{\"db0.tbl0\":[{\"success\":true},{\"success\":true}]}"));
        server.start();
        TDClient.setApiEndpoint(String.format("http://127.0.0.1:%d", server.getPort()));
        TDClient client = new TDClient(APIKEY, cacheDir);

        HashMap<String, Object> event0 = new HashMap<String, Object>();
        event0.put("name", "Bar");
        event0.put("age", 42);
        client.queueEvent("db0.tbl0", event0);

        HashMap<String, Object> event1 = new HashMap<String, Object>();
        event1.put("name", "Foo");
        event1.put("age", 99);
        client.queueEvent("db0.tbl0", event1);

        Map<String, List<Map<String, Object>>> expected = new HashMap<String, List<Map<String, Object>>>();
        expected.put("db0.tbl0", Arrays.<Map<String, Object>>asList(event0, event1));

        sendQueuedEventsAndAssert(client, Arrays.asList(expected));
    }

    @Test
    public void sendToTwoTables()
            throws Exception
    {
        server.enqueue(new MockResponse().setBody(
                "{\"db0.tbl0\":[{\"success\":true}]," +
                        "\"db1.tbl1\":[{\"success\":true}]}"));
        server.start();
        TDClient.setApiEndpoint(String.format("http://127.0.0.1:%d", server.getPort()));
        TDClient client = new TDClient(APIKEY, cacheDir);

        HashMap<String, Object> event0 = new HashMap<String, Object>();
        event0.put("name", "Bar");
        event0.put("age", 42);
        client.queueEvent("db0.tbl0", event0);

        HashMap<String, Object> event1 = new HashMap<String, Object>();
        event1.put("name", "Foo");
        event1.put("age", 99);
        client.queueEvent("db1.tbl1", event1);

        Map<String, List<Map<String, Object>>> expected = new HashMap<String, List<Map<String, Object>>>();
        expected.put("db0.tbl0", Arrays.<Map<String, Object>>asList(event0));
        expected.put("db1.tbl1", Arrays.<Map<String, Object>>asList(event1));

        sendQueuedEventsAndAssert(client, Arrays.asList(expected));
    }

    @Test
    public void sendToSingleTableWithLimitedUploadedEvents()
            throws Exception
    {
        server.enqueue(new MockResponse().setBody("{\"db0.tbl0\":[{\"success\":true},{\"success\":true},{\"success\":true}]}"));
        server.enqueue(new MockResponse().setBody("{\"db0.tbl0\":[{\"success\":true}]}"));
        server.start();
        TDClient.setApiEndpoint(String.format("http://127.0.0.1:%d", server.getPort()));
        final TDClient client = new TDClient(APIKEY, cacheDir);
        client.setMaxUploadEventsAtOnce(3);

        final List<Map<String, Object>> list1 = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> list2 = new ArrayList<Map<String, Object>>();

        final HashMap<String, Object> event0 = new HashMap<String, Object>();
        event0.put("name", "Bar");
        event0.put("age", 42);
        client.queueEvent(null, "db0.tbl0", event0, null, new KeenCallback() {
            @Override
            public void onSuccess() {
                list1.add(event0);
                final HashMap<String, Object> event1 = new HashMap<String, Object>();
                event1.put("name", "Baz");
                event1.put("age", 99);
                client.queueEvent(null,"db0.tbl0", event1, null, new KeenCallback() {
                    @Override
                    public void onSuccess() {
                        list1.add(event1);
                        final HashMap<String, Object> event2 = new HashMap<String, Object>();
                        event2.put("name", "Foo");
                        event2.put("age", 1);
                        client.queueEvent(null, "db0.tbl0", event2, null, new KeenCallback() {
                            @Override
                            public void onSuccess() {
                                list1.add(event2);
                                final HashMap<String, Object> event3 = new HashMap<String, Object>();
                                event3.put("name", "Zzz");
                                event3.put("age", 111);
                                client.queueEvent(null, "db0.tbl0", event3, null, new KeenCallback() {
                                    @Override
                                    public void onSuccess() {
                                        list2.add(event3);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {

                            }
                        });

                    }

                    @Override
                    public void onFailure(Exception e) {

                    }
                });
            }

            @Override
            public void onFailure(Exception e) {

            }
        });

        Map<String, List<Map<String, Object>>> expected0 = new HashMap<String, List<Map<String, Object>>>();
        expected0.put("db0.tbl0", list1);

        Map<String, List<Map<String, Object>>> expected1 = new HashMap<String, List<Map<String, Object>>>();
        expected1.put("db0.tbl0", list2);

        sendQueuedEventsAndAssert(client, Arrays.asList(expected0, expected1));
    }

    @Test
    public void sendToTwoTablesWithLimitedUploadedEvents()
            throws Exception
    {
        server.enqueue(new MockResponse().setBody(
                "{\"db0.tbl0\":[{\"success\":true},{\"success\":true}]," +
                        "\"db1.tbl1\":[{\"success\":true}]}"));
        server.enqueue(new MockResponse().setBody(
                "{\"db1.tbl1\":[{\"success\":true}]}"));
        server.start();
        TDClient.setApiEndpoint(String.format("http://127.0.0.1:%d", server.getPort()));
        final TDClient client = new TDClient(APIKEY, cacheDir);
        client.setMaxUploadEventsAtOnce(3);

        final List<Map<String, Object>> list1 = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> list2 = new ArrayList<Map<String, Object>>();
        final List<Map<String, Object>> list3 = new ArrayList<Map<String, Object>>();

        final HashMap<String, Object> event0 = new HashMap<String, Object>();
        event0.put("name", "Bar");
        event0.put("age", 42);
        client.queueEvent(null, "db0.tbl0", event0, null, new KeenCallback() {
            @Override
            public void onSuccess() {
                list1.add(event0);
                final HashMap<String, Object> event1 = new HashMap<String, Object>();
                event1.put("name", "Foo");
                event1.put("age", 99);
                client.queueEvent(null, "db0.tbl0", event1, null,  new KeenCallback() {
                    @Override
                    public void onSuccess() {
                        list1.add(event1);
                        final HashMap<String, Object> event2 = new HashMap<String, Object>();
                        event2.put("name", "Baz");
                        event2.put("age", 1);
                        client.queueEvent(null,"db1.tbl1", event2, null, new KeenCallback() {
                            @Override
                            public void onSuccess() {
                                list2.add(event2);
                                final HashMap<String, Object> event3 = new HashMap<String, Object>();
                                event3.put("name", "Zzz");
                                event3.put("age", 111);
                                client.queueEvent(null,"db1.tbl1", event3, null, new KeenCallback() {
                                    @Override
                                    public void onSuccess() {
                                        list3.add(event3);
                                    }

                                    @Override
                                    public void onFailure(Exception e) {

                                    }
                                });
                            }

                            @Override
                            public void onFailure(Exception e) {

                            }
                        });
                    }

                    @Override
                    public void onFailure(Exception e) {

                    }
                });
            }

            @Override
            public void onFailure(Exception e) {

            }
        });

        Map<String, List<Map<String, Object>>> expected0 = new HashMap<String, List<Map<String, Object>>>();
        expected0.put("db0.tbl0", list1);
        expected0.put("db1.tbl1", list2);

        Map<String, List<Map<String, Object>>> expected1 = new HashMap<String, List<Map<String, Object>>>();
        expected1.put("db1.tbl1", list3);

        sendQueuedEventsAndAssert(client, Arrays.asList(expected0, expected1));
    }
}