package com.treasure_data.androidsdk;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.msgpack.MessagePack;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.unpacker.MessagePackUnpacker;
import org.msgpack.unpacker.UnpackerIterator;

import com.treasure_data.androidsdk.ApiClient;
import com.treasure_data.androidsdk.DefaultApiClient;
import com.treasure_data.androidsdk.RepeatingWorker;
import com.treasure_data.androidsdk.TdAndroidLogger;
import com.treasure_data.androidsdk.DefaultApiClient.ApiError;

public class TdAndroidLoggerTest {
    private static final String API_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm";

    public static class ApiClientMock implements ApiClient {
        String apikey;
        String host;
        int port;
        List<String[]> createTables = new LinkedList<String[]>();
        List<Object[]> importTables = new LinkedList<Object[]>();
        boolean tableNotfound = false;

        @Override
        public void init(String apikey, String host, int port) {
            this.apikey = apikey;
            this.host = host;
            this.port = port;
        }

        @Override
        public String createTable(String database, String table) throws IOException, ApiError {
            createTables.add(new String[] {database, table});
            return "OK";
        }

        public void setTableNotFound(boolean tableNotfound) {
            this.tableNotfound = tableNotfound;
        }

        @Override
        public String importTable(String database, String table, byte[] data) throws IOException, ApiError {
            if (tableNotfound) {
                tableNotfound = false;
                throw new FileNotFoundException();
            }
            importTables.add(new Object[] {database, table, data});
            return "OK";
        }
    }

    @Before
    public void setup() {
        TdAndroidLogger.setApiClientClass(ApiClientMock.class);
    }

    @After
    public void teardown() {
        TdAndroidLogger.setApiClientClass(DefaultApiClient.class);
    }

