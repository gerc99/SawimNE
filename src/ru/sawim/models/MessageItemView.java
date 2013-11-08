package ru.sawim.models;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;
import android.widget.*;
import ru.sawim.widget.LabelView;
import ru.sawim.widget.MyTextView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MessageItemView extends RelativeLayout {

    public ImageView msgImage;
    public LabelView msgNick;
    public LabelView msgTime;
    public MyTextView msgText;
    private static final Paint paintDivider = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean isShowDivider = false;

    public MessageItemView(Context context) {
        super(context);
        setPadding(5, 0, 5, 0);
        msgImage = new ImageView(context);
        msgNick = new LabelView(context);
        msgTime = new LabelView(context);
        msgText = new MyTextView(context);

        setAnimationCacheEnabled(false);
        setAlwaysDrawnWithCacheEnabled(false);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.alignWithParent = false;
        lp.addRule(ALIGN_PARENT_LEFT);
        lp.addRule(ALIGN_PARENT_TOP);
        msgImage.setId(1);
        addView(msgImage, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(ALIGN_PARENT_RIGHT);
        lp.addRule(ALIGN_PARENT_TOP);
        msgTime.setId(3);
        addView(msgTime, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(LEFT_OF, msgTime.getId());
        lp.addRule(RIGHT_OF, msgImage.getId());
        msgNick.setId(2);
        addView(msgNick, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        //if (msgImage.getVisibility() == ImageView.GONE)
        //    lp.addRule(BELOW, msgImage.getId());
        //else
            lp.addRule(BELOW, msgNick.getId());
        lp.addRule(ALIGN_PARENT_BOTTOM);
        msgText.setId(4);
        msgText.setScrollContainer(false);
        addView(msgText, lp);
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
