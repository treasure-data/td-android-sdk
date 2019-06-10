package com.treasuredata.android.cdp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class AudienceImpl implements Audience {
    private List<String> segments;
    private Map<String, Object> attributes;
    private Key key;
    private String audienceId;

    private AudienceImpl(List<String> segments, Map<String, Object> attributes, Key key, String audienceId) {
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

    static AudienceImpl fromJSONObject(JSONObject audienceJSON) throws JSONException {

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
        Iterator keys = keyJson.keys();
        if (!(keys.hasNext())) {
            throw new JSONException("At least one `key` pair is required!");
        }
        Object keyProp = keys.next();
        if (!(keyProp instanceof String)) {
            throw new JSONException("Expect `key`'s name is a string!");
        }
        // TODO: settle the expected response key
        Key key = new KeyImpl((String) keyProp, keyJson.get((String) keyProp));

        String audienceId = audienceJSON.getString("audienceId");

        return new AudienceImpl(segments, attributes, key, audienceId);
    }

    public static final class KeyImpl implements Key {
        private String name;
        private Object value;

        public KeyImpl(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Object getValue() {
            return value;
        }
    }

}
