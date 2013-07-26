package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.view.MyTextView;
import sawim.Clipboard;
import sawim.TextFormatter;
import sawim.chat.Chat;
import sawim.chat.MessData;
import sawim.chat.message.Message;
import ru.sawim.Scheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.04.13
 * Time: 21:33
 * To change this template use File | Settings | File Templates.
 */
public class MessagesAdapter extends BaseAdapter implements MyTextView.TextLinkClickListener {

    private Context baseContext;
    private List<MessData> items;
    private LayoutInflater inf;

    public void init(Context context, List<MessData> items) {
        baseContext = context;
        inf = LayoutInflater.from(baseContext);
        this.items = items;
    }

    public void refreshList(List<MessData> list) {
        items = list;
        notifyDataSetChanged();
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
            row = inf.inflate(R.layout.chat_item, null);
            holder = new ViewHolder();
            holder.msgImage = (ImageView) row.findViewById(R.id.msg_icon);
            holder.msgNick = (TextView) row.findViewById(R.id.msg_nick);
            holder.msgTime = (TextView) row.findViewById(R.id.msg_time);
            holder.msgText = (MyTextView) row.findViewById(R.id.msg_text);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
        MessData mData = items.get(index);
        String text = mData.getText();
        boolean incoming = mData.isIncoming();

        ((ViewGroup)row).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        byte bg;
        if (mData.isService()) {
            bg = Scheme.THEME_CHAT_BG_SYSTEM;
        } else if ((index & 1) == 0) {
            bg = incoming ? Scheme.THEME_CHAT_BG_IN : Scheme.THEME_CHAT_BG_OUT;
        } else {
            bg = incoming ? Scheme.THEME_CHAT_BG_IN_ODD : Scheme.THEME_CHAT_BG_OUT_ODD;
        }
        row.setBackgroundColor(Scheme.getColor(bg));
        if (mData.fullText == null) {
            holder.msgText.setOnTextLinkClickListener(this);
            mData.fullText = TextFormatter.getFormattedText(text, baseContext);
        }
        if (mData.isMe() || mData.isPresence()) {
            holder.msgImage.setVisibility(ImageView.GONE);
            holder.msgNick.setVisibility(TextView.GONE);
            holder.msgTime.setVisibility(TextView.GONE);
            if (mData.isMe())
                holder.msgText.setText("* " + mData.getNick() + " " + mData.fullText);
            else
                holder.msgText.setText(mData.getNick() + mData.fullText);
            holder.msgText.setTextColor(mData.getColor());
            holder.msgText.setTextSize(General.getFontSize() - 2);
        } else {
            if (mData.iconIndex != Message.ICON_NONE) {
                Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
                if (icon == null) {
                    holder.msgImage.setVisibility(ImageView.GONE);
                } else {
                    holder.msgImage.setVisibility(ImageView.VISIBLE);
                    holder.msgImage.setImageBitmap(icon.getImage());
                }
            }

            holder.msgNick.setVisibility(TextView.VISIBLE);
            holder.msgNick.setText(mData.getNick());
            holder.msgNick.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            holder.msgNick.setTypeface(Typeface.DEFAULT_BOLD);
            holder.msgNick.setTextSize(General.getFontSize());

            holder.msgTime.setVisibility(TextView.VISIBLE);
            holder.msgTime.setText(mData.strTime);
            holder.msgTime.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            holder.msgTime.setTextSize(General.getFontSize() - 4);

            holder.msgText.setText(mData.fullText);
            holder.msgText.setTextColor(mData.getColor());
            holder.msgText.setTextSize(General.getFontSize());
            MovementMethod m = holder.msgText.getMovementMethod();
            if ((m == null) || !(m instanceof LinkMovementMethod)) {
                if (holder.msgText.getLinksClickable()) {
                    holder.msgText.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        }
        return row;
    }

    @Override
    public void onTextLinkClick(View textView, final String clickedString) {
        if (clickedString.length() == 0) return;
        CharSequence[] items = new CharSequence[3];
        items[0] = baseContext.getString(R.string.open_url);
        items[1] = baseContext.getString(R.string.copy);
        items[2] = baseContext.getString(R.string.add_contact);
        final AlertDialog.Builder builder = new AlertDialog.Builder(baseContext);
        builder.setCancelable(true);
        builder.setTitle(R.string.url_menu);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Uri uri = Uri.parse(clickedString);
                        if (uri != null) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(uri);
                            baseContext.startActivity(intent);
                        }
                        break;
                    case 1:
                        Clipboard.setClipBoardText(clickedString);
                        break;
                    case 2:
                        General.openUrl(clickedString);
                        break;
                }
            }
        });
        builder.create().show();
    }

    static class ViewHolder {
        ImageView msgImage;
        TextView msgNick;
        TextView msgTime;
        MyTextView msgText;
    }
}