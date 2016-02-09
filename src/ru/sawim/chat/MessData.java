package ru.sawim.chat;

import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import java.util.List;

import protocol.Contact;
import ru.sawim.R;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.text.TextFormatter;
import ru.sawim.view.menu.JuickMenu;
import ru.sawim.widget.chat.MessageItemView;

public final class MessData {

    public static final short INCOMING = 2;
    public static final short ME = 4;
    public static final short PROGRESS = 8;
    public static final short SERVICE = 16;
    public static final short PRESENCE = 32;
    public static final short MARKED = 64;

    private long time;
    private String nick;
    private String strTime;
    private int iconIndex = Message.ICON_NONE;
    private short rowData;
    private boolean isHighLight;
    private String serverMsgId;
    private List<String> urlLinks;

    public Layout layout;

    public MessData(Contact currentContact, long time, String text, String nick, short flags, boolean highLight) {
        isHighLight = highLight;
        this.nick = nick;
        this.time = time;
        this.rowData = flags;
        //boolean today = (SawimApplication.getCurrentGmtTime() / 1000 - 24 * 60 * 60 < time);
        strTime = ru.sawim.comm.Util.getLocalDateString(time, true);

        CharSequence parsedText = parsedText(currentContact, text);
        layout = MessageItemView.buildLayout(parsedText, isHighLight ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    public MessData(Contact currentContact, long time, String text, String nick, short flags) {
        this(currentContact, time, text, nick, flags, false);
    }

    private CharSequence parsedText(Contact contact, CharSequence text) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        if (isMe()) {
            builder.append(formatCmdMe(nick, text.toString()));
         } else if (isPresence()) {
            builder.append(strTime).append(" ").append(nick);
            builder.append(text);
        } else {
            builder.append(text);
        }
        if (contact != null && !contact.isConference()) {
            if (contact.getProtocol() != null) {
                String userId = contact.getProtocol().getUniqueUserId(contact);
                if (userId.startsWith(JuickMenu.JUICK) || userId.startsWith(JuickMenu.JUBO)) {
                    TextFormatter.getInstance().addLinksForBots(builder, JuickMenu.MODE_JUICK);
                } else if (userId.startsWith(JuickMenu.PSTO) || userId.startsWith(JuickMenu.POINT)) {
                    TextFormatter.getInstance().addLinksForBots(builder, JuickMenu.MODE_PSTO);
                }
            }
        }
        urlLinks = TextFormatter.getInstance().getLinks(builder);
        TextFormatter.getInstance().detectEmotions(builder);
        return builder;
    }

    public static String formatCmdMe(String nick, String messageText) {
        boolean isMe = messageText.startsWith(PlainMessage.CMD_ME);
        if (isMe) {
            StringBuilder builder = new StringBuilder();
            builder.append("* ").append(nick).append(" ")
                   .append(messageText.substring(PlainMessage.CMD_ME.length()));
            return builder.toString();
        }
        return messageText;
    }

    public long getTime() {
        return time;
    }

    public String getNick() {
        return nick;
    }

    public CharSequence getText() {
        return layout.getText();
    }

    public short getRowData() {
        return rowData;
    }

    public boolean isIncoming() {
        return (rowData & INCOMING) != 0;
    }

    public boolean isMe() {
        return (rowData & ME) != 0;
    }

    public boolean isFile() {
        return (rowData & PROGRESS) != 0;
    }

    public boolean isService() {
        return (rowData & SERVICE) != 0;
    }

    public boolean isPresence() {
        return (rowData & PRESENCE) != 0;
    }

    public boolean isMarked() {
        return (rowData & MARKED) != 0;
    }

    public void setMarked(boolean marked) {
        rowData = (short) (marked ? (rowData | MARKED) : (rowData & ~MARKED));
    }

    public boolean isMessage() {
        return !(isPresence() || isService() || isFile());
    }

    public boolean isHighLight() {
        return isHighLight;
    }

    public int getMessColor() {
        int messColor = R.attr.text;
        if (isHighLight)
            messColor = R.attr.chat_highlight_msg;
        return messColor;
    }

    public int getIconIndex() {
        return iconIndex;
    }

    public void setIconIndex(int iconIndex) {
        this.iconIndex = iconIndex;
    }

    public String getStrTime() {
        return strTime;
    }

    public String getServerMsgId() {
        return serverMsgId;
    }

    public void setServerMsgId(String serverMsgId) {
        this.serverMsgId = serverMsgId;
    }

    public List<String> getUrlLinks() {
        return urlLinks;
    }
}
