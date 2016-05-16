package ru.sawim.ui.widget.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.reflect.Field;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.ui.widget.MyImageButton;
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
public class ChatBarView extends LinearLayout {

    private static final int ITEM_VIEW_INDEX = 0;
    private static final int SEARCH_EDITTEXT_VIEW_INDEX = 1;
    private static final int SEARCH_UP_IMAGE_INDEX = 2;
    private static final int SEARCH_DOWN_IMAGE_INDEX = 3;
    private static final int CHATS_IMAGE_INDEX = 4;

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

        EditText searchEditor = new EditText(getContext());
        LinearLayout.LayoutParams messageEditorLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        searchEditor.setBackgroundColor(Color.TRANSPARENT);
        searchEditor.setHint("Search messages");
        searchEditor.setHintTextColor(Color.LTGRAY);
        searchEditor.setTextColor(Color.WHITE);
        searchEditor.setSingleLine();
        searchEditor.setInputType(searchEditor.getInputType() | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            searchEditor.setImeOptions(EditorInfo.IME_FLAG_NO_FULLSCREEN | EditorInfo.IME_ACTION_SEARCH);
            searchEditor.setTextIsSelectable(false);
        } else {
            searchEditor.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        }
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(searchEditor, R.drawable.search_carret);
        } catch (Exception e) {
            //nothing to do
        }
        messageEditorLP.weight = 1;
        addViewInLayout(searchEditor, SEARCH_EDITTEXT_VIEW_INDEX, messageEditorLP);

        MyImageButton upBtn = new MyImageButton(getContext());
        upBtn.setImageResource(R.drawable.ic_expanded_dark);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        if (SawimApplication.isManyPane()) {
            lp.weight = 4;
        }
        addViewInLayout(upBtn, SEARCH_UP_IMAGE_INDEX, lp);

        MyImageButton downBtn = new MyImageButton(getContext());
        downBtn.setImageResource(R.drawable.ic_collapsed_dark);
        lp = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        if (SawimApplication.isManyPane()) {
            lp.weight = 4;
        }
        addViewInLayout(downBtn, SEARCH_DOWN_IMAGE_INDEX, lp);

        if (!SawimApplication.isManyPane()) {
            LinearLayout.LayoutParams chatsImageLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
            chatsImageLP.gravity = Gravity.CENTER_VERTICAL;
            addViewInLayout(chatsImage, CHATS_IMAGE_INDEX, chatsImageLP);
        }
    }

    public MyImageButton getChatsImage() {
        return (MyImageButton) getChildAt(CHATS_IMAGE_INDEX);
    }

    public EditText getSearchEditText() {
        return (EditText) getChildAt(SEARCH_EDITTEXT_VIEW_INDEX);
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
        EditText searchEditor = (EditText) getChildAt(SEARCH_EDITTEXT_VIEW_INDEX);
        searchEditor.setVisibility(VISIBLE);
        //AnimationUtils.circleRevealView(messageEditor);
        searchEditor.requestFocus();

        MyImageButton upBtn = (MyImageButton) getChildAt(SEARCH_UP_IMAGE_INDEX);
        upBtn.setOnClickListener(upBtnClickListener);
        upBtn.setVisibility(VISIBLE);

        MyImageButton downBtn = (MyImageButton) getChildAt(SEARCH_DOWN_IMAGE_INDEX);
        downBtn.setOnClickListener(downBtnClickListener);
        downBtn.setVisibility(VISIBLE);
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