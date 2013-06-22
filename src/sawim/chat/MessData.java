package sawim.chat;

import android.text.Spannable;
import sawim.Sawim;
import sawim.chat.message.Message;
import sawim.comm.Util;

public final class MessData {
    private long time;
    private String text;
    private String nick;
    public String strTime;
    public int iconIndex;
    private short rowData;
	private Message message;

    public Spannable fullMeText;
    public Spannable fullText;

    public static final short URLS = 1;
    public static final short INCOMING = 2;
    public static final short ME = 4;
    public static final short PROGRESS = 8;
    public static final short SERVICE = 16;

	public MessData(Message m, long time, String text, String nick, short flags, int iconIndex) {
		message = m;
        init(time, text, nick, flags, iconIndex);
	}
    public MessData(long time, String text, String nick, short flags, int iconIndex) {
        init(time, text, nick, flags, iconIndex);
    }
    public void init(long time, String text, String nick, short flags, int iconIndex) {
        this.text = text;
        this.nick = nick;
        this.time = time;
        this.rowData = flags;
        this.iconIndex = iconIndex;
        boolean today = (Sawim.getCurrentGmtTime() - 24 * 60 * 60 < time);
        strTime = Util.getLocalDateString(time, today);
    }
	
	public Message getMessage() {
		return message;
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
}

