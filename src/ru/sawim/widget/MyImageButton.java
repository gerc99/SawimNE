package ru.sawim.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageButton;
import ru.sawim.R;
import ru.sawim.Scheme;

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
        setBackgroundResource(Scheme.isBlack()
                ? R.drawable.abc_list_selector_holo_dark : R.drawable.abc_list_selector_holo_light);
        setMinimumWidth(Util.dipToPixels(getContext(), 45));
        setMinimumHeight(Util.dipToPixels(getContext(), 40));
    }
}
