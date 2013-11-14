package ru.sawim.widget;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import ru.sawim.General;
import ru.sawim.Scheme;

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

    private static Paint textPaint;
    private static Resources resources;

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
        if (resources == null) {
            Context c = getContext();
            resources = (c == null) ? Resources.getSystem() : c.getResources();
        }
        if (textPaint == null) {
            textPaint = new Paint();
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

        int ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        int bottomPadding = getPaddingBottom();
        int topPadding = getPaddingTop();
        if (itemName != null && itemDesc != null) {
            ascent *= 2;
            descent *= 2;
        } else if (itemName == null && itemDesc != null) {
            topPadding = 0;
            bottomPadding = 0;
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
        int y = viewHeight / 2;
        int ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        textX = leftPadding;
        if (itemDesc != null) {
            lineOneY = topPadding - ascent;
        } else {
            lineOneY = y + -ascent / 2 - descent;
        }
        if (itemName != null && itemDesc != null) {
            lineTwoY = viewHeight - descent - bottomPadding;
        } else {
            lineTwoY = y + -ascent / 2 - descent;
        }

        firstImageX = leftPadding;
        if (itemFirstImage != null) {
            secondImageX = firstImageX + itemFirstImage.getBitmap().getWidth();
            firstImageY = y - itemFirstImage.getBitmap().getHeight() / 2;
            textX = secondImageX + leftPadding;
        }
        if (itemSecondImage != null) {
            secondImageX += leftPadding;
            secondImageY = y - itemSecondImage.getBitmap().getHeight() / 2;
            textX += itemSecondImage.getBitmap().getWidth() + leftPadding;
        }
        thirdImageX = secondImageX;
        if (itemThirdImage != null) {
            thirdImageX += leftPadding;
            thirdImageY = y - itemThirdImage.getBitmap().getHeight() / 2;
            textX = thirdImageX + itemThirdImage.getBitmap().getWidth() + leftPadding;
        }

        fourthImageX = viewWidth - rightPadding;
        if (itemFourthImage != null) {
            fourthImageX -= itemFourthImage.getBitmap().getWidth();
            fourthImageY = y - itemFourthImage.getBitmap().getHeight() / 2;
        }
        fifthImageX = fourthImageX - rightPadding;
        if (itemFifthImage != null) {
            fifthImageX -= itemFifthImage.getBitmap().getWidth();
            fifthImageY = y - itemFifthImage.getBitmap().getHeight() / 2;
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
            canvas.drawText(itemDesc, textX, lineTwoY, textPaint);
        }
        if (itemFourthImage != null)
            canvas.drawBitmap(itemFourthImage.getBitmap(), fourthImageX, fourthImageY, null);
        if (itemFifthImage != null)
            canvas.drawBitmap(itemFifthImage.getBitmap(), fifthImageX, fifthImageY, null);
    }
}
