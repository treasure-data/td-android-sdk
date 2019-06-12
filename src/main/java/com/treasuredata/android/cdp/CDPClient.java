package com.treasuredata.android.cdp;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static android.os.Looper.getMainLooper;
import static android.os.Looper.myLooper;
import static android.text.TextUtils.join;
import static io.keen.client.java.KeenUtils.convertStreamToString;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * A single-purpose client, to lookup for CDP's Profiles
 */
public class CDPClient {
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 60000;

    private static final URI DEFAULT_ENDPOINT;

    static {
        try {
            DEFAULT_ENDPOINT = new URI("https://cdp.in.treasuredata.com");
        } catch (URISyntaxException e) {
            // Should not ever happen
            throw new IllegalStateException(e);
        }
    }

    private final URI apiURI;
    private final ExecutorService executor;

    public CDPClient() {
        this(DEFAULT_ENDPOINT);
    }

    public CDPClient(String endpoint) throws URISyntaxException {
        this(new URI(endpoint));
    }

    public CDPClient(URI endpoint) {
        // Could be opened for number of threads customization later
        this(endpoint, newFixedThreadPool(1));
    }

    private CDPClient(URI endpoint, ExecutorService executor) {
        this.apiURI = endpoint.resolve("/cdp/lookup/collect/segments");
        this.executor = executor;
    }

    /**
     * @param profileAPITokens that are defined on TreasureData
     * @param keys             lookup keyColumn values
     * @param callback         to receive the looked up result
     */
    public void fetchUserSegments(final List<String> profileAPITokens,
                                  final Map<String, String> keys,
                                  final FetchUserSegmentsCallback callback) {
        if (profileAPITokens == null) throw new NullPointerException("`profileAPITokens` is required!");
        if (keys == null) throw new NullPointerException("`keys` is required!");
        if (callback == null) throw new NullPointerException("`callback` is required");

        // Copy parameters to avoid concurrent modifications from upstream
        final ArrayList<String> profileTokensSafeCopy = new ArrayList<>(profileAPITokens);
        final HashMap<String, String> keysSafeCopy = new HashMap<>(keys);

        // If current thread is associated with a looper,
        // then use that for the callback invocation, use main loop otherwise.
        final Looper callbackLooper = myLooper() != null ? myLooper() : getMainLooper();

        executor.execute(new Runnable() {
            @Override
            public void run() {
                final SegmentsResult result = fetchUserSegmentResultSynchronously(profileTokensSafeCopy, keysSafeCopy);

                if (callbackLooper != null) {
                    new Handler(callbackLooper).post(new Runnable() {
                        @Override
                        public void run() {
                            result.invoke(callback);
                        }
                    });
                } else {
                    // In any case where even mainLooper is null (using on an non-Android runtime?),
                    // just do the callback on this thread.
                    result.invoke(callback);
                }
            }
        });
    }

    // Visible for testing
    SegmentsResult fetchUserSegmentResultSynchronously(final List<String> profileTokens, final Map<String, String> keys) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) apiURI
                    .resolve(makeQueryString(profileTokens, keys))
                    .toURL().openConnection();
            connection.setRequestMethod("GET");

            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            int responseCode = connection.getResponseCode();
            try (InputStream is = connection.getInputStream()) {
                return SegmentsResult.create(responseCode, convertStreamToString(is));
            }
        } catch (IOException e) {
            return SegmentsResult.create(e);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String makeQueryString(List<String> profileTokens, Map<String, String> keys) {
        return makeQueryString(makeParameters(profileTokens, keys));
    }

    private static String makeQueryString(Map<String, String> parameters) {
        List<String> urlEncodedEntries = new ArrayList<>();
        try {
            for (Map.Entry<String, String> param : parameters.entrySet()) {
                urlEncodedEntries.add(
                        URLEncoder.encode(param.getKey(), "UTF-8")
                                + "=" + URLEncoder.encode(param.getValue(), "UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            // Should not happen, unless we're being on an archaic platform
            throw new RuntimeException(e);
        }
        return "?" + join("&", urlEncodedEntries);
    }

    private static Map<String, String> makeParameters(List<String> profileTokens, Map<String, String> keys) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("version", "2");
        parameters.put("token", join(",", profileTokens));
        for (Map.Entry<String, String> entry : keys.entrySet()) {
            parameters.put("key." + entry.getKey(), entry.getValue());
        }
        return parameters;
    }

}
