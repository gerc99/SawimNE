package protocol;

import DrawControls.icons.ImageList;
import DrawControls.tree.TreeNode;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.SubMenu;
import ru.sawim.view.FileProgressView;
import ru.sawim.General;
import sawim.Options;
import sawim.chat.Chat;
import sawim.cl.ContactList;
import sawim.comm.Sortable;
import sawim.comm.StringConvertor;
import sawim.modules.tracking.Tracking;
import ru.sawim.Scheme;
import ru.sawim.R;


abstract public class Contact extends TreeNode implements Sortable {
    public static final ImageList authIcon = ImageList.createImageList("/auth.png");
    public static final ImageList serverListsIcons = ImageList.createImageList("/serverlists.png");

    protected String userId;
    private String name;
    private int groupId = Group.NOT_IN_GROUP;
    private int booleanValues;
    private byte status = StatusInfo.STATUS_OFFLINE;
    private String statusText = null;
    private int xstatus = XStatusInfo.XSTATUS_NONE;
    private String xstatusText = null;
    public short clientIndex = ClientInfo.CLI_NONE;
    String version = "";
    public long chaingingStatusTime = 0;

    public final boolean isOnline() {
        return (StatusInfo.STATUS_OFFLINE != status);
    }
    public void setTimeOfChaingingStatus(long time) {
        chaingingStatusTime = time;
    }
	public String annotations = null;

	public byte isHistory() {
	    if (Tracking.isTracking(getUserId(), Tracking.GLOBAL) == Tracking.TRUE) {
      		if (Tracking.beginTrackActionItem(this, Tracking.ACTION_HISTORY) == Tracking.TRUE) {
			    return Tracking.TRUE;
			}
		}
		return Tracking.FALSE;
	}

	public byte subcontactsS() {
		return (byte)0;
	}
	public boolean isConference() {
	    return false;
	}

    public final String getUserId() {
        return userId;
    }

    public final String getName() {
        return name;
    }
    public void setName(String newName) {
        if (!StringConvertor.isEmpty(newName)) {
    	    name = newName;
        }
    }
    public final void setGroupId(int id) {
        groupId = id;
    }
    public final int getGroupId() {
        return groupId;
    }
    public final void setGroup(Group group) {
        setGroupId((null == group) ? Group.NOT_IN_GROUP : group.getId());
    }
    public String getDefaultGroupName() {
        return null;
    }
    public Protocol getProtocol() {
        return ContactList.getInstance().getProtocol(this);
    }

    public final void setXStatus(int index, String text) {
        xstatus = index;
        xstatusText = (XStatusInfo.XSTATUS_NONE == index) ? null : text;
    }
    public final int getXStatusIndex() {
        return xstatus;
    }
    public final String getXStatusText() {
        return xstatusText;
    }
    
    public void setClient(short clientNum, String ver) {
        clientIndex = clientNum;
        version = StringConvertor.notNull(ver);
    }

