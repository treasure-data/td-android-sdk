package com.treasure_data.td_logger.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

import org.msgpack.MessagePack;
import org.msgpack.packer.BufferPacker;

import com.treasure_data.td_logger.android.ApiClient.ApiError;

import android.content.Context;

public class TdAndroidLogger {
    private static final String TAG = TdAndroidLogger.class.getSimpleName();
    private static final String RES_DEFTYPE = "string";
    private static final String API_SERVER_HOST = "api.treasure-data.com";
    private static final int API_SERVER_PORT = 80;
    private static final int BUFFER_FLUSH_SIZE = 1 * 1024 * 1024;   // TODO: tune up
    private static final String PACKER_KEY_DELIM = "#";
    private final ApiClient apiClient;
    // TODO: add updated_at
    private final Map<String, BufferPacker> bufferPackerMap = new HashMap<String, BufferPacker>();
    private final MessagePack msgpack = new MessagePack();
    private final RepeatingWorker flushWorker = new RepeatingWorker();
    private final CounterContainer counterContainer = new CounterContainer();

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
        this.apiClient = new ApiClient(apikey, host, port);
    }

    public void increment(String database, String table, String key) {
        increment(database, table, key, 1);
    }

    public void increment(String database, String table, String key, long i) {
        counterContainer.increment(toBufferPackerKey(database, table), key, i);
    }

    private void moveCounterToBuffer() {
        for (Entry<String, Counter> counter : counterContainer) {
            String[] databaseAndTable = fromBufferPackerKey(counter.getKey());
            for (Entry<String, Long> kv : counter.getValue()) {
                write(databaseAndTable[0], databaseAndTable[1], kv.getKey(), kv.getValue());
            }
        }
        counterContainer.clear();
    }

    public boolean write(String database, String table, String key, Object value, long timestamp) {
        HashMap<String,Object> data = new HashMap<String, Object>();
        data.put(key, value);
        return write(database, table, data, timestamp);
    }

    public boolean write(String database, String table, String key, Object value) {
        return write(database, table, key, value, 0);
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
        moveCounterToBuffer();
        if (bufferPacker.getBufferSize() == 0) {
            return;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(out);
        gzipOutputStream.write(bufferPacker.toByteArray());
        gzipOutputStream.close();
        apiClient.importTable(database, table, out.toByteArray());
        bufferPacker.clear();
    }

    public boolean write(String database, String table, Map<String, Object>data, long timestamp) {
        String packerKey = toBufferPackerKey(database, table);
        BufferPacker bufferPacker = getBufferPacker(packerKey);

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
        BufferPacker bufferPacker = getBufferPacker(toBufferPackerKey(database, table));
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
