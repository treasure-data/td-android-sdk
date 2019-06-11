package com.treasuredata.android.cdp;

import java.util.List;
import java.util.Map;

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
    Map<String, Object> getKey();

    /**
     * ID of the Master Segment
     */
    String getAudienceId();

}
