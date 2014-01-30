package com.treasure_data.androidsdk.logger;


import android.content.Context;
import android.content.Intent;

import com.treasure_data.androidsdk.apiclient.DbTableDescr;
import com.treasure_data.androidsdk.util.RepeatingWorker;

public class DefaultTdLogger extends AbstractTdLogger {
    private static final String TAG = DefaultTdLogger.class.getSimpleName();
    private final Context context;
    private Intent serviceIntent;

    public DefaultTdLogger(Context context) {
        this(context, true);
    }

    public DefaultTdLogger(Context context, boolean startFlushWorkerOnInit) {
        super(new RepeatingWorker(), startFlushWorkerOnInit);
        this.context = context;
        // instantiate and run the TdLoggerService service in the background
        serviceIntent = new Intent(this.context, TdLoggerService.class);
        this.context.startService(serviceIntent);
    }

    @Override
    boolean outputData(DbTableDescr descr, byte[] data) {
        Intent intentForFlush = TdLoggerService.createIntentForFlush(descr, data);
        context.sendBroadcast(intentForFlush);
        return true;
    }

    @Override
    void cleanup() {
        Intent intentForClose = TdLoggerService.createIntentForClose();
        context.sendBroadcast(intentForClose);
    }

    // set the upload worker's interval in the Service at runtime. The Service
    //  must be up and running for this request to be processed in the
    //  TdLoggerService#LogReceiver BroadcastReceiver.
    // The new interval will be applied immediately.
    // NOTE:
    //  newInterval will only be effective starting with the next upload
    //  interval; the next upload will occur exactly oldInterval after the
    //  upload before it.
    @Override
    public void setUploadWorkerInterval(long intervalMillis) {
        Intent intentForUpdateInterval =
                TdLoggerService.createIntentForUpdateInterval(intervalMillis);
        context.sendBroadcast(intentForUpdateInterval);
    }

}
