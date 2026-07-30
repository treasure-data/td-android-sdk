package com.treasuredata.android.engage;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.treasuredata.android.TreasureData;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A WebView container that hosts the {@code TDBridge} channel for a Treasure Data landing
 * page. It injects {@code window.TDContext} and {@code TDBridge.js} before page scripts run,
 * routes {@code close}/{@code openUrl}/{@code track}/{@code invoke} to the container, the
 * {@link TDBridgeDelegate}, and core SDK ingest.
 *
 * <p>This owns the bridge only — not decisioning, personalization, or auto-events. The page
 * context is supplied by the caller via {@link #setContext}.
 */
public final class LandingPageView extends FrameLayout {
    private static final String TAG = "LandingPageView";
    private static final String NATIVE_INTERFACE_NAME = "__TDBridgeNative";

    private final WebView webView;
    private final TDBridge bridge;
    private final String bridgeJs;

    private Map<String, Object> context = Collections.emptyMap();
    private TDBridgeDelegate delegate;
    private Runnable onClose;
    private String trackDatabase;
    private String trackTable;

    public LandingPageView(@NonNull Context ctx) {
        this(ctx, null);
    }

    public LandingPageView(@NonNull Context ctx, @Nullable AttributeSet attrs) {
        super(ctx, attrs);
        this.webView = new WebView(ctx);
        this.bridgeJs = loadBridgeJs(ctx);
        this.bridge = new TDBridge(webView, new TDBridge.Delegate() {
            @Override
            public void onClose() {
                if (onClose != null) {
                    onClose.run();
                }
            }

            @Override
            public void onOpenUrl(String url) {
                if (delegate != null) {
                    delegate.handleTDBridgeOpenURL(url);
                }
            }

            @Override
            public void onTrack(String event, Map<String, Object> values) {
                trackToCore(event, values);
            }

            @Override
            public void onInvoke(String name, Map<String, Object> params) {
                if (delegate != null) {
                    delegate.handleTDBridgeInvoke(name, params);
                }
            }
        });
        configureWebView();
        addView(webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        // Harden: the bridge never needs file access; deny it to shrink the trust boundary.
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);

        webView.addJavascriptInterface(bridge, NATIVE_INTERFACE_NAME);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                // Earliest safe hook for injection on Android (no true document-start).
                // Push TDContext first, then the bridge, before page scripts run.
                view.evaluateJavascript(contextInjection() + bridgeJs, null);
            }
        });
    }

    /** {@code window.TDContext = <json>} as an injection-safe literal (org.json escaping). */
    private String contextInjection() {
        return "window.TDContext=" + new JSONObject(context).toString() + ";";
    }

    private void trackToCore(String event, Map<String, Object> values) {
        if (trackTable == null) {
            Log.w(TAG, "track dropped: no track table set (call setTrackTable)");
            return;
        }
        Map<String, Object> record = new HashMap<>(values);
        record.put("event", event);
        TreasureData td = TreasureData.sharedInstance();
        if (trackDatabase == null) {
            td.addEvent(trackTable, record);
        } else {
            td.addEvent(trackDatabase, trackTable, record);
        }
    }

    // --- Public API ---

    /** The object injected as {@code window.TDContext}. Set before {@link #loadUrl}/{@link #loadHtml}. */
    public void setContext(@NonNull Map<String, Object> context) {
        this.context = new HashMap<>(context);
    }

    /** Receives {@code invoke} and {@code openUrl} callbacks. */
    public void setDelegate(@Nullable TDBridgeDelegate delegate) {
        this.delegate = delegate;
    }

    /** Invoked (on the UI thread) when the page calls {@code close}. */
    public void setOnClose(@Nullable Runnable onClose) {
        this.onClose = onClose;
    }

    /** Destination for {@code track} events. Without it, {@code track} is dropped. */
    public void setTrackTable(@Nullable String database, @NonNull String table) {
        this.trackDatabase = database;
        this.trackTable = table;
    }

    public void loadUrl(@NonNull String url) {
        webView.loadUrl(url);
    }

    public void loadHtml(@NonNull String html, @Nullable String baseUrl) {
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "utf-8", null);
    }

    private static String loadBridgeJs(Context context) {
        try (InputStream in = context.getAssets().open("TDBridge.js");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            Log.e(TAG, "Failed to load TDBridge.js asset", e);
            return "";
        }
    }
}
