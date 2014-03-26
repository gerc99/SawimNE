package ru.sawim.widget;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;

public abstract class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {

    private static final float MIN_DISTANCE = 80f;
    private static final float MAX_OFF_PATH = 250f;
    private static final float THRESHOLD_VELOCITY = 100f;

    private final int minDistance;
    private final int maxOffPath;
    private final int thresholdVelocity;

    public SwipeGestureListener(Activity activity) {
        final DisplayMetrics dm = activity.getResources().getDisplayMetrics();
        minDistance = (int) (MIN_DISTANCE * dm.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        maxOffPath = (int) (MAX_OFF_PATH * dm.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
        thresholdVelocity = (int) (THRESHOLD_VELOCITY * dm.densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }

    @Override
    public final boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        try {
            if (Math.abs(e1.getY() - e2.getY()) <= maxOffPath) {
                final float dx = e1.getX() - e2.getX();

                if (Math.abs(velocityX) > thresholdVelocity) {
                    if (dx > minDistance) {
                        onSwipeToLeft();
                        return true;
                    } else if (dx < -minDistance) {
                        onSwipeToRight();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    protected abstract void onSwipeToRight();

    protected abstract void onSwipeToLeft();
}