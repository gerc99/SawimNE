package ru.sawim.view;


import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.01.13
 * Time: 17:36
 * To change this template use File | Settings | File Templates.
 */
public class MyListView extends ListView {

    private boolean scroll = true;

    public MyListView(Context context) {
        super(context);
    }

    public MyListView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public void setScroll(boolean scroll) {
        this.scroll = scroll;
    }

    public boolean isScroll() {
        return scroll;
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        /*if (scroll) {
            if (Build.VERSION.SDK_INT >= 8) smoothScrollToPosition(getCount());
            else setSelection(getCount());
        }*/
    }

}

