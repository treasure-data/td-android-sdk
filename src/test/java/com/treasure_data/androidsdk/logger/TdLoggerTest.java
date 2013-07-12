package com.treasure_data.androidsdk.logger;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

import com.treasure_data.androidsdk.apiclient.ApiClient;
import com.treasure_data.androidsdk.apiclient.DefaultApiClient;
import com.treasure_data.androidsdk.apiclient.DefaultApiClient.ApiError;
import com.treasure_data.androidsdk.logger.AbstractTdLogger;
import com.treasure_data.androidsdk.util.RepeatingWorker;

public class TdLoggerTest {
    MockTdLogger logger;

    class Output {
        String database;
        String table;
        ByteBuffer data;

        public Output(String database, String table, ByteBuffer data) {
        this.database = database;
        this.table = table;
        this.data = data;
        }
    }

    private class MyRepeatingWorker extends RepeatingWorker {

        @Override
        public void setInterval(long intervalMilli) {
            this.intervalMilli = intervalMilli;
        }
    }

    private class MockTdLogger extends AbstractTdLogger {
        int cleanUpCallCount;
        List<Output> outputs = new LinkedList<Output>();

        public MockTdLogger() {
            super();
        }

        public MockTdLogger(RepeatingWorker flushWorker) {
            super(false);
            setFlushWorker(flushWorker);
        }

        @Override
        boolean outputData(String database, String table, byte[] data) {
            outputs.add(new Output(database, table, ByteBuffer.wrap(data)));
            return true;
        }

        @Override
        void cleanup() {
            cleanUpCallCount++;
        }
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

    @Test
    public void testWriteOnly() {
        logger = new MockTdLogger();
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());
    }

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

    private String getDb(int i) {
        return logger.outputs.get(i).database;
    }

    private String getTbl(int i) {
        return logger.outputs.get(i).table;
    }

    private List<MapValue> getData(int i) throws IOException {
        return parseMsgpack(logger.outputs.get(i).data.array());
    }

    private boolean isTimeField(Value v) {
        return v.asIntegerValue().getLong() > 1000000000;
    }

    @Test
    public void testWriteAndFlush() throws IOException {
        logger = new MockTdLogger();
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        logger.flush("testdb", "testtbl");
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
    public void testWriteAndFlushWithAutoFlush() throws IOException, InterruptedException {
        MyRepeatingWorker flushWorker = new MyRepeatingWorker();
        flushWorker.setInterval(300);
        logger = new MockTdLogger(flushWorker);
        logger.startFlushWorker();

        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(400);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(400);
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
    public void testIncrementFlushWithAutoFlush() throws IOException, InterruptedException {
        MyRepeatingWorker flushWorker = new MyRepeatingWorker();
        flushWorker.setInterval(300);
        logger = new MockTdLogger(flushWorker);
        logger.startFlushWorker();

        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(0, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(400);
        assertEquals(0, logger.cleanUpCallCount);
        assertEquals(1, logger.outputs.size());

        TimeUnit.MILLISECONDS.sleep(400);
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
        logger.flush("testdb", "testtbl");
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
            String database = output.database;
            String table = output.table;
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