    public void setOfflineStatus() {
        if (isOnline()) {
            setTimeOfChaingingStatus(General.getCurrentGmtTime());
			
			String id = getUserId();
			if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
				if (Tracking.isTracking(id, Tracking.EVENT_EXIT) == Tracking.TRUE) {
                    Tracking.beginTrackAction(this, Tracking.EVENT_EXIT);
			    }
            } else if (Tracking.isTracking(id, Tracking.EVENT_EXIT) == Tracking.FALSE) {
			}
        }
        setStatus(StatusInfo.STATUS_OFFLINE, null);
        setXStatus(XStatusInfo.XSTATUS_NONE, null);
        beginTyping(false);
    }
    public final byte getStatusIndex() {
        return status;
    }
    public final String getStatusText() {
        return statusText;
    }
    protected final void setStatus(byte statusIndex, String text) {
        if (!isOnline() && (StatusInfo.STATUS_OFFLINE != statusIndex)) {
            setTimeOfChaingingStatus(General.getCurrentGmtTime());
        }
        status = statusIndex;
        statusText = (StatusInfo.STATUS_OFFLINE == status) ? null : text;
    }

    public void activate(Protocol p) {
        ContactList.getInstance().setCurrentContact(this);
    }

    FileProgressView fileProgressView;
    public void addFileProgress() {
        fileProgressView = new FileProgressView();
    }

    public void changeFileProgress(int percent, String caption, String text) {
        fileProgressView.changeFileProgress(percent, caption, text);
    }

    public void showFileProgress(FragmentActivity activity) {
        fileProgressView.show(activity.getSupportFragmentManager(), "file");
    }

    public static final byte CONTACT_NO_AUTH       = 1 << 1; 
    private static final byte CONTACT_IS_TEMP      = 1 << 3; 
    
    public static final byte SL_VISIBLE            = 1 << 4; 
    public static final byte SL_INVISIBLE          = 1 << 5; 
    public static final byte SL_IGNORE             = 1 << 6; 

    private static final int TYPING                = 1 << 8; 
    private static final int HAS_CHAT              = 1 << 9; 

    public final void setBooleanValue(byte key, boolean value) {
        if (value) {
            booleanValues |= key;
        } else {
            booleanValues &= ~key;
        }
    }
    public final boolean isTemp() {
        return (booleanValues & CONTACT_IS_TEMP) != 0;
    }
    public final boolean isAuth() {
        return (booleanValues & CONTACT_NO_AUTH) == 0;
    }
    public final void setBooleanValues(byte vals) {
        booleanValues = (booleanValues & ~0xFF) | (vals & 0x7F);
    }
    public final byte getBooleanValues() {
        return (byte)(booleanValues & 0x7F);
    }
    public final void setTempFlag(boolean isTemp) {
        setBooleanValue(Contact.CONTACT_IS_TEMP, isTemp);
    }
    public final void beginTyping(boolean typing) {
        if (typing && isOnline()) {
            booleanValues |= TYPING;
        } else {
            booleanValues &= ~TYPING;
        }
    }
    public final boolean isTyping() {
        return (booleanValues & TYPING) != 0;
    }
    public final boolean hasChat() {
        return (booleanValues & HAS_CHAT) != 0;
    }
    public final void updateChatState(Chat chat) {
        int icon = -1;
        if (null != chat) {
            icon = chat.getNewMessageIcon();
            booleanValues |= HAS_CHAT;
        } else {
            booleanValues &= ~HAS_CHAT;
        }
        booleanValues = (booleanValues & ~0x00FF0000) | ((icon + 1) << 16);
    }
    
    public final boolean inVisibleList() {
        return (booleanValues & SL_VISIBLE) != 0;
    }
    public final boolean inInvisibleList() {
        return (booleanValues & SL_INVISIBLE) != 0;
    }
    public final boolean inIgnoreList() {
        return (booleanValues & SL_IGNORE) != 0;
    }
    protected final void initPrivacyMenu(SubMenu menu) {
        if (!isTemp()) {
            String visibleList = inVisibleList()
                    ? "rem_visible_list" : "add_visible_list";
            String invisibleList = inInvisibleList()
                    ? "rem_invisible_list": "add_invisible_list";
            String ignoreList = inIgnoreList()
                    ? "rem_ignore_list": "add_ignore_list";

            menu.add(Menu.FIRST, USER_MENU_PS_VISIBLE, 2, visibleList);
            menu.add(Menu.FIRST, USER_MENU_PS_INVISIBLE, 2, invisibleList);
            menu.add(Menu.FIRST, USER_MENU_PS_IGNORE, 2, ignoreList);
        }
    }

    public String getMyName() {
        return null;
    }

    public boolean isSingleUserContact() {
        return true;
    }
    public boolean hasHistory() {
        return !isTemp() && isSingleUserContact();
    }

    public boolean isVisibleInContactList() {
        return isOnline() || hasChat() || isTemp();
    }

    public final byte getTextTheme() {
        if (isTemp()) {
            return Scheme.THEME_CONTACT_TEMP;
        }
        if (hasChat()) {
            return Scheme.THEME_CONTACT_WITH_CHAT;
        }
        if (isOnline()) {
            return Scheme.THEME_CONTACT_ONLINE;
        }
        return Scheme.THEME_CONTACT_OFFLINE;
    }

    public final String getText() {
        return name;
    }

     @Override
     protected int getType() {
         return TreeNode.CONTACT;
     }

     public final int getNodeWeight() {
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)
                && hasUnreadMessage()) {
            return 0;
        }
        if (!isSingleUserContact()) {
            return isOnline() ? 9 : 50;
        }
        int sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
        if (ContactList.SORT_BY_NAME == sortType) {
            return 20;
        }
        if (isOnline()) {
			if (hasChat()) {
				return 10;
			}
            switch (sortType) {
                case ContactList.SORT_BY_STATUS:

                    return 20 + StatusInfo.getWidth(getStatusIndex());
                case ContactList.SORT_BY_ONLINE:
                    return 20;
            }
        }
        if (isTemp()) {
            return 60;
        }
        return 51;
    }

    public final boolean hasUnreadMessage() {
        return 0 != (booleanValues & 0x00FF0000);
    }
    public final int getUnreadMessageIcon() {
        return ((booleanValues >>> 16) & 0xFF) - 1;
    }

    public static final int USER_MENU_REQU_AUTH        = 1004;
    public static final int USER_MENU_USER_REMOVE      = 1007;
    public static final int USER_MENU_RENAME           = 1009;
    public static final int USER_MENU_USER_INFO        = 1012;
    public static final int USER_MENU_MOVE             = 1015;
    public static final int USER_MENU_STATUSES         = 1016;
    public static final int USER_MENU_HISTORY          = 1025;
    public static final int USER_MENU_ADD_USER         = 1018;

    public static final int USER_MENU_GRANT_AUTH       = 1021;
    public static final int USER_MENU_DENY_AUTH        = 1022;

    public static final int USER_MENU_PS_VISIBLE       = 1034;
    public static final int USER_MENU_PS_INVISIBLE     = 1035;
    public static final int USER_MENU_PS_IGNORE        = 1036;

    public static final int USER_MENU_USERS_LIST = 1037;
    public static final int USER_MANAGE_CONTACT = 1038;

    public static final int USER_MENU_WAKE = 13;
    public static final int USER_MENU_FILE_TRANS = 1005;
    public static final int USER_MENU_CAM_TRANS  = 1006;
	
	public static final int USER_MENU_TRACK  = 1042;
	public static final int USER_MENU_TRACK_CONF = 1045;
	
	public static final int USER_MENU_ANNOTATION  = 1043;
    public static final int CONFERENCE_DISCONNECT = 1040;

    protected abstract void initManageContactMenu(Protocol protocol, SubMenu menu);
    protected void initContextMenu(Protocol protocol, ContextMenu contactMenu) {
        addChatItems(contactMenu);
        addGeneralItems(protocol, contactMenu);
    }
    public void addChatMenuItems(ContextMenu model) {
    }

    protected final void addChatItems(ContextMenu menu) {
        if (isSingleUserContact()) {
            if (!isAuth()) {
                menu.add(Menu.FIRST, USER_MENU_REQU_AUTH, 2, R.string.requauth);
            }
        }
        if (!isTemp() && !isConference()) {
            menu.add(Menu.FIRST, USER_MENU_TRACK, 2, R.string.extra_settings);
		}
        if (isSingleUserContact() || isOnline()) {
            /*if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.add(Menu.FIRST, USER_MENU_FILE_TRANS, 2, R.string.ft_name);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.add(Menu.FIRST, USER_MENU_CAM_TRANS, 2, R.string.ft_cam);
            }*/
            addChatMenuItems(menu);
        }
    }
    protected final void addGeneralItems(Protocol protocol, ContextMenu menu) {
        menu.add(Menu.FIRST, USER_MENU_USER_INFO, 2, R.string.user_info);
        SubMenu manageContact = menu.addSubMenu(Menu.FIRST, USER_MANAGE_CONTACT, 2, R.string.manage);
        initManageContactMenu(protocol, manageContact);
        if (!isTemp()) {
            menu.add(Menu.FIRST, USER_MENU_HISTORY, 2, R.string.history);
        }
        if (isOnline()) {
            menu.add(Menu.FIRST, USER_MENU_STATUSES, 2, R.string.statuses);
        }
    }
}