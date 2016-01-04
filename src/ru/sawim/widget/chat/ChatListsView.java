package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.text.TextPaint;
import android.widget.LinearLayout;

import ru.sawim.R;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.widget.IcsLinearLayout;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:34
 * To change this template use File | Settings | File Templates.
 */
public class ChatListsView extends IcsLinearLayout {

    private static final int CHAT_LIST_VIEW_INDEX = 0;
    private static final int NICKS_LIST_VIEW_INDEX = 1;

    private static final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private boolean isShowDividerForUnreadMessage = false;

    public ChatListsView(Context context, boolean isTablet, RecyclerView chatListView, RecyclerView nickList) {
        super(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setOrientation(HORIZONTAL);
        layoutParams.weight = 2;
        setLayoutParams(layoutParams);
        LinearLayout.LayoutParams chatListViewLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        if (isTablet)
            chatListViewLayoutParams.weight = 1;
        addViewInLayout(chatListView, CHAT_LIST_VIEW_INDEX, chatListViewLayoutParams);

        if (isTablet) {
            LinearLayout.LayoutParams nickListViewLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            nickListViewLayoutParams.weight = 3;
            addViewInLayout(nickList, NICKS_LIST_VIEW_INDEX, nickListViewLayoutParams);
        }
        textPaint.setStrokeWidth(Util.dipToPixels(context, 5));
    }

    public MyListView getChatListView() {
        return (MyListView) getChildAt(CHAT_LIST_VIEW_INDEX);
    }

    public void update() {
        setDividerDrawable(SawimResources.listDivider);
    }

    public void setShowDividerForUnreadMessage(boolean isShowDividerForUnreadMessage) {
        this.isShowDividerForUnreadMessage = isShowDividerForUnreadMessage;
        repaint();
    }

    public void repaint() {
        requestLayout();
        invalidate();
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (isShowDividerForUnreadMessage) {
            textPaint.setColor(Scheme.getColor(R.attr.unread_message_divider));
            canvas.drawLine(getPaddingLeft(), getHeight(), getChildAt(CHAT_LIST_VIEW_INDEX).getWidth() - getPaddingRight(), getHeight(), textPaint);
        }
    }
}
