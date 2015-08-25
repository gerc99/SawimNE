package ru.sawim.io;


import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by gerc on 23.08.2015.
 */
public class SqlAsyncTask {

    static final Executor executor = Executors.newSingleThreadExecutor();
    OnTaskListener listener;

    private SqlAsyncTask(OnTaskListener successListener) {
        this.listener = successListener;
    }

    public static void execute(OnTaskListener listener) {
        new SqlAsyncTask(listener).execute();
    }

    private void execute() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                listener.run();
            }
        });
    }

    public interface OnTaskListener {
        void run();
    }
}