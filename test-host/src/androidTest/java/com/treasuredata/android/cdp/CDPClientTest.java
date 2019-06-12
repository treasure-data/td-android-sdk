package com.treasuredata.android.cdp;

import android.os.Handler;
import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static android.os.Looper.getMainLooper;
import static android.os.Looper.myLooper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

@RunWith(AndroidJUnit4.class)
public class CDPClientTest {

    @Test
    public void call_fetchUserSegments_from_a_thread_WITHOUT_a_looper_associated_should_be_called_back_into_main_loop() throws Exception {
        assertNull("VERIFICATION: This thread should not be associated with any looper.", myLooper());
        final CountDownLatch latch = new CountDownLatch(2);

        final CDPClient cdpClient = Mockito.spy(new CDPClient("https://cdp.in.treasuredata.com"));
        doReturn(SegmentsResult.create(200, "[]"))
                .when(cdpClient)
                .fetchUserSegmentResultSynchronously(Mockito.<List<String>>any(), Mockito.<Map<String, String>>any());

        // With onSuccess
        cdpClient.fetchUserSegments(
                Collections.<String>emptyList(),
                Collections.<String, String>emptyMap(),
                new FetchUserSegmentsCallback() {
                    @Override
                    public void onSuccess(List<Profile> profiles) {
                        latch.countDown();
                        assertEquals("Expected should be dispatch to main looper", getMainLooper(), myLooper());
                    }
                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        fail();
                    }
                });

        final CDPClient failureCdpClient = Mockito.spy(new CDPClient("https://cdp.in.treasuredata.com"));
        doReturn(SegmentsResult.create(400, "an_error"))
                .when(failureCdpClient)
                .fetchUserSegmentResultSynchronously(Mockito.<List<String>>any(), Mockito.<Map<String, String>>any());

        // Same for onError
        failureCdpClient.fetchUserSegments(
                Collections.<String>emptyList(),
                Collections.<String, String>emptyMap(),
                new FetchUserSegmentsCallback() {
                    @Override
                    public void onSuccess(List<Profile> profiles) {
                        latch.countDown();
                        fail();
                    }
                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        assertEquals("Expected should be dispatch to main looper", getMainLooper(), myLooper());
                    }
                });

        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void call_fetchUserSegments_from_a_thread_WITH_a_looper_associated_should_be_called_back_in_that_looper() throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);

        final CDPClient cdpClient = Mockito.spy(new CDPClient("https://cdp.in.treasuredata.com"));
        doReturn(SegmentsResult.create(200, "[]"))
                .when(cdpClient)
                .fetchUserSegmentResultSynchronously(Mockito.<List<String>>any(), Mockito.<Map<String, String>>any());

        // With onSuccess
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                final Looper callerLooper = myLooper();
                cdpClient.fetchUserSegments(
                        Collections.<String>emptyList(),
                        Collections.<String, String>emptyMap(),
                        new FetchUserSegmentsCallback() {
                            @Override
                            public void onSuccess(List<Profile> profiles) {
                                latch.countDown();
                                assertEquals("Expected this should be dispatch to the caller looper", myLooper(), callerLooper);
                                if (callerLooper != null) callerLooper.quit();
                            }
                            @Override
                            public void onError(Exception e) {
                                latch.countDown();
                                fail();
                                if (callerLooper != null) callerLooper.quit();
                            }
                        });
                Looper.loop();
            }
        }).start();

        final CDPClient failureCdpClient = Mockito.spy(new CDPClient("https://cdp.in.treasuredata.com"));
        doReturn(SegmentsResult.create(400, "an_error"))
                .when(failureCdpClient)
                .fetchUserSegmentResultSynchronously(Mockito.<List<String>>any(), Mockito.<Map<String, String>>any());

        // Same for onError
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                final Looper callerLooper = myLooper();
                failureCdpClient.fetchUserSegments(
                        Collections.<String>emptyList(),
                        Collections.<String, String>emptyMap(),
                        new FetchUserSegmentsCallback() {
                            @Override
                            public void onSuccess(List<Profile> profiles) {
                                latch.countDown();
                                if (callerLooper != null) callerLooper.quit();
                                fail();
                            }
                            @Override
                            public void onError(Exception e) {
                                latch.countDown();
                                assertEquals("Expected this should be dispatch to the caller looper", myLooper(), callerLooper);
                                if (callerLooper != null) callerLooper.quit();
                            }
                        });
                Looper.loop();
            }
        }).start();

        latch.await(1, TimeUnit.SECONDS);
    }

    @Test
    public void call_fetchUserSegments_from_the_main_loop_apparently_should_be_called_back_in_the_main_loop() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        final CDPClient cdpClient = Mockito.spy(new CDPClient("https://cdp.in.treasuredata.com"));
        doReturn(SegmentsResult.create(200, "[]"))
                .when(cdpClient)
                .fetchUserSegmentResultSynchronously(Mockito.<List<String>>any(), Mockito.<Map<String, String>>any());

        // With onSuccess
        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                cdpClient.fetchUserSegments(
                        Collections.<String>emptyList(),
                        Collections.<String, String>emptyMap(),
                        new FetchUserSegmentsCallback() {
                            @Override
                            public void onSuccess(List<Profile> profiles) {
                                latch.countDown();
                                assertEquals("Expected this should be dispatch to the main loop", getMainLooper(), myLooper());
                            }
                            @Override
                            public void onError(Exception e) {
                                latch.countDown();
                                fail();
                            }
                        });
            }
        });

        final CDPClient failureCdpClient = Mockito.spy(new CDPClient("https://cdp.in.treasuredata.com"));
        doReturn(SegmentsResult.create(400, "an_error"))
                .when(failureCdpClient)
                .fetchUserSegmentResultSynchronously(Mockito.<List<String>>any(), Mockito.<Map<String, String>>any());

        new Handler(getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                failureCdpClient.fetchUserSegments(
                        Collections.<String>emptyList(),
                        Collections.<String, String>emptyMap(),
                        new FetchUserSegmentsCallback() {
                            @Override
                            public void onSuccess(List<Profile> profiles) {
                                latch.countDown();
                                fail();
                            }
                            @Override
                            public void onError(Exception e) {
                                latch.countDown();
                                assertEquals("Expected this should be dispatch to the main loop", getMainLooper(), myLooper());
                            }
                        });
            }
        });

        latch.await(1, TimeUnit.SECONDS);
    }

}
