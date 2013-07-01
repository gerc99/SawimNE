package sawim.chat.message;

import DrawControls.icons.ImageList;
import ru.sawim.General;
import sawim.chat.MessData;
import protocol.Contact;
import protocol.Protocol;

public abstract class Message {
    public static final ImageList msgIcons = ImageList.createImageList("/msgs.png");
    public static final int ICON_NONE = -1;
    public static final int ICON_SYSREQ = 0;
    public static final int ICON_SYS_OK = 1;
    public static final int ICON_TYPE = 2;
    public static final int ICON_IN_MSG_HI = 3;
    public static final int ICON_IN_MSG = 4;
    public static final int ICON_OUT_MSG = 5;
    public static final int ICON_OUT_MSG_FROM_SERVER = 6;
    public static final int ICON_OUT_MSG_FROM_CLIENT = 7;
	public static final int ICON_MSG_TRACK = 8;

    public static final int NOTIFY_OFF = -1;
    public static final int NOTIFY_NONE = ICON_OUT_MSG;
    public static final int NOTIFY_FROM_SERVER = ICON_OUT_MSG_FROM_SERVER;
    public static final int NOTIFY_FROM_CLIENT = ICON_OUT_MSG_FROM_CLIENT;

    protected boolean isIncoming;
    protected String contactId;
    protected Contact contact;
    protected Protocol protocol;
    private String senderName;
    private MessData mData = null;
    private long newDate;

    protected Message(long date, Protocol protocol, String contactId, boolean isIncoming) {
    	this.newDate = date;
    	this.protocol = protocol;
    	this.contactId = contactId;
        this.isIncoming = isIncoming;
    }
    protected Message(long date, Protocol protocol, Contact contact, boolean isIncoming) {
    	this.newDate = date;
    	this.protocol = protocol;
    	this.contact = contact;
        this.isIncoming = isIncoming;
    }

    public final void setVisibleIcon(MessData mData) {
        this.mData = mData;
    }
    public final void setSendingState(int state) {
        if (!mData.isMe()) {
            mData.iconIndex = state;
        }
        if (getRcvr().hasChat()) {
            if (General.getInstance().getUpdateChatListener() != null)
                General.getInstance().getUpdateChatListener().updateChat();
        }
    }

    public final void setName(String name) {
        senderName = name;
    }
    private String getContactUin() {
        return (null == contact) ? contactId : contact.getUserId();
    }
    
    public final String getSndrUin() {
        return isIncoming ? getContactUin() : protocol.getUserId();
    }
    
    public final String getRcvrUin() {
        return isIncoming ? protocol.getUserId() : getContactUin();
    }
    public boolean isIncoming() {
        return isIncoming;
    }

    protected final Contact getRcvr() {
        return (null == contact) ? protocol.getItemByUIN(contactId) : contact;
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