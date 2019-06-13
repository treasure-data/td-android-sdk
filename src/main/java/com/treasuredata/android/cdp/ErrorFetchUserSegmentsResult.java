package com.treasuredata.android.cdp;

class ErrorFetchUserSegmentsResult extends FetchUserSegmentsResult {

    private final Exception exception;

    ErrorFetchUserSegmentsResult(Exception e) {
        exception = e;
    }

    @Override
    void invoke(FetchUserSegmentsCallback callback) {
        callback.onError(exception);
    }
}
