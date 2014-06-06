package com.treasure_data.androidsdk.apiclient;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.komamitsu.android.util.Log;

import com.treasure_data.androidsdk.apiclient.DefaultApiClient.ApiError;

public class TdTableImporter {
    private static final String TAG = TdTableImporter.class.getSimpleName();
    private final ApiClient apiClient;

    public TdTableImporter(ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    private void createDatabase(DbTableDescr descr) throws IOException, ApiError {
        final String database = descr.getDatabaseName();
        Log.i(TAG, "creating new database=" + database);
        apiClient.createDatabase(database);
    }

    private void createTable(DbTableDescr descr) throws IOException, ApiError {
        final String database = descr.getDatabaseName();
        final String table = descr.getTableName();

        if(descr.getTableType() == DbItemTableDescr.TABLE_TYPE_ITEM) {
            DbItemTableDescr itemDescr = (DbItemTableDescr) descr;
            String pkName = itemDescr.getPrimaryKeyName();
            String pkType = itemDescr.getPrimaryKeyType();
            Log.i(TAG, "creating new item table=" + table +
                    ", primary_key=" + pkName + ":" + pkType);
            apiClient.createItemTable(database, table, pkName, pkType);
        } else {
            Log.i(TAG, "creating new log table=" + table);
            apiClient.createLogTable(database, table);
        }
    }

    public void output(DbTableDescr descr, byte[] data)
            throws IOException, ApiError {
        String database = descr.getDatabaseName();
        String table = descr.getTableName();

        try {
            apiClient.importTable(database, table, data);
        } catch (FileNotFoundException e1) {
            try {
                createTable(descr);
            } catch (FileNotFoundException e2) {
                createDatabase(descr);
                createTable(descr);
            }
            apiClient.importTable(database, table, data);
        }
    }
}
