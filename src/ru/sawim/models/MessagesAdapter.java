package ru.sawim.models;

import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.chat.Chat;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.text.TextLinkClick;
import ru.sawim.widget.Util;
import ru.sawim.widget.chat.MessageItemView;

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
    private Chat chat;

    private boolean isMultiQuote = false;
    private int position = -1;

    public void init(Chat chat) {
        this.chat = chat;
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

    public static void clearCache() {
        MessageItemView.clearCache();
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
    public View getView(int index, View convView, ViewGroup viewGroup) {
        final MessData mData = getItem(index);
        View row = convView;
        if (row == null) {
            row = new MessageItemView(SawimApplication.getInstance().getBaseContext());
        }
        MessageItemView item = (MessageItemView) row;
        CharSequence parsedText = mData.getText();
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        item.setOnTextLinkClickListener(new TextLinkClick(chat.getProtocol(), chat.getContact().getUserId()));
        item.setLinkTextColor(Scheme.getColor(Scheme.THEME_LINKS));
        item.setTypeface(mData.isConfHighLight() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        item.setBackgroundColor(0);
        if (mData.isMe() || mData.isPresence()) {
            int padding = Util.dipToPixels(item.getContext(), 5);
            item.setPadding(padding, padding, padding, padding);
            item.setNick(0, 0, null, null);
            item.setMsgTime(0, 0, null, null);
            item.setCheckImage(null);
            item.setTextSize(SawimApplication.getFontSize() - 2);
            item.setMsgTextSize(SawimApplication.getFontSize() - 2);
            if (mData.isMe()) {
                SpannableStringBuilder text = new SpannableStringBuilder();
                text.append("* ").append(nick).append(" ").append(parsedText);
                item.setText(text);
                item.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            } else {
                SpannableStringBuilder text = new SpannableStringBuilder();
                text.append(mData.strTime).append(" ").append(nick).append(parsedText);
                item.setText(text);
                item.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
            }
        } else {
            item.setBackgroundResource(incoming ?
                    (Scheme.isBlack() ? R.drawable.msg_in_dark : R.drawable.msg_in)
                    : (Scheme.isBlack() ? R.drawable.msg_out_dark : R.drawable.msg_out));
            if (incoming) {
                item.setPadding(Util.dipToPixels(item.getContext(), 19),
                        Util.dipToPixels(item.getContext(), 7), Util.dipToPixels(item.getContext(), 9), Util.dipToPixels(item.getContext(), 9));
            } else {
                item.setPadding(Util.dipToPixels(item.getContext(), 11),
                        Util.dipToPixels(item.getContext(), 7), Util.dipToPixels(item.getContext(), 18), Util.dipToPixels(item.getContext(), 9));
            }
            item.setTextSize(SawimApplication.getFontSize());
            item.setCheckImage(mData.getIconIndex() == Message.ICON_OUT_MSG_FROM_CLIENT ? SawimResources.messageIconCheck.getBitmap() : null);
            item.setNick(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    SawimApplication.getFontSize(), Typeface.DEFAULT_BOLD, nick);
            item.setMsgTime(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG),
                    SawimApplication.getFontSize() * 2 / 3, Typeface.DEFAULT, mData.strTime);
            item.setMsgTextSize(SawimApplication.getFontSize());
            item.setTextColor(Scheme.getColor(mData.getMessColor()));
            item.setText(parsedText);

        }
        if (mData.isMarked() && isMultiQuote) {
            item.setTypeface(Typeface.DEFAULT_BOLD);
            item.setBackgroundColor(Scheme.getColor(Scheme.THEME_ITEM_SELECTED));
        }
        item.setShowDivider(position == index && index > 0 && position != getCount());
        item.repaint();
        return item;
    }
}