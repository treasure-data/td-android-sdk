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

import com.treasure_data.androidsdk.apiclient.DbItemTableDescr;
import com.treasure_data.androidsdk.apiclient.DbLogTableDescr;
import com.treasure_data.androidsdk.apiclient.DbTableDescr;
import com.treasure_data.androidsdk.counter.Counter;
import com.treasure_data.androidsdk.counter.CounterContainer;
import com.treasure_data.androidsdk.util.RepeatingWorker;


public abstract class AbstractTdLogger {

    private static final String TAG = AbstractTdLogger.class.getSimpleName();
    private static final int BUFFER_FLUSH_SIZE = 1 * 1024 * 1024;   // TODO: tune up
    private static final String PACKER_KEY_DELIM = "#";
    // TODO: add updated_at

    // map of database#table keys and all the data in records associated to each
    private final Map<DbTableDescr, BufferPacker> bufferPackerMap = new HashMap<DbTableDescr, BufferPacker>();
    // map of database#table keys and number of records to each
    // TODO mix in a single class with BufferPacker?
    // TODO this is really overkill - using a 2 levels map with the second level locked to 1 entry in place of a simple map. Temporary§
    private static final String DEFAULT_COUNTER_KEY = "counter";
    private final CounterContainer bufferPackerCounterMap = new CounterContainer();
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

    // these methods need to be implemented in the derived class
    abstract boolean outputData(DbTableDescr descr, byte[] data);
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

    // NOTE: increment only import data to a log table
    public void increment(String database, String table, String key) {
        increment(database, table, key, 1);
    }

    // NOTE: increment only import data to a log table
    // Generic counter increment function.
    //    The counter indexed by 'key' gets incremented by 'amount'.
    public void increment(String database, String table, String key, long amount) {
        // TODO necessary? It seems that the entry is created regardless of this call
        // prepare to flush()
        DbLogTableDescr descr = new DbLogTableDescr(database, table);
        getBufferPacker(descr);
        counterContainer.increment(descr, key, amount);
    }

    // NOTE: increment only import data to a log table
    private void moveCounterToBuffer(DbTableDescr descr) {
        Counter counter = counterContainer.getCounter(descr);
        if (counter == null) {
            return;
        }

        Log.d(TAG, "moveCounterToBuffer: key=" +
               toBufferPackerKey(descr.getDatabaseName(), descr.getTableName()));
        for (Entry<String, Long> kv : counter) {
            write(descr, kv.getKey(), kv.getValue());
        }
        counter.clear();
    }

//    private BufferPacker getBufferPacker(String database, String table) {
//        DbTableDescr descr = new DbTableDescr(database, table);
//        return getBufferPacker(descr);
//    }

    private BufferPacker getBufferPacker(DbTableDescr descr) {
        BufferPacker bufferPacker = bufferPackerMap.get(descr);
        if (bufferPacker == null) {
            synchronized (bufferPackerMap) {
                bufferPacker = bufferPackerMap.get(descr);
                if (bufferPacker == null) {
                    bufferPacker = msgpack.createBufferPacker();
                    bufferPackerMap.put(descr, bufferPacker);

                }
            }
        }
        return bufferPacker;
    }

    protected String toBufferPackerKey(String database, String table) {
        return new StringBuilder(database).append(PACKER_KEY_DELIM).append(table).toString();
    }

    public static String[] fromBufferPackerKey(String bufferPackerKey) {
        return bufferPackerKey.split(PACKER_KEY_DELIM);
    }

