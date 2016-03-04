package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.*;
import android.text.style.BackgroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;

import java.util.List;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.text.InternalURLSpan;
import ru.sawim.text.TextLinkClickListener;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MessageItemView extends View {

    private static final TextPaint messageTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private static final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    static Paint highlightPaint;

    public static final int PADDING_LEFT = Util.dipToPixels(SawimApplication.getContext(), 9);
    public static final int PADDING_TOP = Util.dipToPixels(SawimApplication.getContext(), 10);
    public static final int PADDING_RIGHT = Util.dipToPixels(SawimApplication.getContext(), 20);
    public static final int PADDING_BOTTOM = Util.dipToPixels(SawimApplication.getContext(), 10);

    public static final int BACKGROUND_CORNER = Util.dipToPixels(SawimApplication.getContext(), 7);

    public static final int BACKGROUND_NONE = 0;
    public static final int BACKGROUND_INCOMING = 1;
    public static final int BACKGROUND_OUTCOMING = 2;

    private int backgroundIndex = BACKGROUND_NONE;
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
    private Bitmap image;

    private int textY;

    private Layout layout;
    private TextLinkClickListener textLinkClickListener;
    private boolean isSecondTap;
    private boolean isLongTap;
    private boolean isShowDivider = false;
    private int titleHeight;

    public MessageItemView(Context context) {
        super(context);
        textPaint.setAntiAlias(true);

        if (highlightPaint == null) {
            highlightPaint = new Paint();
            highlightPaint.setStyle(Paint.Style.FILL);
            highlightPaint.setAntiAlias(true);
        }
        highlightPaint.setColor(Scheme.isBlack() ? 0xFF4C4C4C : Color.WHITE);
        Util.setSelectableItemBackground(this);
    }

    public void setTextSize(int size) {
        textPaint.setTextSize(size * getResources().getDisplayMetrics().scaledDensity);
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public static Layout buildLayout(CharSequence parsedText, Typeface msgTextTypeface) {
        return makeLayout(parsedText, msgTextTypeface, getMessageWidth());
    }

    public static int getMessageWidth() {
        return SawimApplication.getContext().getResources().getDisplayMetrics().widthPixels - (PADDING_LEFT * 2 + PADDING_RIGHT);
    }

    public static Layout makeLayout(CharSequence parsedText, Typeface msgTextTypeface, int width) {
        if (width <= 0) return null;
        DisplayMetrics displayMetrics = SawimApplication.getContext().getResources().getDisplayMetrics();
        messageTextPaint.setAntiAlias(true);
        messageTextPaint.linkColor = Scheme.getColor(R.attr.link);
        messageTextPaint.setTextAlign(Paint.Align.LEFT);
        messageTextPaint.setTextSize(SawimApplication.getFontSize() * displayMetrics.scaledDensity);
        messageTextPaint.setTypeface(msgTextTypeface);
        try {
            return new StaticLayout(parsedText, messageTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        } catch (ArrayIndexOutOfBoundsException e) {
            return new StaticLayout(parsedText.toString(), messageTextPaint, width, Layout.Alignment.ALIGN_NORMAL, 1.0F, 0.0F, false);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        boolean isAddTitleView = nickText != null;
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = isAddTitleView ? measureHeight(heightMeasureSpec) : getPaddingTop() + getPaddingBottom();
        int layoutWidth = width - getPaddingRight() - getPaddingLeft() * 2;
        if (layout.getWidth() != layoutWidth) {
            layout = makeLayout(layout.getText(), msgTextTypeface, layoutWidth);
        }
        titleHeight = isAddTitleView ? height - getPaddingTop() : getPaddingTop();
        if (layout != null)
            height += layout.getLineTop(layout.getLineCount());
        if (image != null) {
            height += image.getHeight();
        }
        setMeasuredDimension(width, height);
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        int ascent = (int) messageTextPaint.ascent();
        int descent = (int) messageTextPaint.descent();
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
        textY = getPaddingTop() - (int) messageTextPaint.ascent();
    }

    public void setBackgroundIndex(int index) {
        this.backgroundIndex = index;
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

    public void setTextColor(int color) {
        msgTextColor = color;
    }

    public void setLinks(List<String> links) {
        image = null;
        repaint();
        invalidate();

        String imageLink = getFirstImageLink(links);
        if (imageLink == null) {
            return;
        }

        Glide.with(getContext()).load(imageLink).asBitmap()
             .override(getMessageWidth(), getMessageWidth()).fitCenter()
             .into(new SimpleTarget<Bitmap>() {
                 @Override
                 public void onResourceReady(Bitmap resource, GlideAnimation glideAnimation) {
                     image = resource;
                     repaint();
                     invalidate();
                 }
             });
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
        requestLayout();
        //invalidate();
    }

    @Override
    public void buildDrawingCache(boolean autoScale) {
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int stopX = width - getPaddingRight();
        if (isShowDivider) {
            textPaint.setColor(Scheme.getColor(R.attr.text));
            canvas.drawLine(getPaddingLeft(), getScrollY() - 2, stopX, getScrollY() - 2, textPaint);
        }
        if (backgroundIndex == BACKGROUND_INCOMING) {
            //setDrawableBounds(SawimResources.backgroundDrawableIn, 0, getPaddingTop() / 2, width - getPaddingRight() / 2, getHeight() - getPaddingBottom() / 2);
            //SawimResources.backgroundDrawableIn.draw(canvas);
            canvas.drawRoundRect(new RectF(getPaddingLeft(), getPaddingTop(), width - getPaddingRight() / 2, getHeight()), BACKGROUND_CORNER, BACKGROUND_CORNER, highlightPaint);
        } else if (backgroundIndex == BACKGROUND_OUTCOMING) {
            //setDrawableBounds(SawimResources.backgroundDrawableOut, getPaddingLeft() / 2, getPaddingTop() / 2, width - getPaddingRight() / 2, getHeight() - getPaddingBottom() / 2);
            //SawimResources.backgroundDrawableOut.draw(canvas);
            canvas.drawRoundRect(new RectF((float) (getPaddingLeft() * 1.5), getPaddingTop(), width - getPaddingRight() / 2, getHeight()), BACKGROUND_CORNER, BACKGROUND_CORNER, highlightPaint);
        }

        if (nickText != null) {
            textPaint.setColor(nickColor);
            textPaint.setTextAlign(Paint.Align.LEFT);
            setTextSize(nickSize);
            textPaint.setTypeface(nickTypeface);
            canvas.drawText(nickText, getPaddingLeft() * 2, textY + getPaddingTop() / 2, textPaint);
        }

        if (msgTimeText != null) {
            textPaint.setColor(msgTimeColor);
            textPaint.setTextAlign(Paint.Align.RIGHT);
            setTextSize(msgTimeSize);
            textPaint.setTypeface(msgTimeTypeface);
            canvas.drawText(msgTimeText,
                    stopX - (checkImage == null ? 0 : checkImage.getWidth() << 1), textY, textPaint);
        }
        if (checkImage != null) {
            canvas.drawBitmap(checkImage,
                    stopX - checkImage.getWidth(), getPaddingTop() + checkImage.getHeight() / 2, null);
        }

        if (image != null) {
            canvas.drawBitmap(image, getWidth() / 2 - image.getWidth() / 2, titleHeight + getPaddingTop() / 2, null);
        }
        if (layout != null) {
            canvas.save();
            messageTextPaint.setColor(msgTextColor);
            messageTextPaint.setTextAlign(Paint.Align.LEFT);
            messageTextPaint.setTextSize(msgTextSize * getResources().getDisplayMetrics().scaledDensity);
            messageTextPaint.setTypeface(msgTextTypeface);
            int y = titleHeight + getPaddingTop() / 2;
            if (image != null) {
                y += image.getHeight();
            }
            canvas.translate(getPaddingLeft() * 2, y);
            layout.draw(canvas);
            canvas.restore();
        }
    }

    public void setShowDivider(boolean showDivider) {
        isShowDivider = showDivider;
        textPaint.setStrokeWidth(Util.dipToPixels(getContext(), 5));
    }

    private void setDrawableBounds(Drawable drawable, int x, int y, int w, int h) {
        drawable.setBounds(x, y, x + w, y + h);
    }

    @Override
    public boolean hasFocusable() {
        return false;
    }

    private int getLineForVertical(int vertical) {
        int high = layout.getLineCount(), low = -1, guess;
        while (high - low > 1) {
            guess = (high + low) / 2;
            if (layout.getLineTop(guess) > vertical)
                high = guess;
            else
                low = guess;
        }
        return low;
    }

    private static final BackgroundColorSpan linkHighlightColor = new BackgroundColorSpan(Scheme.getColor(R.attr.link_highlight));
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (layout.getText() == null) return super.onTouchEvent(event);
        if (layout.getText() instanceof Spannable) {
            final Spannable buffer = (Spannable) layout.getText();
            int action = event.getAction();
            int x = (int) event.getX();
            int y = (int) event.getY();
            x += getScrollX();
            y += getScrollY() - titleHeight;
            int line = getLineForVertical(y);
            if (line < 0) return super.onTouchEvent(event);

            int off = layout.getOffsetForHorizontal(line, x);
            final InternalURLSpan[] urlSpans = buffer.getSpans(off, off, InternalURLSpan.class);
            if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_OUTSIDE) {
                isSecondTap = true;
            }
            if (urlSpans.length != 0) {
                Runnable longPressed = new Runnable() {
                    public void run() {
                        if (textLinkClickListener != null && !isSecondTap && !isLongTap) {
                            getParent().requestDisallowInterceptTouchEvent(true);
                            isLongTap = true;
                            textLinkClickListener.onTextLinkClick(MessageItemView.this, buildUrl(urlSpans), true);
                        }
                    }
                };
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_CANCEL) {
                    buffer.removeSpan(linkHighlightColor);
                    removeCallbacks(longPressed);
                    repaint();
                }
                if (action == MotionEvent.ACTION_DOWN) {
                    isSecondTap = false;
                    isLongTap = false;
                    buffer.setSpan(linkHighlightColor,
                            buffer.getSpanStart(urlSpans[0]),
                            buffer.getSpanEnd(urlSpans[0]), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    repaint();
                    removeCallbacks(longPressed);
                    postDelayed(longPressed, ViewConfiguration.getLongPressTimeout());
                }
                if (action == MotionEvent.ACTION_UP) {
                    if (!isLongTap) {
                        isSecondTap = true;
                        if (textLinkClickListener != null)
                            textLinkClickListener.onTextLinkClick(MessageItemView.this, buildUrl(urlSpans), false);
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

    private String getFirstImageLink(List<String> links) {
        if (links == null || links.isEmpty()) {
            return null;
        }
        for (String link : links) {
            if (ru.sawim.comm.Util.isImageFile(link)) {
                return link;
            }
        }
        return links.get(0);
    }

    public void setOnTextLinkClickListener(TextLinkClickListener listener) {
        textLinkClickListener = listener;
    }
}
