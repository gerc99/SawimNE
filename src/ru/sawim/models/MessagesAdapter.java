package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import sawim.chat.Chat;
import sawim.chat.MessData;
import sawim.chat.message.Message;
import sawim.modules.Emotions;
import sawim.ui.base.Scheme;
import ru.sawim.General;
import ru.sawim.R;

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
        ItemWrapper wr;
        if (row == null) {
            LayoutInflater inf = ((Activity) baseContext).getLayoutInflater();
            row = inf.inflate(R.layout.chat_item, null);
            wr = new ItemWrapper(row);
            row.setTag(wr);
        } else {
            wr = (ItemWrapper) row.getTag();
        }
        wr.populateFrom(i);
        return row;
    }

    private void detectEmotions(Context context,
                                SpannableStringBuilder builder, int startPos, int endPos) {
        Emotions smiles = Emotions.instance;
        for (int index = startPos; index < endPos; ++index) {
            int smileIndex = smiles.getSmile(builder.toString(), index);
            if (-1 != smileIndex) {
                int length = smiles.getSmileText(smileIndex).length();
                Icon icon = smiles.getSmileIcon(smileIndex);
                Bitmap bitmap = Bitmap.createBitmap(icon.getImage().getBitmap(), icon.x, icon.y, icon.getWidth(), icon.getHeight());
                ImageSpan imageSpan = new ImageSpan(context, bitmap, ImageSpan.ALIGN_BASELINE);
                builder.setSpan(imageSpan, index, index + length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index += length - 1;
                break;
            }
        }
    }

    public Spannable getFormattedText(Context context, String text, int color) {
        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        detectEmotions(context, builder, 0, text.length());
        builder.setSpan(new ForegroundColorSpan(color), 0, text.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return builder;
    }

    private class ItemWrapper {
        final View item;

        public ItemWrapper(View item) {
            this.item = item;
        }

        void populateFrom(int index) {
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
                msgText.setText(mData.getNick() + " " + getFormattedText(baseContext, text, color));
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
                msgText.setText(getFormattedText(baseContext, text, General.getColor(color)));
                msgText.setTextSize(18);
            }
        }
    }
}
