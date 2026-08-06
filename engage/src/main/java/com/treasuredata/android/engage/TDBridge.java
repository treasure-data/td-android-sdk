package com.treasuredata.android.engage;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The single {@code @JavascriptInterface} object exposed to the campaign WebView as
 * {@code __TDBridgeNative}. It decodes messages posted from JS, enforces the fixed-method
 * allowlist, and routes to the container / delegate / core SDK ingest.
 *
 * <p>Contract (v2):
 * <ul>
 *   <li>{@code close()} → container {@code onClose}</li>
 *   <li>{@code openUrl(url)} → {@link TDBridgeDelegate#handleTDBridgeOpenURL}</li>
 *   <li>{@code track(event, values)} → core {@code TreasureData} ingest</li>
 *   <li>{@code invoke(name, params)} → {@link TDBridgeDelegate#handleTDBridgeInvoke}</li>
 * </ul>
 *
 * <p>Security invariants:
 * <ol>
 *   <li>Allowlist = {@code {close, openUrl, track, invoke}} only. {@code invoke}'s inner
 *       {@code name} is deliberately unbounded — the app dispatches it.</li>
 *   <li>Typed decode of {@code url}/{@code event}/{@code values}/{@code name}/{@code params}
 *       via {@code org.json}; malformed → drop.</li>
 *   <li>No keys, no eval, no direct HTTP from the page; {@code track} goes through the SDK.</li>
 *   <li>JSON-safe injection for callback resolution.</li>
 * </ol>
 *
 * <p>Threading: {@code @JavascriptInterface} methods run on a binder thread, so every
 * WebView / delegate interaction is posted to the UI thread.
 */
final class TDBridge {
    private static final String TAG = "TDBridge";

    static final String METHOD_CLOSE = "close";
    static final String METHOD_OPEN_URL = "openUrl";
    static final String METHOD_TRACK = "track";
    static final String METHOD_INVOKE = "invoke";

    interface Delegate {
        /** Dismiss the container and fire {@code onClose}. Invoked on the UI thread. */
        void onClose();

        /** Route {@code openUrl}. Invoked on the UI thread. */
        void onOpenUrl(String url);

        /** Route {@code track} into core SDK ingest. Invoked on the UI thread. */
        void onTrack(String event, Map<String, Object> values);

        /** Route {@code invoke} to the app delegate. Invoked on the UI thread. */
        void onInvoke(String name, Map<String, Object> params);
    }

    private final WebView webView;
    private final Delegate delegate;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    TDBridge(WebView webView, Delegate delegate) {
        this.webView = webView;
        this.delegate = delegate;
    }

    /**
     * Single entry point from JS. Receives {@code JSON.stringify({method, args, callbackId})}.
     * Runs on a binder thread.
     */
    @JavascriptInterface
    public void __post(String message) {
        final String method;
        final int callbackId;
        final JSONObject args;
        try {
            JSONObject obj = new JSONObject(message);
            method = obj.optString("method", null);
            callbackId = obj.optInt("callbackId", 0);
            args = obj.optJSONObject("args");
        } catch (JSONException e) {
            Log.w(TAG, "Rejected malformed bridge message");
            return;
        }
        if (method == null) {
            Log.w(TAG, "Rejected bridge message with no method");
            return;
        }
        dispatch(method, args, callbackId);
    }

    private void dispatch(String method, JSONObject args, int callbackId) {
        switch (method) {
            case METHOD_CLOSE:
                uiHandler.post(delegate::onClose);
                return;
            case METHOD_OPEN_URL: {
                String url = args == null ? null : args.optString("url", null);
                if (url == null || url.isEmpty()) {
                    Log.w(TAG, "openUrl: missing/invalid url");
                    return; // typed-decode boundary: drop
                }
                final String u = url;
                uiHandler.post(() -> delegate.onOpenUrl(u));
                return;
            }
            case METHOD_TRACK: {
                if (args == null) {
                    Log.w(TAG, "track: missing args");
                    return;
                }
                String event = args.optString("event", null);
                if (event == null || event.isEmpty()) {
                    Log.w(TAG, "track: missing event");
                    return;
                }
                Map<String, Object> values = toMap(args.optJSONObject("values"));
                final String e = event;
                uiHandler.post(() -> delegate.onTrack(e, values));
                return;
            }
            case METHOD_INVOKE: {
                if (args == null) {
                    Log.w(TAG, "invoke: missing args");
                    return;
                }
                String name = args.optString("name", null);
                if (name == null || name.isEmpty()) {
                    Log.w(TAG, "invoke: missing name");
                    return;
                }
                // Inner name is NOT allowlisted — the app dispatches it.
                Map<String, Object> params = toMap(args.optJSONObject("params"));
                final String n = name;
                uiHandler.post(() -> delegate.onInvoke(n, params));
                return;
            }
            default:
                // Allowlist trust boundary: never network, never eval.
                Log.w(TAG, "Ignoring unknown bridge method: " + method);
        }
    }

    /** Shallow typed decode of a JSON object into a Map; null-safe. */
    private static Map<String, Object> toMap(JSONObject obj) {
        Map<String, Object> map = new HashMap<>();
        if (obj == null) {
            return map;
        }
        for (Iterator<String> it = obj.keys(); it.hasNext(); ) {
            String key = it.next();
            map.put(key, obj.opt(key));
        }
        return map;
    }
}
