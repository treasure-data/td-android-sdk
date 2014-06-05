package com.treasuredata.android;

import io.keen.client.java.KeenClient;
import io.keen.client.java.KeenJsonHandler;

class TDClientBuilder extends KeenClient.Builder {
    @Override
    protected KeenJsonHandler getDefaultJsonHandler() throws Exception {
        return null;
    }
}
