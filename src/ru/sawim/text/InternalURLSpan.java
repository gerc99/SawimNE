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
    TextLinkClickListener mListener;

    public InternalURLSpan(TextLinkClickListener onTextLinkClickListener, String clickedString) {
        mListener = onTextLinkClickListener;
        clickedSpan = clickedString;
    }

    @Override
    public void onClick(View textView) {
        mListener.onTextLinkClick(textView, clickedSpan, false);
    }

    public interface TextLinkClickListener {
        public void onTextLinkClick(View textView, String clickedString, boolean isLongTap);
    }
}
