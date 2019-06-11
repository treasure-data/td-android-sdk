package com.treasuredata.android.cdp;

import java.util.List;

public interface FetchUserSegmentsCallback {
    // TODO: domain object instead of JSON?
    void onSuccess(List<Profile> profileImpls);
    void onError(Exception e);
}
