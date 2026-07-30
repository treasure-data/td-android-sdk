package com.treasuredata.android.engage;

import androidx.annotation.NonNull;

import java.util.Map;

/**
 * The single delegate the host app implements to receive campaign WebView callbacks that
 * require business logic. The SDK never inspects the {@code invoke} name — the app dispatches
 * it and performs authorization; unknown names are the app's responsibility, never an SDK error.
 */
public interface TDBridgeDelegate {
    /**
     * Entry point for every {@code TDBridge.invoke(name, params)} call from the page.
     *
     * @param name   the invocation name (unbounded; the SDK does not allowlist it)
     * @param params structured params decoded from the page ({@code Map}, never a JSON string)
     */
    void handleTDBridgeInvoke(@NonNull String name, @NonNull Map<String, Object> params);

    /**
     * Deep-link / native navigation from {@code TDBridge.openUrl(url)}. The decision to
     * navigate is the app's.
     */
    void handleTDBridgeOpenURL(@NonNull String url);
}
