package com.treasure_data.androidsdk.logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

import com.treasure_data.androidsdk.counter.Counter;
import com.treasure_data.androidsdk.counter.CounterContainer;
import com.treasure_data.androidsdk.util.Log;
import com.treasure_data.androidsdk.util.RepeatingWorker;


public abstract class AbstractTdLogger {
    private static final String TAG = AbstractTdLogger.class.getSimpleName();
    private static final int BUFFER_FLUSH_SIZE = 1 * 1024 * 1024;   // TODO: tune up
    private static final String PACKER_KEY_DELIM = "#";
    // TODO: add updated_at
    private final Map<String, BufferPacker> bufferPackerMap = new HashMap<String, BufferPacker>();
    private final MessagePack msgpack = new MessagePack();
    private final CounterContainer counterContainer = new CounterContainer();
    protected RepeatingWorker flushWorker = new RepeatingWorker();

    abstract boolean outputData(String database, String table, byte[] data);

    abstract void cleanup();

    public AbstractTdLogger() {
        this(true);
    }

    public AbstractTdLogger(boolean startFlushWorkerOnInit) {
        flushWorker.setProcedure(new Runnable() {
            @Override
            public void run() {
                flushAll();
            }
        });

        if (startFlushWorkerOnInit) {
            startFlushWorker();
        }
    }

    protected void setFlushWorker(RepeatingWorker worker) {
        if (flushWorker != null) {
            flushWorker.stop();
        }
        flushWorker = worker;
    }

    public void startFlushWorker() {
        if (!flushWorker.isRunning()) {
            flushWorker.start();
        }
    }

    public void increment(String database, String table, String key) {
        increment(database, table, key, 1);
    }

    public void increment(String database, String table, String key, long i) {
        getBufferPacker(database, table);   // prepare to flush()
        counterContainer.increment(toBufferPackerKey(database, table), key, i);
    }

    private void moveCounterToBuffer(String database, String table) {
        Log.d(TAG, "moveCounterToBuffer: database=" + database + ", table=" + table);
        String key = toBufferPackerKey(database, table);
        Counter counter = counterContainer.getCounter(key);
        if (counter == null) {
            return;
        }

        for (Entry<String, Long> kv : counter) {
            write(database, table, kv.getKey(), kv.getValue());
        }
        counter.clear();
    }

    public boolean write(String database, String table, String key, Object value, long timestamp) {
        HashMap<String,Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return write(database, table, data, timestamp);
    }

    public boolean write(String database, String table, String key, Object value) {
        return write(database, table, key, value, 0);
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
        return bufferPacker;
    }

    private String toBufferPackerKey(String database, String table) {
        return new StringBuilder(database).append(PACKER_KEY_DELIM).append(table).toString();
    }

    private String[] fromBufferPackerKey(String bufferPackerKey) {
        return bufferPackerKey.split(PACKER_KEY_DELIM);
    }

    private synchronized void flushBufferPacker(String database, String table, BufferPacker bufferPacker) throws IOException {
        moveCounterToBuffer(database, table);
        ByteArrayOutputStream out = null;
        try {
            if (bufferPacker.getBufferSize() == 0) {
                return;
            }
            out = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
            gzipOutputStream.write(bufferPacker.toByteArray());
            gzipOutputStream.close();
            outputData(database, table, out.toByteArray());
        }
        finally {
            IOUtils.closeQuietly(out);
        }
        bufferPacker.clear();
    }

    public boolean write(String database, String table, Map<String, Object>data, long timestamp) {
        BufferPacker bufferPacker = getBufferPacker(database, table);

        try {
            if (!data.containsKey("time")) {
                data.put("time", timestamp == 0 ? System.currentTimeMillis() / 1000 : timestamp);
            }
            bufferPacker.write(data);
            Log.d(TAG, "write: bufsize=" + bufferPacker.getBufferSize());
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (bufferPacker.getBufferSize() > BUFFER_FLUSH_SIZE) {
            try {
                flushBufferPacker(database, table, bufferPacker);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean write(String database, String table, Map<String, Object>data) {
        return write(database, table, data, 0);
    }

    public boolean flush(String database, String table) {
        BufferPacker bufferPacker = getBufferPacker(database, table);
        try {
            flushBufferPacker(database, table, bufferPacker);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean flushAll() {
        boolean isSuccess = true;
        for (String bufferPackerKey : bufferPackerMap.keySet()) {
            String[] databaseAndTable = fromBufferPackerKey(bufferPackerKey);
            if (!flush(databaseAndTable[0], databaseAndTable[1])) {
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
