package com.treasuredata.android.cdp;

import com.fasterxml.jackson.jr.ob.JSON;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ProfileImpl implements Profile {
    private List<String> segments;
    private Map<String, Object> attributes;
    private Key key;
    private String audienceId;

    private ProfileImpl(List<String> segments, Map<String, Object> attributes, Key key, String audienceId) {
        this.segments = segments;
        this.attributes = attributes;
        this.key = key;
        this.audienceId = audienceId;
    }

    @Override
    public List<String> getSegments() {
        return segments;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Key getKey() {
        return key;
    }

    @Override
    public String getAudienceId() {
        return audienceId;
    }

    /**
     * Try to deserialize the provided json into a Profile object,
     * absent properties will be leaved as null.
     */
    static ProfileImpl fromJSONObject(JSONObject profileJson) throws JSONException {
        List<String> segments = null;
        if (profileJson.has("values")) {
            segments = new ArrayList<>();
            JSONArray segmentsJson = profileJson.getJSONArray("values");
            for (int i = 0; i < segmentsJson.length(); i++) {
                segments.add(segmentsJson.getString(i));
            }
        }

        Map<String, Object> attributes = null;
        if (profileJson.has("attributes")) {
            JSONObject attributesJson = profileJson.getJSONObject("attributes");
            try {
                // Unfortunately, org.json doesn't allow deserialize to objects
                attributes = JSON.std.mapFrom(attributesJson.toString());
            } catch (IOException e) {
                // Wrapping JSONException onto a different original cause is not support until Android API level 27
                throw new JSONException(e.getMessage());
            }
        }

        Key key = null;
        if (profileJson.has("key")) {
            JSONObject keyJson = profileJson.getJSONObject("key");

            // Expect keyJson to be a single property JSON object
            Iterator keys = keyJson.keys();
            if (keys.hasNext()) {
                Object keyProp = keys.next();
                if (!(keyProp instanceof String)) {
                    throw new JSONException("Expect `key` to be a map of <string : object>");
                }
                String keyName = (String) keyProp;
                key = new KeyImpl(keyName, keyJson.get(keyName));
            }
        }

        String audienceId = profileJson.optString("audienceId", null);

        return new ProfileImpl(segments, attributes, key, audienceId);
    }

    private static class KeyImpl implements Key {
        private String name;
        private Object value;

        KeyImpl(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Object getValue() {
            return value;
        }
    }

}
