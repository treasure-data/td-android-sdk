package com.treasure_data.androidsdk.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.komamitsu.android.util.Log;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

import com.treasure_data.androidsdk.counter.Counter;
import com.treasure_data.androidsdk.counter.CounterContainer;
import com.treasure_data.androidsdk.util.RepeatingWorker;


public abstract class AbstractTdLogger {
    private static final String TAG = AbstractTdLogger.class.getSimpleName();
    private static final int BUFFER_FLUSH_SIZE = 1 * 1024 * 1024;   // TODO: tune up
    private static final String PACKER_KEY_DELIM = "#";
    // TODO: add updated_at

    // map of database#table keys and all the data in records associated to each
    private final Map<String, BufferPacker> bufferPackerMap = new HashMap<String, BufferPacker>();
    // map of database#table keys and number of records to each
    // TODO mix in a single class with BufferPacker?
    private final Counter bufferPackerCounterMap = new Counter();
    // map-like object to store key-indexed counter and support updating them
    private final CounterContainer counterContainer = new CounterContainer();
    private final MessagePack msgpack = new MessagePack();

    // worker threads to pass the data to the Service through Intents
    protected RepeatingWorker flushWorker = new RepeatingWorker();
    private final Runnable flushTask = new Runnable() {
        @Override
        public void run() {
            flushAll();
        }
    };

    abstract boolean outputData(String database, String table, byte[] data);

    abstract void cleanup();

    public AbstractTdLogger() {
        // start flushWorker by default
        this(true);
    }

    public AbstractTdLogger(boolean startFlushWorkerOnInit) {
        flushWorker.setProcedure(flushTask);

        if (startFlushWorkerOnInit) {
            startFlushWorker();
        }
    }

    protected void setFlushWorker(RepeatingWorker worker) {
        if (flushWorker != null) {
            flushWorker.stop();
        }
        flushWorker = worker;
        flushWorker.setProcedure(flushTask);
    }

    public static void setRepeatingWorkersInterval(long millis) {
        long actualInterval = RepeatingWorker.setInterval(millis);
        if (actualInterval < millis) {
            Log.w(TAG,
              "Requested interval (" + millis + ") is smaller than " +
              "the minimum allowed (" + actualInterval / 1000 + ")");
        }
        Log.v(TAG, "Changed all RepeatingWorkers intervals to " + millis + " ms");
    }

    public void startFlushWorker() {
        if (!flushWorker.isRunning()) {
            flushWorker.start();
        }
    }

    public void increment(String database, String table, String key) {
        increment(database, table, key, 1);
    }

    // Generic counter increment function.
    //    The counter indexed by 'key' gets incremented by 'amount'.
    public void increment(String database, String table, String key, long amount) {
        getBufferPacker(database, table);   // prepare to flush()
        counterContainer.increment(toBufferPackerKey(database, table), key, amount);
    }

    private void moveCounterToBuffer(String packerKey) {
        Counter counter = counterContainer.getCounter(packerKey);
        if (counter == null) {
            return;
        }

        Log.d(TAG, "moveCounterToBuffer: key=" + packerKey);
        for (Entry<String, Long> kv : counter) {
            write(packerKey, kv.getKey(), kv.getValue());
        }
        counter.clear();
    }

    private boolean write(String packerKey, String key, Object value, long timestamp) {
        HashMap<String,Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return write(packerKey, data, timestamp);
    }

    // This method is used by moveCounterToBuffer to write the counter
    //  to the bufferPackerMap. The timestamp is not provided and will be
    //  generated based on system clock at the time of write.
    private boolean write(String packerKey, String key, Object value) {
        return write(packerKey, key, value, 0);
    }

    private BufferPacker getBufferPacker(String database, String table) {
        return getBufferPacker(toBufferPackerKey(database, table));
    }

    private BufferPacker getBufferPacker(String packerKey) {
        BufferPacker bufferPacker = bufferPackerMap.get(packerKey);
        if (bufferPacker == null) {
            synchronized (bufferPackerMap) {
                bufferPacker = bufferPackerMap.get(packerKey);
                if (bufferPacker == null) {
                    bufferPacker = msgpack.createBufferPacker();
                    bufferPackerMap.put(packerKey, bufferPacker);

                }
            }
        }
        bufferPackerCounterMap.increment(packerKey);
        return bufferPacker;
    }

    protected String toBufferPackerKey(String database, String table) {
        return new StringBuilder(database).append(PACKER_KEY_DELIM).append(table).toString();
    }

    private String[] fromBufferPackerKey(String bufferPackerKey) {
        return bufferPackerKey.split(PACKER_KEY_DELIM);
    }

    private synchronized void flushBufferPacker(String packerKey, BufferPacker bufferPacker) throws IOException {
        moveCounterToBuffer(packerKey);
        ByteArrayOutputStream out = null;
        try {
            if (bufferPacker.getBufferSize() == 0) {
                return;
            }
            out = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
            gzipOutputStream.write(bufferPacker.toByteArray());
            gzipOutputStream.close();
            byte[] data = out.toByteArray();
            Log.d(TAG, "compressed data for key " + packerKey +
                    " from " + bufferPacker.getBufferSize() + " B " +
                    "(" + bufferPackerCounterMap.get(packerKey) + " records) down to " +
                    data.length + " B");
            String[] databaseAndTable = fromBufferPackerKey(packerKey);
            outputData(databaseAndTable[0], databaseAndTable[1], data);
        }
        finally {
            IOUtils.closeQuietly(out);
        }
        bufferPacker.clear();
        bufferPackerCounterMap.clear(packerKey);
    }

    public boolean write(String database, String table, Map<String, Object>data) {
        return write(database, table, data, 0);
    }

    public boolean write(String database, String table, Map<String, Object>data, long timestamp) {
        return write(toBufferPackerKey(database, table), data, timestamp);
    }

    private boolean write(String packerKey, Map<String, Object>data, long timestamp) {
        BufferPacker bufferPacker = getBufferPacker(packerKey);
        try {
            if (!data.containsKey("time")) {
                data.put("time", timestamp == 0 ?
                        System.currentTimeMillis() / 1000 : timestamp);
            }
            bufferPacker.write(data);
            bufferPackerCounterMap.increment(packerKey);
            Log.d(TAG, "write: key=" + packerKey +
                    ", bufsize=" + bufferPacker.getBufferSize());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (bufferPacker.getBufferSize() > BUFFER_FLUSH_SIZE) {
            try {
                flushBufferPacker(packerKey, bufferPacker);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean flush(String packerKey) {
        BufferPacker bufferPacker = getBufferPacker(packerKey);
        try {
            flushBufferPacker(packerKey, bufferPacker);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean flushAll() {
        boolean isSuccess = true;
        for (String packerKey : bufferPackerMap.keySet()) {
            if (!flush(packerKey)) {
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    public void close() {
        flushAll();

        for (String bufferPackerKey : bufferPackerMap.keySet()) {
            try {
                getBufferPacker(bufferPackerKey).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        flushWorker.stop();
        cleanup();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
