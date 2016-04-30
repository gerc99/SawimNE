package ru.sawim.ui.widget.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import ru.sawim.R;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.ui.widget.FixedEditText;
import ru.sawim.ui.widget.IcsLinearLayout;
import ru.sawim.ui.widget.MyImageButton;
import ru.sawim.ui.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 14.11.13
 * Time: 19:35
 * To change this template use File | Settings | File Templates.
 */
@SuppressLint("NewApi")
public class ChatInputBarView extends IcsLinearLayout {

    private static final int MENU_BUTTON_INDEX = 0;
    private static final int SMILE_BUTTON_INDEX = 1;
    private static final int MESSAGE_EDITOR_INDEX = 2;
    private static final int SEND_BUTTON_INDEX = 3;

    public ChatInputBarView(Context context, MyImageButton menuButton, MyImageButton smileButton, FixedEditText messageEditor, MyImageButton sendButton) {
        super(context);
        int padding = Util.dipToPixels(context, 5);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        setOrientation(HORIZONTAL);
        setPadding(padding, padding, padding, padding);
        setDividerPadding(padding);
        setLayoutParams(layoutParams);

        LinearLayout.LayoutParams menuButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addViewInLayout(menuButton, MENU_BUTTON_INDEX, menuButtonLP);

        LinearLayout.LayoutParams smileButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addViewInLayout(smileButton, SMILE_BUTTON_INDEX, smileButtonLP);

        LinearLayout.LayoutParams messageEditorLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        messageEditorLP.gravity = Gravity.CENTER | Gravity.LEFT;
        messageEditorLP.weight = (float) 0.87;
        addViewInLayout(messageEditor, MESSAGE_EDITOR_INDEX, messageEditorLP);

        LinearLayout.LayoutParams sendButtonLP = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        addViewInLayout(sendButton, SEND_BUTTON_INDEX, sendButtonLP);
    }

    public MyImageButton getMenuButton() {
        return (MyImageButton) getChildAt(MENU_BUTTON_INDEX);
    }

    public MyImageButton getSmileButton() {
        return (MyImageButton) getChildAt(SMILE_BUTTON_INDEX);
    }

    public FixedEditText getMessageEditor() {
        return (FixedEditText) getChildAt(MESSAGE_EDITOR_INDEX);
    }

    public MyImageButton getSendButton() {
        return (MyImageButton) getChildAt(SEND_BUTTON_INDEX);
    }

    public void setImageButtons(MyImageButton menuButton, MyImageButton smileButton, MyImageButton sendButton, boolean isSearchMode) {
        menuButton.setImageDrawable(SawimResources.MENU_ICON);
        smileButton.setImageResource(Scheme.isBlack() ? R.drawable.ic_emoji_dark : R.drawable.ic_emoji_light);
        if (isSearchMode) {
            sendButton.setImageResource(android.R.drawable.ic_menu_search);
        } else {
            sendButton.setImageResource(Scheme.isBlack() ? R.drawable.ic_send_dark : R.drawable.ic_send_light);
        }
        setDividerDrawable(SawimResources.listDivider);
    }
}
