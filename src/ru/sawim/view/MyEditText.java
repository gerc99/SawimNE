package ru.sawim.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;
import ru.sawim.R;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 04.08.13
 * Time: 16:50
 * To change this template use File | Settings | File Templates.
 */
public class MyEditText extends EditText {
    public MyEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, 0);
    }

    public MyEditText(Context context, AttributeSet attrs) {
        super(context, attrs);

    }
    public MyEditText(Context context) {
        super(context);

    }

}
