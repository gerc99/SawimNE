package ru.sawim.chat.message;

import android.graphics.drawable.BitmapDrawable;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.SawimResources;
import ru.sawim.chat.MessData;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

public abstract class Message {
    public static final byte ICON_NONE = -1;
    public static final byte ICON_SYSREQ = 0;
    public static final byte ICON_SYS_OK = 1;
    public static final byte ICON_TYPE = 2;
    public static final byte ICON_IN_MSG_HI = 3;
    public static final byte ICON_IN_MSG = 4;
    public static final byte ICON_OUT_MSG_FROM_SERVER = 6;
    public static final byte ICON_OUT_MSG_FROM_CLIENT = 7;

    public static final byte NOTIFY_FROM_SERVER = ICON_OUT_MSG_FROM_SERVER;
    public static final byte NOTIFY_FROM_CLIENT = ICON_OUT_MSG_FROM_CLIENT;

    private boolean isIncoming;
    private String contactId;
    private String myId;
    private String senderName;
    private MessData mData = null;
    private long newDate;

    public static BitmapDrawable getIcon(byte type) {
        switch (type) {
            case ICON_SYSREQ:
                return SawimResources.authReqIcon;
            case ICON_SYS_OK:
                return SawimResources.authGrantIcon;
            case ICON_TYPE:
                return SawimResources.typingIcon;
            case ICON_IN_MSG_HI:
                return SawimResources.personalMessageIcon;
            case ICON_IN_MSG:
                return SawimResources.messageIcon;
        }
        return null;
    }

    protected Message(long date, String myId, String contactId, boolean isIncoming) {
        this.newDate = date;
        this.myId = myId;
        this.contactId = contactId;
        this.isIncoming = isIncoming;
    }

    protected Message(long date, String myId, Contact contact, boolean isIncoming) {
        this.newDate = date;
        this.myId = myId;
        this.contactId = contact.getUserId();
        this.isIncoming = isIncoming;
    }

    public final void setVisibleIcon(MessData mData) {
        this.mData = mData;
    }

    public final void setSendingState(Protocol protocol, byte state) {
        if (mData != null) {
            Contact contact = protocol.getItemByUIN(contactId);
            mData.setIconIndex(state);
            HistoryStorage historyStorage = protocol.getChat(contact).getHistory();
            if (historyStorage != null)
                historyStorage.updateText(mData);
            if (RosterHelper.getInstance().getUpdateChatListener() != null)
                RosterHelper.getInstance().getUpdateChatListener().updateMessages(contact);
        }
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
        return protocol.getItemByUIN(contactId);
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
}