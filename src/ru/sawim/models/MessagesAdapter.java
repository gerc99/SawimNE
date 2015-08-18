package ru.sawim.models;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.roster.RosterHelper;
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

    private List<MessData> items;

    private String query;
    private boolean isMultiQuoteMode;
    private int position;

    public MessagesAdapter() {
        items = new ArrayList<>();
    }

    public List<MessData> getItems() {
        return items;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public boolean isMultiQuoteMode() {
        return isMultiQuoteMode;
    }

    public void setMultiQuoteMode(boolean flag) {
        isMultiQuoteMode = flag;
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
        if (items.size() == 0 || (items.size() <= i)) return null;
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int index, View convView, ViewGroup viewGroup) {
        MessData mData = getItem(index);
        MessageItemView item = (MessageItemView) convView;
        if (item == null) {
            item = new MessageItemView(viewGroup.getContext());
            Contact contact = RosterHelper.getInstance().getCurrentContact();
            item.setOnTextLinkClickListener(new TextLinkClick(contact.getProtocol().getUserId(), contact.getUserId()));
        }
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        item.setLinkTextColor(Scheme.getColor(R.attr.link));
        item.setTypeface(mData.isHighLight() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        item.setBackgroundColor(0);
        item.setLayout(mData.layout);
        if (mData.isMe() || mData.isPresence()) {
            item.setBackgroundIndex(MessageItemView.BACKGROUND_NONE);
            item.setPadding(MessageItemView.PADDING_LEFT + 1, MessageItemView.PADDING_TOP, MessageItemView.PADDING_RIGHT - 1, MessageItemView.PADDING_BOTTOM);
            item.setNick(0, 0, null, null);
            item.setMsgTime(0, 0, null, null);
            item.setCheckImage(null);
            item.setTextSize(SawimApplication.getFontSize());
            item.setMsgTextSize(SawimApplication.getFontSize());
            if (mData.isMe()) {
                item.setTextColor(Scheme.getColor(incoming ? R.attr.chat_in_msg_text : R.attr.chat_out_msg_text));
            } else {
                item.setTextColor(Scheme.getColor(R.attr.chat_in_msg_text));
            }
        } else {
            if (incoming) {
                item.setBackgroundIndex(MessageItemView.BACKGROUND_INCOMING);
                item.setPadding(MessageItemView.PADDING_LEFT, MessageItemView.PADDING_TOP, MessageItemView.PADDING_RIGHT, MessageItemView.PADDING_BOTTOM);
            } else {
                item.setBackgroundIndex(MessageItemView.BACKGROUND_OUTCOMING);
                item.setPadding(MessageItemView.PADDING_RIGHT, MessageItemView.PADDING_TOP, MessageItemView.PADDING_LEFT, MessageItemView.PADDING_BOTTOM);
            }
            item.setTextSize(SawimApplication.getFontSize());
            item.setCheckImage(mData.getIconIndex() == Message.ICON_OUT_MSG_FROM_CLIENT ? SawimResources.MESSAGE_ICON_CHECK.getBitmap() : null);
            item.setNick(Scheme.getColor(incoming ? R.attr.chat_in_msg_text : R.attr.chat_out_msg_text),
                    SawimApplication.getFontSize(), Typeface.DEFAULT_BOLD, nick);
            item.setMsgTime(Scheme.getColor(incoming ? R.attr.chat_in_msg_text : R.attr.chat_out_msg_text),
                    SawimApplication.getFontSize() * 2 / 3, Typeface.DEFAULT, mData.getStrTime());
            item.setMsgTextSize(SawimApplication.getFontSize());
            item.setTextColor(Scheme.getColor(mData.getMessColor()));
        }
        if (query != null) {
            item.setLayout(MessageItemView.makeLayout(
                    Util.highlightText(query, mData.layout.getText().toString(), Scheme.getColor(R.attr.link)),
                    Typeface.DEFAULT_BOLD, mData.layout.getWidth()));
        }
        if (mData.isMarked() && isMultiQuoteMode) {
            item.setTextColor(Scheme.getColor(R.attr.item_selected));
        }
        item.setShowDivider(position == index);
        item.repaint();
        return item;
    }
}