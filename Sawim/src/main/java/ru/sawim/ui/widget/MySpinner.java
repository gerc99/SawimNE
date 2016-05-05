package ru.sawim.ui.widget;

import android.content.Context;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.widget.AdapterView;

public class MySpinner extends AppCompatSpinner {

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
