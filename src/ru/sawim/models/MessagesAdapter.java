package ru.sawim.models;

import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import protocol.Contact;
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

    private boolean isMultiQuote = false;
    private int position;

    public MessagesAdapter() {
        items = new ArrayList<MessData>();
    }

    public void add(MessData messData) {
        items.add(messData);
        notifyDataSetChanged();
    }

    public void addAll(List<MessData> newMessageList) {
        items.clear();
        items.addAll(newMessageList);
        notifyDataSetChanged();
    }

    public List getItems() {
        return items;
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
    public View getView(int index, View convView, ViewGroup viewGroup) {
        final MessData mData = getItem(index);
        MessageItemView item = (MessageItemView) convView;
        if (item == null) {
            item = new MessageItemView(SawimApplication.getInstance().getBaseContext());
            Contact contact = RosterHelper.getInstance().getCurrentContact();
            item.setOnTextLinkClickListener(new TextLinkClick(contact.getProtocol(), contact.getUserId()));
        }
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        item.setLinkTextColor(Scheme.getColor(Scheme.THEME_LINKS));
        item.setTypeface(mData.isConfHighLight() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        item.setBackgroundColor(0);
        item.setLayout(mData.layout);
        if (mData.isMe() || mData.isPresence()) {
            item.setBackgroundIndex(MessageItemView.BACKGROUND_NONE);
            item.setPadding(Util.dipToPixels(item.getContext(), 19),
                    Util.dipToPixels(item.getContext(), 7), Util.dipToPixels(item.getContext(), 19), Util.dipToPixels(item.getContext(), 9));
            item.setNick(0, 0, null, null);
            item.setMsgTime(0, 0, null, null);
            item.setCheckImage(null);
            item.setTextSize(SawimApplication.getFontSize());
            item.setMsgTextSize(SawimApplication.getFontSize());
            if (mData.isMe()) {
                item.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            } else {
                item.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
            }
        } else {
            if (incoming) {
                item.setBackgroundIndex(MessageItemView.BACKGROUND_INCOMING);
                item.setPadding(Util.dipToPixels(item.getContext(), 18),
                        Util.dipToPixels(item.getContext(), 7), Util.dipToPixels(item.getContext(), 20), Util.dipToPixels(item.getContext(), 9));
            } else {
                item.setBackgroundIndex(MessageItemView.BACKGROUND_OUTCOMING);
                item.setPadding(Util.dipToPixels(item.getContext(), 20),
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

        }
        if (mData.isMarked() && isMultiQuote) {
            item.setTypeface(Typeface.DEFAULT_BOLD);
            item.setBackgroundColor(Scheme.getColor(Scheme.THEME_ITEM_SELECTED));
        }
        item.setShowDivider(position == index);
        item.repaint();
        return item;
    }
}