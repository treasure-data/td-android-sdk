package com.treasuredata.android;

import android.content.Context;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class GetAdvertisingAsyncTaskTest extends TestCase {
    @Test
    public void getAdvertisingId() {
        final CountDownLatch latch = new CountDownLatch(1);
        Context context = InstrumentationRegistry.getTargetContext();
        final GetAdvertisingIdAsyncTask task =
                new GetAdvertisingIdAsyncTask(new GetAdvertisingIdAsyncTaskCallback() {
                    @Override
                    public void onGetAdvertisingIdAsyncTaskCompleted(String advertisingId) {
                        latch.countDown();
                        assertNotNull(advertisingId);
                    }
                });
        task.execute(context);
    }
}
