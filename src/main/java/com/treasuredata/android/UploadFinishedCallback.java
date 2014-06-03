package com.treasuredata.android;

public interface UploadFinishedCallback extends io.keen.client.android.UploadFinishedCallback {
    @Override
    void callback();
}
