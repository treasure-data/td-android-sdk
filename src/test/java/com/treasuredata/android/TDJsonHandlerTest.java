package com.treasuredata.android;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import junit.framework.TestCase;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TDJsonHandlerTest extends TestCase {
    private static final String JSON_STR = "{\n" +
                "    \"name\":\"komamitsu\",\n" +
                "    \"age\":123,\n" +
                "    \"keen\": {\n" +
                "        \"timestamp\": \"2014-12-31T23:59:01.123+0000\"\n" +
                "    },\n" +
                "    \"#UUID\":\"2F1FCD4D-74A6-45EF-B9B0-CD82DE49BE69\"\n" +
                "}";
    private TDJsonHandler jsonHandler;
    private Gson gson;

    public void setUp() throws Exception {
        super.setUp();
        jsonHandler = new TDJsonHandler();
        gson = new Gson();
    }

    public void tearDown() throws Exception {
    }

    public void testReadJson() throws Exception {
        Map<String, Object> result = jsonHandler.readJson(new StringReader(JSON_STR));
        assertEquals("komamitsu", result.get("name"));
        assertEquals(123.0, result.get("age"));
        assertEquals("2014-12-31T23:59:01.123+0000", ((Map<String, Object>) result.get("keen")).get("timestamp"));
        assertEquals("2F1FCD4D-74A6-45EF-B9B0-CD82DE49BE69", result.get("#UUID"));
    }

    private Map<String, ?> createExampleMap(Date now) {
        Map<String, List<Map<String, Object>>> root = new HashMap<String, List<Map<String, Object>>>();
        Map<String, Object> value = new HashMap<String, Object>();
        value.put("name", "komamitsu");
        value.put("age", 123);
        Map<String, Object> keen = new HashMap<String, Object>();
        keen.put("timestamp", "2014-12-31T23:59:01.123+0000");
        value.put("keen", keen);
        value.put("now", now);
        value.put("f_nan", Float.NaN);
        value.put("f_pos_inf", Float.POSITIVE_INFINITY);
        value.put("f_neg_inf", Float.NEGATIVE_INFINITY);
        value.put("d_nan", Double.NaN);
        value.put("d_pos_inf", Double.POSITIVE_INFINITY);
        value.put("d_neg_inf", Double.NEGATIVE_INFINITY);
        value.put("#UUID", "2F1FCD4D-74A6-45EF-B9B0-CD82DE49BE69");
        root.put("testdb.testtbl", Arrays.asList(value));

        return root;
    }

    private void assertExampleMap(Date now, Map<String, Object> root) {
        assertEquals(1, root.size());
        Object rootValue = root.get("testdb.testtbl");
        assertTrue(rootValue instanceof List);
        List<Map<String, Object>> list = (List<Map<String, Object>>) rootValue;
        assertEquals(1, list.size());
        assertTrue(list.get(0) instanceof Map);

        Map<String, Object> value = list.get(0);
        assertEquals("komamitsu", value.get("name"));
        assertEquals(123.0, value.get("age"));
        assertEquals("2014-12-31T23:59:01.123+0000", ((Map<String, Object>) value.get("keen")).get("timestamp"));
        assertEquals(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(now), value.get("now"));
        assertEquals(String.valueOf(Float.NaN), value.get("f_nan"));
        assertEquals(String.valueOf(Float.POSITIVE_INFINITY), value.get("f_pos_inf"));
        assertEquals(String.valueOf(Float.NEGATIVE_INFINITY), value.get("f_neg_inf"));
        assertEquals(String.valueOf(Double.NaN), value.get("d_nan"));
        assertEquals(String.valueOf(Double.POSITIVE_INFINITY), value.get("d_pos_inf"));
        assertEquals(String.valueOf(Double.NEGATIVE_INFINITY), value.get("d_neg_inf"));
        assertEquals("2F1FCD4D-74A6-45EF-B9B0-CD82DE49BE69", value.get("#UUID"));
    }

    public void testWriteJson() throws Exception {
        Date now = new Date();
        Map<String, ?> value = createExampleMap(now);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Writer writer = new OutputStreamWriter(out);
        jsonHandler.writeJson(writer, value);

        String s = new String(out.toByteArray());
        Map<String, Object> result = gson.fromJson(s, new TypeToken<Map<String, Object>>() {}.getType());
        assertExampleMap(now, result);
    }

    public void testReadWriteWithEncryption()
            throws IOException
    {
        Date now = new Date();
        Map<String, ?> value = createExampleMap(now);

        TDJsonHandler encJsonHandler = new TDJsonHandler("hello, world", new TDJsonHandler.Base64Encoder() {
            private final Charset UTF8 = Charset.forName("UTF-8");
            @Override
            public String encode(byte[] data)
            {
                return new String(Base64.encodeBase64(data), UTF8);
            }

            @Override
            public byte[] decode(String encoded)
            {
                return Base64.decodeBase64(encoded.getBytes(UTF8));
            }
        });

        StringWriter writer = new StringWriter();
        encJsonHandler.writeJson(writer, value);

        Map<String, Object> result = encJsonHandler.readJson(new StringReader(writer.toString()));
        assertExampleMap(now, result);
    }
}