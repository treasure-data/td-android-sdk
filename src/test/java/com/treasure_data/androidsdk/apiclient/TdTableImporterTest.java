package com.treasure_data.androidsdk.apiclient;

import static org.junit.Assert.assertEquals;

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
        int callCount = 0;
        List<Integer> importTableErrorIndexes = new ArrayList<Integer>();
        List<Integer> createTableErrorIndexes = new ArrayList<Integer>();
        List<Integer> createDatabaseErrorIndexes = new ArrayList<Integer>();

        @Override
        public void init(String apikey, String host, int port) {
        }

        @Override
        public String createDatabase(String database) throws IOException, ApiError {
            if (createDatabaseErrorIndexes.contains(callCount++)) {
                throw new FileNotFoundException();
            }
            events.add("cd:" + database);
            return "OK";
        }

        @Override
        public String createLogTable(String database, String table) throws IOException, ApiError {
            if (createTableErrorIndexes.contains(callCount++)) {
                throw new FileNotFoundException();
            }
            events.add("clt:" + database + "#" + table);
            return "OK";
        }

        @Override
        public String createTable(String database, String table) throws IOException, ApiError {
            return createLogTable(database, table);
        }

        @Override
        public String createItemTable(String database, String table,
                                      String pkName, String pkType)
                throws IOException, ApiError {
            if (createTableErrorIndexes.contains(callCount++)) {
                throw new FileNotFoundException();
            }
            events.add("cit:" + database + "#" + table);
            return "OK";
        }

        @Override
        public String importTable(String database, String table, byte[] data) throws IOException, ApiError {
            if (importTableErrorIndexes.contains(callCount++)) {
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
    public void testImportLogTableWithoutException()
            throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output(new DbLogTableDescr("testdb", "testtable"),
                "testdata".getBytes());
        assertEquals(1, mockApiClient.events.size());
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(0));
    }

    @Test
    public void testImportItemTableWithoutException()
            throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output(
                new DbItemTableDescr("testdb", "testtable", "pk_name", "int"),
                "testdata".getBytes());
        assertEquals(1, mockApiClient.events.size());
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(0));
    }

    @Test
    public void testImportLogTableWithTableNotFound()
            throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;
        mockApiClient.importTableErrorIndexes.add(0);

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output(new DbLogTableDescr("testdb", "testtable"),
                "testdata".getBytes());
        assertEquals(2, mockApiClient.events.size());
        assertEquals("clt:testdb#testtable", mockApiClient.events.get(0));
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(1));
    }

    @Test
    public void testImportLogTableWithDatabaseNotFound()
            throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;
        mockApiClient.importTableErrorIndexes.add(0);
        mockApiClient.createTableErrorIndexes.add(1);

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output(new DbLogTableDescr("testdb", "testtable"),
                "testdata".getBytes());
        assertEquals(3, mockApiClient.events.size());
        assertEquals("cd:testdb", mockApiClient.events.get(0));
        assertEquals("clt:testdb#testtable", mockApiClient.events.get(1));
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(2));
    }

    @Test
    public void testImportItemTableWithTableNotFound()
            throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;
        mockApiClient.importTableErrorIndexes.add(0);

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output(
                new DbItemTableDescr("testdb", "testtable", "pk_name", "int"),
                "testdata".getBytes());
        assertEquals(2, mockApiClient.events.size());
        assertEquals("cit:testdb#testtable", mockApiClient.events.get(0));
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(1));
    }

    @Test
    public void testImportItemTableWithDatabaseNotFound()
            throws IOException, ApiError {
        ApiClient apiClient = new MockApiClient();
        MockApiClient mockApiClient = (MockApiClient) apiClient;
        mockApiClient.importTableErrorIndexes.add(0);
        mockApiClient.createTableErrorIndexes.add(1);

        TdTableImporter importer = new TdTableImporter(apiClient);
        importer.output(
                new DbItemTableDescr("testdb", "testtable", "pk_name", "int"),
                "testdata".getBytes());
        assertEquals(3, mockApiClient.events.size());
        assertEquals("cd:testdb", mockApiClient.events.get(0));
        assertEquals("cit:testdb#testtable", mockApiClient.events.get(1));
        assertEquals("it:testdb#testtable#testdata", mockApiClient.events.get(2));
    }
}
