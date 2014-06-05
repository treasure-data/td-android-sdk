package com.treasuredata.android;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.keen.client.java.KeenJsonHandler;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

/**
 * Implementation of the Keen JSON handler interface using the Jackson JSON library.
 *
 * @author Kevin Litwack (kevin@kevinlitwack.com)
 * @since 2.0.0
 */
class TDJsonHandler implements KeenJsonHandler {
    private static final String TAG = TDJsonHandler.class.getSimpleName();

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readJson(Reader reader) throws IOException {
        return mapper.readValue(reader, MAP_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeJson(Writer writer, Map<String, ?> value) throws IOException {
        mapper.writeValue(writer, value);
    }

    ///// DEFAULT ACCESS CONSTRUCTORS /////

    /**
     * Constructs a new Jackson JSON handler.
     */
    TDJsonHandler() {
        mapper = new ObjectMapper();
        mapper.setDateFormat(SRC_DATA_FORMAT);
    }

    ///// PRIVATE CONSTANTS /////

    private static final MapType MAP_TYPE =
            TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Object.class);

    private static final SimpleDateFormat SRC_DATA_FORMAT;
    private static final SimpleDateFormat DST_DATA_FORMAT;
    static {
        TimeZone timeZone = TimeZone.getTimeZone("GMT");
        SRC_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        SRC_DATA_FORMAT.setTimeZone(timeZone);
        DST_DATA_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        DST_DATA_FORMAT.setTimeZone(timeZone);
    }

    ///// PRIVATE FIELDS /////

    private final ObjectMapper mapper;

}

