package com.treasure_data.td_logger.android;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.junit.Test;
import org.msgpack.type.ArrayValue;

import com.treasure_data.td_logger.android.FluentdMock.Result;

public class TdAndroidLoggerTest {
    private static final String API_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm";

    /*
    @Test
    public void testX() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        assertTrue(logger.flush("testdb", "testtbl"));
        logger.increment("testdb", "testtbl", "increkey1", 100);
        logger.close();
    }
    */

    @Test
    public void test() throws IOException, InterruptedException {
        FluentdMock td = new FluentdMock();
        Result result = td.run(1);

        int port = td.getPort();
        System.out.println("port ===> "+ port);
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY, "localhost", port);
        logger.write("testdb", "testlabel", "testkey0", "testval0");
        logger.flushAll();

        result.waitResult();

        List<ArrayValue> resultValues = result.getResultValues();
        assertEquals(1, resultValues.size());

        logger.close();
    }
}
