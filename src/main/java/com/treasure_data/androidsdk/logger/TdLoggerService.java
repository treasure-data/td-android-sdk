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
import android.content.res.Resources.NotFoundException;
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
    private static final String ACTION_UPDATE_INTERVAL =
                                               TdLoggerService.class.getName() + ".ACTION_UPDATE_INTERVAL";

    private static final String EXTRA_KEY_DATA = "data";
    private static final String EXTRA_KEY_DBTBLDESCR = "descr";
    private static final String EXTRA_KEY_UPDATE_INTERVAL = "interval";

    private static final String RES_DEFTYPE = "string";
    private static final String API_SERVER_HOST = "api.treasure-data.com";
    private static final int API_SERVER_PORT = 443;

    private Map<DbTableDescr, List<ByteBuffer>> msgpackMap = new HashMap<DbTableDescr, List<ByteBuffer>>();
    private final RepeatingWorker uploadWorker = new RepeatingWorker();
    private LogReceiver logReceiver;
    private boolean isClosing;
    private String apikey;
    private static long uploadIntervalMillis = RepeatingWorker.DEFAULT_INTERVAL_MILLI;

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: " + this);

        final ApiClient apiClient= new DefaultApiClient();
        try {
            apikey = getString(getResources().getIdentifier(
                        "td_apikey", RES_DEFTYPE, getPackageName()));
        } catch(NotFoundException e) {
            Log.e(TAG, "'td_apikey' string resource not found. " +
                    "For the Service to properly function a valid API key " +
                    "needs to be provided. Refer to the documentation.");
            throw e;
        }
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
            applyUploadWorkerInterval();
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
            else if (intent.getAction().equals(ACTION_UPDATE_INTERVAL)) {
                int intervalMilli = intent.getIntExtra(EXTRA_KEY_UPDATE_INTERVAL, -1);
                if (intervalMilli > 0) {
                    applyUploadWorkerInterval();
                }
            }
        }
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

    public static Intent createIntentForUpdateInterval(long intervalMillis) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_KEY_UPDATE_INTERVAL, intervalMillis);
        intent.setAction(ACTION_UPDATE_INTERVAL);
        uploadIntervalMillis = intervalMillis;
        return intent;
    }

    // TODO Javadoc
    // This method is effective only if called before the service's
    //  onStartCommand method is called.
    // Since this is a static method, the requested interval will only
    //  be applied when TdLoggerService#onStartCommand is executed as an
    //  effect of a startService call of an Activity.
    // For runtime interval changes please see
    //  DefaultTdLogger#setUploadWorkerInterval.
    public static void setUploadWorkerInterval(long millis) {
        uploadIntervalMillis = millis;
    }

    // apply the most recent flush interval that was requested.
    // NOTE:
    //  newInterval will only be effective starting with the next flush
    //  interval; the next flush will occur exactly oldInterval milliseconds
    //  after the flush preceding it.
    private void applyUploadWorkerInterval() {
        long actualInterval = uploadWorker.setInterval(uploadIntervalMillis);
        if (actualInterval < uploadIntervalMillis)
            Log.w(TAG, "Requested uploadWorker's interval (" + uploadIntervalMillis +
                    ") is smaller than the minimum allowed (" +
                    actualInterval + ")");
        Log.v(TAG, "Changing upload worker's interval to " +
                actualInterval + " ms");
        uploadIntervalMillis = actualInterval;
    }
}
