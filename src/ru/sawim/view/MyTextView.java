package ru.sawim.view;

import android.content.ActivityNotFoundException;
import android.graphics.Color;
import android.text.*;
import android.content.Context;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.view.menu.JuickMenu;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.07.13
 * Time: 22:27
 * To change this template use File | Settings | File Templates.
 */

public class MyTextView extends TextView {

    TextLinkClickListener mListener;
    private boolean isSecondTap;
    private boolean isLongTap;

    Pattern juickPattern = Pattern.compile("(#[0-9]+(/[0-9]+)?)");
    Pattern pstoPattern = Pattern.compile("(#[\\w]+(/[0-9]+)?)");

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MyTextView(Context context) {
        super(context);
    }

    public void setTextWithLinks(SpannableStringBuilder ssb, JuickMenu.Mode mode) {
        if (mode != JuickMenu.Mode.none) {
            ArrayList<Hyperlink> msgList = new ArrayList<Hyperlink>();
            if (mode == JuickMenu.Mode.juick)
                addLinks(msgList, ssb, juickPattern);
            else if (mode == JuickMenu.Mode.psto)
                addLinks(msgList, ssb, pstoPattern);

            for (Hyperlink link : msgList) {
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.SANS_SERIF.getStyle()), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(link.span, link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.setSpan(new ForegroundColorSpan(Color.BLUE), link.start, link.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        setText(ssb);
    }

    private final void addLinks(ArrayList<Hyperlink> links, Spannable s, Pattern pattern) {
        Matcher m = pattern.matcher(s);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            Hyperlink spec = new Hyperlink();
            spec.textSpan = s.subSequence(start, end);
            spec.span = new InternalURLSpan(spec.textSpan.toString());
            spec.start = start;
            spec.end = end;
            links.add(spec);
        }
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

    public boolean onTouchEvent(MotionEvent event) {
        Object text = getText();
        if (text instanceof Spannable) {
            Spannable buffer = (Spannable) text;
            int action = event.getAction();

            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= getTotalPaddingLeft();
            y -= getTotalPaddingTop();

            x += getScrollX();
            y += getScrollY();

            Layout layout = getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            final URLSpan[] link = buffer.getSpans(off, off, URLSpan.class);
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                isSecondTap = true;
            }

            if (link.length != 0) {
                if (action == MotionEvent.ACTION_DOWN) {
                    isSecondTap = false;
                    isLongTap = false;
                    Selection.setSelection(buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]));
                    postDelayed(new Runnable() {
                        public void run() {
                            if (mListener != null && !isSecondTap) {
                                isLongTap = true;
                                mListener.onTextLinkClick(MyTextView.this, link[0].getURL());
                            }
                        }
                    }, 700L);
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (!isLongTap) {
                        isSecondTap = true;
                         try {
                            link[0].onClick(MyTextView.this);
                         } catch (ActivityNotFoundException e) { }
                    }
                }
                return true;
            }
            return action(event, this, buffer);
        }
        return false;
    }

    private boolean action(MotionEvent event, TextView widget, Spannable buffer) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();

            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);

            ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
            if (link.length != 0) {
                if (action == MotionEvent.ACTION_UP) {
                    link[0].onClick(widget);
                } else if (action == MotionEvent.ACTION_DOWN) {
                    Selection.setSelection(buffer,
                            buffer.getSpanStart(link[0]),
                            buffer.getSpanEnd(link[0]));
                }
                return true;
            } else {
                Selection.removeSelection(buffer);
            }
        }
        return false;
    }

    public void setOnTextLinkClickListener(MessagesAdapter onTextLinkClickListener) {
        this.mListener = onTextLinkClickListener;
    }

    public interface TextLinkClickListener {
        public void onTextLinkClick(View textView, String clickedString);
    }

    private class InternalURLSpan extends ClickableSpan {
        private String clickedSpan;

        public InternalURLSpan (String clickedString) {
            clickedSpan = clickedString;
        }

        @Override
        public void onClick(View textView) {
            mListener.onTextLinkClick(textView, clickedSpan);
        }
    }

    class Hyperlink {
        CharSequence textSpan;
        InternalURLSpan span;
        int start;
        int end;
    }
}