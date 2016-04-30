package ru.sawim.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;

/**
 * Created by admin on 24.03.14.
 */
public class MyImageButton extends ImageButton {

    public MyImageButton(Context context) {
        super(context);
        init();
    }

    public MyImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MyImageButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        Util.setSelectableItemBackground(this);
        setMinimumWidth(Util.dipToPixels(getContext(), 45));
        setMinimumHeight(Util.dipToPixels(getContext(), 40));
    }
}
