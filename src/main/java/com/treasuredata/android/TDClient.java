package com.treasuredata.android;

import android.content.Context;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenLogging;
import io.keen.client.java.KeenProject;

import java.io.IOException;
import java.util.concurrent.Executors;

class TDClient extends KeenClient {
    private static final String TAG = TDClient.class.getSimpleName();

    TDClient(Context context, String apiKey) throws IOException {
        super(
                new TDClientBuilder()
                        .withHttpHandler(new TDHttpHandler(apiKey))
                        .withEventStore(new TDEventStore(context.getCacheDir()))
                        .withJsonHandler(new TDJsonHandler())
                        .withPublishExecutor(Executors.newSingleThreadExecutor())
        );
        setDebugMode(true);
        KeenProject project = new KeenProject("_treasure data_", "dummy_write_key", "dummy_read_key");
        setDefaultProject(project);
        setActive(true);
    }

    public static void enableLogging() {
        KeenLogging.enableLogging();
    }

    public static void disableLogging() {
        KeenLogging.disableLogging();
    }
}
