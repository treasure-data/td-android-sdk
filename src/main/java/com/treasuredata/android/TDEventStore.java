package com.treasuredata.android;

import io.keen.client.java.FileEventStore;

import java.io.File;
import java.io.IOException;

class TDEventStore extends FileEventStore {
    public TDEventStore(File root) throws IOException {
        super(root);
    }
}
