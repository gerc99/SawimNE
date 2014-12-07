package ru.sawim.widget.chat;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.widget.IcsLinearLayout;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:31
 * To change this template use File | Settings | File Templates.
 */
public class ChatViewRoot extends IcsLinearLayout {

    private static final int CHAT_LISTS_VIEW_INDEX = 0;
    private static final int CHAT_INPUT_BAR_VIEW_INDEX = 1;
    private static final int HINT_VIEW_INDEX = 2;

    public ChatViewRoot(Context context, View chatListsView, View chatInputBarView) {
        super(context);
        setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        setLayoutParams(layoutParams);
        addViewInLayout(chatListsView, CHAT_LISTS_VIEW_INDEX, chatListsView.getLayoutParams());
        addViewInLayout(chatInputBarView, CHAT_INPUT_BAR_VIEW_INDEX, chatInputBarView.getLayoutParams());
    }

    public void update() {
        setDividerDrawable(SawimResources.listDivider);
    }

    public void showHint() {
        hideHint();
        TextView hint = new TextView(getContext());
        hint.setTextColor(Scheme.getColor(Scheme.THEME_TEXT));
        hint.setTextSize(SawimApplication.getFontSize());
        hint.setGravity(Gravity.CENTER);
        hint.setText(R.string.select_contact);
        addView(hint, HINT_VIEW_INDEX);
    }

    public void hideHint() {
        if (getChildAt(HINT_VIEW_INDEX) != null) removeViewAt(HINT_VIEW_INDEX);
    }
}
