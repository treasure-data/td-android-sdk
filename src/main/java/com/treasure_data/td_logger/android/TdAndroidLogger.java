package com.treasure_data.td_logger.android;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

import com.treasure_data.td_logger.android.DefaultApiClient.ApiError;

import android.content.Context;

public class TdAndroidLogger {
    private static final String TAG = TdAndroidLogger.class.getSimpleName();
    private static final String RES_DEFTYPE = "string";
    private static final String API_SERVER_HOST = "api.treasure-data.com";
    private static final int API_SERVER_PORT = 80;
    private static final int BUFFER_FLUSH_SIZE = 1 * 1024 * 1024;   // TODO: tune up
    private static final String PACKER_KEY_DELIM = "#";
    private static Class<? extends ApiClient> apiClientClass = DefaultApiClient.class;
    final ApiClient apiClient;
    // TODO: add updated_at
    private final Map<String, BufferPacker> bufferPackerMap = new HashMap<String, BufferPacker>();
    private final MessagePack msgpack = new MessagePack();
    private final RepeatingWorker flushWorker = new RepeatingWorker();
    private final CounterContainer counterContainer = new CounterContainer();

    public static void setApiClientClass(Class<? extends ApiClient> klass) {
        apiClientClass = klass;
    }

    public TdAndroidLogger(Context context) {
        this(context.getString(context.getResources().getIdentifier("apikey", RES_DEFTYPE, context.getPackageName())));
    }

    public TdAndroidLogger(Context context, boolean autoFlushing) {
        this(context);
        startAutoFlushing();
    }

    public TdAndroidLogger(String apikey) {
        this(apikey, API_SERVER_HOST, API_SERVER_PORT);
    }

    public TdAndroidLogger(String apikey, boolean autoFlushing) {
        this(apikey, API_SERVER_HOST, API_SERVER_PORT);
        startAutoFlushing();
    }

    public TdAndroidLogger(String apikey, String host, int port) {
        try {
            this.apiClient = apiClientClass.newInstance();
            this.apiClient.init(apikey, host, port);
        } catch (InstantiationException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
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

    private synchronized void flushBufferPacker(String database, String table, BufferPacker bufferPacker) throws IOException, ApiError {
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
            try {
                apiClient.importTable(database, table, out.toByteArray());
            }
            catch (FileNotFoundException e) {
                Log.w(TAG, "flushBufferPacker", e);
                // TODO: retry management
                apiClient.createTable(database, table);
                apiClient.importTable(database, table, out.toByteArray());
            }
        }
        finally {
            IOUtils.closeQuietly(out);
        }
        bufferPacker.clear();
    }

    public boolean write(String database, String table, Map<String, Object>data, long timestamp) {
        BufferPacker bufferPacker = getBufferPacker(database, table);

        try {
            Log.d(TAG, ">>>>>>>>>>> " + database + ", " + table + ", " + timestamp);
            if (!data.containsKey("time")) {
                data.put("time", timestamp == 0 ? System.currentTimeMillis() / 1000 : timestamp);
            }
            for (Entry<String, Object> e : data.entrySet()) {
                Log.d(TAG, ">>>> " + e);
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
            } catch (ApiError e) {
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
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ApiError e) {
            e.printStackTrace();
        }
        return false;
    }

    public void flushAll() {
        for (String bufferPackerKey : bufferPackerMap.keySet()) {
            String[] databaseAndTable = fromBufferPackerKey(bufferPackerKey);
            flush(databaseAndTable[0], databaseAndTable[1]);
        }
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
    }

    public void startAutoFlushing() {
        startAutoFlushing(0);
    }

    public void startAutoFlushing(long intervalMilli) {
        if (intervalMilli != 0) {
            flushWorker.setInterval(intervalMilli);
        }
        flushWorker.setProcedure(new Runnable() {
            @Override
            public void run() {
                flushAll();
            }
        });
        flushWorker.start();
    }

    public void stopAutoFlushing() {
        flushWorker.stop();
    }
}
