package ru.sawim.widget.roster;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import ru.sawim.General;
import ru.sawim.SawimResources;
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

    public Bitmap itemFirstImage = null;
    public Bitmap itemSecondImage = null;
    public Bitmap itemThirdImage = null;
    public Bitmap itemFourthImage = null;
    public Bitmap itemFifthImage = null;

    private static TextPaint textPaint;
    private static Paint paintDivider;

    private int lineOneY, lineTwoY,
            firstImageX, firstImageY,
            secondImageX, secondImageY,
            thirdImageX, thirdImageY,
            fourthImageX, fourthImageY,
            fifthImageX, fifthImageY;
    private int textX;
    public boolean isShowDivider;

    public RosterItemView(Context context) {
        super(context);
        int paddingW = Util.dipToPixels(context, 10);
        int paddingH = Util.dipToPixels(context, 15);
        setPadding(paddingW, paddingH, paddingW, paddingH);
        initPaint();
    }

    public static void initPaint() {
        if (textPaint == null) {
            textPaint = new TextPaint();
            paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
        }
        textPaint.setAntiAlias(true);
    }

    public void addLayer(String text) {
        itemDescColor = Scheme.getColor(Scheme.THEME_PROTOCOL_BACKGROUND);
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
        setTextSize(General.getFontSize());
        requestLayout();
        invalidate();
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(size * General.getResources(getContext()).getDisplayMetrics().scaledDensity);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int ascent = (int) textPaint.ascent();
        int descent = (int) textPaint.descent();
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = measureHeight(ascent, descent, heightMeasureSpec);
        computeCoordinates(ascent, descent, width, height);
        setMeasuredDimension(width, height);
    }

    private int measureHeight(int ascent, int descent, int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        int bottomPadding = getPaddingBottom();
        int topPadding = getPaddingTop();
        if (itemName == null && itemDesc != null) {
            topPadding >>= 2;
            bottomPadding >>= 2;
        } else if (itemName != null && itemDesc != null) {
            ascent <<= 1;
            descent <<= 1;
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

    private void computeCoordinates(int ascent, int descent, int viewWidth, int viewHeight) {
        int leftPadding = getPaddingLeft();
        int rightPadding = getPaddingRight();
        int bottomPadding = getPaddingBottom();
        int topPadding = getPaddingTop();
        int y = viewHeight >> 1;
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
            secondImageX = firstImageX + itemFirstImage.getWidth();
            firstImageY = y - (itemFirstImage.getHeight() >> 1);
            textX = secondImageX + leftPadding;
        }
        if (itemSecondImage != null) {
            secondImageX += leftPadding;
            secondImageY = y - (itemSecondImage.getHeight() >> 1);
            textX += itemSecondImage.getWidth() + leftPadding;
        }
        thirdImageX = secondImageX;
        if (itemThirdImage != null) {
            thirdImageX += leftPadding;
            thirdImageY = y - (itemThirdImage.getHeight() >> 1);
            textX = thirdImageX + itemThirdImage.getWidth() + leftPadding;
        }

        fourthImageX = viewWidth;
        if (itemFourthImage != null) {
            fourthImageX = viewWidth - itemFourthImage.getWidth() - rightPadding;
            fourthImageY = y - (itemFourthImage.getHeight() >> 1);
        }
        fifthImageX = fourthImageX;
        if (itemFifthImage != null) {
            fifthImageX = fourthImageX - itemFifthImage.getWidth() - rightPadding;
            fifthImageY = y - (itemFifthImage.getHeight() >> 1);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (itemFirstImage != null)
            canvas.drawBitmap(itemFirstImage, firstImageX, firstImageY, null);
        if (itemSecondImage != null)
            canvas.drawBitmap(itemSecondImage, secondImageX, secondImageY, null);
        if (itemThirdImage != null)
            canvas.drawBitmap(itemThirdImage, thirdImageX, thirdImageY, null);
        if (itemName != null) {
            textPaint.setColor(itemNameColor);
            setTextSize(General.getFontSize());
            textPaint.setTypeface(itemNameFont);
            canvas.drawText(TextUtils.ellipsize(itemName, textPaint, fifthImageX - textX, TextUtils.TruncateAt.END).toString(), textX, lineOneY, textPaint);
        }
        if (itemDesc != null) {
            textPaint.setColor(itemDescColor);
            setTextSize(General.getFontSize() - 2);
            textPaint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(TextUtils.ellipsize(itemDesc, textPaint, fifthImageX - textX, TextUtils.TruncateAt.END).toString(), textX, lineTwoY, textPaint);
        }
        if (itemFourthImage != null)
            canvas.drawBitmap(itemFourthImage, fourthImageX, fourthImageY, null);
        if (itemFifthImage != null)
            canvas.drawBitmap(itemFifthImage, fifthImageX, fifthImageY, null);

        boolean isLayer = itemName == null && itemDesc != null;
        if (isShowDivider) {
            paintDivider.setStrokeWidth((isLayer ? 4 : 1) * General.getResources(getContext()).getDisplayMetrics().scaledDensity);
            paintDivider.setColor(Scheme.isBlack() ? Scheme.DIVIDER_BLACK : Scheme.DIVIDER_LIGHT);
            canvas.drawLine(getPaddingLeft(), getScrollY() + getHeight(), getWidth() - getPaddingRight(), getScrollY() + getHeight(), paintDivider);
        }
    }
}
