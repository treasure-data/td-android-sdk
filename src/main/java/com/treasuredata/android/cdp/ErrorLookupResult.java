package com.treasuredata.android.cdp;

class ErrorLookupResult extends LookupResult {

    private final Exception exception;

    ErrorLookupResult(Exception e) {
        exception = e;
    }

    @Override
    void invoke(FetchUserSegmentsCallback callback) {
        callback.onError(exception);
    }
}
