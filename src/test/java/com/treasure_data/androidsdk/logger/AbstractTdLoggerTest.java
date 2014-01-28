package com.treasure_data.androidsdk.logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.komamitsu.android.util.Log;
import org.msgpack.MessagePack;
import org.msgpack.type.MapValue;
import org.msgpack.type.Value;
import org.msgpack.unpacker.MessagePackUnpacker;
import org.msgpack.unpacker.UnpackerIterator;

import com.treasure_data.androidsdk.apiclient.DbLogTableDescr;
import com.treasure_data.androidsdk.apiclient.DbTableDescr;
import com.treasure_data.androidsdk.util.RepeatingWorker;

public class AbstractTdLoggerTest {
    private static final String TAG = AbstractTdLoggerTest.class.getSimpleName();
    MockTdLogger logger;

    class Output {
        DbTableDescr descr;
        ByteBuffer data;

        public Output(DbTableDescr descr, ByteBuffer data) {
            this.descr = descr;
            this.data = data;
        }
    }

    private static class MyRepeatingWorker extends RepeatingWorker {
        private static final String TAG = MyRepeatingWorker.class.getSimpleName();

        @Override
        public void start() {
            Log.d(TAG, "start");
            super.start();
        }
    }

    private class MockTdLogger extends AbstractTdLogger {
        private final String TAG = MyRepeatingWorker.class.getSimpleName();
        int cleanUpCallCount;
        volatile List<Output> outputs = new LinkedList<Output>();

        public MockTdLogger() {
            super();
        }

        public MockTdLogger(RepeatingWorker flushWorker) {
            super(false);
            setFlushWorker(flushWorker);
        }

        @Override
        boolean outputData(DbTableDescr descr, byte[] data) {
            Log.d(TAG, "outputData");
            outputs.add(new Output(descr, ByteBuffer.wrap(data)));
            return true;
        }

        @Override
        void cleanup() {
            cleanUpCallCount++;
        }
    }

    private String getDb(int i) {
        return logger.outputs.get(i).descr.getDatabaseName();
    }

    private String getTbl(int i) {
        return logger.outputs.get(i).descr.getTableName();
    }

    private List<MapValue> getData(int i) throws IOException {
        return parseMsgpack(logger.outputs.get(i).data.array());
    }

    private boolean isTimeField(Value v) {
        return v.asIntegerValue().getLong() > 1000000000;
    }

    @Before
    public void setup() {
    }

    @After
    public void teardown() {
        if (logger != null) {
            logger.close();
        }
    }

    //
    // write only (no flush)
    //

