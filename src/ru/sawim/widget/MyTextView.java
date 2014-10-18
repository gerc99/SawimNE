package ru.sawim.widget;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.text.InternalURLSpan;
import ru.sawim.text.TextLinkClickListener;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 11.07.13
 * Time: 22:27
 * To change this template use File | Settings | File Templates.
 */

public class MyTextView extends View {

    private TextPaint textPaint;
    public Layout layout;
    private CharSequence text;
    TextLinkClickListener listener;
    private boolean isSecondTap;
    private boolean isLongTap;
    private boolean isRight = false;

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

    public void initPaint() {
        textPaint = new TextPaint();
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(SawimApplication.getFontSize());
        textPaint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (layout != null)
            layout.draw(canvas);
    }

    public void setText(CharSequence text) {
        this.text = text;
    }

    public void repaint() {
        invalidate();
        requestLayout();
    }

    public void setTextColor(int color) {
        textPaint.setColor(color);
    }

    public void setLinkTextColor(int color) {
        textPaint.linkColor = color;
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
    }

    public void setTextSize(float textSize) {
        textPaint.setTextSize(textSize * SawimApplication.getInstance().getResources().getDisplayMetrics().scaledDensity);
    }

    public void setGravity(boolean isRight) {
        this.isRight = isRight;
    }

    public void makeLayout(int specSize) {
        if (text == null) return;
        try {
            layout = new StaticLayout(text, textPaint, specSize, isRight ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        } catch (ArrayIndexOutOfBoundsException e) {
            layout = new StaticLayout(text.toString(), textPaint, specSize, isRight ? Layout.Alignment.ALIGN_OPPOSITE : Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        makeLayout(specSize);
        setMeasuredDimension(specSize, layout.getLineTop(layout.getLineCount()));
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

    private static final BackgroundColorSpan linkHighlightColor = new BackgroundColorSpan(Scheme.getColor(Scheme.THEME_LINKS_HIGHLIGHT));
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (text == null) return super.onTouchEvent(event);
        if (text instanceof Spannable) {
            Spannable buffer = (Spannable) text;
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
                Runnable longPressed = new Runnable() {
                    public void run() {
                        if (listener != null && !isSecondTap) {
                            isLongTap = true;
                            listener.onTextLinkClick(MyTextView.this, buildUrl(urlSpans), true);
                        }
                    }
                };
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_CANCEL) {
                    buffer.removeSpan(linkHighlightColor);
                    removeCallbacks(longPressed);
                    repaint();
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    isSecondTap = false;
                    isLongTap = false;
                    buffer.setSpan(linkHighlightColor,
                            buffer.getSpanStart(urlSpans[0]),
                            buffer.getSpanEnd(urlSpans[0]), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    repaint();
                    removeCallbacks(longPressed);
                    postDelayed(longPressed, ViewConfiguration.getLongPressTimeout());
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (!isLongTap) {
                        isSecondTap = true;
                        try {
                            if (listener != null)
                                listener.onTextLinkClick(MyTextView.this, buildUrl(urlSpans), false);
                        } catch (ActivityNotFoundException e) {
                        }
                    } else {
                        removeCallbacks(longPressed);
                    }
                }
                return true;
            }
        }
        return super.onTouchEvent(event);
    }

    private String buildUrl(InternalURLSpan[] urlSpans) {
        String link = urlSpans[0].clickedSpan;
        if (urlSpans.length == 2
                && urlSpans[1].clickedSpan.length() > urlSpans[0].clickedSpan.length())
            link = urlSpans[1].clickedSpan;
        return link;
    }

    public void setOnTextLinkClickListener(TextLinkClickListener onTextLinkClickListener) {
        listener = onTextLinkClickListener;
    }
}