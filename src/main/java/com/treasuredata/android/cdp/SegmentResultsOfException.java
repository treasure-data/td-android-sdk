package com.treasuredata.android.cdp;

public class SegmentResultsOfException extends SegmentsResult {

    private final Exception exception;

    public SegmentResultsOfException(Exception e) {
        exception = e;
    }

    @Override
    void invoke(FetchUserSegmentsCallback callback) {
        callback.onError(exception);
    }
}
