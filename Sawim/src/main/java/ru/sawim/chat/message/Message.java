package ru.sawim.chat.message;

import android.graphics.drawable.BitmapDrawable;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.SawimResources;

public abstract class Message {
    public static final int ICON_NONE = -1;
    public static final int ICON_SYSREQ = 0;
    public static final int ICON_SYS_OK = 1;
    public static final int ICON_TYPE = 2;
    public static final int ICON_IN_MSG_HI = 3;
    public static final int ICON_IN_MSG = 4;
    public static final int ICON_OUT_MSG_FROM_SERVER = 6;
    public static final int ICON_OUT_MSG_FROM_CLIENT = 7;

    public static final int NOTIFY_FROM_SERVER = ICON_OUT_MSG_FROM_SERVER;
    public static final int NOTIFY_FROM_CLIENT = ICON_OUT_MSG_FROM_CLIENT;

    private String messageId;
    private boolean isIncoming;
    private String contactId;
    private String myId;
    private String senderName;
    private long newDate;
    private String serverMsgId;

    public static BitmapDrawable getIcon(int type) {
        switch (type) {
            case ICON_SYSREQ:
                return SawimResources.AUTH_REQ_ICON;
            case ICON_SYS_OK:
                return SawimResources.AUTH_GRANT_ICON;
            case ICON_TYPE:
                return SawimResources.typingIcon;
            case ICON_IN_MSG_HI:
                return SawimResources.PERSONAL_MESSAGE_ICON;
            case ICON_IN_MSG:
                return SawimResources.MESSAGE_ICON;
        }
        return null;
    }

    protected Message(String messageId, long date, String myId, String contactId, boolean isIncoming) {
        this.messageId = messageId;
        this.newDate = date;
        this.myId = myId;
        this.contactId = contactId;
        this.isIncoming = isIncoming;
    }

    public final void setName(String name) {
        senderName = name;
    }

    public String getContactUin() {
        return contactId;
    }

    public final String getSndrUin() {
        return isIncoming ? getContactUin() : myId;
    }

    public final String getRcvrUin() {
        return isIncoming ? myId : getContactUin();
    }

    public boolean isIncoming() {
        return isIncoming;
    }

    protected final Contact getRcvr(Protocol protocol) {
        return protocol.getItemByUID(contactId);
    }

    public boolean isOffline() {
        return false;
    }

    public final long getNewDate() {
        return newDate;
    }

    public String getName() {
        return senderName;
    }

    public abstract String getText();

    public String getProcessedText() {
        return getText();
    }

    public boolean isWakeUp() {
        return false;
    }

    public void setServerMsgId(String serverMsgId) {
        this.serverMsgId = serverMsgId;
    }

    public String getServerMsgId() {
        return serverMsgId;
    }

    public String getMessageId() {
        return messageId;
    }
}