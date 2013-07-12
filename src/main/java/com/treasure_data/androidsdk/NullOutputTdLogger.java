package com.treasure_data.androidsdk;

public class NullOutputTdLogger extends AbstractTdLogger {

    @Override
    boolean outputData(String database, String table, byte[] data) {
        return true;
    }

    @Override
    void cleanup() {
    }
}
