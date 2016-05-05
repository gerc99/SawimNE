package ru.sawim.ui.widget.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.ui.adapter.list.VirtualList;
import ru.sawim.ui.widget.AnimationUtils;
import ru.sawim.ui.widget.IcsLinearLayout;
import ru.sawim.ui.widget.MyImageButton;
import ru.sawim.ui.widget.SearchEditText;
import ru.sawim.ui.widget.Util;
import ru.sawim.ui.widget.SimpleItemView;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:33
 * To change this template use File | Settings | File Templates.
 */
@SuppressLint("NewApi")
public class ChatBarView extends IcsLinearLayout {

    private static final int ITEM_VIEW_INDEX = 0;
    private static final int CHATS_IMAGE_INDEX = 1;
    private static final int SEARCH_EDITTEXT_VIEW_INDEX = 2;
    private static final int SEARCH_UP_IMAGE_INDEX = 3;
    private static final int SEARCH_DOWN_IMAGE_INDEX = 4;

    SimpleItemView itemView;

    public ChatBarView(Context context, MyImageButton chatsImage) {
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
        int padding = Util.dipToPixels(context, 3);
        itemView.setPadding(padding * 2, padding, padding, padding);
        addViewInLayout(itemView, ITEM_VIEW_INDEX, labelLayoutParams);

        if (!SawimApplication.isManyPane()) {
            LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            chatsImageLP.gravity = Gravity.CENTER_VERTICAL;
            addViewInLayout(chatsImage, CHATS_IMAGE_INDEX, chatsImageLP);
        }
    }

    public MyImageButton getChatsImage() {
        return (MyImageButton) getChildAt(CHATS_IMAGE_INDEX);
    }

    public SearchEditText getSearchEditText() {
        return (SearchEditText) getChildAt(SEARCH_EDITTEXT_VIEW_INDEX);
    }

    public void update() {
        setDividerDrawable(SawimResources.listDivider);
    }

    public void setVisibilityChatsImage(int visibility) {
        if (getChatsImage() != null)
            getChatsImage().setVisibility(visibility);
    }

    public void showSearch(OnClickListener upBtnClickListener, OnClickListener downBtnClickListener) {
        setVisibilityChatsImage(GONE);
        if (itemView != null)
            itemView.setVisibility(GONE);
        SearchEditText messageEditor = new SearchEditText(getContext());
        LinearLayout.LayoutParams messageEditorLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        messageEditor.setBackgroundColor(Color.TRANSPARENT);
        messageEditor.setHint("Search");
        messageEditor.setHintTextColor(Color.LTGRAY);
        messageEditor.setTextColor(Color.WHITE);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(messageEditor, R.drawable.search_carret);
        } catch (Exception e) {
            //nothing to do
        }
        messageEditorLP.weight = 1;
        if (getChildAt(SEARCH_EDITTEXT_VIEW_INDEX) == null) {
            addViewInLayout(messageEditor, SEARCH_EDITTEXT_VIEW_INDEX, messageEditorLP);
        }
        AnimationUtils.circleRevealView(messageEditor);
        messageEditor.requestFocus();

        MyImageButton upBtn = new MyImageButton(getContext());
        upBtn.setImageResource(R.drawable.ic_collapsed_dark);
        upBtn.setOnClickListener(upBtnClickListener);
        if (getChildAt(SEARCH_UP_IMAGE_INDEX) == null) {
            addViewInLayout(upBtn, SEARCH_UP_IMAGE_INDEX, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
        MyImageButton downBtn = new MyImageButton(getContext());
        downBtn.setImageResource(R.drawable.ic_expanded_dark);
        downBtn.setOnClickListener(downBtnClickListener);
        if (getChildAt(SEARCH_DOWN_IMAGE_INDEX) == null) {
            addViewInLayout(downBtn, SEARCH_DOWN_IMAGE_INDEX, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        }
    }

    public void hideSearch() {
        setVisibilityChatsImage(VISIBLE);
        if (itemView != null)
            itemView.setVisibility(VISIBLE);
        getChildAt(SEARCH_EDITTEXT_VIEW_INDEX).setVisibility(GONE);
        getChildAt(SEARCH_UP_IMAGE_INDEX).setVisibility(GONE);
        getChildAt(SEARCH_DOWN_IMAGE_INDEX).setVisibility(GONE);
    }

    public void updateTextView(String text) {
        if (itemView == null) return;
        itemView.setTextColor(Color.WHITE);
        itemView.setTextSize(SawimApplication.getFontSize());
        itemView.setText(text);
    }

    public void updateLabelIcon(BitmapDrawable drawable) {
        if (itemView == null) return;
        if (drawable == null)
            itemView.setImage(null);
        else
            itemView.setImage(drawable.getBitmap());
    }
}