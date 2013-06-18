package ru.sawim;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * <preferences/>
 * Date: 28.07.12 1:30
 *
 * @author vladimir
 */
public class Tray {
    private final Context context;
    private NotificationManager mNM;
    private Method mSetForeground;
    private Method mStartForeground;
    private Method mStopForeground;
    private Object[] mSetForegroundArgs = new Object[1];
    private Object[] mStartForegroundArgs = new Object[2];
    private Object[] mStopForegroundArgs = new Object[1];

    private static final Class<?>[] mSetForegroundSignature = new Class[]{boolean.class};
    private static final Class<?>[] mStartForegroundSignature = new Class[]{int.class, Notification.class};
    private static final Class<?>[] mStopForegroundSignature = new Class[]{boolean.class};

    public Tray(Context context) {
        this.context = context;
        mNM = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        try {
            mStartForeground = context.getClass().getMethod("startForeground", mStartForegroundSignature);
            mStopForeground = context.getClass().getMethod("stopForeground", mStopForegroundSignature);
        } catch (NoSuchMethodException e) {
            // Running on an older platform.
            mStartForeground = mStopForeground = null;
        }
        try {
            mSetForeground = context.getClass().getMethod("setForeground", mSetForegroundSignature);
        } catch (NoSuchMethodException e) {
            mSetForeground = null;
        }
    }

    /**
     * This is a wrapper around the new startForeground method, using the older
     * APIs if it is not available.
     */
    public void startForegroundCompat(int id, Notification notification) {
        // If we have the new startForeground API, then use it.
        if (mStartForeground != null) {
            mStartForegroundArgs[0] = Integer.valueOf(id);
            mStartForegroundArgs[1] = notification;
            invokeMethod(mStartForeground, mStartForegroundArgs);
            return;
        }

        // Fall back on the old API.
        mSetForegroundArgs[0] = Boolean.TRUE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
        mNM.notify(id, notification);
    }

    /**
     * This is a wrapper around the new stopForeground method, using the older
     * APIs if it is not available.
     */
    public void stopForegroundCompat(int id) {
        // If we have the new stopForeground API, then use it.
        if (mStopForeground != null) {
            mStopForegroundArgs[0] = Boolean.TRUE;
            invokeMethod(mStopForeground, mStopForegroundArgs);
            return;
        }

        // Fall back on the old API.  Note to cancel BEFORE changing the
        // foreground state, since we could be killed at that point.
        mNM.cancel(id);
        mSetForegroundArgs[0] = Boolean.FALSE;
        invokeMethod(mSetForeground, mSetForegroundArgs);
    }

    void invokeMethod(Method method, Object[] args) {
        try {
            method.invoke(context, args);
        } catch (Exception e) {
            // Should not happen.
        }
    }
}
