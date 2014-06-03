package protocol;

import android.view.ContextMenu;
import android.view.Menu;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.comm.Config;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.view.menu.MyMenu;

import java.util.Vector;


public class Contact implements TreeNode {
    public static final ImageList serverListsIcons = ImageList.createImageList("/serverlists.png");

    protected String userId;
    private String name;
    private int groupId = Group.NOT_IN_GROUP;
    private int booleanValues;
    private byte status = StatusInfo.STATUS_OFFLINE;
    private String statusText = null;
    public short clientIndex = ClientInfo.CLI_NONE;
    private int xstatus = XStatusInfo.XSTATUS_NONE;
    private String xstatusText = null;
    String version = "";
    public long chaingingStatusTime = 0;

    public Contact(String jid, String name) {
        this.userId = jid;
        this.setName((null == name) ? jid : name);
        setOfflineStatus();
    }

    protected String currentResource;

    public boolean isConference() {
        return false;
    }

    public String getDefaultGroupName() {
        return JLocale.getString(Protocol.GENERAL_GROUP);
    }

    public void addChatMenuItems(ContextMenu model) {
        if (isOnline()) {
            if (Options.getBoolean(Options.OPTION_ALARM) && isSingleUserContact()) {
                model.add(Menu.NONE, ContactMenu.USER_MENU_WAKE, Menu.NONE, R.string.wake);
            }
        }
    }

    protected void initContextMenu(Protocol protocol, ContextMenu contactMenu) {
        addChatItems(contactMenu);

        if (!isOnline() && isAuth() && !isTemp()) {
            contactMenu.add(Menu.NONE, ContactMenu.USER_MENU_SEEN, Menu.NONE, R.string.contact_seen);
        }
        if (isOnline() && isAuth()) {
            contactMenu.add(Menu.NONE, ContactMenu.USER_INVITE, Menu.NONE, R.string.invite);
        }
        contactMenu.add(Menu.NONE, ContactMenu.USER_MENU_ANNOTATION, Menu.NONE, R.string.notes);
        if (0 < subcontacts.size()) {
            contactMenu.add(Menu.NONE, ContactMenu.USER_MENU_CONNECTIONS, Menu.NONE, R.string.list_of_connections);
        }
        addGeneralItems(protocol, contactMenu);
    }

    protected void initManageContactMenu(Protocol protocol, MyMenu menu) {
        if (protocol.isConnected()) {
            if (isOnline()) {
                menu.add(SawimApplication.getContext().getString(R.string.adhoc), ContactMenu.USER_MENU_ADHOC);
            }
            if (isTemp()) {
                menu.add(SawimApplication.getContext().getString(R.string.add_user), ContactMenu.USER_MENU_ADD_USER);

            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.add(SawimApplication.getContext().getString(R.string.move_to_group), ContactMenu.USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.add(SawimApplication.getContext().getString(R.string.requauth), ContactMenu.USER_MENU_REQU_AUTH);
                }
            }
            if (!isTemp()) {
                menu.add(SawimApplication.getContext().getString(R.string.rename), ContactMenu.USER_MENU_RENAME);
            }
        }
        if (protocol.isConnected() || (isTemp() && protocol.inContactList(this))) {
            if (protocol.isConnected()) {
                menu.add(SawimApplication.getContext().getString(R.string.remove_me), ContactMenu.USER_MENU_REMOVE_ME);
            }
            if (protocol.inContactList(this)) {
                menu.add(SawimApplication.getContext().getString(R.string.remove), ContactMenu.USER_MENU_USER_REMOVE);
            }
        }
    }

    String getReciverJid() {
        if (this instanceof ServiceContact) {
        } else if (!StringConvertor.isEmpty(currentResource)) {
            return getUserId() + "/" + currentResource;
        }
        return getUserId();
    }

