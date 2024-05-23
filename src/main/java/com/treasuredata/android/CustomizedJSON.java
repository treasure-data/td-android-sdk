package com.treasuredata.android;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.TreeCodec;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;
import com.fasterxml.jackson.jr.ob.impl.ValueWriterLocator;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomizedJSON extends JSON {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static class CustomizedJSONWriter extends JSONWriter {
        public CustomizedJSONWriter()
        {
            super();
        }

        protected CustomizedJSONWriter(CustomizedJSONWriter base, int features,
                             ValueWriterLocator loc, TreeCodec tc,
                             JsonGenerator g)
        {
            super(base, features, loc, tc, g);
        }

        private String getFormattedDate(Date date) {
            String dateStr;
            synchronized (SimpleDateFormat.class) {
                dateStr = DATE_FORMAT.format(date);
            }
            return dateStr;
        }

        public JSONWriter perOperationInstance(int features,
                                               ValueWriterLocator loc, TreeCodec tc,
                                               JsonGenerator g)
        {
            if (getClass() != CustomizedJSONWriter.class) { // sanity check
                throw new IllegalStateException("Sub-classes MUST override perOperationInstance(...)");
            }
            return new CustomizedJSONWriter(this, features, loc, tc, g);
        }

        @Override
        protected void writeDateValue(Date v) throws IOException {
            writeStringValue(getFormattedDate(v));
        }

        @Override
        protected void writeDateField(String fieldName, Date v) throws IOException {
            writeStringField(fieldName, getFormattedDate(v));
        }
    }

    @Override
    protected JSONWriter _defaultWriter() {
        return new CustomizedJSONWriter();
    }
}
