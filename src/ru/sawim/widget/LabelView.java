package ru.sawim.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.09.13
 * Time: 21:47
 * To change this template use File | Settings | File Templates.
 */

public class LabelView extends View {
    private Paint mTextPaint;
    private String mText = "";
    private int mAscent;
    private boolean isCenter = false;
    private int textWidth = 1;
    private int textHeight = 1;

    public LabelView(Context context) {
        super(context);
        initLabelView();
    }

    public LabelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initLabelView();
    }

    public LabelView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initLabelView();
    }

    private final void initLabelView() {
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(16);
        mTextPaint.setColor(0xFF000000);
        setPadding(3, 3, 3, 3);
    }

    public void repaint() {
        requestLayout();
        invalidate();
    }

    public void setText(String text) {
        mText = text;
        repaint();
    }

    public void setTextSize(int size) {
        Context c = getContext();
        Resources r = (c == null) ? Resources.getSystem() : c.getResources();
        mTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, r.getDisplayMetrics()));
    }

    public void setTextColor(int color) {
        mTextPaint.setColor(color);
    }

    public void setTypeface(Typeface typeface) {
        mTextPaint.setTypeface(typeface);
    }

    public void setCenter() {
        isCenter = true;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec));
    }

    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            textWidth = (int) mTextPaint.measureText(mText);
            result = textWidth + getPaddingLeft()
                    + getPaddingRight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) mTextPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            textHeight = (int) (-mAscent + mTextPaint.descent());
            result = textHeight + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    public int getTextX() {
        return isCenter ? (int) (getPaddingLeft() + getMeasuredWidth() / 2 - (mTextPaint.measureText(mText) + mTextPaint.getTextSize()) / 2) : getPaddingLeft();
    }

    public int getTextY() {
        return isCenter ? getMeasuredHeight() / 2 + (getPaddingTop() - mAscent) / 2 - getPaddingBottom() : getPaddingTop() - mAscent;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mText != null)
            canvas.drawText(mText, getTextX(), getTextY(), mTextPaint);
    }
}