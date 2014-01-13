package ru.sawim.widget.chat;

import android.content.Context;
import ru.sawim.widget.MyTextView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.07.13
 * Time: 22:27
 * To change this template use File | Settings | File Templates.
 */

public class MessageTextView extends MyTextView {

    public MessageTextView(Context context) {
        super(context);
    }

    @Override
    public void requestLayout() {
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        if (layout == null)
            makeLayout(specSize);
        setMeasuredDimension(specSize, layout.getLineTop(layout.getLineCount()));
    }
}