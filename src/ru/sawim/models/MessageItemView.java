package ru.sawim.models;

import android.content.Context;
import android.view.View;
import android.widget.*;
import ru.sawim.view.MyTextView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 25.08.13
 * Time: 20:37
 * To change this template use File | Settings | File Templates.
 */
public class MessageItemView extends RelativeLayout {

    public ImageView msgImage;
    public MyTextView msgNick;
    public TextView msgTime;
    public MyTextView msgText;

    public MessageItemView(Context context) {
        super(context);
        msgImage = new ImageView(context);
        msgNick = new MyTextView(context);
        msgTime = new TextView(context);
        msgText = new MyTextView(context);

        setAnimationCacheEnabled(false);
        setAlwaysDrawnWithCacheEnabled(false);
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
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
        msgNick.setLines(1);
        msgNick.setId(2);
        addView(msgNick, lp);

        lp = new RelativeLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.addRule(BELOW, msgNick.getId());
        msgText.setId(4);
        msgText.setScrollContainer(false);
        addView(msgText, lp);
    }

    public void addDivider(Context context, boolean isAdd, int color) {
        View v = new View(context);
        v.setId(5);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        v.setBackgroundColor(color);
        if (getChildAt(4) != null) removeViewAt(4);
        if (isAdd) addView(v);
    }
}
