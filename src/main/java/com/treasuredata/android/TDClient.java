package com.treasuredata.android;

import android.content.Context;
import io.keen.client.java.GlobalPropertiesEvaluator;
import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenProject;
import org.komamitsu.android.util.Log;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

class TDClient extends KeenClient {
    private static final String TAG = TDClient.class.getSimpleName();
    private static String defaultApiKey;
    private static String apiEndpoint;
    private static String encryptionKey;

    TDClient(Context context, String apiKey) throws IOException {
        super(
                new TDClientBuilder()
                        .withHttpHandler(new TDHttpHandler((apiKey == null ? TDClient.defaultApiKey : apiKey), apiEndpoint))
                        .withEventStore(new TDEventStore(context.getCacheDir()))
                        .withJsonHandler(new TDJsonHandler(encryptionKey))
                        .withPublishExecutor(Executors.newSingleThreadExecutor())
        );
        // setDebugMode(true);
        setApiKey(apiKey == null ? TDClient.defaultApiKey : apiKey);
        setActive(true);
        setGlobalPropertiesEvaluator(new GlobalPropertiesEvaluator() {
            @Override
            public Map<String, Object> getGlobalProperties(String s) {
                Map<String, Object> properties = new HashMap<String, Object>(1);
                properties.put("#UUID", UUID.randomUUID().toString());
                return properties;
            }
        });
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

    public static void setEncryptionKey(String encryptionKey) {
        TDClient.encryptionKey = encryptionKey;
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
            projectId = createProjectIdFromApiKey(apiKey);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create md5 instance", e);
            projectId = "_td default";
        }
        KeenProject project = new KeenProject(projectId, "dummy_write_key", "dummy_read_key");
        setDefaultProject(project);
    }

    private String createProjectIdFromApiKey(String apiKey) throws NoSuchAlgorithmException {
        StringBuffer hexString = new StringBuffer();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] hash = md5.digest(apiKey.getBytes());

        for (int i = 0; i < hash.length; i++) {
            if ((0xff & hash[i]) < 0x10) {
                hexString.append("0"
                        + Integer.toHexString((0xFF & hash[i])));
            } else {
                hexString.append(Integer.toHexString(0xFF & hash[i]));
            }
        }
        return "_td " + hexString.toString();
    }
}
