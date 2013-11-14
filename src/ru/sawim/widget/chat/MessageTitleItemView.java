package ru.sawim.widget.chat;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import ru.sawim.Scheme;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 17:58
 * To change this template use File | Settings | File Templates.
 */
public class MessageTitleItemView extends View {
    private BitmapDrawable msgImage;
    private String msgTimeText;
    private String nickText;
    private int nickColor;
    private int msgTimeColor;
    private Typeface nickTypeface;
    private Typeface msgTimeTypeface;
    private int nickSize;
    private int msgTimeSize;

    private int nickX;
    private float msgTimeX;
    private int msgTimeWidth;
    private int textY;
    private int ascent;
    private static Paint textPaint;
    private static Resources resources;

    public MessageTitleItemView(Context context) {
        super(context);
        setPadding(5, 0, 5, 0);
        if (resources == null) {
            Context c = getContext();
            resources = (c == null) ? Resources.getSystem() : c.getResources();
        }
        if (textPaint == null) {
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(16);
            textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
        }
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, resources.getDisplayMetrics()));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = msgImage == null
                    ? (-ascent + descent) + getPaddingTop() + getPaddingBottom()
                    : msgImage.getBitmap().getHeight();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right,
                            int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        computeCoordinates(right - left, bottom - top);
    }

    private void computeCoordinates(int viewWidth, int viewHeight) {
        nickX = msgImage == null ? getPaddingLeft() : msgImage.getBitmap().getWidth() + getPaddingLeft() + getPaddingRight();
        if (msgTimeText != null) {
            msgTimeWidth = (int) textPaint.measureText(msgTimeText);
            msgTimeX = viewWidth - msgTimeWidth - getPaddingRight();
        } else {
            msgTimeX = viewWidth - getPaddingRight();
        }
        textY = viewHeight / 2 + -ascent / 2;
    }

    public void setMsgImage(BitmapDrawable msgImage) {
        this.msgImage = msgImage;
    }

    public void setNick(String nickText, int nickColor, Typeface nickTypeface, int nickSize) {
        this.nickText = nickText;
        this.nickColor = nickColor;
        this.nickTypeface = nickTypeface;
        this.nickSize = nickSize;
    }

    public void setMsgTime(String msgTimeText, int msgTimeColor, Typeface msgTimeTypeface, int msgTimeSize) {
        this.msgTimeText = msgTimeText;
        this.msgTimeColor = msgTimeColor;
        this.msgTimeTypeface = msgTimeTypeface;
        this.msgTimeSize = msgTimeSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (msgImage != null)
            canvas.drawBitmap(msgImage.getBitmap(), getPaddingLeft(), getPaddingTop(), null);
        if (nickText != null) {
            textPaint.setColor(nickColor);
            setTextSize(nickSize);
            textPaint.setTypeface(nickTypeface);
            if (msgTimeText != null)
                textPaint.setStrokeWidth(getWidth() - msgTimeWidth - getPaddingRight());
            canvas.drawText(nickText, nickX, textY, textPaint);
        }
        if (msgTimeText != null) {
            textPaint.setColor(msgTimeColor);
            setTextSize(msgTimeSize);
            textPaint.setTypeface(msgTimeTypeface);
            canvas.drawText(msgTimeText, msgTimeX, textY, textPaint);
        }
    }
}