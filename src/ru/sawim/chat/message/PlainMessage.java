
package ru.sawim.chat.message;

import protocol.Contact;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;

public class PlainMessage extends Message {

    private String text;
    private int messageId;
    private boolean offline;
    public static final String CMD_WAKEUP = "/wakeup";
    public static final String CMD_ME = "/me ";

    public PlainMessage(String contactUin, Protocol protocol, long date, String text, boolean offline) {
        super(date, protocol.getUserId(), contactUin, true);
        if (text.length() > 0 && '\n' == text.charAt(0)) {
            text = text.substring(1);
        }
        this.text = text;
        this.offline = offline;
    }

    public PlainMessage(Protocol protocol, String rcvr, long date, String text) {
        super(date, protocol.getUserId(), rcvr, false);
        this.text = StringConvertor.notNull(text);
        this.offline = false;
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

