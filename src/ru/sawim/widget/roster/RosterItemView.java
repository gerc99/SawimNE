package ru.sawim.widget.roster;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 03.08.13
 * Time: 13:54
 * To change this template use File | Settings | File Templates.
 */
public class RosterItemView extends View {

    public String itemName;
    public String itemDesc;
    public int itemNameColor;
    public int itemDescColor;
    public Typeface itemNameFont;

    public BitmapDrawable itemFirstImage = null;
    public BitmapDrawable itemSecondImage = null;
    public BitmapDrawable itemThirdImage = null;
    public BitmapDrawable itemFourthImage = null;
    public BitmapDrawable itemFifthImage = null;

    private static TextPaint textPaint;

    private int lineOneY, lineTwoY,
            firstImageX, firstImageY,
            secondImageX, secondImageY,
            thirdImageX, thirdImageY,
            fourthImageX, fourthImageY,
            fifthImageX, fifthImageY;
    private int textX;

    public RosterItemView(Context context) {
        super(context);
        int padding = Util.dipToPixels(context, 15);
        setPadding(padding, padding, padding, padding);
        if (textPaint == null) {
            textPaint = new TextPaint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(General.getFontSize());
            textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
        }
    }

    public void addLayer(String text) {
        setNull();
        itemDescColor = Scheme.getColor(Scheme.THEME_GROUP);
        itemDesc = text;
    }

    public void setNull() {
        itemName = null;
        itemDesc = null;
        itemFirstImage = null;
        itemSecondImage = null;
        itemThirdImage = null;
        itemFourthImage = null;
        itemFifthImage = null;
    }

    public void repaint() {
        requestLayout();
        invalidate();
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, General.getResources(getContext()).getDisplayMetrics()));
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
        int bottomPadding = getPaddingBottom();
        int topPadding = getPaddingTop();
        if (itemName != null && itemDesc != null) {
            ascent <<= 1;
            descent <<= 1;
        } else if (itemName == null && itemDesc != null) {
            topPadding >>= 2;
            bottomPadding >>= 2;
        }
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = (-ascent + descent) + topPadding
                    + bottomPadding;
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
        int leftPadding = getPaddingLeft();
        int rightPadding = getPaddingRight();
        int bottomPadding = getPaddingBottom();
        int topPadding = getPaddingTop();
        int y = viewHeight >> 1;
        int ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        textX = leftPadding;
        if (itemDesc != null) {
            lineOneY = topPadding - ascent;
        } else {
            lineOneY = viewHeight - descent - bottomPadding;
        }
        if (itemName != null && itemDesc != null) {
            lineTwoY = viewHeight - descent - bottomPadding;
        } else {
            lineTwoY = y + descent;
        }

        firstImageX = leftPadding;
        if (itemFirstImage != null) {
            secondImageX = firstImageX + itemFirstImage.getBitmap().getWidth();
            firstImageY = y - (itemFirstImage.getBitmap().getHeight() >> 1);
            textX = secondImageX + leftPadding;
        }
        if (itemSecondImage != null) {
            secondImageX += leftPadding;
            secondImageY = y - (itemSecondImage.getBitmap().getHeight() >> 1);
            textX += itemSecondImage.getBitmap().getWidth() + leftPadding;
        }
        thirdImageX = secondImageX;
        if (itemThirdImage != null) {
            thirdImageX += leftPadding;
            thirdImageY = y - (itemThirdImage.getBitmap().getHeight() >> 1);
            textX = thirdImageX + itemThirdImage.getBitmap().getWidth() + leftPadding;
        }

        fourthImageX = viewWidth;
        if (itemFourthImage != null) {
            fourthImageX = viewWidth - itemFourthImage.getBitmap().getWidth() - rightPadding;
            fourthImageY = y - (itemFourthImage.getBitmap().getHeight() >> 1);
        }
        fifthImageX = fourthImageX;
        if (itemFifthImage != null) {
            fifthImageX = fourthImageX - itemFifthImage.getBitmap().getWidth() - rightPadding;
            fifthImageY = y - (itemFifthImage.getBitmap().getHeight() >> 1);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (itemFirstImage != null)
            canvas.drawBitmap(itemFirstImage.getBitmap(), firstImageX, firstImageY, null);
        if (itemSecondImage != null)
            canvas.drawBitmap(itemSecondImage.getBitmap(), secondImageX, secondImageY, null);
        if (itemThirdImage != null)
            canvas.drawBitmap(itemThirdImage.getBitmap(), thirdImageX, thirdImageY, null);
        if (itemName != null) {
            textPaint.setColor(itemNameColor);
            setTextSize(General.getFontSize());
            textPaint.setTypeface(itemNameFont);
            canvas.drawText(itemName, textX, lineOneY, textPaint);
        }
        if (itemDesc != null) {
            textPaint.setColor(itemDescColor);
            setTextSize(General.getFontSize() - 2);
            textPaint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(TextUtils.ellipsize(itemDesc, textPaint, fifthImageX - textX, TextUtils.TruncateAt.END).toString(), textX, lineTwoY, textPaint);
        }
        if (itemFourthImage != null)
            canvas.drawBitmap(itemFourthImage.getBitmap(), fourthImageX, fourthImageY, null);
        if (itemFifthImage != null)
            canvas.drawBitmap(itemFifthImage.getBitmap(), fifthImageX, fifthImageY, null);
    }
}
