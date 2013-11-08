package ru.sawim.widget;

import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.*;
import android.content.Context;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import ru.sawim.text.InternalURLSpan;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.07.13
 * Time: 22:27
 * To change this template use File | Settings | File Templates.
 */

public class MyTextView extends View {

    private TextPaint textPaint;
    private Layout layout;
    private CharSequence mText = "";
    private CharSequence oldText = "";
    private int oldWidth;
    private float oldTextSize;
    TextLinkClickListener listener;
    private int curTextColor;
    private ColorStateList textColor;
    private ColorStateList linkTextColor;
    private boolean isSecondTap;
    private boolean isLongTap;
    private String link = "";

    public MyTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initPaint();
    }

    public MyTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initPaint();
    }

    public MyTextView(Context context) {
        super(context);
        initPaint();
    }

    private void initPaint() {
        if (textPaint == null) {
            textPaint = new TextPaint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(20);
            textPaint.setColor(Color.BLACK);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        textPaint.setColor(curTextColor);
        textPaint.drawableState = getDrawableState();
        if (layout != null)
            layout.draw(canvas);
    }

    @Override
    public void requestLayout() {
        //super.requestLayout();
    }

    public void setText(CharSequence text) {
        mText = text;
        invalidate();
    }

    public void setTextColor(int color) {
        textColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setLinkTextColor(int color) {
        linkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
    }

    public void setTextSize(float textSize) {
        Context c = getContext();
        Resources r = (c == null) ? Resources.getSystem() : c.getResources();
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSize, r.getDisplayMetrics()));
    }

    private void makeLayout(int specSize) {
        layout = new StaticLayout(mText, textPaint, specSize, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        if (oldText != mText || oldWidth != specSize || textPaint.getTextSize() != oldTextSize) {
            oldText = mText;
            oldWidth = specSize;
            oldTextSize = textPaint.getTextSize();
            makeLayout(specSize);
        }
        if (layout == null) makeLayout(specSize);
        setMeasuredDimension(specSize, layout.getLineTop(layout.getLineCount()));
    }

    private void updateTextColors() {
        boolean inval = false;
        int color = textColor.getColorForState(getDrawableState(), 0);
        if (color != curTextColor) {
            curTextColor = color;
            inval = true;
        }
        if (linkTextColor != null) {
            color = linkTextColor.getColorForState(getDrawableState(), 0);
            if (color != textPaint.linkColor) {
                textPaint.linkColor = color;
                inval = true;
            }
        }
        if (inval) {
            invalidate();
        }
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (textColor != null && textColor.isStateful()
                || (linkTextColor != null && linkTextColor.isStateful())) {
            updateTextColors();
        }
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

    private Runnable mLongPressed = new Runnable() {
        public void run() {
            if (listener != null && !isSecondTap) {
                isLongTap = true;
                listener.onTextLinkClick(MyTextView.this, link, true);
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mText instanceof Spannable) {
            Spannable buffer = (Spannable) mText;
            int action = event.getAction();
            int x = (int) event.getX();
            int y = (int) event.getY();
            x += getScrollX();
            y += getScrollY();
            int line = layout.getLineForVertical(y);
            int off = layout.getOffsetForHorizontal(line, x);
            final InternalURLSpan[] urlSpans = buffer.getSpans(off, off, InternalURLSpan.class);
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                isSecondTap = true;
            }
            if (urlSpans.length != 0) {
                link = urlSpans[0].clickedSpan;
                if (action == MotionEvent.ACTION_DOWN) {
                    isSecondTap = false;
                    isLongTap = false;
                    Selection.setSelection(buffer,
                            buffer.getSpanStart(urlSpans[0]),
                            buffer.getSpanEnd(urlSpans[0]));
                    postDelayed(mLongPressed, 700L);
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (!isLongTap) {
                        isSecondTap = true;
                        try {
                            if (listener != null)
                                listener.onTextLinkClick(MyTextView.this, link, false);
                        } catch (ActivityNotFoundException e) { }
                    } else {
                        removeCallbacks(mLongPressed);
                    }
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    public void setOnTextLinkClickListener(TextLinkClickListener onTextLinkClickListener) {
        listener = onTextLinkClickListener;
    }

    public interface TextLinkClickListener {
        public void onTextLinkClick(View textView, String clickedString, boolean isLongTap);
    }
}