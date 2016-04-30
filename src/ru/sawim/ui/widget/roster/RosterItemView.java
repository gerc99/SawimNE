package ru.sawim.ui.widget.roster;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.View;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.ui.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 03.08.13
 * Time: 13:54
 * To change this template use File | Settings | File Templates.
 */
public class RosterItemView extends View {

    private static final int PADDING_W = Util.dipToPixels(SawimApplication.getContext(), 14);
    private static final int PADDING_H = Util.dipToPixels(SawimApplication.getContext(), 15);

    public String itemName;
    public String itemDesc;
    public int itemNameColor;
    public int itemDescColor;
    public Typeface itemNameFont;

    public int avatarBorderColor = -1;

    public Bitmap itemFirstImage = null;
    public Drawable itemSecondImage = null;
    public Bitmap itemThirdImage = null;
    public Bitmap itemFourthImage = null;
    public Drawable itemFifthImage = null;
    public Bitmap itemSixthImage = null;

    private static TextPaint textPaint;
    private static Paint paintDivider;

    private int lineOneY, lineTwoY,
            firstImageX, firstImageY,
            secondImageX, secondImageY,
            thirdImageX, thirdImageY,
            fourthImageX, fourthImageY,
            fifthImageX, fifthImageY,
            sixthImageX, sixthImageY,
            textX;
    public boolean isShowDivider;

    public RosterItemView(Context context) {
        super(context);
        setPadding(PADDING_W, PADDING_H, PADDING_W, PADDING_H);
        initPaint();
        setBackgroundResource(R.drawable.list_selector);
    }

    public static void initPaint() {
        if (textPaint == null) {
            textPaint = new TextPaint();
            paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
            paintDivider.setStyle(Paint.Style.STROKE);
        }
        textPaint.setAntiAlias(true);
    }

    public void addLayer(String text) {
        itemDescColor = Scheme.getColor(R.attr.protocol_background);
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
        itemSixthImage = null;
        itemNameFont = null;
    }

    public void repaint() {
        setTextSize(SawimApplication.getFontSize());
        requestLayout();
        invalidate();
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(size * SawimApplication.getInstance().getResources().getDisplayMetrics().scaledDensity);
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
            secondImageX = firstImageX + itemFirstImage.getWidth() + leftPadding;
            firstImageY = y - (itemFirstImage.getHeight() / 2);
            textX = secondImageX;
        } else
            secondImageX = firstImageX;
        if (itemSecondImage != null) {
            thirdImageX = secondImageX + itemSecondImage.getIntrinsicWidth() + leftPadding;
            secondImageY = y - (itemSecondImage.getIntrinsicHeight() / 2);
            textX = thirdImageX;
        } else
            thirdImageX = secondImageX;
        if (itemThirdImage != null) {
            fourthImageX = thirdImageX + itemThirdImage.getWidth() + leftPadding;
            thirdImageY = y - (itemThirdImage.getHeight() / 2);
            textX = fourthImageX;
        } else
            fourthImageX = thirdImageX;
        if (itemFourthImage != null) {
            fourthImageY = y - (itemFourthImage.getHeight() / 2);
            textX = fourthImageX + itemFourthImage.getWidth() + leftPadding;
        }

        fifthImageX = viewWidth;
        if (itemFifthImage != null) {
            fifthImageX = viewWidth - itemFifthImage.getIntrinsicWidth() - rightPadding;
            fifthImageY = y - (itemFifthImage.getIntrinsicHeight() / 2);
        }
        sixthImageX = fifthImageX;
        if (itemSixthImage != null) {
            sixthImageX = fifthImageX - itemSixthImage.getWidth() - rightPadding;
            sixthImageY = y - (itemSixthImage.getHeight() / 2);
        }
    }

    private Bitmap getRoundedBitmap(Bitmap bitmap, int borderWidth, int borderColor) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        Bitmap output = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paintBorder = new Paint();
        paintBorder.setStyle(Paint.Style.STROKE);
        paintBorder.setAntiAlias(true);
        paintBorder.setStrokeWidth(borderWidth);
        paintBorder.setColor(borderColor);

        canvas.drawBitmap(bitmap, 0, 0, null);

        float halfWidth = bitmapWidth / 2;
        canvas.drawCircle(halfWidth, halfWidth, halfWidth - borderWidth / 2, paintBorder);
        return output;
    }

    @Override
    public void onDraw(@NonNull Canvas canvas) {
        if (itemFirstImage != null) {
            Bitmap bitmap;
            if (avatarBorderColor == -1) {
                bitmap = itemFirstImage;
            } else {
                bitmap = getRoundedBitmap(itemFirstImage, (int) (3 * getResources().getDisplayMetrics().density), avatarBorderColor);
            }
            canvas.drawBitmap(bitmap, firstImageX, firstImageY, null);
        }
        if (itemSecondImage != null) {
            itemSecondImage.setBounds(secondImageX, secondImageY,
                    secondImageX + itemSecondImage.getIntrinsicWidth(), secondImageY + itemSecondImage.getIntrinsicHeight());
            itemSecondImage.draw(canvas);
        }
        if (itemThirdImage != null)
            canvas.drawBitmap(itemThirdImage, thirdImageX, thirdImageY, null);
        if (itemFourthImage != null)
            canvas.drawBitmap(itemFourthImage, fourthImageX, fourthImageY, null);
        if (itemName != null) {
            textPaint.setColor(itemNameColor);
            setTextSize(SawimApplication.getFontSize());
            textPaint.setTypeface(itemNameFont);
            canvas.drawText(TextUtils.ellipsize(itemName, textPaint, fifthImageX - textX, TextUtils.TruncateAt.END).toString(), textX, lineOneY, textPaint);
        }
        if (itemDesc != null) {
            textPaint.setColor(itemDescColor);
            setTextSize(SawimApplication.getFontSize() - 2);
            textPaint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(TextUtils.ellipsize(itemDesc, textPaint, fifthImageX - textX, TextUtils.TruncateAt.END).toString(), textX, lineTwoY, textPaint);
        }
        if (itemFifthImage != null) {
            itemFifthImage.setBounds(fifthImageX, fifthImageY,
                    fifthImageX + itemFifthImage.getIntrinsicWidth(), fifthImageY + itemFifthImage.getIntrinsicHeight());
            itemFifthImage.draw(canvas);
        }
        if (itemSixthImage != null)
            canvas.drawBitmap(itemSixthImage, sixthImageX, sixthImageY, null);

        boolean isLayer = itemName == null && itemDesc != null;
        if (isShowDivider) {
            paintDivider.setColor(Scheme.getColor(R.attr.list_divider));
            paintDivider.setStrokeWidth(Util.dipToPixels(getContext(), isLayer ? 2 : 1));
            canvas.drawLine(getPaddingLeft() + (itemFirstImage == null ? 0 : itemFirstImage.getWidth()),
                    getScrollY() + getMeasuredHeight(), getWidth() - getPaddingRight(), getScrollY() + getMeasuredHeight(), paintDivider);
        }
    }
}
