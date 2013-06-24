package com.treasure_data.td_logger.android;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.msgpack.MessagePack;
import org.msgpack.type.ArrayValue;
import org.msgpack.type.Value;

public class FluentdMock {
    private volatile ServerSocket fluentd;

    public static class Result {
        private final List<ArrayValue> resultValues;
        private final CountDownLatch countDownLatch;

        public Result(List<ArrayValue> resultValues, CountDownLatch countDownLatch) {
            this.resultValues = resultValues;
            this.countDownLatch = countDownLatch;
        }

        public List<ArrayValue> getResultValues() {
            return resultValues;
        }

        public void waitResult() {
            try {
                countDownLatch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getPort() {
        // should call after run()
        return fluentd.getLocalPort();
    }

    class Worker implements Runnable {
        private final Socket socket;
        private final List<ArrayValue> resultEntries;
        private final CountDownLatch countDownLatch;

        public Worker(Socket s, List<ArrayValue> resultEntries, CountDownLatch countDownLatch) {
            this.socket = s;
            this.resultEntries = resultEntries;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void run() {
            try {
                InputStream is = socket.getInputStream();
                MessagePack messagePack = new MessagePack();
                while (true) {
                    Value value = messagePack.read(is);
                    ArrayValue arrayValue = value.asArrayValue();
                    resultEntries.add(arrayValue);
                    countDownLatch.countDown();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public Result run(final int recCount) throws IOException {
        fluentd = new ServerSocket();
        final List<ArrayValue> resultEntries = new LinkedList<ArrayValue>();
        final CountDownLatch syncPoint = new CountDownLatch(1);

        final ExecutorService executor = Executors.newCachedThreadPool();
        final CountDownLatch cdl = new CountDownLatch(recCount);

        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    fluentd.bind(new InetSocketAddress(0));
                    while (cdl.getCount() >= 0) {
                        Socket clientSocket = fluentd.accept();
                        System.out.println("receive!!!!!!!");
                        executor.execute(new Worker(clientSocket, resultEntries, cdl));
                    }
                    syncPoint.countDown();
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (fluentd != null) {
                            fluentd.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    cdl.await(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executor.shutdownNow();
                syncPoint.countDown();
            }
        });

        return new Result(resultEntries, syncPoint);
    }
}