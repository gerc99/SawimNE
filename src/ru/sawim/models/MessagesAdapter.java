package ru.sawim.models;

import DrawControls.icons.Icon;
import android.content.Context;
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
import sawim.ui.base.Scheme;

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

    public MessagesAdapter(Context context, Chat chat) {
        this.baseContext = context;
        this.chat = chat;
        this.items = chat.getMessData();
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
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View row = convertView;
        if (row == null) {
            LayoutInflater inf = LayoutInflater.from(baseContext);
            row = inf.inflate(R.layout.chat_item, null);
        } else {
            row = convertView;
        }
        populateFrom(row, i);
        return row;
    }

    void populateFrom(View item, int index) {
        MessData mData = items.get(index);
        String text = mData.getText();
        ImageView msgImage = (ImageView) item.findViewById(R.id.msg_icon);
        TextView msgNick = (TextView) item.findViewById(R.id.msg_nick);
        TextView msgTime = (TextView) item.findViewById(R.id.msg_time);
        TextView msgText = (TextView) item.findViewById(R.id.msg_text);

        byte bg;
        if (mData.isMarked()) {
            bg = Scheme.THEME_CHAT_BG_MARKED;
        } else if (mData.isService()) {
            bg = Scheme.THEME_CHAT_BG_SYSTEM;
        } else if ((index & 1) == 0) {
            bg = mData.isIncoming() ? Scheme.THEME_CHAT_BG_IN : Scheme.THEME_CHAT_BG_OUT;
        } else {
            bg = mData.isIncoming() ? Scheme.THEME_CHAT_BG_IN_ODD : Scheme.THEME_CHAT_BG_OUT_ODD;
        }
        item.setBackgroundColor(General.getColor(bg));

        if (mData.isMe()) {
            msgImage.setVisibility(ImageView.GONE);
            msgNick.setVisibility(TextView.GONE);
            msgTime.setVisibility(TextView.GONE);
            int color = General.getColor(mData.isIncoming() ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG);
            if (mData.fullMeText == null)
                mData.fullMeText = TextFormatter.getFormattedText(text, baseContext, color);
            msgText.setText(mData.getNick() + " " + mData.fullMeText);
            msgText.setTextSize(14);
        } else {
            Icon icon = Message.msgIcons.iconAt(chat.getIcon(mData.getMessage(), mData.isIncoming()));
            if (icon == null) {
                msgImage.setVisibility(ImageView.GONE);
            } else {
                msgImage.setVisibility(ImageView.VISIBLE);
                msgImage.setImageBitmap(General.iconToBitmap(icon));
            }

            msgNick.setVisibility(TextView.VISIBLE);
            msgNick.setText(mData.getNick());
            msgNick.setTextColor(General.getColor(mData.isIncoming() ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));

            msgTime.setVisibility(TextView.VISIBLE);
            msgTime.setText("(" + mData.strTime + ")");
            msgTime.setTextColor(General.getColor(mData.isIncoming() ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));

            byte color = Scheme.THEME_TEXT;
            if (mData.isIncoming() && !chat.getContact().isSingleUserContact()
                    && Chat.isHighlight(text, chat.getMyName())) {
                color = Scheme.THEME_CHAT_HIGHLIGHT_MSG;
            }
            if (mData.fullText == null)
                mData.fullText = TextFormatter.getFormattedText(text, baseContext, General.getColor(color));
            msgText.setText(mData.fullText);
            msgText.setTextSize(18);
        }
    }
}
