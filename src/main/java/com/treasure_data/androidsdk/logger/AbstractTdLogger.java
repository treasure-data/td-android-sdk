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
    private long flushIntervalMillis = RepeatingWorker.DEFAULT_INTERVAL_MILLI;

    // worker thread to pass the data to the Service through Intents
    protected RepeatingWorker flushWorker;
    private final Runnable flushTask = new Runnable() {
        @Override
        public void run() {
            flushAll();
        }
    };

    // these methods need to be implemented in the derived class
    abstract boolean outputData(DbTableDescr descr, byte[] data);
    abstract void cleanup();
    public abstract void setUploadWorkerInterval(long intervalMillis);

    // default constructor
    //  NOTE: instantiates and starts the default flushWorker on call
    public AbstractTdLogger() {
        this(new RepeatingWorker(), true);
    }

    // constructor, loads the flushWorker and starts it on request.
    //  the worker's execution interval is also set here
    public AbstractTdLogger(RepeatingWorker worker, boolean startFlushWorkerOnInit) {
        // instantiate worker
        flushWorker = worker;
        flushWorker.setProcedure(flushTask);

        // start worker if requested
        if (startFlushWorkerOnInit) {
            startFlushWorker();
        }
    }

    // apply the most recent flush interval that was requested.
    // NOTE:
    //  newInterval will only be effective starting with the next flush
    //  interval; the next flush will occur exactly oldInterval milliseconds
    //  after the flush preceding it.
    private void applyFlushWorkerInteval(long millis) {
        long actualIntervalMillis = flushWorker.setInterval(flushIntervalMillis);
        if (actualIntervalMillis < flushIntervalMillis) {
            Log.w(TAG, "Requested flushWorker's interval " +
                    "(" + flushIntervalMillis + ") is smaller than the minimum " +
                    "allowed (" + actualIntervalMillis / 1000 + ")");
        }
        flushIntervalMillis = actualIntervalMillis;
        Log.v(TAG, "Changed flush worker's interval to " +
                flushIntervalMillis + " ms");
    }

    // start the flush worker.
    // This method is intended to be used if the logger was created with
    //  the alternate constructor AbstractTdLogger(RepeatingWorker, boolean)
    //  and with the boolean set to false.
    public void startFlushWorker() {
        if (!flushWorker.isRunning()) {
            applyFlushWorkerInteval(flushIntervalMillis);
            flushWorker.start();
        }
    }

    // stop the flush worker.
    public void stopFlushWorker() {
        if (flushWorker.isRunning()) {
            flushWorker.stop();
        }
    }

    // set the flush worker's interval.
    // If the flush worker is already running, apply the new interval
    //  immediately.
    // This method is best called before AbstractTdLogger#setFlushWorkerInterval
    //  right after the logger is instantiated for the interval to take
    //  immediately effect; e.g.:
    //      AbstractTdLogger logger = new DefaultTdLogger(this);
    //      logger.setFlushWorkerInterval(10000);
    //      logger.startFlushWorker();
    public void setFlushWorkerInterval(long millis) {
        flushIntervalMillis = millis;
        // apply immediately if the worker is running
        if (flushWorker.isRunning())
            applyFlushWorkerInteval(flushIntervalMillis);
    }

    //
    // key/value map store for counters. These counters are written as
    //  a single record every time the flush worker executes
    //

    // NOTE:
    //  increment only imports data to a log table. Item table not supported.
    // Generic counter increment function.
    //    The counter indexed by 'key' gets incremented by 'amount'.
    public void increment(String database, String table, String key, long amount) {
        DbLogTableDescr descr = new DbLogTableDescr(database, table);
        getBufferPacker(descr);     // prepare to flush()
        Log.d(TAG, "increment: key=" + descr +
                " counter_key=" + key + " amount=" + amount);
        counterContainer.increment(descr, key, amount);
    }

    // NOTE: increment only imports data to a log table. Item table not supported.
    public void increment(String database, String table, String key) {
        increment(database, table, key, 1);
    }

    // NOTE: increment only imports data to a log table. Item table not supported.
    private void moveCounterToBuffer(DbTableDescr descr) {
        Counter counter = counterContainer.getCounter(descr);
        if (counter == null || counter.size() == 0) {
            return;
        }

        Map <String, Object> countersMap = new HashMap<String, Object> ();
        for (Entry<String, Long> kv : counter) {
            countersMap.put(kv.getKey(), kv.getValue());
        }
        Log.d(TAG, "moveCounterToBuffer: key=" + descr +
                " counters=" + counter.size());
        writeLog(descr, countersMap, 0L);
        counter.clear();
    }

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

    private synchronized void flushBufferPacker(DbTableDescr descr, BufferPacker bufferPacker) throws IOException {
        moveCounterToBuffer(descr);
        ByteArrayOutputStream out = null;
        synchronized (bufferPacker) {
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
            bufferPacker.clear();
        }
        bufferPackerCounterMap.clear(descr);
    }

    //
    // writeLog APIs
    //

    //
    // APIs to log a single key/value pair
    //

    // add a single key/value to the bufferPackerMap.
    private boolean writeLog(DbTableDescr descr, String key, Object value, long timestamp) {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return writeLog(descr, data, timestamp);
    }

