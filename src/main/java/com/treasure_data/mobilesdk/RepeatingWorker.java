package com.treasure_data.mobilesdk;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RepeatingWorker {
    public static final long DEFAULT_INTERVAL_MILLI = 5 * 60 * 1000;
    public static final long MIN_INTERVAL_MILLI = 2 * 60 * 1000;
    private volatile ExecutorService executorService;
    volatile long intervalMilli = DEFAULT_INTERVAL_MILLI;
    private BlockingQueue<Boolean> wakeupQueue = new LinkedBlockingQueue<Boolean>();
    private Runnable procedure;

    public void setProcedure(Runnable r) {
        this.procedure = r;
    }

    public void setInterval(long intervalMilli) {
        if (intervalMilli < MIN_INTERVAL_MILLI) {
            intervalMilli = MIN_INTERVAL_MILLI;
        }
        this.intervalMilli = intervalMilli;
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

    @Override
    protected void finalize() throws Throwable {
        stop();
        super.finalize();
    }
}
