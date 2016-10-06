package ru.sawim.ui.adapter;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import io.realm.OrderedRealmCollection;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;
import protocol.Contact;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;
import ru.sawim.Scheme;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.comm.JLocale;
import ru.sawim.icons.AvatarCache;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;
import ru.sawim.text.OnTextLinkClick;
import ru.sawim.ui.widget.Util;
import ru.sawim.ui.widget.chat.MessageItemView;
import ru.sawim.ui.widget.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder>
        implements StickyRecyclerHeadersAdapter<MessagesAdapter.HeaderHolder>, View.OnClickListener {

    private String query;
    private boolean isMultiQuoteMode;
    public boolean showLoader;
    private int position;
    private OnLoadItemsListener loadItemsListener;
    private OnItemClickListener itemClickListener;
    private OnTextLinkClick textLinkClick;
    private HashMap<String, MessData> itemsCache = new HashMap<>();
    private String userId;
    private boolean showTimes;
    private Handler handler = new Handler();

    @Nullable
    private OrderedRealmCollection<ru.sawim.db.model.Message> items;
    private final RealmChangeListener listener = new RealmChangeListener() {
        @Override
        public void onChange(Object results) {
            notifyDataSetChanged();
        }
    };

    public MessagesAdapter(Realm realmDb, String userId) {
        this.userId = userId;
        items = realmDb.where(ru.sawim.db.model.Message.class).equalTo("contactId", userId).findAllSorted("date", Sort.ASCENDING);
        textLinkClick = new OnTextLinkClick();
        setHasStableIds(true);
    }
/*
    @Override
    public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        if (isDataValid()) {
            addListener(items);
        }
    }

    @Override
    public void onDetachedFromRecyclerView(final RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (isDataValid()) {
            removeListener(items);
        }
    }
*/
    public void setItems(RealmResults<ru.sawim.db.model.Message> items) {
        this.items = items;
    }

    public long getItemId(int position) {
        ru.sawim.db.model.Message messData = getItem(position);
        if (messData == null) return -1;
        return messData.getDate() + messData.getText().length();
    }

    public List<ru.sawim.db.model.Message> getItems() {
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(new MessageItemView(parent.getContext()));
    }

    @Override
    public boolean onFailedToRecycleView(ViewHolder holder) {
        return true;
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int index) {
        if (loadItemsListener != null) {
            loadItemsListener.onLoadItems(index);
        }
        if (showLoader && index == 0) {
            return;
        } else {
            MessData mData = getMessData(getItem(index));
            if (mData != null) {
                final MessageItemView item = (MessageItemView) viewHolder.itemView;
                item.setTag(index);
                Contact contact = RosterHelper.getInstance().getCurrentContact();
                textLinkClick.setContact(contact.getUserId());
                item.setOnTextLinkClickListener(textLinkClick);
                item.setOnClickListener(this);
                String nick = mData.getNick();

                if (Options.getBoolean(JLocale.getString(R.string.pref_users_avatars))) {
                    AvatarCache.getInstance().load(contact.getUserId() + "/" + nick,
                            contact.avatarHash, contact.getText(),
                            AvatarCache.MESSAGE_AVATAR_SIZE, new AvatarCache.OnImageLoadListener() {
                                @Override
                                public void onLoad(Bitmap avatar) {
                                    item.setAvatarBitmap(avatar);
                                }
                            });
                }

                boolean incoming = mData.isIncoming();
                item.setLinkTextColor(Scheme.getColor(R.attr.link));
                item.setTypeface(mData.isHighLight() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
                item.setLayout(mData.layout);
                if (SawimApplication.showPicturesInChat) {
                    item.setLinks(mData.getUrlLinks());
                }
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
                            SawimApplication.getFontSize() - SawimApplication.getFontSize() / 7, Typeface.DEFAULT, nick);
                    if (showTimes) {
                        item.setMsgTime(Scheme.getColor(incoming ? R.attr.chat_in_msg_text : R.attr.chat_out_msg_text),
                                SawimApplication.getFontSize() * 2 / 3, Typeface.DEFAULT, mData.getStrTime());
                    } else {
                        item.setMsgTime(0, 0, null, null);
                    }
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
        }
    }

    public MessData getMessData(ru.sawim.db.model.Message message) {
        if (message == null) return null;
        MessData messData = itemsCache.get(message.getMessageId());
        if (messData == null) {
            messData = HistoryStorage.buildMessage(ChatHistory.instance.getChat(userId), message);
            itemsCache.put(message.getMessageId(), messData);
        }
        return messData;
    }

    public MessData getVisibleMessData(String messageId) {
        return itemsCache.get(messageId);
    }

    @Override
    public long getHeaderId(int position) {
        if (getItem(position) != null) {
            MessData current = getMessData(getItem(position));
            if (current != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(current.getTime());
                return calendar.get(Calendar.DATE);
            }
        }
        return -1;
    }

    @Override
    public HeaderHolder onCreateHeaderViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chat_view_header, parent, false);
        return new HeaderHolder(view);
    }

    @Override
    public void onBindHeaderViewHolder(HeaderHolder holder, int position) {
        MessData current = getMessData(getItem(position));
        if (current != null) {
            holder.header.setText(formatDate(current.getTime()));
        }
    }

    @Override
    public int getItemCount() {
        if (items == null) return 0;
        return items.size();
    }

    public ru.sawim.db.model.Message getItem(int i) {
        if (items.isEmpty() || items.size() <= i || i < 0) return null;
        return items.get(i);
    }

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
    public void onClick(View v) {
        if (itemClickListener != null)
            itemClickListener.onItemClick(v, (Integer) v.getTag());
    }

    public void showLoader() {
        showLoader = true;
        notifyDataSetChanged();
    }

    public void hideLoader() {
        showLoader = false;
    }

    public boolean isLoader() {
        return showLoader;
    }

    public void showTimes() {
        showTimes = true;
        notifyDataSetChanged();
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showTimes = false;
                notifyDataSetChanged();
            }
        }, 1500);
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
        TextView header;

        public HeaderHolder(View itemView) {
            super(itemView);

            header = (TextView) itemView.findViewById(R.id.text);
        }
    }

    private class ProgressViewHolder extends ViewHolder {
        public ProgressViewHolder(View v) {
            super(v);
            ProgressBar progressBar = (ProgressBar) v;
            progressBar.setIndeterminate(true);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View v, int position);
    }

    /*private void addListener(@NonNull OrderedRealmCollection<ru.sawim.db.model.Message> data) {
        RealmResults realmResults = (RealmResults) data;
        realmResults.addChangeListener(listener);
    }

    private void removeListener(@NonNull OrderedRealmCollection<ru.sawim.db.model.Message> data) {
        RealmResults realmResults = (RealmResults) data;
        realmResults.removeChangeListener(listener);
    }

    private boolean isDataValid() {
        return items != null && items.isValid();
    }*/
}