    public boolean execCommand(Protocol protocol, String msg) {
        final String cmd;
        final String param;
        int endCmd = msg.indexOf(' ');
        if (-1 != endCmd) {
            cmd = msg.substring(1, endCmd);
            param = msg.substring(endCmd + 1);
        } else {
            cmd = msg.substring(1);
            param = "";
        }
        String resource = param;
        String newMessage = "";

        int endNick = param.indexOf('\n');
        if (-1 != endNick) {
            resource = param.substring(0, endNick);
            newMessage = param.substring(endNick + 1);
        }
        String xml = null;
        final String on = "on";
        final String off = "off";
        if (on.equals(param) || off.equals(param)) {
            xml = Config.getConfigValue(cmd + ' ' + param, "/jabber-commands.txt");
        }
        if (null == xml) {
            xml = Config.getConfigValue(cmd, "/jabber-commands.txt");
        }
        if (null == xml) {
            return false;
        }

        Connection connection = protocol.getConnection();

        String jid = Jid.SawimJidToRealJid(getUserId());
        String fullJid = jid;
        if (isConference()) {
            String nick = ((ServiceContact) this).getMyName();
            fullJid = Jid.SawimJidToRealJid(getUserId() + '/' + nick);
        }

        xml = xml.replace("${sawim.caps}", connection.getCaps());
        xml = xml.replace("${c.jid}", Util.xmlEscape(jid));
        xml = xml.replace("${c.fulljid}", Util.xmlEscape(fullJid));
        xml = xml.replace("${param.full}", Util.xmlEscape(param));
        xml = xml.replace("${param.res}", Util.xmlEscape(resource));
        xml = xml.replace("${param.msg}", Util.xmlEscape(newMessage));
        xml = xml.replace("${param.res.realjid}",
                Util.xmlEscape(getSubContactRealJid(resource)));
        xml = xml.replace("${param.full.realjid}",
                Util.xmlEscape(getSubContactRealJid(param)));

        connection.requestRawXml(xml);
        return true;
    }

    private String getSubContactRealJid(String resource) {
        SubContact c = getExistSubContact(resource);
        return StringConvertor.notNull((null == c) ? null : c.realJid);
    }

    public static class SubContact {
        public String resource;
        public String statusText;
        public String roleText;
        public String realJid;

        public short client = ClientInfo.CLI_NONE;

        public byte status;
        public byte priority;
        public byte priorityA;
    }

    public Vector<SubContact> subcontacts = new Vector<SubContact>();

