package com.treasuredata.android;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class SessionTest {
    @Test
    public void getIdReturnsNullWithoutStart() throws InterruptedException {
        Session session = new Session(1000);
        assertNull(session.getId());
    }

    @Test
    public void startShouldActivateId() throws InterruptedException {
        Session session = new Session(1000);

        session.start();
        String firstSessionId = session.getId();
        assertNotNull(firstSessionId);

        String sessionId = session.getId();
        assertEquals(firstSessionId, sessionId);
    }

    @Test
    public void finishShouldInactivateId() throws InterruptedException {
        Session session = new Session(1000);
        session.start();
        session.finish();
        assertNull(session.getId());
    }

    @Test
    public void reStartWithinIntervalShouldReuseId() throws InterruptedException {
        Session session = new Session(1000);

        session.start();
        String firstSessionId = session.getId();
        session.finish();

        session.start();
        String secondSessionId = session.getId();
        assertEquals(firstSessionId, secondSessionId);
    }

    @Test
    public void reStartAfterExpirationShouldNotReuseId() throws InterruptedException {
        Session session = new Session(500);

        session.start();
        String firstSessionId = session.getId();
        session.finish();

        TimeUnit.MILLISECONDS.sleep(1000);

        session.start();
        String secondSessionId = session.getId();
        assertNotEquals(firstSessionId, secondSessionId);
    }

    @Test
    public void reStartWithoutFinishShouldNotUpdateId() throws InterruptedException {
        Session session = new Session(1000);

        session.start();
        String firstSessionId = session.getId();

        session.start();
        String secondSessionId = session.getId();

        assertEquals(firstSessionId, secondSessionId);
    }
}