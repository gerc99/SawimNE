package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.LinearLayout;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MessageItemView extends LinearLayout {

    public MessageTitleItemView titleItemView;
    public MessageTextView msgText;
    private static final Paint paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isShowDivider = false;

    public MessageItemView(Context context, boolean isAddTitleView) {
        super(context);
        titleItemView = new MessageTitleItemView(context);
        msgText = new MessageTextView(context);
        int padding = Util.dipToPixels(context, 5);
        setPadding(padding, 0, padding, 0);
        setOrientation(VERTICAL);
        setPadding(padding, padding, padding, padding);
        titleItemView.setPadding(0, padding, padding, padding);
        msgText.setPadding(0, 0, 0, padding);

        if (isAddTitleView)
            addViewInLayout(titleItemView, 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        addViewInLayout(msgText, isAddTitleView ? 1 : 0, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (isShowDivider)
            canvas.drawLine(getLeft(), getScrollY(), getWidth(), getScrollY(), paintDivider);
    }

    public void setShowDivider(int color, boolean showDivider) {
        paintDivider.setColor(color);
        isShowDivider = showDivider;
    }
}
