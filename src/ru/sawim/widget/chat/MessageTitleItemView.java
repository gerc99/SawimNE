package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.view.View;
import ru.sawim.General;
import ru.sawim.Scheme;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 17:58
 * To change this template use File | Settings | File Templates.
 */
public class MessageTitleItemView extends View {

    private String msgTimeText;
    private String nickText;
    private int nickColor;
    private int msgTimeColor;
    private Typeface nickTypeface;
    private Typeface msgTimeTypeface;
    private int nickSize;
    private int msgTimeSize;
    private Bitmap checkImage;

    private float msgTimeX;
    private int textY;
    private static TextPaint textPaint;

    public MessageTitleItemView(Context context) {
        super(context);
        if (textPaint == null) {
            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        }
        textPaint.setAntiAlias(true);
        textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(size * General.getResources(getContext()).getDisplayMetrics().scaledDensity);
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
        int ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            int textHeight = (-ascent + descent) + getPaddingTop() + getPaddingBottom();
            int iconHeight = checkImage == null ? 0 : checkImage.getHeight();
            result = Math.max(textHeight, iconHeight);
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
        msgTimeX = viewWidth - getPaddingRight();
        textY = getPaddingTop() - (int) textPaint.ascent();
    }

    public void setNick(int nickColor, int nickSize, Typeface nickTypeface, String nickText) {
        this.nickColor = nickColor;
        this.nickSize = nickSize;
        this.nickTypeface = nickTypeface;
        this.nickText = nickText;
    }

    public void setMsgTime(int msgTimeColor, int msgTimeSize, Typeface msgTimeTypeface, String msgTimeText) {
        this.msgTimeColor = msgTimeColor;
        this.msgTimeSize = msgTimeSize;
        this.msgTimeTypeface = msgTimeTypeface;
        this.msgTimeText = msgTimeText;
    }

    public void setCheckImage(Bitmap image) {
        checkImage = image;
    }

    public void repaint() {
        setTextSize(General.getFontSize());
        requestLayout();
        invalidate();
    }

    @Override
    public void requestLayout() {
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (nickText != null) {
            textPaint.setColor(nickColor);
            textPaint.setTextAlign(Paint.Align.LEFT);
            setTextSize(nickSize);
            textPaint.setTypeface(nickTypeface);
            canvas.drawText(nickText, getPaddingLeft(), textY, textPaint);
        }
        if (msgTimeText != null) {
            textPaint.setColor(msgTimeColor);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            setTextSize(msgTimeSize);
            textPaint.setTypeface(msgTimeTypeface);
            canvas.drawText(msgTimeText,
                    msgTimeX - (checkImage == null ? 0 : (checkImage.getWidth() << 1) - getPaddingLeft()), textY, textPaint);
        }
        if (checkImage != null) {
            canvas.drawBitmap(checkImage,
                    msgTimeX - checkImage.getWidth(), getPaddingTop() + checkImage.getHeight() / 2, null);
        }
    }
}