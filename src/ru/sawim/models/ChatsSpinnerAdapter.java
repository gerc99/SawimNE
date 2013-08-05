package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import sawim.chat.Chat;
import sawim.chat.message.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 21.07.13
 * Time: 23:44
 * To change this template use File | Settings | File Templates.
 */
public class ChatsSpinnerAdapter extends BaseAdapter implements SpinnerAdapter {

    private List<Chat> items = new ArrayList<Chat>();
    Context context;
    LayoutInflater layoutInflater;

    public ChatsSpinnerAdapter(Context context, List<Chat> items) {
        this.context = context;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.items = items;
        refreshList(items);
    }

    public void refreshList(List<Chat> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Chat getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup viewGroup) {
        View v = convertView;
        ViewHolder viewHolder;
        final Chat chat = getItem(position);
        if (v == null) {
            v = layoutInflater.inflate(R.layout.chats_spinner_dropdown_item, null);
            viewHolder = new ViewHolder();
            viewHolder.imageView = (ImageView) v.findViewById(R.id.image_icon);
            viewHolder.label = (TextView) v.findViewById(R.id.label);
            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }
        if (chat == null) return v;
        Icon icStatus = chat.getContact().getLeftIcon();
        Icon icMess = Message.msgIcons.iconAt(chat.getContact().getUnreadMessageIcon());
        if (icMess == null)
            viewHolder.imageView.setImageBitmap(icStatus.getImage());
        else
            viewHolder.imageView.setImageBitmap(icMess.getImage());
        viewHolder.label.setTextSize(General.getFontSize());
        viewHolder.label.setTextColor(Scheme.getColor(Scheme.THEME_CAP_TEXT));
        viewHolder.label.setText(chat.getContact().getName());
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        ViewHolder dropDownViewHolder;
        final Chat chat = getItem(position);
        if (v == null) {
            v = layoutInflater.inflate(R.layout.chats_spinner_dropdown_item, null);
            dropDownViewHolder = new ViewHolder();
            dropDownViewHolder.imageView = (ImageView) v.findViewById(R.id.image_icon);
            dropDownViewHolder.label = (TextView) v.findViewById(R.id.label);
            v.setTag(dropDownViewHolder);
        } else {
            dropDownViewHolder = (ViewHolder) v.getTag();
        }
        if (chat == null) return v;
        v.setBackgroundColor(Scheme.getInversColor(Scheme.THEME_CAP_BACKGROUND));
        Icon icStatus = chat.getContact().getLeftIcon();
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

    static class ViewHolder {
        ImageView imageView;
        TextView label;
    }
}
