package com.treasuredata.android;

import android.content.Context;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenLogging;
import io.keen.client.java.KeenProject;

import java.io.IOException;
import java.util.concurrent.Executors;

class TDClient extends KeenClient {
    private static final String TAG = TDClient.class.getSimpleName();
    private static String defaultApiKey;
    private static String apiEndpoint;

    TDClient(Context context, String apiKey) throws IOException {
        super(
                new TDClientBuilder()
                        .withHttpHandler(new TDHttpHandler((apiKey == null ? TDClient.defaultApiKey : apiKey), apiEndpoint))
                        .withEventStore(new TDEventStore(context.getCacheDir()))
                        .withJsonHandler(new TDJsonHandler())
                        .withPublishExecutor(Executors.newSingleThreadExecutor())
        );
        // setDebugMode(true);
        KeenProject project = new KeenProject("_treasure data_", "dummy_write_key", "dummy_read_key");
        setDefaultProject(project);
        setActive(true);
    }

    static void setDefaultApiKey(String defaultApiKey) {
        TDClient.defaultApiKey = defaultApiKey;
    }

    static String getDefaultApiKey() {
        return TDClient.defaultApiKey;
    }

    static void setApiEndpoint(String apiEndpoint) {
        TDClient.apiEndpoint = apiEndpoint;
    }

    // Only for test
    @Deprecated
    TDClient() {
        super(new TDClientBuilder());
    }

    public static void enableLogging() {
        KeenLogging.enableLogging();
    }

    public static void disableLogging() {
        KeenLogging.disableLogging();
    }
}
