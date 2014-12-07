package ru.sawim.widget.chat;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.widget.IcsLinearLayout;
import ru.sawim.widget.SimpleItemView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:33
 * To change this template use File | Settings | File Templates.
 */
public class ChatBarView extends IcsLinearLayout {

    private static final int ITEM_VIEW_INDEX = 0;
    private static final int CHATS_IMAGE_INDEX = 1;

    SimpleItemView itemView;

    public ChatBarView(Context context, View chatsImage) {
        super(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setOrientation(HORIZONTAL);
        if (SawimApplication.isManyPane())
            layoutParams.weight = 1;
        setLayoutParams(layoutParams);
        setDividerPadding(10);

        LinearLayout.LayoutParams labelLayoutParams = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        labelLayoutParams.weight = 1;

        itemView = new SimpleItemView(context);
        int padding = ru.sawim.widget.Util.dipToPixels(context, 3);
        itemView.setPadding(padding, padding, padding, padding);
        addViewInLayout(itemView, ITEM_VIEW_INDEX, labelLayoutParams);

        if (!SawimApplication.isManyPane()) {
            LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            chatsImageLP.gravity = Gravity.CENTER_VERTICAL;
            addViewInLayout(chatsImage, CHATS_IMAGE_INDEX, chatsImageLP);
        }
    }

    public void update() {
        setDividerDrawable(SawimResources.listDivider);
    }

    public void setVisibilityChatsImage(int visibility) {
        if (getChildAt(CHATS_IMAGE_INDEX) != null)
            getChildAt(CHATS_IMAGE_INDEX).setVisibility(visibility);
    }

    public void updateTextView(String text) {
        itemView.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        itemView.setTextSize(SawimApplication.getFontSize());
        itemView.setText(text);
    }

    public void updateLabelIcon(BitmapDrawable drawable) {
        if (drawable == null)
            itemView.setImage(null);
        else
            itemView.setImage(drawable.getBitmap());
    }
}