package com.treasuredata.android.cdp;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static android.os.Looper.getMainLooper;
import static android.os.Looper.myLooper;
import static android.text.TextUtils.join;
import static java.lang.Runtime.getRuntime;
import static java.util.concurrent.Executors.newFixedThreadPool;

class CdpClient {

    private final String endpoint;
    private final ExecutorService executor;

    public CdpClient() {
        this("https://cdp.in.treasuredata.com");
    }

    public CdpClient(String endpoint) {
        this(endpoint, newFixedThreadPool(getRuntime().availableProcessors()));
    }

    public CdpClient(String endpoint, ExecutorService executor) {
        this.endpoint = endpoint;
        this.executor = executor;
    }

    public void fetchUserSegment(final List<String> audienceTokens, final Map<String, String> keys, final FetchUserSegmentsCallback callback) {
        // Copy parameters to avoid concurrent modifications upstream
        final ArrayList<String> profileTokensSafeCopy = new ArrayList<>(audienceTokens);
        final HashMap<String, String> keysSafeCopy = new HashMap<>(keys);

        // If current thread is associated with a looper, then use that for the callback invocation,
        // Use main loop otherwise.
        final Looper callbackLooper = myLooper() != null ? myLooper() : getMainLooper();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final SegmentsResult result = fetchUserSegmentResultSynchronously(profileTokensSafeCopy, keysSafeCopy);

                new Handler(callbackLooper).post(new Runnable() {
                    @Override
                    public void run() {
                        result.invoke(callback);
                    }
                });
            }
        });
    }

    private SegmentsResult fetchUserSegmentResultSynchronously(final List<String> profileTokens, final Map<String, String> keys) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("version", "2");
        parameters.put("token", join(",", profileTokens));
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            parameters.put("key." + entry.getKey(), entry.getValue());
        }

        // FIXME: proper path concatenation
        try {
            HttpURLConnection connection = null;
            connection = (HttpURLConnection) new URL(endpoint + "/cdp/lookup/collect/segments?" + queryString(parameters))
                    .openConnection();
            connection.setRequestMethod("GET");

            // TODO: tune
            connection.setConnectTimeout(99999);
            connection.setReadTimeout(999999);
            // connection.setDoOutput(true);
            // connection.getOutputStream().write("Hello".getBytes());
            // connection.connect();

            int responseCode = connection.getResponseCode();
            return SegmentsResult.create(
                    responseCode,
                    connection.getInputStream());
            // return new SegmentsJSONObjectResult(responseCode, convertStreamToString(connection.getInputStream()));
        } catch (IOException e) {
            return SegmentsResult.create(e);
        }
    }

    private static String queryString(Map<String, String> parameters) {
        List<String> urlEncodedEntries = new ArrayList<>();
        try {
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                urlEncodedEntries.add(
                        URLEncoder.encode(param.getKey(), "UTF-8")
                                + "=" + URLEncoder.encode(param.getValue(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            // FIXME: handle this
            e.printStackTrace();
        }
        return join("&", urlEncodedEntries);
    }

}
