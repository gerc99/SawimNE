package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.TypedValue;
import android.view.View;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import ru.sawim.General;
import ru.sawim.Scheme;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.modules.tracking.Tracking;
import sawim.roster.Roster;

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
    private int itemNameColor;
    private int itemDescColor;
    private Typeface itemNameFont;
    private static Paint textPaint;
    public BitmapDrawable itemFirstImage = null;
    public BitmapDrawable itemSecondImage = null;
    public BitmapDrawable itemThirdImage = null;
    public BitmapDrawable itemFourthImage = null;
    public BitmapDrawable itemFifthImage = null;

    private int lineOneY;
    private int lineTwoY;
    private int firstImageX = 0;
    private int firstImageY = 0;
    private int secondImageX = 0;
    private int secondImageY = 0;
    private int thirdImageX = 0;
    private int thirdImageY = 0;
    private int fourthImageX = 0;
    private int fourthImageY = 0;
    private int fifthImageX = 0;
    private int fifthImageY = 0;
    private int textX = 0;

    public RosterItemView(Context context) {
        super(context);
        setPadding(10, 15, 10, 15);
        if (textPaint == null) {
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(16);
            textPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
        }
    }

    void populateFromGroup(Group g) {
        setNull();
        itemNameColor = Scheme.getColor(Scheme.THEME_GROUP);
        itemNameFont = Typeface.DEFAULT;
        itemName = g.getText();

        Icon icGroup = g.getLeftIcon(null);
        if (icGroup != null)
            itemFirstImage = icGroup.getImage();

        Icon messIcon = ChatHistory.instance.getUnreadMessageIcon(g.getContacts());
        if (!g.isExpanded() && messIcon != null)
            itemFourthImage = messIcon.getImage();
    }

    void populateFromContact(Roster roster, Protocol p, Contact item) {
        setNull();
        itemNameColor = Scheme.getColor(item.getTextTheme());
        itemNameFont = item.hasChat() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT;
        itemName = (item.subcontactsS() == 0) ?
                item.getText() : item.getText() + " (" + item.subcontactsS() + ")";
        if (General.showStatusLine) {
            String statusMessage = roster.getStatusMessage(item);
            itemDescColor = Scheme.getColor(Scheme.THEME_CONTACT_STATUS);
            itemDesc = statusMessage;
        }

        Icon icStatus = item.getLeftIcon(p);
        if (icStatus != null)
            itemFirstImage = icStatus.getImage();
        if (item.isTyping()) {
            itemFirstImage = Message.msgIcons.iconAt(Message.ICON_TYPE).getImage();
        } else {
            Icon icMess = Message.msgIcons.iconAt(item.getUnreadMessageIcon());
            if (icMess != null)
                itemFirstImage = icMess.getImage();
        }

        if (item.getXStatusIndex() != XStatusInfo.XSTATUS_NONE)
            itemSecondImage = p.getXStatusInfo().getIcon(item.getXStatusIndex()).getImage();

        if (!item.isTemp()) {
            Icon icAuth = item.authIcon.iconAt(0);
            if (item.isAuth()) {
                int privacyList = -1;
                if (item.inIgnoreList()) {
                    privacyList = 0;
                } else if (item.inInvisibleList()) {
                    privacyList = 1;
                } else if (item.inVisibleList()) {
                    privacyList = 2;
                }
                if (privacyList != -1)
                    itemThirdImage = item.serverListsIcons.iconAt(privacyList).getImage();
            } else {
                itemThirdImage = icAuth.getImage();
            }
        }

        Icon icClient = (null != p.clientInfo) ? p.clientInfo.getIcon(item.clientIndex) : null;
        if (icClient != null && !General.hideIconsClient)
            itemFourthImage = icClient.getImage();

        String id = item.getUserId();
        if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE)
            itemFifthImage = (BitmapDrawable) Tracking.getTrackIcon(id);
    }

    public void addLayer(String text) {
        setNull();
        itemDescColor = Scheme.getColor(Scheme.THEME_GROUP);
        itemDesc = text;
    }

    private void setNull() {
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
        Context c = getContext();
        Resources r = (c == null) ? Resources.getSystem() : c.getResources();
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, r.getDisplayMetrics()));
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

        int ascent = (int) textPaint.ascent() * 2;
        int descent = (int) textPaint.descent() * 2;
        if (itemName == null && itemDesc != null) {
            ascent = (int) textPaint.ascent();
            descent = (int) textPaint.descent();
        }
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = (-ascent + descent) + getPaddingTop()
                    + getPaddingBottom();
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
        int bottomPadding = getPaddingBottom();
        int topPadding = getPaddingTop();
        int y = viewHeight / 2;
        int descent = (int) textPaint.descent();

        textX = leftPadding;
        if (itemDesc != null) {
            lineOneY = topPadding - (int) textPaint.ascent();
        } else {
            lineOneY = viewHeight - descent - bottomPadding - topPadding;
        }
        lineTwoY = viewHeight - descent - bottomPadding;

        firstImageX = getPaddingLeft();
        if (itemFirstImage != null) {
            secondImageX = firstImageX + itemFirstImage.getBitmap().getWidth() - getPaddingLeft();
            firstImageY = y - itemFirstImage.getBitmap().getHeight() / 2;
            textX = firstImageX + itemFirstImage.getBitmap().getWidth() + getPaddingLeft();
        }
        if (itemSecondImage != null) {
            secondImageX += getPaddingLeft();
            secondImageY = y - itemSecondImage.getBitmap().getHeight() / 2;
            textX = secondImageX + itemSecondImage.getBitmap().getWidth() + getPaddingLeft();
        }
        thirdImageX = secondImageX;
        if (itemThirdImage != null) {
            thirdImageX += getPaddingLeft();
            thirdImageY = y - itemThirdImage.getBitmap().getHeight() / 2;
            textX = thirdImageX + itemThirdImage.getBitmap().getWidth() + getPaddingLeft();
        }

        fourthImageX = viewWidth - getPaddingRight();
        if (itemFourthImage != null) {
            fourthImageX -= itemFourthImage.getBitmap().getWidth();
            fourthImageY = y - itemFourthImage.getBitmap().getHeight() / 2;
        }
        fifthImageX = fourthImageX - getPaddingRight();
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
