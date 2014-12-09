package protocol;

import protocol.xmpp.XmppContact;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.Icon;
import ru.sawim.io.Storage;
import ru.sawim.modules.Answerer;
import ru.sawim.modules.AntiSpam;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.FileTransfer;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;

import java.io.*;
import java.util.Vector;

abstract public class Protocol {

    private static final int ROSTER_STORAGE_VERSION = 1;
    private static final int RECONNECT_COUNT = 10;

    private static final int TYPE_GROUP = 0;
    private static final int TYPE_CONTACT = 1;

    private final Object rosterLockObject = new Object();
    public ClientInfo clientInfo;
    protected Roster roster = new Roster();
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
    private Group notInListGroup;

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
        return (nick.length() == 0) ? JLocale.getString(R.string.me) : nick;
    }

    public final Profile getProfile() {
        return profile;
    }

    public final void setProfile(Profile account) {
        profile = account;
        String rawUin = StringConvertor.notNull(account.userId);
        if (!StringConvertor.isEmpty(rawUin)) {
            byte type = account.protocolType;
            String domain = getDefaultDomain(type);
            if ((null != domain) && (-1 == rawUin.indexOf('@'))) {
                rawUin += domain;
            }
        }
        userid = StringConvertor.isEmpty(rawUin) ? "" : processUin(rawUin);
        if (!StringConvertor.isEmpty(account.password)) {
            setPassword(null);
        }

        String rms = "roster-" + getUserId();
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

    public final void setContactListStub() {
        synchronized (rosterLockObject) {
            roster = new Roster();
        }
    }

    public final void setRoster(Vector<Group> groups, Vector<Contact> contacts) {
        setRoster(new Roster(groups, contacts), false);
    }

    public final void setRoster(Roster roster, boolean needSave) {
        Roster oldRoster;
        synchronized (rosterLockObject) {
            oldRoster = this.roster;
            if (null != oldRoster) {
                Util.removeAll(oldRoster.getGroupItems(), roster.getGroupItems());
                Util.removeAll(oldRoster.getContactItems(), roster.getContactItems());
            }
            this.roster = roster;
        }
        ChatHistory.instance.restoreContactsWithChat(this);
        synchronized (rosterLockObject) {
            for (int i = 0; i < roster.getContactItems().size(); ++i) {
                oldRoster.getContactItems().addElement(roster.getContactItems().elementAt(i));
            }
            for (int i = 0; i < roster.getGroupItems().size(); ++i) {
                Group g = roster.getGroupItems().elementAt(i);
                RosterHelper.getInstance().updateGroup(this, g);
                oldRoster.getGroupItems().addElement(g);
            }
            RosterHelper.getInstance().updateGroup(this, notInListGroup);
        }
        if (RosterHelper.getInstance().getProtocolCount() == 0) return;
        RosterHelper.getInstance().updateRoster();
        if (needSave)
            needSave();
    }

    public final void setContactListAddition(Group group) {
        synchronized (rosterLockObject) {
            RosterHelper.getInstance().updateGroup(this, group);
            RosterHelper.getInstance().updateGroup(this, notInListGroup);
            Vector<Contact> groupItems = group.getContacts();
            for (int i = 0; i < groupItems.size(); ++i) {
                if (-1 == roster.getContactItems().indexOf(groupItems.elementAt(i))) {
                    roster.getContactItems().addElement(groupItems.elementAt(i));
                }
            }
        }
        RosterHelper.getInstance().updateRoster();
        needSave();
    }

    public final boolean isConnecting() {
        return 100 != progress;
    }

    public final byte getConnectingProgress() {
        return progress;
    }

    public final void setConnectingProgress(int percent) {
        progress = (byte) ((percent < 0) ? 100 : percent);
        if (100 == percent) {
            reconnect_attempts = RECONNECT_COUNT;
            SawimApplication.getInstance().updateConnectionState();
            RosterHelper.getInstance().updateConnectionStatus();
            RosterHelper.getInstance().updateRoster();
        } else if (0 == percent) {
            SawimApplication.getInstance().updateConnectionState();
            RosterHelper.getInstance().updateConnectionStatus();
            RosterHelper.getInstance().updateRoster();
        }
        RosterHelper.getInstance().updateProgressBar();
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
            setRoster(new Roster(), false);
            return;
        }
        if (isConnected()) {
            return;
        }
        try {
            load();
        } catch (Exception e) {
            DebugLog.panic("roster load", e);
            setRoster(new Roster(), false);
        }
    }

    public final void needSave() {
        needSave = true;
        RosterHelper.getInstance().needRosterSave();
    }

    public final boolean safeSave() {
        boolean save = needSave;
        needSave = false;

        if (!save || "".equals(getUserId())) {
            return false;
        }
        synchronized (this) {
            try {
                Storage.delete(rmsName);
            } catch (Exception e) {
            }
            Storage storage = new Storage(rmsName);
            try {
                storage.open();
                save(storage);
            } catch (Exception e) {
                DebugLog.panic("roster save", e);
            }
            storage.close();
        }
        return true;
    }

    private void load() throws Exception {
        Roster roster = new Roster();
        Storage storage = new Storage(rmsName);
        storage.open();
        try {
            byte[] buf = storage.getRecord(1);
            if (buf.length != 0) {
                ByteArrayInputStream bais = new ByteArrayInputStream(buf);
                DataInputStream dis = new DataInputStream(bais);
                if (dis.readInt() != ROSTER_STORAGE_VERSION) {
                    throw new Exception();
                }
                loadProtocolData(storage.getRecord(2));
                for (int marker = 3; marker <= storage.getNumRecords(); ++marker) {
                    try {
                        buf = storage.getRecord(marker);
                        if ((null == buf) || (0 == buf.length)) {
                            continue;
                        }

                        bais = new ByteArrayInputStream(buf);
                        dis = new DataInputStream(bais);
                        while (0 < dis.available()) {
                            byte type = dis.readByte();
                            switch (type) {
                                case TYPE_GROUP:
                                    roster.getContactItems().addElement(loadContact(dis));
                                    break;
                                case TYPE_CONTACT:
                                    roster.getGroupItems().addElement(loadGroup(dis));
                                    break;
                            }
                        }
                    } catch (EOFException e) {
                    }
                }
                DebugLog.memoryUsage("clload");
            }
        } finally {
            storage.close();
        }
        setRoster(roster, false);
    }

    private void save(Storage storage) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(ROSTER_STORAGE_VERSION);
        byte[] buf = baos.toByteArray();
        storage.addRecord(buf);
        baos.reset();
        buf = saveProtocolData();
        storage.addRecord(buf);
        baos.reset();
        int cItemsCount = roster.getContactItems().size();
        int totalCount = cItemsCount + roster.getGroupItems().size();
        for (int i = 0; i < totalCount; ++i) {
            if (i < cItemsCount) {
                dos.writeByte(TYPE_GROUP);
                saveContact(dos, roster.getContactItems().elementAt(i));
            } else {
                dos.writeByte(TYPE_CONTACT);
                saveGroup(dos, roster.getGroupItems().elementAt(i - cItemsCount));
            }
            if ((baos.size() >= 4000) || (i == totalCount - 1)) {
                buf = baos.toByteArray();
                storage.addRecord(buf);
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
    }

    protected void s_removedContact(Contact contact) {
    }

    public final void removeContact(Contact contact) {
        if (contact.isTemp()) {
        } else if (isConnected()) {
            s_removeContact(contact);
        } else {
            return;
        }
        removeLocalContact(contact);
        RosterHelper.getInstance().updateRoster();
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
    }

    protected void s_addedContact(Contact contact) {
    }

    public final void addContact(Contact contact) {
        s_addContact(contact);
        contact.setTempFlag(false);
        cl_addContact(contact);
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

    public abstract void startConnection();

    abstract protected void closeConnection();

    protected void userCloseConnection() {
    }

    public void disconnect(boolean user) {
        setConnectingProgress(-1);
        if (user) {
            userCloseConnection();
        }
        setStatusesOffline();
        closeConnection();
        RosterHelper.getInstance().updateProgressBar();
        RosterHelper.getInstance().updateRoster();
        SawimApplication.getInstance().updateConnectionState();
        RosterHelper.getInstance().updateConnectionStatus();
        if (user) {
            DebugLog.println("disconnect " + getUserId());
        }
    }

    abstract public Group createGroup(String name);

    abstract protected Contact createContact(String uin, String name);

    public final Contact createTempContact(String uin, String name) {
        Contact contact = getItemByUID(uin);
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
        if (roster.getGroupItems().isEmpty()) {
            return null;
        }
        return new Search(this);
    }

    private ProtocolBranch branch;

    public final ProtocolBranch getProtocolBranch() {
        if (null == branch) {
            branch = new ProtocolBranch(this);
        }
        return branch;
    }

    public final Vector<Contact> getContactItems() {
        return roster.getContactItems();
    }

    public final Vector<Group> getGroupItems() {
        return roster.getGroupItems();
    }

    public final void beginTyping(String uin, boolean type) {
        Contact item = getItemByUID(uin);
        if (null != item) {
            beginTyping(item, type);
            RosterHelper.getInstance().updateRoster();
            if (RosterHelper.getInstance().getUpdateChatListener() != null)
                RosterHelper.getInstance().getUpdateChatListener().updateChat();
        }
    }

    private void beginTyping(Contact item, boolean type) {
        if (item.isTyping() != type) {
            item.beginTyping(type);
            Chat chat = ChatHistory.instance.getChat(item);
            if (null != chat) {
                chat.beginTyping(type);
            }
        }
    }

    protected void setStatusesOffline() {
        for (int i = roster.getContactItems().size() - 1; i >= 0; --i) {
            Contact c = roster.getContactItems().elementAt(i);
            c.setOfflineStatus();
        }
        synchronized (rosterLockObject) {
            if (RosterHelper.getInstance().useGroups) {
                for (int i = roster.getGroupItems().size() - 1; i >= 0; --i) {
                    (roster.getGroupItems().elementAt(i)).updateGroupData();
                }
            }
        }
    }

    public final Contact getItemByUID(String uid) {
        return roster.getItemByUID(uid);
    }

    public final Group getGroupById(int id) {
        return roster.getGroupById(id);
    }

    public final Group getGroup(Contact contact) {
        return getGroupById(contact.getGroupId());
    }

    public final Group getGroup(String name) {
        return roster.getGroup(name);
    }

    public final boolean inContactList(Contact contact) {
        return roster.hasContact(contact);
    }

    public final StatusInfo getStatusInfo() {
        return info;
    }

    protected abstract void s_updateOnlineStatus();

    public final void setOnlineStatus(int statusIndex, String msg) {
        setOnlineStatus(statusIndex, msg, true);
    }

    public final void setOnlineStatus(int statusIndex, String msg, boolean save) {
        profile.statusIndex = (byte) statusIndex;
        profile.statusMessage = msg;
        if (save) {
            Options.saveAccount(profile);
        }

        setLastStatusChangeTime();
        if (isConnected()) {
            s_updateOnlineStatus();
            RosterHelper.getInstance().updateProgressBar();
        }
    }

    public final void setStatus(int statusIndex, String msg) {
        if (profile == null) return;
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
        Group g = getGroup(c);
        if (null == g) {
            g = notInListGroup;
        }
        RosterHelper.getInstance().removeFromGroup(this, g, c);
    }

    private void ui_addContactToGroup(Contact contact, Group group) {
        RosterHelper.getInstance().removeFromGroup(this, notInListGroup, contact);
        contact.setGroup(group);
        if (null == group) {
            group = notInListGroup;
        }
        RosterHelper.getInstance().updateGroup(this, group);
    }

    private void ui_updateGroup(final Group group) {
        if (RosterHelper.getInstance().useGroups) {
            RosterHelper.getInstance().updateRoster(group);
        }
    }

    public final Group getNotInListGroup() {
        return notInListGroup;
    }

    public final Object getRosterLockObject() {
        return rosterLockObject;
    }

    public final void markMessages(Contact contact) {
        if (Options.getBoolean(JLocale.getString(R.string.pref_sort_up_with_msg))) {
            ui_updateContact(contact);
        }
        RosterHelper.getInstance().markMessages(contact);
    }

    public final void ui_changeContactStatus(Contact contact) {
        ui_updateContact(contact);
    }

    public final void ui_updateContact(final Contact contact) {
        synchronized (rosterLockObject) {
            Group group = getGroup(contact);
            if (null == group) {
                group = notInListGroup;
            }
            RosterHelper.getInstance().putIntoQueue(group);
        }
        RosterHelper.getInstance().updateRoster(contact);
        if (RosterHelper.getInstance().getUpdateChatListener() != null)
            RosterHelper.getInstance().getUpdateChatListener().updateChat();
    }

    private void cl_addContact(Contact contact) {
        if (null == contact) {
            return;
        }
        Group g = getGroup(contact);
        boolean hasnt = !inContactList(contact);
        if (hasnt) {
            roster.getContactItems().addElement(contact);
        }
        ui_addContactToGroup(contact, g);
        ui_updateContact(contact);
    }

    private void cl_moveContact(Contact contact, Group to) {
        synchronized (rosterLockObject) {
            ui_addContactToGroup(contact, to);
        }
        ui_updateContact(contact);
    }

    private void cl_addGroup(Group group) {
        if (-1 != roster.getGroupItems().indexOf(group)) {
            DebugLog.panic("Group '" + group.getName() + "' already added");
        }
        synchronized (rosterLockObject) {
            roster.getGroupItems().addElement(group);
            ui_updateGroup(group);
        }
    }

    private void cl_renameGroup(Group group) {
        ui_updateGroup(group);
    }

    private void cl_removeGroup(final Group group) {
        roster.getGroupItems().removeElement(group);
        RosterHelper.getInstance().updateRoster(group);
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
            roster.getContactItems().removeElement(contact);
            ui_removeFromAnyGroup(contact);
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
        lastStatusChangeTime = SawimApplication.getCurrentGmtTime();
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
        Contact contact = getItemByUID(message.getSndrUin());
        boolean isPlain = message instanceof PlainMessage;
        boolean isSystem = message instanceof SystemNotice;
        if ((null == contact) && (AntiSpam.isSpam(this, message, isSystem, isPlain) && contact.isConference())) {
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
        if (isPlain && isEmptyMessage(message.getText())) {
            return;
        }
        Chat chat = getChat(contact);
        boolean isHighlight = Chat.isHighlight(message.getProcessedText(), contact.getMyName());
        chat.addMessage(message, isPlain, isSystem, isHighlight);
        if (isSystem) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                if (autoGrand.contains(contact.getUserId())) {
                    grandAuth(contact.getUserId());
                    autoGrand.removeElement(contact.getUserId());
                    chat.resetAuthRequests();
                }
            }
        }
        boolean notifyMessage = false;
        boolean isPersonal = contact.isSingleUserContact();
        boolean isBlog = isBlogBot(contact.getUserId());
        boolean isMention = false;
        boolean isWakeUp = false;
        if (!silent) {
            if (Options.getBoolean(JLocale.getString(R.string.pref_answerer))) {
                Answerer.getInstance().checkMessage(this, contact, message);
            }
            if (!isPersonal && !message.isOffline() && (contact instanceof XmppContact)) {
                String msg = message.getText();
                String myName = contact.getMyName();
                isPersonal = msg.startsWith(myName)
                        && msg.startsWith(" ", myName.length() + 1);
                isMention = isHighlight;
            }
            if (message.isOffline()) {
            } else if (isPersonal || isMention) {
                isWakeUp = contact.isAuth() && !contact.isTemp()
                        && message.isWakeUp() && Options.getBoolean(JLocale.getString(R.string.pref_alarm));
                if (isWakeUp) {
                    SawimNotification.alarm(contact.getUserId());
                } else {
                    //playNotification(Notify.NOTIFY_MESSAGE);
                    notifyMessage = true;
                }
            } else if (isMention) {
                //playNotification(Notify.NOTIFY_MULTIMESSAGE);
            }
        }
        if (!isWakeUp) {
            if (!chat.isVisibleChat()) {
                chat.typeNewMessageIcon = chat.getNewMessageIcon();
                RosterHelper.getInstance().updateRoster(contact);
                SawimApplication.getInstance().sendNotify(notifyMessage && !chat.isVisibleChat());
            }
        }
    }

    protected boolean isBlogBot(String userId) {
        return false;
    }

    public final boolean isBot(Contact contact) {
        return contact.getName().endsWith("-bot");
    }

    public final void setAuthResult(String uin, boolean auth) {
        Contact c = getItemByUID(uin);
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
        DebugLog.println("connect");
        isReconnect = false;
        reconnect_attempts = RECONNECT_COUNT;
        disconnect(false);
        startConnection();
        setLastStatusChangeTime();
    }

    public final boolean isReconnect() {
        return isReconnect;
    }

    public final void processException(SawimException e) {
        DebugLog.println("process exception: " + e.getMessage());
        RosterHelper.getInstance().activateWithMsg(getUserId() + "\n" + e.getMessage());
        if (!SawimApplication.getInstance().isNetworkAvailable()) {
            e = new SawimException(123, 0);
        }
        if (e.isReconnectable()) {
            reconnect_attempts--;
            if (0 < reconnect_attempts) {
                if (isConnected() && !isConnecting()) {
                    isReconnect = true;
                    RosterHelper.getInstance().updateProgressBar();
                }
                try {
                    int iter = RECONNECT_COUNT - reconnect_attempts;
                    int sleep = Math.min(iter * 10, 2 * 60);
                    Thread.sleep(sleep * 1000);
                } catch (Exception ignored) {
                }
                if (isConnected() || isConnecting()) {
                    disconnect(false);
                    startConnection();
                }
                return;
            }
        }
        disconnect(false);
        setOnlineStatus(StatusInfo.STATUS_OFFLINE, null, false);
        showException(e);
    }

    public final void showException(SawimException e) {
        RosterHelper.getInstance().activateWithMsg(getUserId() + "\n" + e.getMessage());
    }

    public final void dismiss() {
        disconnect(false);
        userCloseConnection();
        ChatHistory.instance.unregisterChats(this);
        safeSave();
        profile = null;
        roster.setNull();
        roster = null;
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
        if (isConnected() && isMeVisible(to)) {
            s_sendTypingNotify(to, isTyping);
        }
    }

    protected abstract void sendSomeMessage(PlainMessage msg);

    public final void sendMessage(Contact to, String msg, boolean addToChat) {
        msg = StringConvertor.trim(msg);
        if (StringConvertor.isEmpty(msg)) {
            return;
        }
        PlainMessage plainMsg = new PlainMessage(this, to.getUserId(), SawimApplication.getCurrentGmtTime(), msg);
        if (isConnected()) {
            if (msg.startsWith("/") && !msg.startsWith("/me ") && !msg.startsWith("/wakeup") && (to instanceof XmppContact)) {
                boolean cmdExecuted = ((XmppContact) to).execCommand(this, msg);
                if (!cmdExecuted) {
                    String text = JLocale.getString(R.string.jabber_command_not_found);
                    SystemNotice notice = new SystemNotice(this, SystemNotice.SYS_NOTICE_MESSAGE, to.getUserId(), text);
                    getChat(to).addMessage(notice, false, true, false);
                }
                return;
            }
            sendSomeMessage(plainMsg);
        }
        if (addToChat) {
            getChat(to).addMyMessage(plainMsg);
        }
    }

    protected void doAction(BaseActivity activity, Contact contact, int cmd) {
    }

    public void showUserInfo(BaseActivity activity, Contact contact) {
    }

    public void showStatus(BaseActivity activity, Contact contact) {
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
                //playNotification(Notify.NOTIFY_ONLINE);
            }
        }
    }

    public String getUniqueUserId(Contact contact) {
        return contact.getUserId();
    }

    public String getUniqueUserId(String userId) {
        return userId;
    }

    public final Chat getChat(Contact contact) {
        Chat chat = ChatHistory.instance.getChat(contact);
        if (null == chat) {
            chat = new Chat(this, contact);
            if (!inContactList(contact)) {
                contact.setTempFlag(true);
                addLocalContact(contact);
            }
            ChatHistory.instance.registerChat(chat);
        }
        return chat;
    }
}
