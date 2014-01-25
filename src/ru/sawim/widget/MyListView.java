package ru.sawim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.AbsListView;
import android.widget.ListView;
import ru.sawim.General;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 29.08.13
 * Time: 14:54
 * To change this template use File | Settings | File Templates.
 */
public class MyListView extends ListView {

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

    public void stopScroll() {
        try {
            Field field = android.widget.AbsListView.class.getDeclaredField("mFlingRunnable");
            field.setAccessible(true);
            Object flingRunnable = field.get(this);
            if (flingRunnable != null) {
                Method method = Class.forName("android.widget.AbsListView$FlingRunnable").getDeclaredMethod("endFling");
                method.setAccessible(true);
                method.invoke(flingRunnable);
            }
        }
        catch (Exception e) {}
    }
}
