package ru.sawim.models;

import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import protocol.Contact;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.roster.RosterHelper;
import ru.sawim.text.OnTextLinkClick;
import ru.sawim.widget.Util;
import ru.sawim.widget.chat.MessageItemView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder>
        implements View.OnLongClickListener, View.OnClickListener {

    private List<MessData> items;

    private String query;
    private boolean isMultiQuoteMode;
    private int position;
    private OnLoadItemsListener loadItemsListener;
    private OnItemClickListener itemClickListener;
    private OnTextLinkClick textLinkClick;

    public MessagesAdapter() {
        items = new ArrayList<>();
        textLinkClick = new OnTextLinkClick();
        //setHasStableIds(true);
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
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        MessageItemView item = new MessageItemView(viewGroup.getContext());
        return new ViewHolder(item);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int index) {
        if (loadItemsListener != null) {
            loadItemsListener.onLoadItems(index);
        }
        MessageItemView item = (MessageItemView) viewHolder.itemView;
        item.setTag(index);
        Contact contact = RosterHelper.getInstance().getCurrentContact();
        textLinkClick.setContact(contact.getProtocol().getUserId(), contact.getUserId());
        item.setOnTextLinkClickListener(textLinkClick);
        item.setOnClickListener(this);
        item.setOnLongClickListener(this);

        MessData mData = getItem(index);
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        if (SawimApplication.showPicturesInChat) {
            item.setLinks(mData.getUrlLinks());
        }
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
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        MessageItemView item = (MessageItemView) holder.itemView;
        item.setOnTextLinkClickListener(null);
        item.setOnClickListener(null);
        item.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public MessData getItem(int i) {
        if (items.isEmpty() || (items.size() <= i)) return null;
        return items.get(i);
    }

    /*@Override
    public long getHeaderId(int position) {
        MessData next;
        if (position > 1) {
            next = getItem(position - 1);
            if (next != null) {
                MessData current = getItem(position);
                if (current != null) {
                    if (!ru.sawim.comm.Util.compareDate(next.getTime(), current.getTime())) {
                        return current.getTime() / 1000 - 24 * 60 * 60;
                    }
                }
            }
        } else {
            return getItem(0).getTime() / 1000 - 24 * 60 * 60;
        }
        return -1;
    }*/

    private static final SimpleDateFormat chatDate = new SimpleDateFormat("d MMMM");
    private static final SimpleDateFormat chatFullDate = new SimpleDateFormat("d, MMMM yyyy");
    public String formatDate(long date) {
        Calendar rightNow = Calendar.getInstance();
        int year = rightNow.get(Calendar.YEAR);

        rightNow.setTimeInMillis(date);
        int dateYear = rightNow.get(Calendar.YEAR);

        if (year == dateYear) {
            return chatDate.format(date);
        }
        return chatFullDate.format(date);
    }

    public void setOnLoadItemsListener(OnLoadItemsListener loadItemsListener) {
        this.loadItemsListener = loadItemsListener;
    }

    public void setOnItemClickListener(OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public boolean onLongClick(View v) {
        if (itemClickListener != null)
            itemClickListener.onItemLongClick(v, (Integer) v.getTag());
        return false;
    }

    @Override
    public void onClick(View v) {
        if (itemClickListener != null)
            itemClickListener.onItemClick(v, (Integer) v.getTag());
    }

    public interface OnLoadItemsListener {
        void onLoadItems(int i);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        public TextView header;

        public HeaderHolder(View itemView) {
            super(itemView);

            header = (TextView) itemView;
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
        void onItemLongClick(View v, int position);
    }
}