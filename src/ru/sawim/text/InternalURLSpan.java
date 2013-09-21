package ru.sawim.text;

import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.09.13
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class InternalURLSpan extends ClickableSpan {

    public String clickedSpan;

    public InternalURLSpan(String clickedString) {
        clickedSpan = clickedString;
    }

    @Override
    public void onClick(View textView) {
    }
}