    @Test
    public void testSingleWriteOnlyToLogTable() {
        logger = new MockTdLogger();
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    // TODO test/spec for validation of time column single key/value pair

    @Test
    public void testMapWriteOnlyToLogTable() {
        logger = new MockTdLogger();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");
        data.put("key4", "value4");
        assertTrue(logger.write("testdb", "testtbl", data));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    // TODO test/spec for validation of time column in map

    // TODO writeItem should return false
    @Test
    public void testSingleWriteWithoutPkOnlyToItemTable() {
        logger = new MockTdLogger();
        assertTrue(logger.writeItem("testdb", "testtbl", "pk_name", "string",
                "keykey", "valval"));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    @Test
    public void testSingleWriteWithPkOnlyToItemTable() {
        logger = new MockTdLogger();
        assertTrue(logger.writeItem("testdb", "testtbl", "pk_name", "int",
                "pk_name", 11));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    // TODO writeItem should return false
    @Test
    public void testMapWriteWithoutPkOnlyToItemTable() {
        logger = new MockTdLogger();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("key3", "value3");
        data.put("key4", "value4");
        assertTrue(logger.writeItem("testdb", "testtbl", "pk_name", "string", data));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    @Test
    public void testMapWriteWithPkOnlyToItemTable() {
        logger = new MockTdLogger();
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("key1", "value1");
        data.put("key2", "value2");
        data.put("pkName", "value3");
        data.put("key4", 4);
        assertTrue(logger.writeItem("testdb", "testtbl", "pk_name", "int",
                data));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    // TODO test/spec for validation of primary key in map
    // TODO test/spec for validation of primary key type in map

    @Test
    public void testIncrementOnly() {
        logger = new MockTdLogger();
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    @Test
    public void testFinishOnly() {
        logger = new MockTdLogger();
        logger.flushAll();
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    @Test
    public void testCloseOnly() {
        logger = new MockTdLogger();
        logger.close();
        assertEquals(1, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

    //
    // write and flush
    //
    @Test
    public void testSingleWriteAndFlushToLogTable() throws IOException {
        logger = new MockTdLogger();

        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertTrue(logger.flush(new DbLogTableDescr("testdb", "testtbl")));

        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        int idx = 0;
        assertEquals("testdb", getDb(idx));
        assertEquals("testtbl", getTbl(idx));
        List<MapValue> mapValues = getData(idx);

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
    public void testSinglelWriteAndAutoFlushToLogTable()
            throws IOException, InterruptedException {
        final long timeBase = MyRepeatingWorker.MIN_INTERVAL_MILLI + 10 * 1000;
        MyRepeatingWorker.setInterval(timeBase);
        MyRepeatingWorker flushWorker = new MyRepeatingWorker();
        logger = new MockTdLogger(flushWorker);
        logger.startFlushWorker();

        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(timeBase + 10 * 1000);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(timeBase + 10 * 1000);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        int idx = 0;
        assertEquals("testdb", getDb(idx));
        assertEquals("testtbl", getTbl(idx));
        List<MapValue> mapValues = getData(idx);

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

    // TODO testSinglelWriteAndAutoFlushToLogTable with interval < minimum

    // TODO testMapWriteAndFlushToLogTable
    // TODO testMapWriteAndAutoFlushToLogTable

    // TODO testSingleWriteAndFlushToItemTable
    // TODO testSingleWriteAndAutoFlushToItemTable
    // TODO testMapWriteAndFlushToItemTable
    // TODO testMapWriteAndAutoFlushToItemTable

    @Test
    public void testIncrementFlushWithAutoFlush()
            throws IOException, InterruptedException {
        final long timeBase = MyRepeatingWorker.MIN_INTERVAL_MILLI + 10 * 1000;
        MyRepeatingWorker.setInterval(timeBase);
        MyRepeatingWorker flushWorker = new MyRepeatingWorker();
        logger = new MockTdLogger(flushWorker);
        logger.startFlushWorker();

        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(timeBase + 10 * 1000);
        assertEquals(0, logger.cleanUpCallCount);
        Log.d(TAG, "checking logger.outputs.size()");
        assertEquals(1, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(timeBase + 10 * 1000);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        int idx = 0;
        assertEquals("testdb", getDb(idx));
        assertEquals("testtbl", getTbl(idx));
        List<MapValue> mapValues = getData(idx);

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
    public void testIncrementFlush() throws IOException {
        logger = new MockTdLogger();
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        logger.flush(new DbLogTableDescr("testdb", "testtbl"));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        int idx = 0;
        assertEquals("testdb", getDb(idx));
        assertEquals("testtbl", getTbl(idx));
        List<MapValue> mapValues = getData(idx);

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
    public void testMultiImportWithFlash() throws IOException {
       _testMultiImport(true);
    }

   @Test
    public void testMultiImportWithClose() throws IOException {
       _testMultiImport(false);
    }

    private void _testMultiImport(boolean flushAll) throws IOException {
        logger = new MockTdLogger();
        logger.increment("testdb1", "testtbl1", "increkey1", 1);
        logger.increment("testdb1", "testtbl1", "increkey1", 20);
        assertTrue(logger.write("testdb1", "testtbl2", "keykey", "valval"));
        logger.increment("testdb2", "testtbl1", "increkey2", 3);
        logger.increment("testdb2", "testtbl1", "increkey2", 40);

        if (flushAll) {
            logger.flushAll();
            assertEquals(0, logger.cleanUpCallCount);
        }
        else {
            logger.close();
            assertEquals(1, logger.cleanUpCallCount);
        }
        assertEquals(3, logger.outputs.size());

        for (Output output : logger.outputs) {
            String database = output.descr.getDatabaseName();
            String table = output.descr.getTableName();
            byte[] data = output.data.array();
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