    private synchronized void flushBufferPacker(DbTableDescr descr, BufferPacker bufferPacker) throws IOException {
        moveCounterToBuffer(descr);
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
            Log.d(TAG, "compressed data for key " + descr +
                    " from " + bufferPacker.getBufferSize() + " B " +
                    "(" + bufferPackerCounterMap.getCounter(descr).get(DEFAULT_COUNTER_KEY) + " records) down to " +
                    data.length + " B");
            outputData(descr, data);
        }
        finally {
            IOUtils.closeQuietly(out);
        }
        synchronized (bufferPacker) {
            bufferPacker.clear();
        }
        bufferPackerCounterMap.clear(descr);
    }

    //
    // write APIs
    //

    // This method is used by moveCounterToBuffer to write the counter
    //  to the bufferPackerMap. The timestamp is not provided and will be
    //  generated based on system clock at the time of write.
    private boolean write(DbTableDescr descr, String counterKey, Object value) {
        return write(descr, counterKey, value, 0);
    }

    // expand the key/counter map such that the key for each counter is used
    //  as the column name and every key/counter value pair is uploaded as
    //  a new record.
    private boolean write(DbTableDescr descr, String counterKey, Object value, long timestamp) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(counterKey, value);
        return write(descr, data, timestamp);
    }

    // the name of the column used for time is assumed to be 'time' and the
    //  time is automatically retrieved from the current system clock value.
    public boolean write(String database, String table, Map<String, Object>data) {
        return write(database, table, data, 0);
    }

    public boolean write(String database, String table, String timeColumn, Map<String, Object>data) {
        return write(database, table, data, 0);
    }

    // the name of the column used for time is assumed to be 'time'
    public boolean write(String database, String table, Map<String, Object>data, long timestamp) {
        DbTableDescr descr = new DbLogTableDescr(database, table);
        return write(descr, data, timestamp);
    }

    public boolean write(String database, String table, String timeColumn,
                         Map<String, Object>data, long timestamp) {
        DbTableDescr descr = new DbLogTableDescr(database, table, timeColumn);
        return write(descr, data, timestamp);
    }

    private boolean write(DbTableDescr descr, Map<String, Object>data, long timestamp) {
        String packerKey = toBufferPackerKey(descr.getDatabaseName(), descr.getTableName());
        BufferPacker bufferPacker = getBufferPacker(descr);
        try {
            if (!data.containsKey("time")) {
                data.put("time", timestamp == 0 ?
                        System.currentTimeMillis() / 1000 : timestamp);
            }
            synchronized (bufferPacker) {
                bufferPacker.write(data);
            }
            bufferPackerCounterMap.increment(descr, DEFAULT_COUNTER_KEY, 1);
            Log.d(TAG, "write: key=" + packerKey +
                    ", bufsize=" + bufferPacker.getBufferSize());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (bufferPacker.getBufferSize() > BUFFER_FLUSH_SIZE) {
            try {
                flushBufferPacker(descr, bufferPacker);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean writeItem(String database, String table, String primaryKeyName, String primaryKeyType, Map<String, Object>data) {
        DbTableDescr descr = new DbItemTableDescr(database, table, primaryKeyName, primaryKeyType);
        return writeItem(descr, data);
    }

    private boolean writeItem(DbTableDescr descr, Map<String, Object>data) {
        BufferPacker bufferPacker = getBufferPacker(descr);
        try {
            // TODO enforce presence of primary_key in the data but only this info is passed on as parameter
            synchronized (bufferPacker) {
                bufferPacker.write(data);
            }
            bufferPackerCounterMap.increment(descr, DEFAULT_COUNTER_KEY, 1);
            Log.d(TAG, "writeItem: key=" + descr +
                    ", bufsize=" + bufferPacker.getBufferSize());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean flush(DbTableDescr descr) {
        BufferPacker bufferPacker = getBufferPacker(descr);
        try {
            flushBufferPacker(descr, bufferPacker);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean flushAll() {
        boolean isSuccess = true;
        for (DbTableDescr descr : bufferPackerMap.keySet()) {
            if (!flush(descr)) {
                isSuccess = false;
            }
        }
        return isSuccess;
    }

    public void close() {
        flushAll();

        for (DbTableDescr descr : bufferPackerMap.keySet()) {
            try {
                getBufferPacker(descr).close();
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
