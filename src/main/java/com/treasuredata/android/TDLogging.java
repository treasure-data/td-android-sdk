package com.treasuredata.android;

import io.keen.client.java.KeenLogging;
import org.komamitsu.android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TDLogging {
    private static final String TAG = TDLogging.class.getSimpleName();
    private static volatile boolean initialized;
    private static volatile boolean enabled;

    private static void initializeIfNot() {
        if (initialized) {
            return;
        }

        try {
            Field fieldOfLogger = KeenLogging.class.getDeclaredField("LOGGER");
            fieldOfLogger.setAccessible(true);
            TDLoggingHandler tdLoggingHandler = new TDLoggingHandler();
            Method m = Logger.class.getDeclaredMethod("addHandler", Handler.class);
            m.invoke(fieldOfLogger.get(null), tdLoggingHandler);
            initialized = true;
        } catch (NoSuchFieldException e) {
            Log.e(TAG, "enableLogging failed", e);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "enableLogging failed", e);
        } catch (InvocationTargetException e) {
            Log.e(TAG, "enableLogging failed", e);
        } catch (IllegalAccessException e) {
            Log.e(TAG, "enableLogging failed", e);
        }
    }

    public synchronized static void enableLogging() {
        /*
        private static final Logger LOGGER;

        static {
            LOGGER = Logger.getLogger(KeenLogging.class.getName());
            LOGGER.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
            disableLogging();
        }
        */
        initializeIfNot();

        enabled = true;
        KeenLogging.enableLogging();
    }

    public static void disableLogging() {
        enabled = false;
        KeenLogging.disableLogging();
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    static class TDLoggingHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            Log.i(TAG, record.getMessage());
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }
}
