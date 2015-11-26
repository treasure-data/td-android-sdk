package com.treasuredata.android;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeCodec;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;
import com.fasterxml.jackson.jr.ob.impl.TypeDetector;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomizedJSON extends JSON {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static class CustomizedJSONWriter extends JSONWriter {
        public CustomizedJSONWriter(int features, TypeDetector td, TreeCodec tc) {
            super(features, td, tc);
        }

        public CustomizedJSONWriter(CustomizedJSONWriter customizedJSONWriter, JsonGenerator jg)
        {
            super(customizedJSONWriter, jg);
        }

        private String getFormatedDate(Date date) {
            String dateStr;
            synchronized (SimpleDateFormat.class) {
                dateStr = DATE_FORMAT.format(date);
            }
            return dateStr;
        }

        // Workaround of https://github.com/FasterXML/jackson-jr/pull/31
        // Let's remove this override after it's merged.
        @Override
        public void writeField(String fieldName, Object value) throws IOException, JsonProcessingException
        {
            if (value == null) {
                super.writeField(fieldName, null);
                return;
            }

            int type = _typeDetector.findFullType(value.getClass());
            switch (type) {
            case TypeDetector.SER_UUID:
            case TypeDetector.SER_URL:
            case TypeDetector.SER_URI:
                writeStringLikeField(fieldName, value.toString(), type);
                return;
            }
            super.writeField(fieldName, value);
        }

        @Override
        protected void writeDateValue(Date v) throws IOException {
            writeStringValue(getFormatedDate(v));
        }

        @Override
        protected void writeDateField(String fieldName, Date v) throws IOException {
            writeStringField(fieldName, getFormatedDate(v));
        }

        @Override
        public JSONWriter perOperationInstance(JsonGenerator jg)
        {
            if (getClass() != CustomizedJSONWriter.class) { // sanity check
                throw new IllegalStateException("Sub-classes MUST override perOperationInstance(...)");
            }
            return new CustomizedJSONWriter(this, jg);
        }
    }

    @Override
    protected JSONWriter _defaultWriter(int features, TreeCodec tc) {
        return new CustomizedJSONWriter(features, TypeDetector.forWriter(features), tc);
    }
}
