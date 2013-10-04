package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
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
    public BitmapDrawable itemFirstImage = null;
    public BitmapDrawable itemSecondImage = null;
    public BitmapDrawable itemThirdImage = null;
    public BitmapDrawable itemFourthImage = null;
    public BitmapDrawable itemFifthImage = null;
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

    private static Paint mTextPaint;
    private int mAscent;
    private int mDescent;

    public RosterItemView(Context context) {
        super(context);
        initView();
        setPadding(10, 20, 10, 20);
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

    private final void initView() {
        if (mTextPaint == null) {
            mTextPaint = new Paint();
            mTextPaint.setAntiAlias(true);
            mTextPaint.setTextSize(16);
            mTextPaint.setColor(Scheme.getColor(Scheme.THEME_TEXT));
        }
    }

    private void setNull() {
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
        mTextPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, size, r.getDisplayMetrics()));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = measureHeight(heightMeasureSpec);
        measureItem(width, height);
        setMeasuredDimension(width, height);
    }

    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        mAscent = (int) mTextPaint.ascent();
        mDescent = (int) mTextPaint.descent();
        if (itemDesc != null) {
            mAscent *= 2;
            mDescent *= 2;
        }
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        } else {
            result = (-mAscent + mDescent) + getPaddingTop()
                    + getPaddingBottom();
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    private void measureItem(int width, int height) {
        if (itemFirstImage != null) {
            firstImageX = getPaddingLeft();
            firstImageY = height / 2 - itemFirstImage.getBitmap().getHeight() / 2;
            secondImageX = itemFirstImage.getBitmap().getWidth() + getPaddingRight() + getPaddingLeft();
            textX = secondImageX + getPaddingLeft();
        }
        if (itemSecondImage != null) {
            secondImageX += itemSecondImage.getBitmap().getWidth() - getPaddingRight() - getPaddingLeft();
            secondImageY = height / 2 - itemSecondImage.getBitmap().getHeight() / 2;
            textX += itemSecondImage.getBitmap().getWidth() + getPaddingRight() + getPaddingLeft();
        }
        thirdImageX = secondImageX + getPaddingRight();
        if (itemThirdImage != null) {
            thirdImageX += itemThirdImage.getBitmap().getWidth() - getPaddingRight() - getPaddingLeft();
            thirdImageY = height / 2 - itemThirdImage.getBitmap().getHeight() / 2;
            textX += itemThirdImage.getBitmap().getWidth() + getPaddingRight() + getPaddingLeft();
        }

        fourthImageX = width - getPaddingRight();
        if (itemFourthImage != null) {
            fourthImageX -= itemFourthImage.getBitmap().getWidth();
            fourthImageY = height / 2 - itemFourthImage.getBitmap().getHeight() / 2;
        }
        fifthImageX = fourthImageX - getPaddingRight();
        if (itemFifthImage != null) {
            fifthImageX -= itemFifthImage.getBitmap().getWidth();
            fifthImageY = height / 2 - itemFifthImage.getBitmap().getHeight() / 2;
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
            mTextPaint.setColor(itemNameColor);
            setTextSize(General.getFontSize());
            mTextPaint.setTypeface(itemNameFont);
            if (itemDesc != null)
                canvas.drawText(itemName, textX, getPaddingTop() + mDescent + getPaddingTop() / 2, mTextPaint);
            else
                canvas.drawText(itemName, textX, getPaddingTop() - mAscent, mTextPaint);
        }
        if (itemDesc != null) {
            mTextPaint.setColor(itemDescColor);
            setTextSize(General.getFontSize() - 2);
            mTextPaint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(itemDesc, textX, getPaddingBottom() - mAscent + getPaddingBottom() / 2, mTextPaint);
        }
        if (itemFourthImage != null)
            canvas.drawBitmap(itemFourthImage.getBitmap(), fourthImageX, fourthImageY, null);
        if (itemFifthImage != null)
            canvas.drawBitmap(itemFifthImage.getBitmap(), fifthImageX, fifthImageY, null);
    }
}
