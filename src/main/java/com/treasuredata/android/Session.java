package com.treasuredata.android;

import java.util.UUID;

public class Session {
    private static final long DEFAULT_SESSION_PENDING_MILLIS = 10 * 1000;
    private final long sessionPendingMillis;
    private String id;
    private Long finishedAt;

    public Session() {
        this(DEFAULT_SESSION_PENDING_MILLIS);
    }

    public Session(long sessionPendingMillis) {
        this.sessionPendingMillis = sessionPendingMillis;
    }

    public synchronized void start() {
        // Checking `id` just in case
        if (id == null || finishedAt == null || (System.currentTimeMillis() - finishedAt) > sessionPendingMillis) {
            id = UUID.randomUUID().toString();
        }
        finishedAt = null;
    }

    public synchronized void finish() {
        finishedAt = System.currentTimeMillis();
    }

    public synchronized String getId() {
        if (id == null || finishedAt != null) {
            return null;
        }
        return id;
    }
}
