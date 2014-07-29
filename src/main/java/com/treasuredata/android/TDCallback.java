package com.treasuredata.android;

public interface TDCallback {
    void onSuccess();

    void onError(String errorCode, Exception e);
}
