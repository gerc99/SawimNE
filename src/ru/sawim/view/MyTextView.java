package ru.sawim.view;

import android.content.ActivityNotFoundException;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.*;
import android.content.Context;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
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

    private boolean isSecondTap;
    private boolean isLongTap;
    private TextPaint mTextPaint;
    private int maxLines = 0;
    private Layout layout;
    private CharSequence mText = "";
    private CharSequence oldText = "";
    InternalURLSpan.TextLinkClickListener mListener;
    private ColorStateList mTextColor;
    private ColorStateList mLinkTextColor;
    private int mCurTextColor;

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
        mTextPaint = new TextPaint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(20);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setStyle(Paint.Style.FILL);
        mTextPaint.setAntiAlias(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        mTextPaint.setColor(mCurTextColor);
        mTextPaint.drawableState = getDrawableState();
        if (layout != null)
            layout.draw(canvas);
        canvas.restore();
    }

    @Override
    public void requestLayout() {
        //super.requestLayout();
    }

    public void setText(CharSequence text) {
        this.mText = text;
        requestLayout();
        invalidate();
    }

    public void setTextColor(int color) {
        mTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public final void setLinkTextColor(int color) {
        mLinkTextColor = ColorStateList.valueOf(color);
        updateTextColors();
    }

    public void setTypeface(Typeface typeface) {
        mTextPaint.setTypeface(typeface);
    }

    public void setLines(int lines) {
        maxLines = lines;
    }

    public void setTextSize(float textSize) {
        Context c = getContext();
        Resources r = (c == null) ? Resources.getSystem() : c.getResources();
        mTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, textSize, r.getDisplayMetrics()));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int specSize = MeasureSpec.getSize(widthMeasureSpec);
        if (oldText != mText) {
            oldText = mText;
            if (maxLines != 0)
                mText = TextUtils.ellipsize(mText, mTextPaint, specSize * maxLines, TextUtils.TruncateAt.END);
            layout = new StaticLayout(mText, mTextPaint, specSize, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0, false);
        } else {
            layout.increaseWidthTo(layout.getWidth());
        }
        setMeasuredDimension(specSize, layout.getLineTop(layout.getLineCount()));
    }

    private void updateTextColors() {
        boolean inval = false;
        int color = mTextColor.getColorForState(getDrawableState(), 0);
        if (color != mCurTextColor) {
            mCurTextColor = color;
            inval = true;
        }
        if (mLinkTextColor != null) {
            color = mLinkTextColor.getColorForState(getDrawableState(), 0);
            if (color != mTextPaint.linkColor) {
                mTextPaint.linkColor = color;
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
        if (mTextColor != null && mTextColor.isStateful()
                || (mLinkTextColor != null && mLinkTextColor.isStateful())) {
            updateTextColors();
        }
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

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

    private boolean action(MotionEvent event, View widget, Spannable buffer) {
        int action = event.getAction();
        if (action == MotionEvent.ACTION_UP ||
                action == MotionEvent.ACTION_DOWN) {
            int x = (int) event.getX();
            int y = (int) event.getY();

            x += widget.getScrollX();
            y += widget.getScrollY();

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

    public void setOnTextLinkClickListener(InternalURLSpan.TextLinkClickListener onTextLinkClickListener) {
        this.mListener = onTextLinkClickListener;
    }
}