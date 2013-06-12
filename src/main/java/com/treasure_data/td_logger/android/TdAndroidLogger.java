package com.treasure_data.td_logger.android;

import java.util.Map;
import java.util.Properties;

import android.content.Context;

import com.treasure_data.logger.TreasureDataLogger;

public class TdAndroidLogger {
    private static final String TD_APK_KEY = "td.logger.api.key";
    private static final String TD_API_SERVER_HOST = "td.logger.api.server.host";
    private static final String TD_API_SERVER_PORT = "td.logger.api.server.port";
    private static final String TD_AGENT_MODE = "td.logger.agentmode";
    private final TreasureDataLogger logger;

    /* TODO: read configuration from res/values/td_logger.xml
    public TdAndroidLogger(Context context) {
    }
    */

    public TdAndroidLogger(String apikey, String database) {
        this(apikey, database, null, 0);
    }

    public TdAndroidLogger(String apikey, String database, String host, int port) {
        Properties properties = new Properties();
        properties.put(TD_APK_KEY, apikey);
        properties.put(TD_AGENT_MODE, "false");
        if (host != null) {
            properties.put(TD_API_SERVER_HOST, host);
        }
        if (port > 0) {
            properties.put(TD_API_SERVER_PORT, port);
        }
        this.logger = TreasureDataLogger.getLogger(database, properties);
    }

    public boolean write(String label, String key, Object value, long timestamp) {
        return logger.log(label, key, value, timestamp);
    }

    public boolean write(String label, String key, Object value) {
        return logger.log(label, key, value);
    }

    public boolean write(String label, Map<String, Object>data, long timestamp) {
        return logger.log(label, data, timestamp);
    }

    public boolean write(String label, Map<String, Object>data) {
        return logger.log(label, data);
    }

    public void flush() {
        logger.flush();
    }

    public void close() {
        logger.close();
    }

    // TODO: implement flush worker
}
