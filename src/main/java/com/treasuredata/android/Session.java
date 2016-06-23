package com.treasuredata.android;

import java.util.UUID;

public class Session {
    public static final long DEFAULT_SESSION_PENDING_MILLIS = 10 * 1000;
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
        if (id == null || (finishedAt != null && (System.currentTimeMillis() - finishedAt) > sessionPendingMillis)) {
            id = UUID.randomUUID().toString();
        }
        finishedAt = null;
    }

    public synchronized void finish() {
        // Checking `id` just for case of calling finish() first before start()
        if (id != null && finishedAt == null) {
            finishedAt = System.currentTimeMillis();
        }
    }

    public synchronized String getId() {
        if (id == null || finishedAt != null) {
            return null;
        }
        return id;
    }
}
