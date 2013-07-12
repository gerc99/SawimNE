package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.*;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import protocol.jabber.Jabber;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.view.ChatView;
import ru.sawim.view.MyTextView;
import sawim.Clipboard;
import sawim.TextFormatter;
import sawim.chat.Chat;
import sawim.chat.MessData;
import sawim.chat.message.Message;
import ru.sawim.Scheme;
import sawim.util.JLocale;

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
            holder.msgText = (MyTextView) row.findViewById(R.id.msg_text);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
        MessData mData = items.get(index);
        String text = mData.getText();
        ImageView msgImage = holder.msgImage;
        TextView msgNick = holder.msgNick;
        TextView msgTime = holder.msgTime;
        MyTextView msgText = holder.msgText;

        msgText.setOnTextLinkClickListener(this);
        ((ViewGroup)row).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
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
                    msgImage.setImageBitmap(icon.getImage());
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

            MovementMethod m = msgText.getMovementMethod();
            if ((m == null) || !(m instanceof LinkMovementMethod)) {
                if (msgText.getLinksClickable()) {
                    msgText.setMovementMethod(LinkMovementMethod.getInstance());
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
        final AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(baseContext, R.style.AlertDialogCustom));
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