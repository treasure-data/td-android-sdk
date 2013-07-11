package com.treasure_data.androidsdk;

import android.content.Context;
import android.content.Intent;

public class DefaultTdLogger extends AbstractTdLogger {
    private final Context context;
    private Intent serviceIntent;
    public DefaultTdLogger(Context context) {
        this.context = context;
        serviceIntent = new Intent(this.context, TdLoggerService.class);
        this.context.startService(serviceIntent);
    }

    @Override
    boolean outputData(String database, String table, byte[] data) {
        Intent intentForFlush = TdLoggerService.createIntentForFlush(database, table, data);
        context.sendBroadcast(intentForFlush);
        return true;
    }

    @Override
    void cleanup() {
        context.stopService(serviceIntent);
    }
}
