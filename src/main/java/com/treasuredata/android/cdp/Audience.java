package com.treasuredata.android.cdp;

import java.util.List;
import java.util.Map;

public interface Audience {

    /**
     * Segment IDs where this audience belongs to
     */
    List<String> getSegments();

    /**
     * This audience's attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Key columns / values of this audience
     * FIXME: currently assuming key is a single pair
     */
    Key getKey();

    /**
     * ID of this audience, for example: "326"
     */
    String getAudienceId();

    interface Key {
        String getName();
        Object getValue();
    }

}