//    // add a single key/value to the bufferPackerMap.
//    // The timestamp is not provided and will be generated based on system
//    //  clock at the time of write.
//    // It is also used by moveCounterToBuffer to write the counter status
//    //  at any point in time.
//    private boolean writeLog(DbTableDescr descr, String key, Object value) {
//        return writeLog(descr, key, value, 0);
//    }

    // public API for writing a single key/value with timestamp.
    // Backwards compatible API.
    // @deprecated refer to
    //  boolean writeLog(String database, String table,
    //                   String key, Object value, long timestamp)
    // TODO Javadoc
    @Deprecated
    public boolean write(String database, String table,
            String key, Object value, long timestamp) {
        DbTableDescr descr = new DbLogTableDescr(database, table);
        return writeLog(descr, key, value, timestamp);
    }

    // public API for writing a single key/value with timestamp.
    // TODO Javadoc
    public boolean writeLog(String database, String table,
            String key, Object value, long timestamp) {
        DbTableDescr descr = new DbLogTableDescr(database, table);
        return writeLog(descr, key, value, timestamp);
    }

    // public API for writing a single key/value without timestamp.
    //  The timestamp is generated using the system clock.
    // Backwards compatible API.
    // @deprecated refer to
    //  boolean writeLog(String database, String table, String key, Object value)
    // TODO Javadoc
    @Deprecated
    public boolean write(String database, String table, String key, Object value) {
        return writeLog(database, table, key, value, 0);
    }

    // public API for writing a single key/value without timestamp.
    //  The timestamp is generated using the system clock.
    // TODO Javadoc
    public boolean writeLog(String database, String table, String key, Object value) {
        return writeLog(database, table, key, value, 0);
    }

    //
    // APIs for logging an arbitrarily long set of key/value pairs
    //

    // add the key/value pairs to the bufferPackerMap using the timestamp
    //  provided.
    private boolean writeLog(DbTableDescr descr, Map<String, Object>data, long timestamp) {
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
            Log.d(TAG, "writeLog: key=" + descr +
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

    // public API for writing a set of key/value pairs and associated timestamp
    //  into a table identified by database & table names, and timeColumn time
    //  column name.
    // Backwards compatible API.
    // @deprecated refer to
    //  boolean writeLog(String database, String table, String timeColumn,
    //                   Map<String, Object>data, long timestamp)
    // TODO Javadoc
    @Deprecated
    public boolean write(String database, String table, String timeColumn,
            Map<String, Object>data, long timestamp) {
        DbTableDescr descr = new DbLogTableDescr(database, table, timeColumn);
        return writeLog(descr, data, timestamp);
    }

    // public API for writing a set of key/value pairs and associated timestamp
    //  into a table identified by database & table names, and timeColumn time
    //  column name.
    // TODO Javadoc
    public boolean writeLog(String database, String table, String timeColumn,
            Map<String, Object>data, long timestamp) {
        DbTableDescr descr = new DbLogTableDescr(database, table, timeColumn);
        return writeLog(descr, data, timestamp);
    }

    // public API for writing a set of key/value pairs with no associated
    //  timestamp into a table identified by database & table names, and
    //  timeColumn time column name.
    // The timestamp is retrieved from system time.
    // Backwards compatible API.
    // @deprecated refer to
    //  boolean writeLog(String database, String table, String timeColumn,
    //                   Map<String, Object>data)
    // TODO Javadoc
    @Deprecated
    public boolean write(String database, String table, String timeColumn,
            Map<String, Object>data) {
        return writeLog(database, table, timeColumn, data, 0);
    }

    // public API for writing a set of key/value pairs with no associated
    //  timestamp into a table identified by database & table names, and
    //  timeColumn time column name.
    // The timestamp is retrieved from system time.
    // TODO Javadoc
    public boolean writeLog(String database, String table, String timeColumn,
            Map<String, Object>data) {
        return writeLog(database, table, timeColumn, data, 0);
    }

    // public API for writing a set of key/value pairs with no associated
    //  timestamp. The table is identified by the the database and table name,
    //  while the time column name is not provided and assumed to be 'time'
    // The timestamp is retrieved from system time.
    // Backwards compatible API.
    // @deprecated refer to
    //  boolean writeLog(String database, String table, Map<String, Object>data)
    // TODO Javadoc
    @Deprecated
    public boolean write(String database, String table,
            Map<String, Object>data) {
        return writeLog(database, table, "time", data, 0);
    }

    // public API for writing a set of key/value pairs with no associated
    //  timestamp. The table is identified by the the database and table name,
    //  while the time column name is not provided and assumed to be 'time'
    // The timestamp is retrieved from system time.
    // TODO Javadoc
    public boolean writeLog(String database, String table,
            Map<String, Object>data) {
        return writeLog(database, table, "time", data, 0);
    }

    // public API for writing a set of key/value pairs with associated
    //  timestamp into a table identified by database and table names and
    //  with time column name defaulting to 'time'.
    // Backwards compatible API.
    // @see boolean writeLog(String database, String table,
    //                  Map<String, Object>data, long timestamp)
    // TODO Javadoc
    public boolean write(String database, String table,
            Map<String, Object>data, long timestamp) {
        return writeLog(database, table, "time", data, timestamp);
    }

    // public API for writing a set of key/value pairs with associated
    //  timestamp into a table identified by database and table names and
    //  with time column name defaulting to 'time'.
    // TODO Javadoc
    public boolean writeLog(String database, String table,
            Map<String, Object>data, long timestamp) {
        return writeLog(database, table, "time", data, timestamp);
    }

    //
    //  APIs for writing to item table
    //

    // add a set of key/value pairs to the bufferPacker for the item
    //  table identified by the descr.
    // TODO Javadoc
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

    // public API for writing a set of key/value pairs. The table is identified
    //  by the database and table name as well as the primary key name and type.
    //  This table of type 'item', hence data logged here does not need an
    //  associated timestamp.
    // TODO Javadoc
    public boolean writeItem(String database, String table,
            String primaryKeyName, String primaryKeyType,
            Map<String, Object>data) {
        DbTableDescr descr = new DbItemTableDescr(database, table,
                primaryKeyName, primaryKeyType);
        return writeItem(descr, data);
    }

    // public API for writing a single key/value pair. The table is identified
    //  by the database and table name as well as the primary key name and type.
    //  This table of type 'item', hence data logged here does not need an
    //  associated timestamp.
    // TODO Javadoc
    public boolean writeItem(String database, String table,
            String primaryKeyName, String primaryKeyType,
            String key, Object value) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return writeItem(database, table, primaryKeyName, primaryKeyType, data);
    }

    //
    // flush APIs
    //

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
        // flush buffered data one last time
        flushAll();
        // close msgpack objects
        for (DbTableDescr descr : bufferPackerMap.keySet()) {
            try {
                getBufferPacker(descr).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // stop the worker
        stopFlushWorker();
        // sent close message to the service
        cleanup();
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }
}
