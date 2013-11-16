package ru.sawim.models;

import DrawControls.icons.Icon;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.text.TextLinkClickListener;
import ru.sawim.widget.chat.MessageItemView;
import sawim.chat.Chat;
import sawim.chat.MessData;
import sawim.chat.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends BaseAdapter {

    private List<MessData> items = new ArrayList<MessData>();
    private Protocol currentProtocol;
    private String currentContact;

    private boolean isMultiQuote = false;
    private int position = -1;

    public void init(Chat chat) {
        currentProtocol = chat.getProtocol();
        currentContact = chat.getContact().getUserId();
        refreshList(chat.getMessData());
    }

    public void refreshList(List<MessData> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void repaintList(List<MessData> list) {
        items.clear();
        items.addAll(list);
        for (int i = 0; i < list.size(); ++i) {
            list.get(i).messView = null;
        }
        notifyDataSetChanged();
    }

    public boolean isMultiQuote() {
        return isMultiQuote;
    }

    public void setMultiQuote(boolean multiShot) {
        isMultiQuote = multiShot;
    }

    public void setPosition(int position) {
        this.position = position;
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
        final MessData mData = items.get(index);
        if (mData.messView == null)
            mData.messView = new MessageItemView(General.currentActivity, !(mData.isMe() || mData.isPresence()));

        MessageItemView item = mData.messView;
        CharSequence parsedText = mData.parsedText();
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        item.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        item.msgText.setOnTextLinkClickListener(new TextLinkClickListener(currentProtocol, currentContact));
        item.msgText.setTypeface(Typeface.DEFAULT);
        item.setBackgroundColor(0);
        if (mData.isMarked() && isMultiQuote) {
            item.msgText.setTypeface(Typeface.DEFAULT_BOLD);
            item.setBackgroundColor(Scheme.getColor(Scheme.THEME_MARKED_BACKGROUND));
        }

        if (mData.isMe() || mData.isPresence()) {
            item.msgText.setTextSize(General.getFontSize() - 2);
            if (mData.isMe()) {
                item.msgText.setText("* " + nick + " " + parsedText);
                item.msgText.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            } else {
                item.msgText.setText(nick + parsedText);
                item.msgText.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
            }
        } else {
            if (mData.iconIndex != Message.ICON_NONE) {
                Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
                if (icon != null) {
                    item.titleItemView.setMsgImage(icon.getImage());
                }
            }

            item.titleItemView.setNick(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    General.getFontSize(), Typeface.DEFAULT_BOLD, nick);

            item.titleItemView.setMsgTime(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    General.getFontSize() * 2 / 3, Typeface.DEFAULT, mData.strTime);

            item.msgText.setTextSize(General.getFontSize());
            item.msgText.setTextColor(Scheme.getColor(mData.getMessColor()));
            item.msgText.setLinkTextColor(0xff00e4ff);
            item.msgText.setText(parsedText);
        }
        item.msgText.repaint();
        item.initDivider(Scheme.getColor(Scheme.THEME_TEXT));
        item.setShowDivider(position == index && index > 0 && position != getCount());
        item.titleItemView.repaint();
        return item;
    }
}