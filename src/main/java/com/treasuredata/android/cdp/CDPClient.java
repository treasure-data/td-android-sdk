package com.treasuredata.android.cdp;

import java.util.List;
import java.util.Map;

/**
 * A single-purpose client for now,
 * Use to lookup for CDP's Profiles
 */
public interface CDPClient {

    /**
     * @param profilesTokens list of Profile API Token that are defined on TreasureData
     * @param keys           lookup keyColumn values
     * @param callback       to receive the looked-up result
     */
    void fetchUserSegments(final List<String> profilesTokens,
                           final Map<String, String> keys,
                           final FetchUserSegmentsCallback callback);

}
