package protocol;

import DrawControls.icons.Icon;
import android.util.Log;
import sawim.FileTransfer;
import ru.sawim.General;
import sawim.SawimException;
import sawim.Options;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.chat.message.PlainMessage;
import sawim.chat.message.SystemNotice;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.io.Storage;
import sawim.modules.Answerer;
import sawim.modules.AntiSpam;
import sawim.modules.DebugLog;
import sawim.modules.Notify;
import sawim.modules.tracking.Tracking;
import sawim.search.Search;
import sawim.search.UserInfo;
import sawim.util.JLocale;
import protocol.jabber.JabberContact;
import ru.sawim.SawimApplication;
import ru.sawim.R;

import javax.microedition.rms.RecordStore;
import java.io.*;
import java.util.Vector;

abstract public class Protocol {
    private static final int RECONNECT_COUNT = 20;
    private final Object rosterLockObject = new Object();
    public ClientInfo clientInfo;
    protected Vector contacts = new Vector();
    protected Vector groups = new Vector();
    protected StatusInfo info;
    protected XStatusInfo xstatusInfo;
    private Profile profile;
    private String password;
    private String userid = "";
    private byte privateStatus = 0;
    private String rmsName = null;
    private boolean isReconnect;
    private int reconnect_attempts;
    private boolean needSave = false;
    private long lastStatusChangeTime;
    private byte progress = 100;
    private Vector autoGrand = new Vector();
    private Vector sortedContacts = new Vector();
    private Vector sortedGroups = new Vector();
    private Group notInListGroup;

    private String getContactListRS() {
        return rmsName;
    }

    public abstract String getUserIdName();

    public final String getUserId() {
        return userid;
    }

    protected final void setUserId(String userId) {
        userid = userId;
    }

    public boolean isEmpty() {
        return StringConvertor.isEmpty(userid);
    }

    public final String getNick() {
        String nick = profile.nick;
        return (nick.length() == 0) ? JLocale.getString("me") : nick;
    }

    public final Profile getProfile() {
        return profile;
    }

    public final void setProfile(Profile account) {
        this.profile = account;
        String rawUin = StringConvertor.notNull(account.userId);
        if (!StringConvertor.isEmpty(rawUin)) {
            byte type = account.protocolType;
            if ((Profile.PROTOCOL_VK == type)
                    && (0 < Util.strToIntDef(rawUin, 0))) {
                rawUin = "id" + rawUin;
                account.userId = rawUin;
            }
            String domain = getDefaultDomain(type);
            if ((null != domain) && (-1 == rawUin.indexOf('@'))) {
                rawUin += domain;
            }
        }
        userid = StringConvertor.isEmpty(rawUin) ? "" : processUin(rawUin);
        if (!StringConvertor.isEmpty(account.password)) {
            setPassword(null);
        }

        String rms = "cl-" + getUserId();
        rmsName = (32 < rms.length()) ? rms.substring(0, 32) : rms;
    }

    public final String getPassword() {
        return (null == password) ? profile.password : password;
    }

    public final void setPassword(String pass) {
        password = pass;
    }

    private String getDefaultDomain(byte type) {
        switch (type) {
            case Profile.PROTOCOL_GTALK:
                return "@gmail.com";
            case Profile.PROTOCOL_FACEBOOK:
                return "@chat.facebook.com";
            case Profile.PROTOCOL_LJ:
                return "@livejournal.com";
            case Profile.PROTOCOL_YANDEX:
                return "@ya.ru";
            case Profile.PROTOCOL_VK:
                return "@vk.com";
            case Profile.PROTOCOL_QIP:
                return "@qip.ru";
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return "@odnoklassniki.ru";
        }
        return null;
    }

    protected String processUin(String uin) {
        return uin;
    }

    public final XStatusInfo getXStatusInfo() {
        return xstatusInfo;
    }

    public final void init() {
        notInListGroup = new Group(SawimApplication.getContext().getString(R.string.group_not_in_list));
        notInListGroup.setMode(Group.MODE_NONE);
        notInListGroup.setGroupId(Group.NOT_IN_GROUP);
        initStatusInfo();
        initStatus();
    }

