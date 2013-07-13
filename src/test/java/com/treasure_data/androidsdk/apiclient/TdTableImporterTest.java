package com.treasure_data.androidsdk.apiclient;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.treasure_data.androidsdk.apiclient.DefaultApiClient.ApiError;

public class TdTableImporterTest {
    private class MockApiClient implements ApiClient {
        List<String> events = new LinkedList<String>();
        int importTableCallCount = 0;
        int createTableCallCount = 0;
        List<Integer> importTableErrorIndexes = new ArrayList<Integer>();
        List<Integer> createTableErrorIndexes = new ArrayList<Integer>();

        @Override
        public void init(String apikey, String host, int port) {
        }

        @Override
        public String createTable(String database, String table) throws IOException, ApiError {
            if (createTableErrorIndexes.contains(createTableCallCount++)) {
                throw new FileNotFoundException();
            }
            events.add("ct:" + database + "#" + table);
            return "OK";
        }

        @Override
        public String importTable(String database, String table, byte[] data) throws IOException, ApiError {
            if (importTableErrorIndexes.contains(importTableCallCount++)) {
                throw new FileNotFoundException();
            }
            events.add("it:" + database + "#" + table + "#" + new String(data));
            return "OK";
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testImportTableWithoutException() throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output("testdb", "testtable", "testdata".getBytes());
        assertEquals(1, mockApiClient.events.size());
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(0));
    }

    @Test
    public void testImportTableWithTableNotFound() throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;
        mockApiClient.importTableErrorIndexes.add(0);

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output("testdb", "testtable", "testdata".getBytes());
        assertEquals(2, mockApiClient.events.size());
        assertEquals("ct:testdb#testtable", mockApiClient.events.get(0));
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(1));
    }
}
