package com.treasuredata.android.cdp;

import java.util.List;
import java.util.Map;

/**
 * Represent a profile in segments looked-up's result,
 */
public interface Profile {
    /**
     * @return Segment IDs where this profile belongs
     */
    List<String> getSegments();

    /**
     * @return This profile's attributes
     */
    Map<String, Object> getAttributes();

    /**
     * @return Key columns : values of segments
     */
    Key getKey();

    /**
     * @return ID of the Master Segment
     */
    String getAudienceId();

    interface Key {

        /**
         * @return Name of key column
         */
        String getName();

        /**
         * @return Key value of the looked up profile
         */
        Object getValue();

    }

}
