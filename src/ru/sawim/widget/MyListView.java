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
                    setSelectionFromTop(scrollTo, offset - getPaddingTop());
                }
            });
        } else {
            super.onLayout(changed, left, top, right, bottom);
        }
        height = (bottom - top);
    }

    public void stopScroll() {
        onTouchEvent(
                MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_CANCEL,
                        0, 0, 0));
    }
}
