package sawim.chat;

import protocol.Contact;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.text.TextFormatter;
import ru.sawim.widget.chat.MessageItemView;
import sawim.chat.message.Message;
import sawim.comm.Util;

public final class MessData {

    private long time;
    private String nick;
    public String strTime;
    private byte iconIndex = Message.ICON_NONE;
    private short rowData;
    private final boolean confHighLight;
    private boolean isHighLight;

    private CharSequence parsedText;
    public MessageItemView messView;

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
        strTime = Util.getLocalDateString(time, today);

        confHighLight = (isIncoming() && !currentContact.isSingleUserContact() && isHighLight());
        parsedText = TextFormatter.getInstance().parsedText(currentContact, text);
    }

    public long getTime() {
        return time;
    }

    public String getNick() {
        return nick;
    }

    public CharSequence getText() {
        return parsedText;
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

    public boolean isHighLight() {
        return isHighLight;
    }

    public byte getMessColor() {
        byte messColor = Scheme.THEME_TEXT;
        if (confHighLight)
            messColor = Scheme.THEME_CHAT_HIGHLIGHT_MSG;
        return messColor;
    }

    public byte getIconIndex() {
        return iconIndex;
    }

    public void setIconIndex(byte iconIndex) {
        this.iconIndex = iconIndex;
    }

    public boolean isConfHighLight() {
        return confHighLight;
    }
}