    @Test
    public void testInit() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        assertEquals(API_KEY, apiClient.apikey);
        assertEquals("api.treasure-data.com", apiClient.host);
        assertEquals(443, apiClient.port);
    }

    @Test
    public void testWriteOnly() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertEquals(0, apiClient.createTables.size());
        assertEquals(0, apiClient.importTables.size());
    }

    @Test
    public void testIncrementOnly() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        assertEquals(0, apiClient.createTables.size());
        assertEquals(0, apiClient.importTables.size());
    }

    @Test
    public void testFinishOnly() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.flushAll();
        assertEquals(0, apiClient.createTables.size());
        assertEquals(0, apiClient.importTables.size());
    }

    private String getDbFromImportTables(ApiClientMock apiClientMock, int idx) {
        return (String) apiClientMock.importTables.get(idx)[0];
    }

    private String getTblFromImportTables(ApiClientMock apiClientMock, int idx) {
        return (String) apiClientMock.importTables.get(idx)[1];
    }

    private List<MapValue> getDataFromImportTables(ApiClientMock apiClientMock, int idx) throws IOException {
        return parseMsgpack((byte[]) apiClientMock.importTables.get(idx)[2]);
    }

    private boolean isTimeField(Value v) {
        return v.asIntegerValue().getLong() > 1000000000;
    }

    @Test
    public void testWriteAndFlush() throws IOException {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        logger.flush("testdb", "testtbl");
        assertEquals(0, apiClient.createTables.size());
        assertEquals(1, apiClient.importTables.size());

        int idx = 0;
        assertEquals("testdb", getDbFromImportTables(apiClient, idx));
        assertEquals("testtbl", getTblFromImportTables(apiClient, idx));
        List<MapValue> mapValues = getDataFromImportTables(apiClient, idx);

        assertEquals(1, mapValues.size());
        MapValue mapValue = mapValues.get(0);
        for (Entry<Value, Value> kv : mapValue.entrySet()) {
            String key = kv.getKey().asRawValue().getString();
            if (key.equals("time"))
                assertTrue(isTimeField(kv.getValue()));
            else if (key.equals("keykey"))
                assertEquals("valval", kv.getValue().asRawValue().getString());
            else
                assertTrue(false);
        }
    }

    @Test
    public void testWriteAndFlushWithCreateTable() throws IOException {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        apiClient.setTableNotFound(true);
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        logger.flush("testdb", "testtbl");
        assertEquals(1, apiClient.createTables.size());
        assertEquals("testdb", apiClient.createTables.get(0)[0]);
        assertEquals("testtbl", apiClient.createTables.get(0)[1]);

        assertEquals(1, apiClient.importTables.size());

        int idx = 0;
        assertEquals("testdb", getDbFromImportTables(apiClient, idx));
        assertEquals("testtbl", getTblFromImportTables(apiClient, idx));
        List<MapValue> mapValues = getDataFromImportTables(apiClient, idx);

        assertEquals(1, mapValues.size());
        MapValue mapValue = mapValues.get(0);
        for (Entry<Value, Value> kv : mapValue.entrySet()) {
            String key = kv.getKey().asRawValue().getString();
            if (key.equals("time"))
                assertTrue(isTimeField(kv.getValue()));
            else if (key.equals("keykey"))
                assertEquals("valval", kv.getValue().asRawValue().getString());
            else
                assertTrue(false);
        }
    }

    @Test
    public void testIncrementFlush() throws IOException {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        logger.flush("testdb", "testtbl");
        assertEquals(0, apiClient.createTables.size());
        assertEquals(1, apiClient.importTables.size());

        int idx = 0;
        assertEquals("testdb", getDbFromImportTables(apiClient, idx));
        assertEquals("testtbl", getTblFromImportTables(apiClient, idx));
        List<MapValue> mapValues = getDataFromImportTables(apiClient, idx);

        assertEquals(2, mapValues.size());
        for (MapValue v : mapValues) {
            boolean timeExists = false;
            for (Entry<Value, Value> kv : v.entrySet()) {
                String key = kv.getKey().asRawValue().getString();
                if (key.equals("time")) {
                    timeExists = true;
                    assertTrue(isTimeField(kv.getValue()));
                }
                else if (key.equals("increkey1"))
                    assertEquals(21, kv.getValue().asIntegerValue().getInt());
                else if (key.equals("increkey2"))
                    assertEquals(43, kv.getValue().asIntegerValue().getInt());
                else
                    assertTrue(false);
            }
            assertTrue(timeExists);
        }
    }

    @Test
    public void testIncrementFlushWithAutoFlushing() throws IOException, InterruptedException {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY, true);
        logger.flushWorker.intervalMilli = 500;     // for test
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);

        assertEquals(0, apiClient.importTables.size());

        TimeUnit.MILLISECONDS.sleep(700);

        assertEquals(1, apiClient.importTables.size());

        int idx = 0;
        assertEquals("testdb", getDbFromImportTables(apiClient, idx));
        assertEquals("testtbl", getTblFromImportTables(apiClient, idx));
        List<MapValue> mapValues = getDataFromImportTables(apiClient, idx);

        assertEquals(1, mapValues.size());
        for (MapValue v : mapValues) {
            boolean timeExists = false;
            for (Entry<Value, Value> kv : v.entrySet()) {
                String key = kv.getKey().asRawValue().getString();
                if (key.equals("time")) {
                    timeExists = true;
                    assertTrue(isTimeField(kv.getValue()));
                }
                else if (key.equals("increkey1"))
                    assertEquals(21, kv.getValue().asIntegerValue().getInt());
                else
                    assertTrue(false);
            }
            assertTrue(timeExists);
        }

        TimeUnit.MILLISECONDS.sleep(500);

        assertEquals(1, apiClient.importTables.size());

        logger.increment("testdb", "testtbl", "increkey1", 20);

        assertEquals(1, apiClient.importTables.size());

        TimeUnit.MILLISECONDS.sleep(600);

        assertEquals(2, apiClient.importTables.size());

        logger.stopAutoFlushing();

        TimeUnit.MILLISECONDS.sleep(300);

        logger.increment("testdb", "testtbl", "increkey1", 20);

        TimeUnit.MILLISECONDS.sleep(600);

        assertEquals(0, apiClient.createTables.size());
        assertEquals(2, apiClient.importTables.size());
    }

   @Test
    public void testMultiImport() throws IOException {
       _testMultiImport(true);
       _testMultiImport(false);
    }

    public void _testMultiImport(boolean flushAll) throws IOException {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.increment("testdb1", "testtbl1", "increkey1", 1);
        logger.increment("testdb1", "testtbl1", "increkey1", 20);
        assertTrue(logger.write("testdb1", "testtbl2", "keykey", "valval"));
        logger.increment("testdb2", "testtbl1", "increkey2", 3);
        logger.increment("testdb2", "testtbl1", "increkey2", 40);

        if (flushAll) logger.flushAll();
        else logger.close();

        assertEquals(0, apiClient.createTables.size());
        assertEquals(3, apiClient.importTables.size());

        for (Object[] importTable : apiClient.importTables) {
            String database = (String) importTable[0];
            String table = (String) importTable[1];
            byte[] data = (byte[]) importTable[2];
            if (database.equals("testdb1") && table.equals("testtbl1")) {
                List<MapValue> mapValues = parseMsgpack(data);
                assertEquals(1, mapValues.size());
                for (MapValue v : mapValues) {
                    boolean timeExists = false;
                    for (Entry<Value, Value> kv : v.entrySet()) {
                        String key = kv.getKey().asRawValue().getString();
                        if (key.equals("time")) {
                            timeExists = true;
                            assertTrue(isTimeField(kv.getValue()));
                        }
                        else if (key.equals("increkey1"))
                            assertEquals(21, kv.getValue().asIntegerValue().getInt());
                        else
                            assertTrue(false);
                    }
                    assertTrue(timeExists);
                }
            }
            else if (database.equals("testdb1") && table.equals("testtbl2")) {
                List<MapValue> mapValues = parseMsgpack(data);
                assertEquals(1, mapValues.size());
                for (MapValue v : mapValues) {
                    boolean timeExists = false;
                    for (Entry<Value, Value> kv : v.entrySet()) {
                        String key = kv.getKey().asRawValue().getString();
                        if (key.equals("time")) {
                            timeExists = true;
                            assertTrue(isTimeField(kv.getValue()));
                        }
                        else if (key.equals("keykey"))
                            assertEquals("valval", kv.getValue().asRawValue().getString());
                        else
                            assertTrue(false);
                    }
                    assertTrue(timeExists);
                }
            }
            else if (database.equals("testdb2") && table.equals("testtbl1")) {
                List<MapValue> mapValues = parseMsgpack(data);
                assertEquals(1, mapValues.size());
                for (MapValue v : mapValues) {
                    boolean timeExists = false;
                    for (Entry<Value, Value> kv : v.entrySet()) {
                        String key = kv.getKey().asRawValue().getString();
                        if (key.equals("time")) {
                            timeExists = true;
                            assertTrue(isTimeField(kv.getValue()));
                        }
                        else if (key.equals("increkey2"))
                            assertEquals(43, kv.getValue().asIntegerValue().getInt());
                        else
                            assertTrue(false);
                    }
                    assertTrue(timeExists);
                }
            }
            else
                assertTrue(false);
        }
    }

    @Test
    public void testSetFlushInterval() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        assertEquals(RepeatingWorker.DEFAULT_INTERVAL_MILLI, logger.flushWorker.intervalMilli);

        logger.startAutoFlushing(1);
        assertEquals(RepeatingWorker.MIN_INTERVAL_MILLI, logger.flushWorker.intervalMilli);
    }

    private List<MapValue> parseMsgpack(byte[] data) throws IOException {
        MessagePackUnpacker unpacker = new MessagePackUnpacker(new MessagePack(), new GZIPInputStream(new ByteArrayInputStream(data)));
        UnpackerIterator iterator = unpacker.iterator();
        List<MapValue> result = new LinkedList<MapValue>();
        while (iterator.hasNext()) {
            MapValue value = (MapValue) iterator.next();
            result.add(value);
        }
        unpacker.close();
        return result;
    }
}
