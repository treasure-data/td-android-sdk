package com.treasuredata.android.cdp;

import java.util.List;
import java.util.Map;

/**
 * Represent a profile in segments looked-up's result,
 */
public interface Profile {
    /**
     * Segment IDs where this profile belongs
     */
    List<String> getSegments();

    /**
     * This profile's attributes
     */
    Map<String, Object> getAttributes();

    /**
     * Key columns : values of segments
     */
    Key getKey();

    /**
     * ID of the Master Segment
     */
    String getAudienceId();

    interface Key {

        /**
         * Name of key column
         */
        String getName();

        /**
         * Key value of the looked up profile
         */
        Object getValue();

    }

}
