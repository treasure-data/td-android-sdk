package com.treasuredata.android.cdp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ProfileImpl implements Profile {
    private List<String> segments;
    private Map<String, Object> attributes;
    private Map<String, Object> key;
    private String audienceId;

    private ProfileImpl(List<String> segments, Map<String, Object> attributes, Map<String, Object> key, String audienceId) {
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
    public Map<String, Object> getKey() {
        return key;
    }

    @Override
    public String getAudienceId() {
        return audienceId;
    }

    static ProfileImpl fromJSONObject(JSONObject audienceJSON) throws JSONException {

        // On the API spec, it states the property name here is "segments",
        // but the actual name is "values" !?!
        List<String> segments = new ArrayList<>();
        JSONArray segmentsJson = audienceJSON.getJSONArray("values");
        for (int i = 0; i < segmentsJson.length(); i++) {
            segments.add(segmentsJson.getString(i));
        }

        Map<String, Object> attributes = new HashMap<>();
        JSONObject attributesJson = audienceJSON.getJSONObject("attributes");

        Iterator attributeNames = attributesJson.keys();
        while (attributeNames.hasNext()) {
            // TODO: recursively resolve collection-type inner json node
            Object attributeName = attributeNames.next();
            if (!(attributeName instanceof String)) {
                throw new JSONException("Expect all attribute names are string");
            }
            attributes.put((String) attributeName, attributesJson.get((String) attributeName));
        }

        JSONObject keyJson = audienceJSON.getJSONObject("key");

        Map<String, Object> key = new HashMap<>();
        Iterator keys = keyJson.keys();
        while (keys.hasNext()) {
            Object keyName = keys.next();
            if (!(keyName instanceof String)) {
                throw new JSONException("Expect `key`'s name is a string!");
            }
            key.put((String) keyName, keyJson.get((String) keyName));
        }

        String audienceId = audienceJSON.getString("audienceId");

        return new ProfileImpl(segments, attributes, key, audienceId);
    }

}
