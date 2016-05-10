package ru.sawim.io;


import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by gerc on 23.08.2015.
 */
public class SqlAsyncTask extends Thread {

    private volatile Handler handler = null;
    private CountDownLatch syncLatch = new CountDownLatch(1);
    //private Executor executor = Executors.newSingleThreadExecutor();

    public SqlAsyncTask(String threadName) {
        setName(threadName);
        setPriority(MAX_PRIORITY);
        start();
    }

    public void cancelRunnable(Runnable runnable) {
        try {
            syncLatch.await();
            handler.removeCallbacks(runnable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void postRunnable(Runnable runnable) {
        postRunnable(runnable, 0);
    }

    public void postRunnable(Runnable runnable, long delay) {
        //executor.execute(runnable);
        try {
            syncLatch.await();
            if (delay <= 0) {
                handler.post(runnable);
            } else {
                handler.postDelayed(runnable, delay);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanupQueue() {
        try {
            syncLatch.await();
            handler.removeCallbacksAndMessages(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Looper.prepare();
        handler = new Handler();
        syncLatch.countDown();
        Looper.loop();
    }

    public void execute(final OnTaskListener listener) {
        postRunnable(new Runnable() {
            @Override
            public void run() {
                if (listener != null) {
                    listener.run();
                }
            }
        });
    }

    public interface OnTaskListener {
        void run();
    }
}