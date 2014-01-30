package com.treasure_data.androidsdk.apiclient;

import java.io.IOException;

import com.treasure_data.androidsdk.apiclient.DefaultApiClient.ApiError;

public interface ApiClient {

    void init(String apikey, String host, int port);

    String createDatabase(String database) throws IOException, ApiError;
    String createTable(String database, String table) throws IOException, ApiError;
    String createLogTable(String database, String table) throws IOException, ApiError;
    String createItemTable(String database, String table, String pkName, String pkType) throws IOException, ApiError;

    String importTable(String database, String table, byte[] data) throws IOException, ApiError;
}