package com.treasuredata.android;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public class TDJsonHandlerTest extends TestCase {
    private TDJsonHandler jsonHandler;
    private ObjectMapper objectMapper;

    public void setUp() throws Exception {
        super.setUp();
        jsonHandler = new TDJsonHandler();
        objectMapper = new ObjectMapper();
    }

    public void tearDown() throws Exception {
    }

    public void testReadJson() throws Exception {
    }

    public void testWriteJson() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out);
        Map<String, Object> value = new HashMap<String, Object>();
        value.put("name", "komamitsu");
        value.put("age", 123);
        Map<String, Object> keen = new HashMap<String, Object>();
        keen.put("timestamp", "2014-12-31T23:59:01.123+0000");
        value.put("keen", keen);
        jsonHandler.writeJson(writer, value);

        String s = new String(out.toByteArray());
        JsonNode jsonNode = objectMapper.readTree(s);
        assertEquals("komamitsu", jsonNode.get("name").asText());
        assertEquals(123, jsonNode.get("age").asInt());
        assertEquals("2014-12-31T23:59:01.123+0000", jsonNode.get("keen").findValue("timestamp").asText());
    }
}