package ru.sawim.models;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.text.TextLinkClick;
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

    public static boolean isRepaint;
    private boolean isMultiQuote = false;
    private int position = -1;

    public void init(Chat chat) {
        currentProtocol = chat.getProtocol();
        currentContact = chat.getContact().getUserId();
        refreshList(chat.getMessData());
    }

    public void refreshList(List<MessData> list) {
        items.clear();
        for (int i = 0; i < list.size(); ++i) {
            items.add(list.get(i));
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
        if (mData.messView == null || isRepaint)
            mData.messView = new MessageItemView(General.currentActivity, !(mData.isMe() || mData.isPresence()));

        MessageItemView item = mData.messView;
        CharSequence parsedText = mData.getText();
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        item.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        item.msgText.setOnTextLinkClickListener(new TextLinkClick(currentProtocol, currentContact));
        item.msgText.setLinkTextColor(Scheme.LINKS);
        item.msgText.setTypeface(Typeface.DEFAULT);
        item.setBackgroundColor(0);
        if (mData.isMe() || mData.isPresence()) {
            item.msgText.setTextSize(General.getFontSize() - 2);
            if (mData.isMe()) {
                SpannableStringBuilder text = new SpannableStringBuilder();
                text.append("* ").append(nick).append(" ").append(parsedText);
                item.msgText.setText(text);
                item.msgText.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            } else {
                SpannableStringBuilder text = new SpannableStringBuilder();
                text.append(mData.strTime).append(" ").append(nick).append(parsedText);
                item.msgText.setText(text);
                item.msgText.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
            }
        } else {
            item.setBackgroundResource(incoming ?
                    (Scheme.isBlack() ? R.drawable.msg_in_dark : R.drawable.msg_in)
                    : (Scheme.isBlack() ? R.drawable.msg_out_dark : R.drawable.msg_out));
            float displayDensity = General.getInstance().getDisplayDensity();
            if (incoming) {
                item.setPadding((int) (19 * displayDensity), (int) (7 * displayDensity), (int) (9 * displayDensity), (int) (9 * displayDensity));
            } else {
                item.setPadding((int) (11 * displayDensity), (int) (7 * displayDensity), (int) (18 * displayDensity), (int) (9 * displayDensity));
            }
            if (mData.getIconIndex() == Message.ICON_OUT_MSG_FROM_CLIENT) {
                item.titleItemView.setCheckImage(SawimResources.messageIconCheck.getBitmap());
            }

            item.titleItemView.setNick(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    General.getFontSize(), Typeface.DEFAULT_BOLD, nick);

            item.titleItemView.setMsgTime(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    General.getFontSize() * 2 / 3, Typeface.DEFAULT, mData.strTime);

            item.msgText.setTextSize(General.getFontSize());
            item.msgText.setTextColor(Scheme.getColor(mData.getMessColor()));
            item.msgText.setText(parsedText);
        }
        if (mData.isMarked() && isMultiQuote) {
            item.msgText.setTypeface(Typeface.DEFAULT_BOLD);
            item.setBackgroundColor(Scheme.getColor(Scheme.THEME_MARKED_BACKGROUND));
        }
        item.setShowDivider(Scheme.getColor(Scheme.THEME_TEXT), position == index && index > 0 && position != getCount());
        item.titleItemView.repaint();
        return item;
    }
}