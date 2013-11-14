package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.*;
import ru.sawim.widget.MyTextView;
import ru.sawim.widget.chat.MessageTitleItemView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MessageItemView extends LinearLayout {

    public MessageTitleItemView titleItemView;
    public MyTextView msgText;
    private static final Paint paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isShowDivider = false;

    public MessageItemView(Context context, boolean isAddTitleView) {
        super(context);
        setPadding(5, 5, 5, 5);
        setOrientation(VERTICAL);
        titleItemView = new MessageTitleItemView(context);
        msgText = new MyTextView(context);

        if (isAddTitleView)
            addViewInLayout(titleItemView, 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        msgText.setScrollContainer(false);
        addViewInLayout(msgText, isAddTitleView ? 1 : 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    public void initDivider(int color) {
        paintDivider.setColor(color);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (isShowDivider)
            canvas.drawLine(getLeft(), getScrollY(), getWidth(), getScrollY(), paintDivider);
    }

    public void setShowDivider(boolean showDivider) {
        isShowDivider = showDivider;
    }
}
