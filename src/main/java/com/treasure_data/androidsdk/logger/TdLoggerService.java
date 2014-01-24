package com.treasure_data.androidsdk.logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.komamitsu.android.util.Log;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.treasure_data.androidsdk.apiclient.ApiClient;
import com.treasure_data.androidsdk.apiclient.DbTableDescr;
import com.treasure_data.androidsdk.apiclient.DefaultApiClient;
import com.treasure_data.androidsdk.apiclient.DefaultApiClient.ApiError;
import com.treasure_data.androidsdk.apiclient.TdTableImporter;
import com.treasure_data.androidsdk.util.RepeatingWorker;

public class TdLoggerService extends Service {
    private static final String TAG = TdLoggerService.class.getSimpleName();
    private static final String ACTION_FLUSH = TdLoggerService.class.getName() + ".ACTION_FLUSH";
    private static final String ACTION_CLOSE = TdLoggerService.class.getName() + ".ACTION_CLOSE";
    private static final String EXTRA_KEY_DATA = "data";
    private static final String EXTRA_KEY_DBTBLDESCR = "descr";

    private static final String RES_DEFTYPE = "string";
    private static final String API_SERVER_HOST = "api.treasure-data.com";
    private static final int API_SERVER_PORT = 443;

    private Map<DbTableDescr, List<ByteBuffer>> msgpackMap = new HashMap<DbTableDescr, List<ByteBuffer>>();
    private final RepeatingWorker uploadWorker = new RepeatingWorker();
    private LogReceiver logReceiver;
    private boolean isClosing;
    private String apikey;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: " + this);

        final ApiClient apiClient= new DefaultApiClient();
        apikey = getString(getResources().getIdentifier("td_apikey", RES_DEFTYPE, getPackageName()));
        apiClient.init(apikey, API_SERVER_HOST, API_SERVER_PORT);

        final TdTableImporter tdTableImporter = new TdTableImporter(apiClient);

        uploadWorker.setProcedure(new Runnable() {
            @Override
            public void run() {
                // avoid concurrency issues with msgpackMap with adding data
                //  to the msgpackMap[packerKey] LinkedList (happens in the
                //  'LogReceiver') and removing from it (happens in this
                //  worker - uploadWorker).
                synchronized(msgpackMap) {
                    LinkedList<DbTableDescr> uploadedKeys = new LinkedList<DbTableDescr>();
                    for (Entry<DbTableDescr, List<ByteBuffer>> keyAndMsgpacks : msgpackMap.entrySet()) {
                        //String[] databaseAndTable = fromMsgpackMapKey(keyAndMsgpacks.getKey());
                        Iterator<ByteBuffer> msgpacks = keyAndMsgpacks.getValue().iterator();
                        while (msgpacks.hasNext()) {
                            ByteBuffer buff = msgpacks.next();
                            try {
                                tdTableImporter.output(keyAndMsgpacks.getKey(), buff.array());
                                msgpacks.remove();
                                TimeUnit.SECONDS.sleep(5);
                            } catch (IOException e) {
                                Log.e(TAG, "import table error: IOException " +
                                        e + ". Upload will be retried later, " +
                                        "moving onto the next data. ");
                            } catch (ApiError e) {
                                Log.e(TAG, "import table error: ApiError " +
                                        e + ". Upload will be retried later, " +
                                        "moving onto the next data.");
                            } catch (InterruptedException e) {
                                // do nothing, will be retried at the next run of the uploadWorker
                            }
                        }
                        if (keyAndMsgpacks.getValue().size() == 0) {
                            uploadedKeys.add(keyAndMsgpacks.getKey());
                        }
                    }
                    // remove data for all the keys whose data was uploaded.
                    //  Access to the msgpackMap is prevented until after this
                    //  is done.
                    for (DbTableDescr descr : uploadedKeys) {
                        msgpackMap.remove(descr);
                    }
                }    // unlock msgpackMap object

                if (isClosing && msgpackMap.keySet().size() == 0) {
                    Log.d(TAG, "closing...");
                    uploadWorker.stop();
                }
            }
        });

        IntentFilter intentFilterFlush = new IntentFilter(ACTION_FLUSH);
        IntentFilter intentFilterClose = new IntentFilter(ACTION_CLOSE);
        logReceiver = new LogReceiver();
        registerReceiver(logReceiver, intentFilterFlush);
        registerReceiver(logReceiver, intentFilterClose);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        isClosing = true;
        unregisterReceiver(logReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isClosing = false;
        synchronized (uploadWorker) {
            if (!uploadWorker.isRunning()) {
                uploadWorker.start();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private class LogReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "received intent " + intent);
            if (intent.getAction().equals(ACTION_FLUSH)) {
                DbTableDescr descr = intent.getExtras().getParcelable(EXTRA_KEY_DBTBLDESCR);
                descr.getTableType();
                byte[] data = intent.getExtras().getByteArray(EXTRA_KEY_DATA);
                // avoid concurrency issues with msgpackMap with adding data
                //  to the msgpackMap[packerKey] Linked list (happens in this
                //  'Receiver') and removing from it (happens in the
                //  uploadWorker).
                synchronized (msgpackMap) {
                    List<ByteBuffer> msgpacks = msgpackMap.get(descr);
                    if (msgpacks == null) {
                        msgpacks = new LinkedList<ByteBuffer>();
                        msgpackMap.put(descr, msgpacks);
                    }
                    msgpacks.add(ByteBuffer.wrap(data));
                }
            }
            else if (intent.getAction().equals(ACTION_CLOSE)) {
                isClosing = true;
            }
        }
    }

    private static String toMsgpackMapKey(String database, String table) {
        return new StringBuilder().append(database).append("#").append(table).toString();
    }

    private static String[] fromMsgpackMapKey(String key) {
        return key.split("#");
    }

    public static Intent createIntentForFlush(DbTableDescr descr, byte[] data) {
        Intent intent = new Intent();
        intent.setAction(ACTION_FLUSH);
        intent.putExtra(EXTRA_KEY_DBTBLDESCR, descr);
        intent.putExtra(EXTRA_KEY_DATA, data);
        return intent;
    }

    public static Intent createIntentForClose() {
        Intent intent = new Intent();
        intent.setAction(ACTION_CLOSE);
        return intent;
    }
}
