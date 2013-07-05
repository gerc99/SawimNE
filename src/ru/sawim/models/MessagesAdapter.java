package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ru.sawim.General;
import ru.sawim.R;
import sawim.TextFormatter;
import sawim.chat.Chat;
import sawim.chat.MessData;
import sawim.chat.message.Message;
import ru.sawim.Scheme;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends BaseAdapter {

    private Context baseContext;
    private List<MessData> items;
    private Chat chat;

    public MessagesAdapter(Context context, Chat chat, List<MessData> items) {
        this.baseContext = context;
        this.chat = chat;
        this.items = items;
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
    public View getView(int index, View convertView, ViewGroup viewGroup) {
        View row = convertView;
        ViewHolder holder;
        if (row == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            row = inf.inflate(R.layout.chat_item, null);
            holder = new ViewHolder();
            holder.msgImage = (ImageView) row.findViewById(R.id.msg_icon);
            holder.msgNick = (TextView) row.findViewById(R.id.msg_nick);
            holder.msgTime = (TextView) row.findViewById(R.id.msg_time);
            holder.msgText = (TextView) row.findViewById(R.id.msg_text);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
        ((ViewGroup)row).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        MessData mData = items.get(index);
        String text = mData.getText();
        ImageView msgImage = holder.msgImage;
        TextView msgNick = holder.msgNick;
        TextView msgTime = holder.msgTime;
        TextView msgText = holder.msgText;
        byte bg;
        if (mData.isService()) {
            bg = Scheme.THEME_CHAT_BG_SYSTEM;
        } else if ((index & 1) == 0) {
            bg = mData.isIncoming() ? Scheme.THEME_CHAT_BG_IN : Scheme.THEME_CHAT_BG_OUT;
        } else {
            bg = mData.isIncoming() ? Scheme.THEME_CHAT_BG_IN_ODD : Scheme.THEME_CHAT_BG_OUT_ODD;
        }
        row.setBackgroundColor(General.getColor(bg));
        if (mData.fullText == null) {
            mData.fullText = TextFormatter.getFormattedText(text, baseContext);
        }
        if (mData.isMe()) {
            msgImage.setVisibility(ImageView.GONE);
            msgNick.setVisibility(TextView.GONE);
            msgTime.setVisibility(TextView.GONE);
            msgText.setText("* " + mData.getNick() + " " + mData.fullText);
            msgText.setTextColor(General.getColor(mData.isIncoming() ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            msgText.setTextSize(17);
        } else if (mData.isPresence()) {
            msgImage.setVisibility(ImageView.GONE);
            msgNick.setVisibility(TextView.GONE);
            msgTime.setVisibility(TextView.GONE);
            msgText.setText(mData.getNick() + mData.fullText);
            msgText.setTextColor(General.getColor(Scheme.THEME_CHAT_INMSG));
            msgText.setTextSize(17);
        } else {
            if (mData.iconIndex != Message.ICON_NONE) {
                Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
                if (icon == null) {
                    msgImage.setVisibility(ImageView.GONE);
                } else {
                    msgImage.setVisibility(ImageView.VISIBLE);
                    msgImage.setImageBitmap(General.iconToBitmap(icon));
                }
            }

            msgNick.setVisibility(TextView.VISIBLE);
            msgNick.setText(mData.getNick());
            msgNick.setTextColor(General.getColor(mData.isIncoming() ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            msgNick.setTypeface(Typeface.DEFAULT_BOLD);
            msgNick.setTextSize(18);

            msgTime.setVisibility(TextView.VISIBLE);
            msgTime.setText(mData.strTime);
            msgTime.setTextColor(General.getColor(mData.isIncoming() ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            msgTime.setTextSize(18);

            byte color = Scheme.THEME_TEXT;
            if (mData.isIncoming() && !chat.getContact().isSingleUserContact()
                    && Chat.isHighlight(text, chat.getMyName())) {
                color = Scheme.THEME_CHAT_HIGHLIGHT_MSG;
            }

            msgText.setText(mData.fullText);
            msgText.setTextColor(General.getColor(color));
            msgText.setTextSize(18);
        }
        return row;
    }

    static class ViewHolder {
        ImageView msgImage;
        TextView msgNick;
        TextView msgTime;
        TextView msgText;
    }
}