package com.treasure_data.td_logger.android;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.treasure_data.td_logger.android.DefaultApiClient.ApiError;

public class TdAndroidLoggerTest {
    private static final String API_KEY = "1234567890qwertyuiopasdfghjklzxcvbnm";

    public static class ApiClientMock implements ApiClient {
        String apikey;
        String host;
        int port;
        List<String[]> createTables = new LinkedList<String[]>();
        List<Object[]> importTables = new LinkedList<Object[]>();

        @Override
        public void init(String apikey, String host, int port) {
            this.apikey = apikey;
            this.host = host;
            this.port = port;
        }

        @Override
        public String createTable(String database, String table) throws IOException, ApiError {
            createTables.add(new String[] {database, table});
            return "OK";
        }

        @Override
        public String importTable(String database, String table, byte[] data) throws IOException, ApiError {
            importTables.add(new Object[] {database, table, data});
            return "OK";
        }
    }

    @Before
    public void setup() {
        TdAndroidLogger.setApiClientClass(ApiClientMock.class);
    }

    @After
    public void teardown() {
        TdAndroidLogger.setApiClientClass(DefaultApiClient.class);
    }

    @Test
    public void testInit() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        assertEquals(API_KEY, apiClient.apikey);
        assertEquals("api.treasure-data.com", apiClient.host);
        assertEquals(80, apiClient.port);   // TODO: 443
    }

    @Test
    public void testWriteOnly() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        assertTrue(logger.write("testdb", "testtbl", "keykey", "valval"));
        assertEquals(0, apiClient.createTables.size());
        assertEquals(0, apiClient.importTables.size());
    }

    @Test
    public void testIncrementOnly() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.increment("testdb", "testtbl", "increkey1", 1);
        logger.increment("testdb", "testtbl", "increkey1", 20);
        logger.increment("testdb", "testtbl", "increkey2", 3);
        logger.increment("testdb", "testtbl", "increkey2", 40);
        assertEquals(0, apiClient.createTables.size());
        assertEquals(0, apiClient.importTables.size());
    }

    @Test
    public void testFinishOnly() {
        TdAndroidLogger logger = new TdAndroidLogger(API_KEY);
        ApiClientMock apiClient = (ApiClientMock) logger.apiClient;
        logger.flushAll();
        assertEquals(0, apiClient.createTables.size());
        assertEquals(0, apiClient.importTables.size());
    }
}