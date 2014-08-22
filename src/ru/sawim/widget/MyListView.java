package ru.sawim.widget;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 29.08.13
 * Time: 14:54
 * To change this template use File | Settings | File Templates.
 */
public class MyListView extends ListView {

    private int height = -1;

    public MyListView(Context context) {
        super(context);
        init();
    }

    public MyListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        setCacheColorHint(0x00000000);
        setFastScrollEnabled(true);
        setScrollingCacheEnabled(false);
        setAnimationCacheEnabled(false);
        setDivider(null);
        setDividerHeight(0);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View v = getChildAt(getChildCount() - 1);
        if (v != null && height > 0 && changed && ((bottom - top) < height)) {
            int b = height - v.getTop();
            final int scrollTo = getLastVisiblePosition();
            super.onLayout(changed, left, top, right, bottom);
            final int offset = (bottom - top) - b;
            post(new Runnable() {
                @Override
                public void run() {
                    try {
                        setSelectionFromTop(scrollTo, offset - getPaddingTop());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } else {
            try {
                super.onLayout(changed, left, top, right, bottom);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        height = (bottom - top);
    }

    /**
     * Check if this view can be scrolled vertically in a certain direction.
     *
     * @param direction Negative to check scrolling up, positive to check scrolling down.
     * @return true if this view can be scrolled in the specified direction, false otherwise.
     */
    public boolean canScrollVertically(int direction) {
        final int offset = computeVerticalScrollOffset();
        final int range = computeVerticalScrollRange() - computeVerticalScrollExtent();
        if (range == 0) return false;
        if (direction < 0) {
            return offset > 0;
        } else {
            return offset < range - 1;
        }
    }

    public void stopScroll() {
        onTouchEvent(
                MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL,
                        0, 0, 0));
    }
}
