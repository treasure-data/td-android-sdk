package com.treasuredata.android.cdp;

import org.json.JSONObject;
import org.junit.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ProfileImplTest {

    @Test
    public void full_profile() throws Exception {
        ProfileImpl profile = ProfileImpl.fromJSONObject(new JSONObject(
                "{" +
                        "  \"values\": [\"123\"]," +
                        "  \"attributes\": {" +
                        "    \"a\": 1," +
                        "    \"b\": \"x\"," +
                        "    \"c\": [2, 3]," +
                        "    \"d\": {\"e\": 2}" +
                        "  }," +
                        "  \"audienceId\": \"234\"," +
                        "  \"key\": {\"a_key_column\": \"key_value\"}" +
                        " }"
        ));

        assertEquals(singletonList("123"), profile.getSegments());

        assertEquals(1, profile.getAttributes().get("a"));
        assertEquals("x", profile.getAttributes().get("b"));
        assertEquals(asList(2, 3), profile.getAttributes().get("c"));
        assertEquals(singletonMap("e", 2), profile.getAttributes().get("d"));

        assertEquals("234", profile.getAudienceId());
        assertEquals("a_key_column", profile.getKey().getName());
        assertEquals("key_value", profile.getKey().getValue());
    }

    @Test
    public void profile_with_missing_props() throws Exception {
        ProfileImpl profile = ProfileImpl.fromJSONObject(new JSONObject(
                "{" +
                        "  \"values\": [\"123\"]," +
                        "  \"key\": {\"a_key_column\": \"key_value\"}" +
                        " }"
        ));

        assertEquals(singletonList("123"), profile.getSegments());
        assertNull(profile.getAttributes());
        assertNull(profile.getAudienceId());
        assertEquals("a_key_column", profile.getKey().getName());
        assertEquals("key_value", profile.getKey().getValue());
    }
}