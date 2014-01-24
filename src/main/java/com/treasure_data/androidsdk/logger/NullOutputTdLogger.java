package com.treasure_data.androidsdk.logger;

import com.treasure_data.androidsdk.apiclient.DbTableDescr;


public class NullOutputTdLogger extends AbstractTdLogger {

    @Override
    boolean outputData(DbTableDescr descr, byte[] data) {
        return true;
    }

    @Override
    void cleanup() {
    }
}
