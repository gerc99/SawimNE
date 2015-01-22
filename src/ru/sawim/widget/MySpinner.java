package ru.sawim.widget;

import android.content.Context;
import android.support.v7.internal.widget.TintSpinner;
import android.util.AttributeSet;
import android.widget.AdapterView;
import android.widget.Spinner;

public class MySpinner extends TintSpinner {

    AdapterView.OnItemSelectedListener listener;

    public MySpinner(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public MySpinner(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MySpinner(Context context) {
        super(context);
    }

    @Override
    public void setSelection(int position) {
        super.setSelection(position);
        if (listener != null)
            listener.onItemSelected(null, null, position, 0);
    }

    public void setOnItemSelectedEvenIfUnchangedListener(
            OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public OnItemSelectedListener getOnItemSelectedEvenIfUnchangedListener() {
        return listener;
    }
}