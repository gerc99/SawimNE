


package sawim.chat.message;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import sawim.chat.MessData;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.General;
import ru.sawim.activities.ChatActivity;

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
	public int iconIndex = ICON_OUT_MSG;

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
        if (mData.isMe()) {
            Icon icon = msgIcons.iconAt(state);
            //if ((null != par) && (null != icon)) {
            //    par.replaceFirstIcon(icon);
            //}
        } else {
			iconIndex = state;
            mData.iconIndex = state;
        }
        Contact rcvr = getRcvr();
        if (rcvr.hasChat()) {
            if (General.getInstance().getUpdateChatListener() != null)
                General.getInstance().getUpdateChatListener().updateChat();
        }
    }

    private Icon iconStatus;
    private String nameStatus; 
    public void setStatusIcon(Icon status_icon) {
        iconStatus = status_icon;
    }
    public Icon getStatusIcon() {
        return iconStatus;
    }
    public void setStatusName(String status_name) {
        nameStatus = status_name;
    }
    public String getStatusName() {
        return nameStatus;
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

