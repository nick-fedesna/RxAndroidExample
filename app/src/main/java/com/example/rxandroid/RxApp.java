package com.example.rxandroid;

import android.app.Application;
import android.os.StrictMode;

import com.example.rxandroid.api.ExampleApi;
import timber.log.Timber;

public class RxApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.uprootAll();
        Timber.plant(new Timber.DebugTree());

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                                           .detectDiskReads()
                                           .detectDiskWrites()
                                           .detectNetwork()   // or .detectAll() for all detectable problems
                                           .penaltyLog()
                                           .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                       .detectLeakedSqlLiteObjects()
                                       .detectLeakedClosableObjects()
                                       .penaltyLog()
                                       .penaltyDeath()
                                       .build());
    }

}
