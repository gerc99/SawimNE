package ru.sawim.widget.chat;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.*;
import android.text.*;
import android.view.MotionEvent;
import android.view.View;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.text.InternalURLSpan;
import ru.sawim.text.TextLinkClickListener;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MessageItemView extends View {

    private static final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private String msgTimeText;
    private String nickText;
    private int nickColor;
    private int msgTimeColor;
    private int msgTextColor;
    private Typeface nickTypeface;
    private Typeface msgTimeTypeface;
    private Typeface msgTextTypeface;
    private int nickSize;
    private int msgTimeSize;
    private int msgTextSize;
    private Bitmap checkImage;

    private int textY;

    private Layout layout;
    private CharSequence text;
    private TextLinkClickListener listener;
    private boolean isSecondTap;
    private boolean isLongTap;
    private boolean isShowDivider = false;

    private int titleHeight;
    private boolean isAddTitleView;

    public MessageItemView(Context context, boolean addTitleView) {
        super(context);
        isAddTitleView = addTitleView;
        int padding = Util.dipToPixels(context, 5);

        textPaint.setAntiAlias(true);
        textPaint.setTextSize(SawimApplication.getFontSize());
        textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));

        setPadding(padding, padding, padding, padding);
    }

    private void setTextSize(int size) {
        textPaint.setTextSize(size * getResources().getDisplayMetrics().scaledDensity);
    }

    public void makeLayout(int specSize) {
        if (text == null) return;
        try {
            layout = new StaticLayout(text, textPaint, specSize, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        } catch (ArrayIndexOutOfBoundsException e) {
            layout = new StaticLayout(text.toString(), textPaint, specSize, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = isAddTitleView ? measureHeight(heightMeasureSpec) : getPaddingTop() + getPaddingBottom();
        if (layout == null)
            makeLayout(width - getPaddingRight() - getPaddingLeft());
        titleHeight = height;
        height += layout.getLineTop(layout.getLineCount());
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

    public void setText(CharSequence text) {
        this.text = text;
    }

    public void setTextColor(int color) {
        msgTextColor = color;
    }

    public void setLinkTextColor(int color) {
        textPaint.linkColor = color;
    }

    public void setTypeface(Typeface typeface) {
        msgTextTypeface = typeface;
    }

    public void setMsgTextSize(int size) {
        msgTextSize = size;
    }

    public void repaint() {
        setTextSize(SawimApplication.getFontSize());
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int stopX = getWidth() - getPaddingRight();
        if (isShowDivider)
            canvas.drawLine(getPaddingLeft(), getScrollY() - 2, stopX, getScrollY() - 2, textPaint);

        if (isAddTitleView) {
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
                        stopX - (checkImage == null ? 0 : (checkImage.getWidth() << 1) - getPaddingLeft()), textY, textPaint);
            }
            if (checkImage != null) {
                canvas.drawBitmap(checkImage,
                        stopX - checkImage.getWidth(), getPaddingTop() + checkImage.getHeight() / 2, null);
            }
        }

        canvas.save();
        textPaint.setColor(msgTextColor);
        textPaint.setTextAlign(Paint.Align.LEFT);
        setTextSize(msgTextSize);
        textPaint.setTypeface(msgTextTypeface);
        canvas.translate(getPaddingLeft(), isAddTitleView ? titleHeight - getPaddingTop() : getPaddingTop());
        layout.draw(canvas);
        canvas.restore();
    }

    public void setShowDivider(boolean showDivider) {
        isShowDivider = showDivider;
        textPaint.setStrokeWidth((int) (4 * getResources().getDisplayMetrics().density + 0.5f));
        textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

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
            final InternalURLSpan[] firstUrlSpans = buffer.getSpans(off, off, InternalURLSpan.class);
            final InternalURLSpan[] urlSpans = buffer.getSpans(buffer.getSpanStart(firstUrlSpans), off, InternalURLSpan.class);
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                isSecondTap = true;
            }

            if (urlSpans.length != 0) {
                Runnable longPressed = new Runnable() {
                    public void run() {
                        if (listener != null && !isSecondTap) {
                            isLongTap = true;
                            listener.onTextLinkClick(MessageItemView.this, buildUrl(urlSpans), true);
                        }
                    }
                };
                if (action == MotionEvent.ACTION_DOWN) {
                    isSecondTap = false;
                    isLongTap = false;
                    postDelayed(longPressed, 700L);
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (!isLongTap) {
                        isSecondTap = true;
                        try {
                            if (listener != null)
                                listener.onTextLinkClick(MessageItemView.this, buildUrl(urlSpans), false);
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
                && urlSpans[1].clickedSpan.length() > urlSpans[0].clickedSpan.length()) {
            link = urlSpans[1].clickedSpan;
        }
        return link;
    }

    public void setOnTextLinkClickListener(TextLinkClickListener onTextLinkClickListener) {
        listener = onTextLinkClickListener;
    }
}
