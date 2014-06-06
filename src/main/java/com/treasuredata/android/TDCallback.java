package com.treasuredata.android;

public interface TDCallback {
    void onSuccess();

    void onError(Exception e);
}
