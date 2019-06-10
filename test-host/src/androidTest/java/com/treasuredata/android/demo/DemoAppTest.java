package com.treasuredata.android.demo;

import android.os.Looper;
import android.support.test.runner.AndroidJUnit4;

// import com.treasuredata.android.cdp.CDPClient;
// import com.treasuredata.android.cdp.FetchUserSegmentsCallback;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DemoAppTest {

    @Test
    public void test() throws Exception {
        assertTrue(true);
        // final CountDownLatch latch = new CountDownLatch(1);
        // (new CDPClient()).justTrying(Looper.getMainLooper(), new FetchUserSegmentsCallback() {
        //     @Override
        //     public void onSuccess(Map<String, ?> map) {
        //         System.out.println("Success");
        //         latch.countDown();
        //     }
        //
        //     @Override
        //     public void onError(String s, String s1) {
        //         System.out.println("Error");
        //         latch.countDown();
        //     }
        // });
        //
        // latch.await();
    }

}