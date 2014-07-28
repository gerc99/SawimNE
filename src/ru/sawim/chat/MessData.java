package ru.sawim.chat;

import android.text.Layout;
import android.text.SpannableStringBuilder;
import protocol.Contact;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.chat.message.Message;
import ru.sawim.text.TextFormatter;
import ru.sawim.widget.chat.MessageItemView;

public final class MessData {

    private long time;
    private String nick;
    private String strTime;
    private byte iconIndex = Message.ICON_NONE;
    private short rowData;
    private boolean isHighLight;

    public Layout layout;

    public static final short INCOMING = 2;
    public static final short ME = 4;
    public static final short PROGRESS = 8;
    public static final short SERVICE = 16;
    public static final short PRESENCE = 32;
    public static final short MARKED = 64;

    public MessData(Contact currentContact, long time, String text, String nick, short flags, boolean highLight) {
        isHighLight = highLight;
        this.nick = nick;
        this.time = time;
        this.rowData = flags;
        boolean today = (SawimApplication.getCurrentGmtTime() - 24 * 60 * 60 < time);
        strTime = ru.sawim.comm.Util.getLocalDateString(time, today);

        CharSequence parsedText = TextFormatter.getInstance().parsedText(currentContact, text);
        if (isMe()) {
            parsedText = new SpannableStringBuilder().append("* ").append(nick).append(" ").append(parsedText);
        } else if (isPresence()) {
            parsedText = new SpannableStringBuilder().append(strTime).append(" ").append(nick).append(parsedText);
        }
        layout = MessageItemView.buildLayout(parsedText);
    }

    public MessData(Contact currentContact, long time, String text, String nick, short flags) {
        isHighLight = false;
        this.nick = nick;
        this.time = time;
        this.rowData = flags;
        boolean today = (SawimApplication.getCurrentGmtTime() - 24 * 60 * 60 < time);
        strTime = ru.sawim.comm.Util.getLocalDateString(time, today);

        CharSequence parsedText = TextFormatter.getInstance().parsedText(currentContact, text);
        layout = MessageItemView.buildLayout(parsedText);
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

    public byte getMessColor() {
        byte messColor = Scheme.THEME_TEXT;
        if (isHighLight)
            messColor = Scheme.THEME_CHAT_HIGHLIGHT_MSG;
        return messColor;
    }

    public byte getIconIndex() {
        return iconIndex;
    }

    public void setIconIndex(byte iconIndex) {
        this.iconIndex = iconIndex;
    }

    public String getStrTime() {
        return strTime;
    }
}