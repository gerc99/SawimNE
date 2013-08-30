package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Typeface;

import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.view.MyTextView;
import ru.sawim.view.menu.JuickMenu;
import sawim.Clipboard;
import sawim.TextFormatter;
import sawim.chat.Chat;
import sawim.chat.MessData;
import sawim.chat.message.Message;
import ru.sawim.Scheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends BaseAdapter implements MyTextView.TextLinkClickListener {

    private FragmentActivity activity;
    private Chat chat;
    private List<MessData> items = new ArrayList<MessData>();
    private Protocol currentProtocol;
    private String currentContact;

    private boolean isMultiСitation = false;
    private StringBuffer sb = new StringBuffer();

    public void init(FragmentActivity activity, Chat chat) {
        this.activity = activity;
        this.chat = chat;
        currentProtocol = chat.getProtocol();
        currentContact = chat.getContact().getUserId();
        refreshList(chat.getMessData());
    }

    public void refreshList(List<MessData> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public boolean isMultiСitation() {
        return isMultiСitation;
    }

    public void setMultiСitation(boolean multiShot) {
        isMultiСitation = multiShot;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public MessData getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int index, View row, ViewGroup viewGroup) {
        if (row == null) {
            row = new MessageItemView(activity);
            ((MessageItemView) row).checkBox.setVisibility(isMultiСitation ? CheckBox.VISIBLE : CheckBox.GONE);
            ((MessageItemView) row).build();
        }
        final MessageItemView item = ((MessageItemView) row);
        final MessData mData = items.get(index);
        String nick = mData.getNick();
        String text = mData.getText();
        boolean incoming = mData.isIncoming();

        ((ViewGroup)row).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        item.msgText.setOnTextLinkClickListener(this);
        item.checkBox.setChecked(mData.isMarked());
        item.checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mData.setMarked(!mData.isMarked());
                item.checkBox.setChecked(mData.isMarked());
                String msg = mData.getText();
                if (mData.isMe()) {
                    msg = "*" + mData.getNick() + " " + msg;
                }
                sb.append(msg);
                sb.append("\n");
                Clipboard.setClipBoardText(0 == sb.length() ? null : sb.toString());
            }
        });
        byte bg;
        if (mData.isMarked()) bg = Scheme.THEME_CHAT_BG_MARKED;
        else if (mData.isService()) bg = Scheme.THEME_CHAT_BG_SYSTEM;
        else if ((index & 1) == 0) bg = incoming ? Scheme.THEME_CHAT_BG_IN : Scheme.THEME_CHAT_BG_OUT;
        else bg = incoming ? Scheme.THEME_CHAT_BG_IN_ODD : Scheme.THEME_CHAT_BG_OUT_ODD;
        row.setBackgroundColor(Scheme.getColor(bg));
        if (mData.fullText == null) {
            mData.fullText = TextFormatter.getFormattedText(text, activity);
        }
        if (mData.isMe() || mData.isPresence()) {
            item.msgImage.setVisibility(ImageView.GONE);
            item.msgNick.setVisibility(TextView.GONE);
            item.msgTime.setVisibility(TextView.GONE);
            item.msgText.setTextSize(General.getFontSize() - 2);
            if (mData.isMe()) {
                item.msgText.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
                item.msgText.setText("* " + nick + " " + mData.fullText);
            } else {
                item.msgText.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
                item.msgText.setText(nick + mData.fullText);
            }
        } else {
            if (mData.iconIndex != Message.ICON_NONE) {
                Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
                if (icon == null) {
                    item.msgImage.setVisibility(ImageView.GONE);
                } else {
                    item.msgImage.setVisibility(ImageView.VISIBLE);
                    item.msgImage.setImageDrawable(icon.getImage());
                }
            }

            item.msgNick.setVisibility(TextView.VISIBLE);
            item.msgNick.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            item.msgNick.setTypeface(Typeface.DEFAULT_BOLD);
            item.msgNick.setTextSize(General.getFontSize());
            item.msgNick.setText(nick);

            item.msgTime.setVisibility(TextView.VISIBLE);
            item.msgTime.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            item.msgTime.setTextSize(General.getFontSize() - 4);
            item.msgTime.setText(mData.strTime);

            byte color = Scheme.THEME_TEXT;
            if (incoming && !chat.getContact().isSingleUserContact()
                    && Chat.isHighlight(text, chat.getMyName()))
                color = Scheme.THEME_CHAT_HIGHLIGHT_MSG;
            item.msgText.setTextColor(Scheme.getColor(color));
            item.msgText.setTextSize(General.getFontSize());
            if (currentContact.equals(JuickMenu.JUICK) || currentContact.equals(JuickMenu.JUBO))
                item.msgText.setTextWithLinks(mData.fullText, JuickMenu.Mode.juick);
            else if (currentContact.equals(JuickMenu.PSTO))
                item.msgText.setTextWithLinks(mData.fullText, JuickMenu.Mode.psto);
            else
                item.msgText.setTextWithLinks(mData.fullText, JuickMenu.Mode.none);
        }
        return row;
    }

    @Override
    public void onTextLinkClick(View textView, final String clickedString) {
        if (clickedString.length() == 0) return;
        if (clickedString.substring(0, 1).equals("@") || clickedString.substring(0, 1).equals("#")) {
            new JuickMenu(activity, currentProtocol, currentContact, clickedString).show();
        } else {
            CharSequence[] items = new CharSequence[2];
            items[0] = activity.getString(R.string.copy);
            items[1] = activity.getString(R.string.add_contact);
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(true);
            builder.setTitle(R.string.url_menu);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Clipboard.setClipBoardText(clickedString);
                            break;
                        case 1:
                            General.openUrl(clickedString);
                            break;
                    }
                }
            });
            builder.create().show();
        }
    }
}