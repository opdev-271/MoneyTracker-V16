package com.moneytracker.app.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** Small fixed set of executors shared by the whole app: one for DB/disk work, one for the main thread. */
public class AppExecutors {

    private static volatile AppExecutors instance;

    private final Executor diskIO = Executors.newSingleThreadExecutor();
    private final Executor mainThread = new MainThreadExecutor();

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    public Executor diskIO() {
        return diskIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}
