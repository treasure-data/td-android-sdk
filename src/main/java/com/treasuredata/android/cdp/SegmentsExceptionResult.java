package com.treasuredata.android.cdp;

public class SegmentsExceptionResult extends SegmentsResult {

    private final Exception exception;

    public SegmentsExceptionResult(Exception e) {
        exception = e;
    }

    @Override
    void invoke(FetchUserSegmentsCallback callback) {
        callback.onError(exception);
    }
}