    protected abstract void initStatusInfo();

    public boolean hasVCardEditor() {
        return true;
    }

    public Icon getCurrentStatusIcon() {
        if (isConnected() && !isConnecting()) {
            return getStatusInfo().getIcon(getProfile().statusIndex);
        }
        return getStatusInfo().getIcon(StatusInfo.STATUS_OFFLINE);
    }

    public final void sort() {
        synchronized (rosterLockObject) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                Util.sort(getSortedGroups());
            } else {
                Util.sort(getSortedContacts());
            }
        }
    }

    public final void setContactListStub() {
        synchronized (rosterLockObject) {
            contacts = new Vector();
            groups = new Vector();
        }
    }

    public final void setContactList(Vector groups, Vector contacts) {
        if ((contacts.size() > 0) && !(contacts.elementAt(0) instanceof Contact)) {
            DebugLog.panic("contacts is not list of Contact");
            contacts = new Vector();
        }
        if ((groups.size() > 0) && !(groups.elementAt(0) instanceof Group)) {
            DebugLog.panic("groups is not list of Group");
            groups = new Vector();
        }

        synchronized (rosterLockObject) {
            this.contacts = contacts;
            this.groups = groups;
        }
        ChatHistory.instance.restoreContactsWithChat(this);
        synchronized (rosterLockObject) {
            sortedContacts = new Vector();
            for (int i = 0; i < contacts.size(); ++i) {
                sortedContacts.addElement(contacts.elementAt(i));
            }
            sortedGroups = new Vector();
            for (int i = 0; i < groups.size(); ++i) {
                Group g = (Group) groups.elementAt(i);
                updateContacts(g);
                sortedGroups.addElement(g);
            }
            Util.sort(sortedGroups);
            updateContacts(notInListGroup);
        }
        if (getContactList().getManager().getProtocolCount() == 0) return;
        getContactList().getManager().update();
        needSave();
    }

    public final void setContactListAddition(Group group) {
        synchronized (rosterLockObject) {
            updateContacts(group);
            updateContacts(notInListGroup);
            Vector groupItems = group.getContacts();
            for (int i = 0; i < groupItems.size(); ++i) {
                if (-1 == Util.getIndex(sortedContacts, groupItems.elementAt(i))) {
                    sortedContacts.addElement(groupItems.elementAt(i));
                }
            }
        }
        getContactList().getManager().update();
        needSave();
    }

    private void updateContacts(Group group) {
        getContactList().getManager().updateGroup(this, group);
    }

    public final boolean isConnecting() {
        return 100 != progress;
    }

    public final byte getConnectingProgress() {
        return progress;
    }

    public final void setConnectingProgress(int percent) {
        this.progress = (byte) ((percent < 0) ? 100 : percent);
        if (100 == percent) {
            reconnect_attempts = RECONNECT_COUNT;
            getContactList().updateConnectionStatus();
            getContactList().getManager().updateBarProtocols();
        }
        getContactList().getManager().updateProgressBar();
    }

    public void sendFile(FileTransfer transfer, String filename, String description) {
    }

    public void getAvatar(UserInfo userInfo) {
    }

    protected abstract void requestAuth(String userId);

    protected abstract void grandAuth(String userId);

    protected abstract void denyAuth(String userId);

    protected abstract void s_setPrivateStatus();

    public final byte getPrivateStatus() {
        return privateStatus;
    }

    public final void setPrivateStatus(byte status) {
        privateStatus = status;
        if (isConnected()) {
            s_setPrivateStatus();
        }
    }

    public final void requestAuth(Contact contact) {
        requestAuth(contact.getUserId());
        autoGrandAuth(contact.getUserId());
    }

    private void autoGrandAuth(String userId) {
        autoGrand.addElement(userId);
    }

    public final void safeLoad() {
        if ("".equals(getUserId())) {
            return;
        }
        if (isConnected()) {
            return;
        }
        try {
            if (new Storage(getContactListRS()).exist()) {
                load();
            }
        } catch (Exception e) {
            DebugLog.panic("roster load", e);
            setContactList(new Vector(), new Vector());
        }
    }

    public final void needSave() {
        needSave = true;
        ContactList.getInstance().needRosterSave();
    }

    public final boolean safeSave() {
        boolean save = needSave;
        needSave = false;

        if (!save || "".equals(getUserId())) {
            return false;
        }
        synchronized (this) {
            String storage = getContactListRS();
            try {
                RecordStore.deleteRecordStore(storage);
            } catch (Exception e) {
            }
            RecordStore cl = null;
            try {
                cl = RecordStore.openRecordStore(storage, true);
                save(cl);
            } catch (Exception e) {
                DebugLog.panic("roster save", e);
            }
            try {
                cl.closeRecordStore();
            } catch (Exception e) {
            }
        }
        return true;
    }

    private void load() throws Exception {
        Vector cItems = new Vector();
        Vector gItems = new Vector();
        RecordStore cl = RecordStore.openRecordStore(getContactListRS(), false);
        try {
            byte[] buf;
            ByteArrayInputStream bais;
            DataInputStream dis;
            buf = cl.getRecord(1);
            bais = new ByteArrayInputStream(buf);
            dis = new DataInputStream(bais);
            if (!dis.readUTF().equals(General.VERSION)) {
                throw new Exception();
            }
            loadProtocolData(cl.getRecord(2));
            for (int marker = 3; marker <= cl.getNumRecords(); ++marker) {
                try {
                    buf = cl.getRecord(marker);
                    if ((null == buf) || (0 == buf.length)) {
                        continue;
                    }

                    bais = new ByteArrayInputStream(buf);
                    dis = new DataInputStream(bais);
                    while (0 < dis.available()) {
                        byte type = dis.readByte();
                        switch (type) {
                            case 0:
                                cItems.addElement(loadContact(dis));
                                break;
                            case 1:
                                gItems.addElement(loadGroup(dis));
                                break;
                        }
                    }
                } catch (EOFException e) {
                }
            }
            DebugLog.memoryUsage("clload");
        } finally {
            cl.closeRecordStore();
        }
        setContactList(gItems, cItems);
    }

    private void save(RecordStore cl) throws Exception {
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        byte[] buf;
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        dos.writeUTF(General.VERSION);
        buf = baos.toByteArray();
        cl.addRecord(buf, 0, buf.length);
        baos.reset();
        buf = saveProtocolData();
        cl.addRecord(buf, 0, buf.length);
        baos.reset();
        int cItemsCount = contacts.size();
        int totalCount = cItemsCount + groups.size();
        for (int i = 0; i < totalCount; ++i) {
            if (i < cItemsCount) {
                saveContact(dos, (Contact) contacts.elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, (Group) groups.elementAt(i - cItemsCount));
            }
            if ((baos.size() >= 4000) || (i == totalCount - 1)) {
                buf = baos.toByteArray();
                cl.addRecord(buf, 0, buf.length);
                baos.reset();
            }
        }
    }

    protected Contact loadContact(DataInputStream dis) throws Exception {
        String uin = dis.readUTF();
        String name = dis.readUTF();
        int groupId = dis.readInt();
        byte booleanValues = dis.readByte();
        Contact contact = createContact(uin, name);
        contact.setGroupId(groupId);
        contact.setBooleanValues(booleanValues);
        return contact;
    }

    protected Group loadGroup(DataInputStream dis) throws Exception {
        int groupId = dis.readInt();
        String name = dis.readUTF();
        Group group = createGroup(name);
        group.setGroupId(groupId);
        group.setExpandFlag(dis.readBoolean());
        return group;
    }

    protected void loadProtocolData(byte[] data) throws Exception {
    }

    protected byte[] saveProtocolData() throws Exception {
        return new byte[0];
    }

    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        out.writeByte(0);
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
    }

    protected void saveGroup(DataOutputStream out, Group group) throws Exception {
        out.writeInt(group.getId());
        out.writeUTF(group.getName());
        out.writeBoolean(group.isExpanded());
    }

    protected void s_removeContact(Contact contact) {
    };

    protected void s_removedContact(Contact contact) {
    };

    public final void removeContact(Contact contact) {
        if (contact.isTemp()) {
        } else if (isConnected()) {
            s_removeContact(contact);
        } else {
            return;
        }
        removeLocalContact(contact);
    }

    abstract protected void s_renameContact(Contact contact, String name);

    public final void renameContact(Contact contact, String name) {
        if (StringConvertor.isEmpty(name)) {
            return;
        }
        if (!inContactList(contact)) {
            contact.setName(name);
            return;
        }
        if (contact.isTemp()) {
        } else if (isConnected()) {
            s_renameContact(contact, name);
        } else {
            return;
        }
        contact.setName(name);
        ui_updateContact(contact);
        needSave();
    }

    abstract protected void s_moveContact(Contact contact, Group to);

    public final void moveContactTo(Contact contact, Group to) {
        s_moveContact(contact, to);
        cl_moveContact(contact, to);
    }

    protected void s_addContact(Contact contact) {
    };

    protected void s_addedContact(Contact contact) {
    }

    public final void addContact(Contact contact) {
        s_addContact(contact);
        contact.setTempFlag(false);
        cl_addContact(contact);
        getContactList().getManager().setActiveContact(contact);
        needSave();
        s_addedContact(contact);
    }

    public final void addTempContact(Contact contact) {
        cl_addContact(contact);
    }

    abstract protected void s_removeGroup(Group group);

    public final void removeGroup(Group group) {
        s_removeGroup(group);
        cl_removeGroup(group);
        needSave();
    }

    abstract protected void s_renameGroup(Group group, String name);

    public final void renameGroup(Group group, String name) {
        s_renameGroup(group, name);
        group.setName(name);
        cl_renameGroup(group);
        needSave();
    }

    abstract protected void s_addGroup(Group group);

    public final void addGroup(Group group) {
        s_addGroup(group);
        cl_addGroup(group);
        needSave();
    }

    abstract public boolean isConnected();

    abstract protected void startConnection();

    abstract protected void closeConnection();

    protected void userCloseConnection() {
    }

    public final void disconnect(boolean user) {
        setConnectingProgress(-1);
        closeConnection();
        if (user) {
            userCloseConnection();
        }
        setStatusesOffline();
        getContactList().getManager().updateBarProtocols();
        getContactList().getManager().updateProgressBar();
        getContactList().getManager().update();
        getContactList().updateConnectionStatus();
        if (user) {
            DebugLog.println("disconnect " + getUserId());
        }
    }

    abstract public Group createGroup(String name);

    abstract protected Contact createContact(String uin, String name);

    public final Contact createTempContact(String uin, String name) {
        Contact contact = getItemByUIN(uin);
        if (null != contact) {
            return contact;
        }
        contact = createContact(uin, name);
        if (null != contact) {
            contact.setTempFlag(true);
        }
        return contact;
    }

    public final Contact createTempContact(String uin) {
        return createTempContact(uin, uin);
    }

    abstract protected void s_searchUsers(Search cont);

    public final void searchUsers(Search cont) {
        s_searchUsers(cont);
    }

    public final Search getSearchForm() {
        if (groups.isEmpty()) {
            return null;
        }
        return new Search(this);
    }

    public final Vector getContactItems() {
        return contacts;
    }

    public final Vector getGroupItems() {
        return groups;
    }

    public final void beginTyping(String uin, boolean type) {
        Contact item = getItemByUIN(uin);
        if (null != item) {
            beginTyping(item, type);
            getContactList().getManager().update();
        }
    }

    private void beginTyping(Contact item, boolean type) {
        if (null == item) {
            return;
        }
        if (item.isTyping() != type) {
            item.beginTyping(type);
            Chat chat = ChatHistory.instance.getChat(item);
            if (null != chat) {
                chat.beginTyping(type);
            }
            if (type && isConnected()) {
                String id = item.getUserId();
                if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                    if (Tracking.isTracking(id, Tracking.EVENT_TYPING) == Tracking.TRUE) {
                        Tracking.beginTrackAction(item, Tracking.EVENT_TYPING);
                    }
                } else if (Tracking.isTracking(id, Tracking.EVENT_TYPING) == Tracking.FALSE) {
                    playNotification(Notify.isSound(Notify.NOTIFY_TYPING), Notify.NOTIFY_TYPING);
                }
                playNotification(Notify.NOTIFY_TYPING);
            }
        }
    }

    private void updateChatStatus(Contact c) {
        Chat chat = ChatHistory.instance.getChat(c);
        if (null != chat) {
            chat.updateStatus();
        }
    }

    protected final void setStatusesOffline() {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact c = (Contact) contacts.elementAt(i);
            c.setOfflineStatus();
        }
        synchronized (rosterLockObject) {
            if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
                for (int i = groups.size() - 1; i >= 0; --i) {
                    ((Group) groups.elementAt(i)).updateGroupData();
                }
            }
        }
    }

    public final Contact getItemByUIN(String uin) {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = (Contact) contacts.elementAt(i);
            if (contact.getUserId().equals(uin)) {
                return contact;
            }
        }
        return null;
    }

    public final Group getGroupById(int id) {
        synchronized (rosterLockObject) {
            for (int i = groups.size() - 1; 0 <= i; --i) {
                Group group = (Group) groups.elementAt(i);
                if (group.getId() == id) {
                    return group;
                }
            }
        }
        return null;
    }

    public final Group getGroup(Contact contact) {
        return getGroupById(contact.getGroupId());
    }

    public final Group getGroup(String name) {
        synchronized (rosterLockObject) {
            for (int i = groups.size() - 1; 0 <= i; --i) {
                Group group = (Group) groups.elementAt(i);
                if (group.getName().equals(name)) {
                    return group;
                }
            }
        }
        return null;
    }

    public final ContactList getContactList() {
        return ContactList.getInstance();
    }

    public final boolean inContactList(Contact contact) {
        return -1 != Util.getIndex(contacts, contact);
    }

    public final StatusInfo getStatusInfo() {
        return info;
    }

    protected abstract void s_updateOnlineStatus();

    public final void setOnlineStatus(int statusIndex, String msg) {
        profile.statusIndex = (byte) statusIndex;
        profile.statusMessage = msg;
        Options.saveAccount(profile);

        setLastStatusChangeTime();
        if (isConnected()) {
            s_updateOnlineStatus();
            getContactList().getManager().updateProgressBar();
        }
    }

    public final void setStatus(int statusIndex, String msg) {
        boolean connected = StatusInfo.STATUS_OFFLINE != profile.statusIndex;
        boolean connecting = StatusInfo.STATUS_OFFLINE != statusIndex;
        if (connected && !connecting) {
            disconnect(true);
        }
        setOnlineStatus(statusIndex, msg);
        if (!connected && connecting) {
            connect();
        }
    }

    protected abstract void s_updateXStatus();

    public final void setXStatus(int xstatus, String title, String desc) {
        profile.xstatusIndex = (byte) xstatus;
        profile.xstatusTitle = title;
        profile.xstatusDescription = desc;
        Options.saveAccount(profile);
        if (isConnected()) {
            s_updateXStatus();
        }
    }

    private void initStatus() {
        setLastStatusChangeTime();
        setPrivateStatus((byte) Options.getInt(Options.OPTION_PRIVATE_STATUS));
    }

    private void ui_removeFromAnyGroup(Contact c) {
        Group g = getGroupById(c.getGroupId());
        if (null == g) {
            g = notInListGroup;
        }
        getContactList().getManager().removeFromGroup(g, c);
    }

    private void ui_addContactToGroup(Contact contact, Group group) {
        ui_removeFromAnyGroup(contact);
        contact.setGroup(group);
        if (null == group) {
            group = notInListGroup;
        }
        getContactList().getManager().addToGroup(group, contact);
    }

    private void ui_updateGroup(Group group) {
        if (Options.getBoolean(Options.OPTION_USER_GROUPS)) {
            synchronized (rosterLockObject) {
                group.updateGroupData();
                Util.sort(sortedGroups);
            }
            getContactList().getManager().update(group);
        }
    }

    public final Group getNotInListGroup() {
        return notInListGroup;
    }

    public final Vector getSortedContacts() {
        return sortedContacts;
    }

    public final Vector getSortedGroups() {
        return sortedGroups;
    }

    public final Object getRosterLockObject() {
        return rosterLockObject;
    }

    public final void markMessages(Contact contact) {
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
            ui_updateContact(contact);
        }
        getContactList().markMessages(contact);
    }

    public final void ui_changeContactStatus(Contact contact) {
        updateChatStatus(contact);
        ui_updateContact(contact);
    }

    public final void ui_updateContact(Contact contact) {
        synchronized (rosterLockObject) {
            Group group = getGroup(contact);
            if (null == group) {
                group = notInListGroup;
            }
            getContactList().getManager().putIntoQueue(group);
        }
        getContactList().getManager().update(contact);
    }

    private void cl_addContact(Contact contact) {
        if (null == contact) {
            return;
        }
        Group g = getGroup(contact);
        boolean hasnt = !inContactList(contact);
        if (hasnt) {
            contacts.addElement(contact);
        }
        synchronized (rosterLockObject) {
            if (hasnt) {
                sortedContacts.addElement(contact);
            }
            ui_addContactToGroup(contact, g);
        }
        ui_updateContact(contact);
    }

    private void cl_moveContact(Contact contact, Group to) {
        synchronized (rosterLockObject) {
            ui_addContactToGroup(contact, to);
        }
        ui_updateContact(contact);
    }

    private void cl_removeContact(Contact contact) {
        contacts.removeElement(contact);
        synchronized (rosterLockObject) {
            sortedContacts.removeElement(contact);
            ui_removeFromAnyGroup(contact);
        }
        getContactList().getManager().update(contact);
    }

    private void cl_addGroup(Group group) {
        if (-1 != Util.getIndex(groups, group)) {
            DebugLog.panic("Group '" + group.getName() + "' already added");
        }
        groups.addElement(group);
        synchronized (rosterLockObject) {
            sortedGroups.addElement(group);
            ui_updateGroup(group);
        }
    }

    private void cl_renameGroup(Group group) {
        ui_updateGroup(group);
    }

    private void cl_removeGroup(Group group) {
        groups.removeElement(group);
        synchronized (rosterLockObject) {
            sortedGroups.removeElement(group);
        }
        getContactList().getManager().update(group);
    }

    public final void addLocalContact(Contact contact) {
        cl_addContact(contact);
    }

    public final void removeLocalContact(Contact contact) {
        if (null == contact) {
            return;
        }
        boolean inCL = inContactList(contact);
        if (inCL) {
            cl_removeContact(contact);
        }
        if (contact.hasChat()) {
            ChatHistory.instance.unregisterChat(ChatHistory.instance.getChat(contact));
        }
        if (inCL) {
            if (isConnected()) {
                s_removedContact(contact);
            }
            needSave();
        }
    }

    public final long getLastStatusChangeTime() {
        return lastStatusChangeTime;
    }

    private void setLastStatusChangeTime() {
        lastStatusChangeTime = General.getCurrentGmtTime();
    }

    private boolean isEmptyMessage(String text) {
        for (int i = 0; i < text.length(); ++i) {
            if (' ' < text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    public final void addMessage(Message message) {
        addMessage(message, false);
    }

    public final void addMessage(Message message, boolean silent) {
        Contact contact = getItemByUIN(message.getSndrUin());
        if ((null == contact) && AntiSpam.isSpam(this, message)) {
            return;
        }
        if (null == contact) {
            contact = createTempContact(message.getSndrUin());
            addTempContact(contact);
        }
        if (null == contact) {
            return;
        }
        if (contact.inIgnoreList()) {
            return;
        }
        beginTyping(contact, false);
        boolean isPlain = (message instanceof PlainMessage);
        if (isPlain && isEmptyMessage(message.getText())) {
            return;
        }
        Chat chat = getChat(contact);
        chat.addMessage(message, !silent && !message.isWakeUp());
        if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                if (autoGrand.contains(contact.getUserId())) {
                    grandAuth(contact.getUserId());
                    autoGrand.removeElement(contact.getUserId());
                    chat.resetAuthRequests();
                }
            }
        }
        if (!silent) {
            addMessageNotify(chat, contact, message);
        }
        getContactList().markMessages(contact);
        getContactList().getManager().update(contact);
        getContactList().getManager().updateBarProtocols();
    }

    private void addMessageNotify(Chat chat, Contact contact, Message message) {
        boolean isPersonal = contact.isSingleUserContact();
        boolean isBlog = isBlogBot(contact.getUserId());
        boolean isHuman = isBlog || chat.isHuman() || !contact.isSingleUserContact();
        if (isBot(contact)) {
            isHuman = false;
        }
        boolean isMention = false;
        if (!isPersonal && !message.isOffline() && (contact instanceof JabberContact)) {
            String msg = message.getText();
            String myName = contact.getMyName();
            isPersonal = msg.startsWith(myName)
                    && msg.startsWith(" ", myName.length() + 1);
            isMention = Chat.isHighlight(msg, myName);
        }
        if (Options.getBoolean(Options.OPTION_ANSWERER)) {
            Answerer.getInstance().checkMessage(this, contact, message);
        }
        String id = contact.getUserId();
        if (message.isOffline()) {
        } else if (isPersonal) {
            if (contact.isSingleUserContact()
                    && contact.isAuth() && !contact.isTemp()
                    && message.isWakeUp()) {
                playNotification(Notify.NOTIFY_ALARM);

            } else if (isBlog) {
                playNotification(Notify.NOTIFY_BLOG);

            } else {
                if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                    if (Tracking.isTracking(id, Tracking.EVENT_MESSAGE) == Tracking.TRUE) {
                        Tracking.beginTrackAction(contact, Tracking.EVENT_MESSAGE);
                    }
                } else if (Tracking.isTracking(id, Tracking.EVENT_MESSAGE) == Tracking.FALSE) {
                    playNotification(Notify.isSound(Notify.NOTIFY_MESSAGE), Notify.NOTIFY_MESSAGE);
                }
                playNotification(Notify.NOTIFY_MESSAGE);
            }
        } else if (isMention) {
            if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                if (Tracking.isTracking(id, Tracking.EVENT_MESSAGE) == Tracking.TRUE) {
                    Tracking.beginTrackAction(contact, Tracking.EVENT_MESSAGE);
                }
            } else if (Tracking.isTracking(id, Tracking.EVENT_MESSAGE) == Tracking.FALSE) {
                playNotification(Notify.isSound(Notify.NOTIFY_MULTIMESSAGE), Notify.NOTIFY_MULTIMESSAGE);
            }
            playNotification(Notify.NOTIFY_MULTIMESSAGE);
        }
        if (!isPersonal) {
            return;
        }
        getContactList().getManager().setActiveContact(contact);
    }

    protected boolean isBlogBot(String userId) {
        return false;
    }

    public final boolean isBot(Contact contact) {
        return contact.getName().endsWith("-bot");
    }

    public final void setAuthResult(String uin, boolean auth) {
        Contact c = getItemByUIN(uin);
        if (null == c) {
            return;
        }
        if (auth == c.isAuth()) {
            return;
        }
        c.setBooleanValue(Contact.CONTACT_NO_AUTH, !auth);
        if (!auth) {
            c.setOfflineStatus();
        }
        ui_changeContactStatus(c);
    }

    public final void connect() {
        isReconnect = false;
        reconnect_attempts = RECONNECT_COUNT;
        disconnect(false);
        startConnection();
        setLastStatusChangeTime();
    }

    public final boolean isReconnect() {
        return isReconnect;
    }

    public final void playNotification(boolean isSound, int type) {
        Notify.getSound().playSoundNotification(isSound, type);
    }

    public final void playNotification(int type) {
        Notify.getSound().playSoundNotification(type);
    }

    public final void processException(SawimException e) {
        DebugLog.println("process exception: " + e.getMessage());
        getContactList().activateWithMsg(getUserId() + "\n" + e.getMessage());
        if (!SawimApplication.getInstance().isNetworkAvailable()) {
            e = new SawimException(123, 0);
        }
        if (e.isReconnectable()) {
            reconnect_attempts--;
            if (0 < reconnect_attempts) {
                if (isConnected() && !isConnecting()) {
                    isReconnect = true;
                }
                try {
                    int iter = RECONNECT_COUNT - reconnect_attempts;
                    int sleep = Math.min(iter * 10, 2 * 60);
                    Thread.sleep(sleep * 1000);
                } catch (Exception ignored) {
                }
                if (isConnected() || isConnecting()) {
                    disconnect(false);
                    playNotification(Notify.NOTIFY_RECONNECT);
                    startConnection();
                }
                return;
            }
        }
        disconnect(false);
        setOnlineStatus(StatusInfo.STATUS_OFFLINE, null);
        showException(e);
    }

    public final void showException(SawimException e) {
        getContactList().activateWithMsg(getUserId() + "\n" + e.getMessage());
    }

    public final void dismiss() {
        disconnect(false);
        userCloseConnection();
        ChatHistory.instance.unregisterChats(this);
        safeSave();
        sortedContacts = null;
        sortedGroups = null;
        profile = null;
        contacts = null;
        groups = null;
    }

    public void autoDenyAuth(String uin) {
    }

    public abstract void saveUserInfo(UserInfo info);

    public boolean isMeVisible(Contact to) {
        return true;
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
    }

    public final void sendTypingNotify(Contact to, boolean isTyping) {
        if (isConnected() && isMeVisible(to)
                && (1 < Options.getInt(Options.OPTION_TYPING_MODE))) {
            s_sendTypingNotify(to, isTyping);
        }
    }

    protected abstract void sendSomeMessage(PlainMessage msg);

    public final void sendMessage(Contact to, String msg, boolean addToChat) {
        msg = StringConvertor.trim(msg);
        if (StringConvertor.isEmpty(msg)) {
            return;
        }
        PlainMessage plainMsg = new PlainMessage(this, to, General.getCurrentGmtTime(), msg);
        if (isConnected()) {
            if (msg.startsWith("/") && !msg.startsWith("/me ") && !msg.startsWith("/wakeup") && (to instanceof JabberContact)) {
                boolean cmdExecuted = ((JabberContact) to).execCommand(this, msg);
                if (!cmdExecuted) {
                    String text = JLocale.getString("jabber_command_not_found");
                    SystemNotice notice = new SystemNotice(this, SystemNotice.SYS_NOTICE_MESSAGE, to.getUserId(), text);
                    getChat(to).addMessage(notice, false);
                }
                return;
            }
            sendSomeMessage(plainMsg);
        }
        if (addToChat) {
            getChat(to).addMyMessage(plainMsg);
        }
    }

    protected void doAction(Contact contact, int cmd) {
    }

    public void showUserInfo(Contact contact) {
    }

    public void showStatus(Contact contact) {
    }

    public final void setContactStatus(Contact contact, byte status, String text) {
        byte prev = contact.getStatusIndex();
        contact.setStatus(status, text);
        if (isConnected() && !isConnecting()) {
            byte curr = contact.getStatusIndex();
            if ((prev == curr) || !contact.isSingleUserContact()) {
                return;
            }
            boolean prevAway = getStatusInfo().isOffline(prev);
            boolean currAway = getStatusInfo().isOffline(curr);
            if (!currAway && prevAway) {

                String id = contact.getUserId();
                if (Tracking.isTrackingEvent(id, Tracking.GLOBAL) == Tracking.TRUE) {
                    if (Tracking.isTracking(id, Tracking.EVENT_ENTER) == Tracking.TRUE) {
                        Tracking.beginTrackAction(contact, Tracking.EVENT_ENTER);
                    }
                } else if (Tracking.isTracking(id, Tracking.EVENT_ENTER) == Tracking.FALSE) {
                    Notify.getSound().playSoundNotification(Notify.isSound(Notify.NOTIFY_ONLINE), Notify.NOTIFY_ONLINE);
                }
            }
        }
    }

    public String getUniqueUserId(Contact contact) {
        return contact.getUserId();
    }

    public final Chat getChat(Contact contact) {
        Chat chat = ChatHistory.instance.getChat(contact);
        if (null == chat) {
            chat = new Chat(this, contact);
            if (!inContactList(contact)) {
                contact.setTempFlag(true);
                addLocalContact(contact);
            }
            if (!chat.empty() || !contact.isSingleUserContact()) {
                ChatHistory.instance.registerChat(chat);
            }
        }
        return chat;
    }
}