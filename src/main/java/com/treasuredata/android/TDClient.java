package com.treasuredata.android;

import android.content.Context;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenProject;
import org.komamitsu.android.util.Log;

import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import java.io.IOException;
import java.security.MessageDigest;
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
        setApiKey(apiKey);
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
    TDClient(String apiKey) {
        super(new TDClientBuilder());
        setApiKey(apiKey);
    }

    private void setApiKey(String apiKey) {
        String projectId = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            projectId = "_td " + (new HexBinaryAdapter()).marshal(md5.digest(apiKey.getBytes()));
        } catch (Exception e) {
            Log.e(TAG, "Failed to create md5 instance", e);
            projectId = "_td default";
        }
        KeenProject project = new KeenProject(projectId, "dummy_write_key", "dummy_read_key");
        setDefaultProject(project);
    }
}
