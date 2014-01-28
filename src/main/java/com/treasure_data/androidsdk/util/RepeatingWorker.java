package com.treasure_data.androidsdk.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RepeatingWorker {
    public static final long DEFAULT_INTERVAL_MILLI = 10 * 60 * 1000;
//    public static final long MIN_INTERVAL_MILLI =
//            Debug.isDebuggerConnected() ? (5 * 1000) : (1 * 60 * 1000);
    public static final long MIN_INTERVAL_MILLI = 5 * 1000;

    private volatile ExecutorService executorService;
    protected static volatile long intervalMilli = DEFAULT_INTERVAL_MILLI;
    private BlockingQueue<Boolean> wakeupQueue = new LinkedBlockingQueue<Boolean>();
    private volatile Runnable procedure;
    private boolean isClosing = false;

    public void setProcedure(Runnable r) {
        this.procedure = r;
    }

    public static long setInterval(long intervalMilli) {
        if (intervalMilli < MIN_INTERVAL_MILLI) {
            intervalMilli = MIN_INTERVAL_MILLI;
        }
        RepeatingWorker.intervalMilli = intervalMilli;
        return intervalMilli;
    }

    public void start() {
        stop();

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final ExecutorService myExecutorService = executorService;

                wakeupQueue.clear();

                while (myExecutorService != executorService || !executorService.isShutdown()) {
                    try {
                        wakeupQueue.poll(intervalMilli, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {
                    }

                    if (procedure != null) {
                        procedure.run();
                        if (isClosing) {
                            isClosing = false;
                            stop();
                        }
                    }
                }
            }
        });
    }

    public void stop() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
        wakeupQueue.add(true);
    }

    public boolean isRunning() {
        return executorService != null && !executorService.isShutdown();
    }

    public void setClosing(boolean isClosing) {
        this.isClosing = isClosing;
    }

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}
