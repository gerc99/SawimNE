package ru.sawim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class SizeNotifierLinearLayout extends LinearLayout {

    private OnSizeChangedListener onSizeChangedListener;

    public SizeNotifierLinearLayout(Context context) {
        super(context);
    }

    public SizeNotifierLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SizeNotifierLinearLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (onSizeChangedListener != null) {
            onSizeChangedListener.onSizeChanged();
        }
    }

    public void setOnSizeChangedListener(OnSizeChangedListener onSizeChangedListener) {
        this.onSizeChangedListener = onSizeChangedListener;
    }

    public abstract interface OnSizeChangedListener {
        public abstract void onSizeChanged();
    }
}
