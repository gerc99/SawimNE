package ru.sawim.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import ru.sawim.SawimApplication;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.09.13
 * Time: 21:47
 * To change this template use File | Settings | File Templates.
 */

public class LabelView extends View {

    private Paint textPaint = new Paint();
    private String text = "";
    private int ascent;

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
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(16);
        textPaint.setColor(0xFF000000);
    }

    public void repaint() {
        requestLayout();
        invalidate();
    }

    public void setText(String text) {
        this.text = text;
        repaint();
    }

    public void setTextSize(int size) {
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, SawimApplication.getInstance().getResources().getDisplayMetrics()));
    }

    public void setTextColor(int color) {
        textPaint.setColor(color);
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
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
            int textWidth = (int) textPaint.measureText(text);
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

        ascent = (int) textPaint.ascent();
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            int textHeight = (int) (-ascent + textPaint.descent());
            result = textHeight + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (text != null)
            canvas.drawText(text, getPaddingLeft(), getPaddingTop() - ascent, textPaint);
    }
}