    private void removeSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = subcontacts.elementAt(i);
            if (c.resource.equals(resource)) {
                c.status = StatusInfo.STATUS_OFFLINE;
                c.statusText = null;
                subcontacts.removeElementAt(i);
                return;
            }
        }
    }

    public SubContact getExistSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = subcontacts.elementAt(i);
            if (c.resource.equals(resource)) {
                return c;
            }
        }
        return null;
    }

    protected SubContact getSubContact(String resource) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            return c;
        }
        c = new SubContact();
        c.resource = resource;
        c.status = StatusInfo.STATUS_OFFLINE;
        subcontacts.addElement(c);
        return c;
    }

    void setRealJid(String resource, String realJid) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.realJid = realJid;
        }
    }

    public SubContact getCurrentSubContact() {
        if ((0 == subcontacts.size()) || isConference()) {
            return null;
        }
        SubContact currentContact = getExistSubContact(currentResource);
        if (null != currentContact) {
            return currentContact;
        }
        try {
            currentContact = subcontacts.elementAt(0);
            byte maxPriority = currentContact.priority;
            for (int i = 1; i < subcontacts.size(); ++i) {
                SubContact contact = subcontacts.elementAt(i);
                if (maxPriority < contact.priority) {
                    maxPriority = contact.priority;
                    currentContact = contact;
                }
            }
        } catch (Exception e) {

        }
        return currentContact;
    }

    public byte subcontactsS() {
        if (!isConference() && 1 < subcontacts.size()) {
            return (byte) subcontacts.size();
        }
        return (byte) 0;
    }

    public void __setStatus(String resource, int priority, int priorityA, byte index, String statusText, String roleText) {
        if (StatusInfo.STATUS_OFFLINE == index) {
            resource = StringConvertor.notNull(resource);
            if (resource.equals(currentResource)) {
                currentResource = null;
            }
            removeSubContact(resource);
            if (0 == subcontacts.size()) {
                setOfflineStatus();
            }

        } else {
            SubContact c = getSubContact(resource);
            c.priority = (byte) priority;
            c.priorityA = (byte) priorityA;
            c.status = index;
            c.statusText = statusText;
            c.roleText = roleText;
        }
    }

    void updateMainStatus(Protocol xmpp) {
        if (isSingleUserContact()) {
            SubContact c = getCurrentSubContact();
            if (null == c) {
                setOfflineStatus();

            } else if (this instanceof ServiceContact) {
                setStatus(c.status, c.statusText);

            } else {
                xmpp.setContactStatus(this, c.status, c.statusText);
            }
        }
    }

    public void setClient(String resource, String caps) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.client = Client.createClient(caps);
        }
        SubContact cur = getCurrentSubContact();
        setClient((null == cur) ? ClientInfo.CLI_NONE : cur.client, null);
    }

    public void setXStatus(String id, String text) {
        setXStatus(Protocol.xStatus.createXStatus(id), text);
    }

    public final void setOfflineStatus() {
        subcontacts.removeAllElements();
        if (isOnline()) {
            setTimeOfChaingingStatus(SawimApplication.getCurrentGmtTime());
        }
        setStatus(StatusInfo.STATUS_OFFLINE, null);
        setXStatus(XStatusInfo.XSTATUS_NONE, null);
        beginTyping(false);
    }

    public void setActiveResource(String resource) {
        SubContact c = getExistSubContact(resource);
        currentResource = (null == c) ? null : c.resource;

        SubContact cur = getCurrentSubContact();
        if (null == cur) {
            setStatus(StatusInfo.STATUS_OFFLINE, null);
        } else {
            setStatus(cur.status, cur.statusText);
        }
        setClient((null == cur) ? ClientInfo.CLI_NONE : cur.client, null);
    }

    public boolean isSingleUserContact() {
        return true;
    }

    public boolean hasHistory() {
        return !isTemp();
    }

    public final boolean isOnline() {
        return (StatusInfo.STATUS_OFFLINE != status);
    }

    public void setTimeOfChaingingStatus(long time) {
        chaingingStatusTime = time;
    }

    public String annotations = null;

    public boolean isPresence() {
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

    public Icon getLeftIcon(Protocol p) {
        return p.getStatusInfo().getIcon(getStatusIndex());
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

    public final byte getStatusIndex() {
        return status;
    }

    public final String getStatusText() {
        return statusText;
    }

    protected final void setStatus(byte statusIndex, String text) {
        if (!isOnline() && (StatusInfo.STATUS_OFFLINE != statusIndex)) {
            setTimeOfChaingingStatus(SawimApplication.getCurrentGmtTime());
        }
        status = statusIndex;
        statusText = (StatusInfo.STATUS_OFFLINE == status) ? null : text;
    }

    public void activate(BaseActivity activity, Protocol p) {
        RosterHelper.getInstance().setCurrentContact(this);
    }

    public static final byte CONTACT_NO_AUTH = 1 << 1;
    private static final byte CONTACT_IS_TEMP = 1 << 3;

    public static final byte SL_VISIBLE = 1 << 4;
    public static final byte SL_INVISIBLE = 1 << 5;
    public static final byte SL_IGNORE = 1 << 6;

    private static final int TYPING = 1 << 8;
    private static final int HAS_CHAT = 1 << 9;

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
        return (byte) (booleanValues & 0x7F);
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

    protected final void initPrivacyMenu(MyMenu menu) {
        if (!isTemp()) {
            int visibleList = inVisibleList()
                    ? R.string.rem_visible_list : R.string.add_visible_list;
            int invisibleList = inInvisibleList()
                    ? R.string.rem_invisible_list : R.string.add_invisible_list;
            int ignoreList = inIgnoreList()
                    ? R.string.rem_ignore_list : R.string.add_ignore_list;

            menu.add(JLocale.getString(visibleList), ContactMenu.USER_MENU_PS_VISIBLE);
            menu.add(JLocale.getString(invisibleList), ContactMenu.USER_MENU_PS_INVISIBLE);
            menu.add(JLocale.getString(ignoreList), ContactMenu.USER_MENU_PS_IGNORE);
        }
    }

    public String getMyName() {
        return null;
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
    public byte getType() {
        return CONTACT;
    }

    public final int getNodeWeight() {
        if (/*Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)
                && */hasUnreadMessage()) {
            return 5;
        }
        if (!isSingleUserContact()) {
            return isOnline() ? 9 : 50;
        }
        if (RosterHelper.SORT_BY_NAME == SawimApplication.sortType) {
            return 20;
        }
        if (isOnline()) {
            if (hasChat()) {
                return 10;
            }
            switch (SawimApplication.sortType) {
                case RosterHelper.SORT_BY_STATUS:
                    return 20 + StatusInfo.getWidth(getStatusIndex());
                case RosterHelper.SORT_BY_ONLINE:
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

    protected final void addChatItems(ContextMenu menu) {
        if (isSingleUserContact()) {
            if (!isAuth()) {
                menu.add(Menu.NONE, ContactMenu.USER_MENU_REQU_AUTH, Menu.NONE, R.string.requauth);
            }
        }
        if (isSingleUserContact() || isOnline()) {
            addChatMenuItems(menu);
        }
    }

    protected final void addGeneralItems(Protocol protocol, ContextMenu menu) {
        menu.add(Menu.NONE, ContactMenu.USER_MENU_USER_INFO, Menu.NONE, R.string.user_info);
        menu.add(Menu.NONE, ContactMenu.USER_MANAGE_CONTACT, Menu.NONE, R.string.manage);
        if (isOnline()) {
            menu.add(Menu.NONE, ContactMenu.USER_MENU_STATUSES, Menu.NONE, R.string.statuses);
        }
        if (hasChat()) {
            menu.add(Menu.NONE, ContactMenu.USER_MENU_CLOSE_CHAT, Menu.NONE, R.string.close);
        }
    }
}