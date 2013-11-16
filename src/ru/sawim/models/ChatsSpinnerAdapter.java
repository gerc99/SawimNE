package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.widget.LabelView;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.roster.Roster;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 21.07.13
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class ChatsSpinnerAdapter extends BaseAdapter {

    private List<Object> items = new ArrayList<Object>();
    Context context;
    LayoutInflater layoutInflater;

    public ChatsSpinnerAdapter(Context context) {
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        refreshList();
    }

    public void refreshList() {
        items.clear();
        ChatHistory.instance.sort();
        if (Roster.getInstance().getProtocolCount() > 0)
            for (int i = 0; i < Roster.getInstance().getProtocolCount(); ++i) {
                ChatHistory.instance.addLayerToListOfChats(Roster.getInstance().getProtocol(i), items);
            }
        else
            items.addAll(ChatHistory.instance.historyTable);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int index) {
        return items.get(index);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View v = convertView;
        ViewHolder viewHolder;
        if (v == null) {
            v = layoutInflater.inflate(R.layout.chats_spinner_dropdown_item, null);
            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) v.findViewById(R.id.image_icon);
            viewHolder.label = (LabelView) v.findViewById(R.id.label);
            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }
        final Object object = items.get(position);
        if (object instanceof String) {
            viewHolder.imageView.setVisibility(View.GONE);
            viewHolder.label.setTypeface(Typeface.SANS_SERIF);
            viewHolder.label.setTextSize(General.getFontSize() - 2);
            viewHolder.label.setTextColor(Scheme.getInversColor(Scheme.THEME_CAP_TEXT));
            viewHolder.label.setText((String) object);
        }
        if (object instanceof Chat) {
            final Chat chat = (Chat) object;
            if (chat == null) return v;
            viewHolder.imageView.setVisibility(View.VISIBLE);
            if (chat.getContact().isTyping()) {
                viewHolder.imageView.setImageDrawable(Message.msgIcons.iconAt(Message.ICON_TYPE).getImage());
            } else {
                viewHolder.imageView.setImageDrawable(getImageChat(chat, true));
            }
            viewHolder.label.setTextSize(General.getFontSize());
            viewHolder.label.setTextColor(Scheme.getColor(Scheme.THEME_CAP_TEXT));
            viewHolder.label.setText(chat.getContact().getName());
        }
        return v;
    }

    public Drawable getImageChat(Chat chat, boolean showMess) {
        if (chat.getContact().isTyping()) {
            return Message.msgIcons.iconAt(Message.ICON_TYPE).getImage();
        } else {
            Icon icStatus = chat.getContact().getLeftIcon(chat.getProtocol());
            Icon icMess = Message.msgIcons.iconAt(chat.getContact().getUnreadMessageIcon());
            return icMess == null || !showMess ? icStatus.getImage() : icMess.getImage();
        }
    }

    static class ViewHolder {
        ImageView imageView;
        LabelView label;
    }
}
