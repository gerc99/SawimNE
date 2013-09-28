package ru.sawim.models;

import DrawControls.icons.Icon;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Browser;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableStringBuilder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.view.PictureView;
import ru.sawim.widget.MyTextView;
import ru.sawim.view.menu.JuickMenu;
import sawim.Clipboard;
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
public class MessagesAdapter extends BaseAdapter {

    private FragmentActivity activity;
    private List<MessData> items = new ArrayList<MessData>();
    private Protocol currentProtocol;
    private String currentContact;

    private boolean isMultiQuote = false;
    private int position = -1;

    public void init(FragmentActivity activity, Chat chat) {
        this.activity = activity;
        currentProtocol = chat.getProtocol();
        currentContact = chat.getContact().getUserId();
        refreshList(chat.getMessData());
    }

    public void refreshList(List<MessData> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public boolean isMultiQuote() {
        return isMultiQuote;
    }

    public void setMultiQuote(boolean multiShot) {
        isMultiQuote = multiShot;
    }

    public void setPosition(int position) {
        this.position = position;
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
    public View getView(int index, View row, ViewGroup viewGroup) {
        final MessData mData = items.get(index);
        if (mData.messView == null) {
            mData.messView = new MessageItemView(activity);
        }
        MessageItemView item = mData.messView;
        SpannableStringBuilder parsedText = mData.parsedText();
        String nick = mData.getNick();
        boolean incoming = mData.isIncoming();

        ((ViewGroup)item).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        item.msgText.setOnTextLinkClickListener(textLinkClickListener);
        item.msgText.setTypeface(Typeface.DEFAULT);
        byte bg;
        if (mData.isMarked() && isMultiQuote) {
            bg = Scheme.THEME_CHAT_BG_MARKED;
            item.msgText.setTypeface(Typeface.DEFAULT_BOLD);
        } else if (mData.isService())
            bg = Scheme.THEME_CHAT_BG_SYSTEM;
        else if ((index & 1) == 0)
            bg = incoming ? Scheme.THEME_CHAT_BG_IN : Scheme.THEME_CHAT_BG_OUT;
        else
            bg = incoming ? Scheme.THEME_CHAT_BG_IN_ODD : Scheme.THEME_CHAT_BG_OUT_ODD;
        item.setBackgroundColor(Scheme.getColor(bg));

        if (mData.isMe() || mData.isPresence()) {
            item.msgImage.setVisibility(ImageView.GONE);
            item.msgNick.setVisibility(TextView.GONE);
            item.msgTime.setVisibility(TextView.GONE);
            item.msgText.setTextSize(General.getFontSize() - 2);
            if (mData.isMe()) {
                item.msgText.setText("* " + nick + " " + parsedText);
                item.msgText.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            } else {
                item.msgText.setText(nick + parsedText);
                item.msgText.setTextColor(Scheme.getColor(Scheme.THEME_CHAT_INMSG));
            }
        } else {
            if (mData.iconIndex != Message.ICON_NONE) {
                Icon icon = Message.msgIcons.iconAt(mData.iconIndex);
                if (icon == null) {
                    item.msgImage.setVisibility(ImageView.GONE);
                } else {
                    item.msgImage.setVisibility(ImageView.VISIBLE);
                    item.msgImage.setImageDrawable(icon.getImage());
                }
            }

            item.msgNick.setVisibility(TextView.VISIBLE);
            item.msgNick.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            item.msgNick.setTypeface(Typeface.DEFAULT_BOLD);
            item.msgNick.setTextSize(General.getFontSize());
            item.msgNick.setText(nick);

            item.msgTime.setVisibility(TextView.VISIBLE);
            item.msgTime.setTextColor(Scheme.getColor(incoming ? Scheme.THEME_CHAT_INMSG : Scheme.THEME_CHAT_OUTMSG));
            item.msgTime.setTextSize(General.getFontSize() / 2);
            item.msgTime.setText(mData.strTime);

            item.msgText.setText(parsedText);
            item.msgText.setTextSize(General.getFontSize());
            item.msgText.setTextColor(Scheme.getColor(mData.getMessColor()));
            item.msgText.setLinkTextColor(0xff00e4ff);
        }
        item.addDivider(activity, Scheme.getColor(Scheme.THEME_TEXT),
                position == index && index > 0 && position != getCount());
        return item;
    }

    private MyTextView.TextLinkClickListener textLinkClickListener = new MyTextView.TextLinkClickListener() {
        @Override
        public void onTextLinkClick(View textView, String clickedString, boolean isLongTap) {
            if (clickedString.length() == 0) return;
            boolean isJuick = clickedString.substring(0, 1).equals("@") || clickedString.substring(0, 1).equals("#");
            if (isJuick) {
                new JuickMenu(activity, currentProtocol, currentContact, clickedString).show();
                return;
            }
            if (isLongTap) {
                CharSequence[] items = new CharSequence[2];
                items[0] = activity.getString(R.string.copy);
                items[1] = activity.getString(R.string.add_contact);
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(true);
                builder.setTitle(R.string.url_menu);
                final String finalClickedString = clickedString;
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                Clipboard.setClipBoardText(finalClickedString);
                                break;
                            case 1:
                                General.openUrl(finalClickedString);
                                break;
                        }
                    }
                });
                try {
                    builder.create().show();
                } catch(Exception e){
                    // WindowManager$BadTokenException will be caught and the app would not display
                }
            } else {
                if (!clickedString.startsWith("http://") && !clickedString.startsWith("https://"))
                    clickedString = "http://" + clickedString;
                if ((clickedString.endsWith(".jpg"))
                        || (clickedString.endsWith(".jpeg"))
                        || (clickedString.endsWith(".png"))
                        || (clickedString.endsWith(".gif"))
                        || (clickedString.endsWith(".bmp"))) {
                    PictureView pictureView = new PictureView();
                    pictureView.setLink(clickedString);
                    FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                    transaction.add(pictureView, PictureView.TAG);
                    transaction.commitAllowingStateLoss();
                } else {
                    Uri uri = Uri.parse(clickedString);
                    Context context = textView.getContext();
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                    context.startActivity(intent);
                }
            }
        }
    };
}