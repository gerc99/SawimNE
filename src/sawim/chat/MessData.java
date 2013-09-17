package sawim.chat;

import android.text.SpannableStringBuilder;
import protocol.Contact;
import ru.sawim.General;
import ru.sawim.Scheme;
import ru.sawim.models.MessagesAdapter;
import ru.sawim.text.TextFormatter;
import ru.sawim.view.menu.JuickMenu;
import sawim.comm.Util;

public final class MessData {

    private long time;
    private String text;
    private String nick;
    public String strTime;
    public int iconIndex;
    private short rowData;
    private final boolean confHighLight;
    private boolean isHighLight;
    private Contact currentContact;

    private SpannableStringBuilder parsedText;
    private static final TextFormatter textFormatter = new TextFormatter();

    public static final short URLS = 1;
    public static final short INCOMING = 2;
    public static final short ME = 4;
    public static final short PROGRESS = 8;
    public static final short SERVICE = 16;
    public static final short PRESENCE = 32;
    public static final short MARKED = 64;

    public MessData(Contact currentContact, long time, String text, String nick, short flags, int iconIndex, boolean highLight) {
        this.currentContact = currentContact;
        isHighLight = highLight;
        this.text = text;
        this.nick = nick;
        this.time = time;
        this.rowData = flags;
        this.iconIndex = iconIndex;
        boolean today = (General.getCurrentGmtTime() - 24 * 60 * 60 < time);
        strTime = Util.getLocalDateString(time, today);

        confHighLight = (isIncoming() && !currentContact.isSingleUserContact() && isHighLight());

        parsedText = TextFormatter.getFormattedText(text);
        if (currentContact.getUserId().equals(JuickMenu.JUICK) || currentContact.getUserId().equals(JuickMenu.JUBO))
            parsedText = textFormatter.getTextWithLinks(parsedText, 0xff00e4ff, JuickMenu.Mode.juick, MessagesAdapter.textLinkClickListener);
        else if (currentContact.getUserId().equals(JuickMenu.PSTO))
            parsedText = textFormatter.getTextWithLinks(parsedText, 0xff00e4ff, JuickMenu.Mode.psto, MessagesAdapter.textLinkClickListener);
        else
            parsedText = textFormatter.getTextWithLinks(parsedText, 0xff00e4ff, JuickMenu.Mode.none, MessagesAdapter.textLinkClickListener);
    }

    public Contact getCurrentContact() {
        return currentContact;
    }

    public long getTime() {
        return time;
    }

    public String getNick() {
        return nick;
    }

    public String getText() {
        return text;
    }

    public SpannableStringBuilder parsedText() {
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
}