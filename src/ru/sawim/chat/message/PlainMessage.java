
package ru.sawim.chat.message;

import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.comm.JLocale;

public class PlainMessage extends Message {

    private String text;
    private int messageId;
    private boolean offline;
    public static final String CMD_WAKEUP = "/wakeup";
    public static final String CMD_ME = "/me ";

    public PlainMessage(String contactUin, Protocol protocol, long date, String text, boolean offline) {
        this(contactUin, protocol, date, text, offline, true);
    }

    public PlainMessage(Protocol protocol, String rcvr, long date, String text) {
        this(rcvr, protocol, date, text, false, false);
    }

    public PlainMessage(String contactUin, Protocol protocol, long date, String text, boolean offline, boolean isIncoming) {
        this(contactUin, protocol.getUserId(), date, text, offline, isIncoming);
    }

    public PlainMessage(String contactUin, String myId, long date, String text, boolean offline, boolean isIncoming) {
        super(date, myId, contactUin, isIncoming);
        this.text = text;
        this.offline = offline;
    }

    public boolean isOffline() {
        return offline;
    }

    public String getText() {
        return text;
    }

    public boolean isWakeUp() {
        return text.startsWith(PlainMessage.CMD_WAKEUP);
    }

    public String getProcessedText() {
        String messageText = text;
        if (isWakeUp()) {
            if (isIncoming()) {
                messageText = PlainMessage.CMD_ME + JLocale.getString(R.string.wake_you_up);
            } else {
                messageText = PlainMessage.CMD_ME + JLocale.getString(R.string.wake_up);
            }
        }
        return messageText;
    }

    public void setMessageId(int id) {
        messageId = id;
    }

    public int getMessageId() {
        return messageId;
    }
}

