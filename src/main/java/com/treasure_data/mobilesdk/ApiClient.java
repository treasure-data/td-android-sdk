package com.treasure_data.mobilesdk;

import java.io.IOException;

import com.treasure_data.mobilesdk.DefaultApiClient.ApiError;

public interface ApiClient {

    void init(String apikey, String host, int port);

    String createTable(String database, String table) throws IOException, ApiError;

    String importTable(String database, String table, byte[] data) throws IOException, ApiError;
}