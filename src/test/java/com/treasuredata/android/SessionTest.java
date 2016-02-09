package com.treasuredata.android;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SessionTest {
    @Test
    public void startAndFinish() throws InterruptedException {
        Session session = new Session(1000);

        assertNull(session.getId());

        session.start();
        String firstSessionId = session.getId();
        assertNotNull(firstSessionId);

        String sessionId = session.getId();
        assertEquals(firstSessionId, sessionId);

        session.finish();
        assertNull(session.getId());

        // Wait to expire the session
        TimeUnit.MILLISECONDS.sleep(1500);
        assertNull(session.getId());

        session.start();
        String secondSessionId = session.getId();
        assertNotNull(secondSessionId);

        sessionId = session.getId();
        assertEquals(secondSessionId, sessionId);

        session.finish();
        assertNull(session.getId());

        // This wait is too short and the session won't be expired
        TimeUnit.MILLISECONDS.sleep(300);
        assertNull(session.getId());

        session.start();
        assertEquals(secondSessionId, session.getId());
    }

    @Test
    public void shouldNotUpdateFinishedAtSecondCallingFinish() throws InterruptedException {
        Session session = new Session(500);
        session.start();

        String firstSessionId = session.getId();
        session.finish();
        TimeUnit.MILLISECONDS.sleep(1000);

        session.finish();
        session.start();

        assertNotEquals(firstSessionId, session.getId());
    }

    @Test
    public void shouldNotReturnIdBeforeCallingStart() throws InterruptedException {
        Session session = new Session(500);

        session.finish();

        assertNull(session.getId());
    }
}