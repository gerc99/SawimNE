package ru.sawim;

import android.app.Application;
import android.content.Context;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.06.13
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
public class SawimApplication extends Application {

    public static SawimApplication instance;
    public boolean useAbsence = false;

    @Override
    public void onCreate() {
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getApplicationContext()));
        super.onCreate();
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }
}
