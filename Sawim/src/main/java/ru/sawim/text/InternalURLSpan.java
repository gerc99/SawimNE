package ru.sawim.text;

import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.UpdateAppearance;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.09.13
 * Time: 13:30
 * To change this template use File | Settings | File Templates.
 */
public class InternalURLSpan extends CharacterStyle implements UpdateAppearance {

    public String clickedSpan;

    public InternalURLSpan(String clickedString) {
        clickedSpan = clickedString;
    }

    @Override
    public void updateDrawState(TextPaint ds) {
        ds.setColor(ds.linkColor);
        ds.setUnderlineText(false);
    }
}
