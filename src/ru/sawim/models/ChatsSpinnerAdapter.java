package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 21.07.13
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class ChatsSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

    private final List<Chat> chats;
    Context context;
    LayoutInflater layoutInflater;

    public ChatsSpinnerAdapter(Context context) {
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        chats = ChatHistory.instance.historyTable;
    }

    @Override
    public int getCount() {
        return chats.size();
    }

    @Override
    public Chat getItem(int i) {
        return ChatHistory.instance.chatAt(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View v = convertView;
        HeaderViewHolder headerViewHolder;
        Chat chat = getItem(position);
        if (v == null) {
            v = layoutInflater.inflate(R.layout.chats_spinner_item, null);
            headerViewHolder = new HeaderViewHolder();
            headerViewHolder.header = (TextView) v.findViewById(R.id.header);
            v.setTag(headerViewHolder);
        } else {
            headerViewHolder = (HeaderViewHolder) v.getTag();
        }
        if (chat == null) return v;
        headerViewHolder.header.setTextSize(General.getFontSize());
        headerViewHolder.header.setTextColor(Scheme.getColor(Scheme.THEME_CAP_TEXT));
        headerViewHolder.header.setText(chat.getContact().getName());
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        DropDownViewHolder dropDownViewHolder;
        Chat chat = getItem(position);
        if (v == null) {
            v = layoutInflater.inflate(R.layout.chats_spinner_dropdown_item, null);
            dropDownViewHolder = new DropDownViewHolder();
            dropDownViewHolder.imageView = (ImageView) v.findViewById(R.id.image_icon);
            dropDownViewHolder.label = (TextView) v.findViewById(R.id.label);
            v.setTag(dropDownViewHolder);
        } else {
            dropDownViewHolder = (DropDownViewHolder) v.getTag();
        }
        if (chat == null) return v;
        v.setBackgroundColor(Scheme.getColor(Scheme.THEME_CAP_BACKGROUND));
        Icon icStatus = chat.getProtocol().getStatusInfo().getIcon(chat.getContact().getStatusIndex());
        Icon icMess = Message.msgIcons.iconAt(chat.getContact().getUnreadMessageIcon());
        if (icMess == null)
            dropDownViewHolder.imageView.setImageBitmap(icStatus.getImage());
        else
            dropDownViewHolder.imageView.setImageBitmap(icMess.getImage());

        dropDownViewHolder.label.setTextSize(General.getFontSize());
        dropDownViewHolder.label.setTextColor(Scheme.getColor(Scheme.THEME_CAP_TEXT));
        dropDownViewHolder.label.setText(chat.getContact().getName());
        return v;
    }

    static class HeaderViewHolder {
        TextView header;
    }

    static class DropDownViewHolder {
        ImageView imageView;
        TextView label;
    }
}
