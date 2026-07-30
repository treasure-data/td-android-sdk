package com.treasuredata.android.engage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import android.webkit.WebView;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import java.util.Map;

/**
 * Money-path check for the native side of the v2 bridge contract: fixed-method allowlist,
 * typed decode, delegate routing, and FIFO ordering. Drives {@code __post(...)} exactly as
 * the injected {@code TDBridge.js} would, and asserts what native routes to the delegate.
 *
 * <p>The JS↔native transport (a real WebView executing TDBridge.js) and {@code track}'s core
 * ingest are exercised by the example smoke test — Robolectric's WebView does not run JS.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class TDBridgeTest {

    private WebView webView;
    private TDBridge.Delegate delegate;
    private TDBridge bridge;

    @Before
    public void setUp() {
        webView = spy(new WebView(ApplicationProvider.getApplicationContext()));
        doNothing().when(webView).evaluateJavascript(any(), any());
        delegate = mock(TDBridge.Delegate.class);
        bridge = new TDBridge(webView, delegate);
    }

    private void drainMainLooper() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
    }

    @Test
    public void close_firesOnClose() {
        bridge.__post("{\"method\":\"close\",\"args\":null,\"callbackId\":0}");
        drainMainLooper();
        verify(delegate).onClose();
    }

    @Test
    public void openUrl_reachesDelegate() {
        bridge.__post("{\"method\":\"openUrl\",\"args\":{\"url\":\"https://td.example/x\"},\"callbackId\":0}");
        drainMainLooper();
        verify(delegate).onOpenUrl("https://td.example/x");
    }

    @Test
    public void openUrl_missingUrl_isDropped() {
        bridge.__post("{\"method\":\"openUrl\",\"args\":{},\"callbackId\":0}");
        drainMainLooper();
        verify(delegate, never()).onOpenUrl(any());
    }

    @Test
    public void track_reachesIngestWithEventAndValues() {
        bridge.__post("{\"method\":\"track\",\"args\":{\"event\":\"click\","
                + "\"values\":{\"content_id\":\"42\"}},\"callbackId\":0}");
        drainMainLooper();

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> values =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(delegate).onTrack(eq("click"), values.capture());
        assertEquals("42", values.getValue().get("content_id"));
    }

    @Test
    public void track_missingEvent_isDropped() {
        bridge.__post("{\"method\":\"track\",\"args\":{\"values\":{}},\"callbackId\":0}");
        drainMainLooper();
        verify(delegate, never()).onTrack(any(), any());
    }

    @Test
    public void invoke_reachesDelegateWithNameAndParams() {
        bridge.__post("{\"method\":\"invoke\",\"args\":{\"name\":\"submitRaffleEntries\","
                + "\"params\":{\"entries\":3}},\"callbackId\":0}");
        drainMainLooper();

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<Map<String, Object>> params =
                org.mockito.ArgumentCaptor.forClass(Map.class);
        verify(delegate).onInvoke(eq("submitRaffleEntries"), params.capture());
        assertEquals(3, params.getValue().get("entries"));
    }

    @Test
    public void invoke_unknownName_stillReachesDelegate_noSdkError() {
        // The SDK does NOT allowlist invoke's inner name — the app decides.
        bridge.__post("{\"method\":\"invoke\",\"args\":{\"name\":\"anythingAtAll\","
                + "\"params\":{}},\"callbackId\":0}");
        drainMainLooper();
        verify(delegate).onInvoke(eq("anythingAtAll"), any());
    }

    @Test
    public void unknownMethod_isNoOp() {
        bridge.__post("{\"method\":\"deleteEverything\",\"args\":null,\"callbackId\":0}");
        drainMainLooper();
        verifyNoInteractions(delegate);
    }

    @Test
    public void malformedMessage_isNoOp() {
        bridge.__post("not json");
        drainMainLooper();
        verifyNoInteractions(delegate);
    }

    @Test
    public void invokeBeforeClose_deliveredInOrder() {
        // A preceding invoke must be delivered before close tears down the WebView.
        bridge.__post("{\"method\":\"invoke\",\"args\":{\"name\":\"n\",\"params\":{}},\"callbackId\":0}");
        bridge.__post("{\"method\":\"close\",\"args\":null,\"callbackId\":0}");
        drainMainLooper();

        InOrder order = Mockito.inOrder(delegate);
        order.verify(delegate).onInvoke(eq("n"), any());
        order.verify(delegate).onClose();
    }
}
