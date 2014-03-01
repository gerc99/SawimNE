package protocol;

import DrawControls.icons.Icon;
import org.microemu.util.RecordStoreImpl;
import protocol.xmpp.XmppContact;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import sawim.FileTransfer;
import sawim.Options;
import sawim.SawimException;
import sawim.chat.Chat;
import sawim.chat.ChatHistory;
import sawim.chat.message.Message;
import sawim.chat.message.PlainMessage;
import sawim.chat.message.SystemNotice;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.io.Storage;
import sawim.modules.Answerer;
import sawim.modules.AntiSpam;
import sawim.modules.DebugLog;
import sawim.modules.Notify;
import sawim.modules.tracking.Tracking;
import sawim.roster.RosterHelper;
import sawim.search.Search;
import sawim.search.UserInfo;
import sawim.util.JLocale;

import java.io.*;
import java.util.Vector;

abstract public class Protocol {
    private static final int RECONNECT_COUNT = 20;
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
        return (nick.length() == 0) ? JLocale.getString(R.string.me) : nick;
    }

    public final Profile getProfile() {
        return profile;
    }

    public final void setProfile(Profile account) {
        this.profile = account;
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
                updateContacts(g);
                oldRoster.getGroupItems().addElement(g);
            }
            updateContacts(notInListGroup);
        }
        if (RosterHelper.getInstance().getProtocolCount() == 0) return;
        RosterHelper.getInstance().updateRoster();
        if (needSave)
            needSave();
    }

    public final void setContactListAddition(Group group) {
        synchronized (rosterLockObject) {
            updateContacts(group);
            updateContacts(notInListGroup);
            Vector groupItems = group.getContacts();
            for (int i = 0; i < groupItems.size(); ++i) {
                if (-1 == Util.getIndex(roster.getContactItems(), groupItems.elementAt(i))) {
                    roster.getContactItems().addElement((Contact) groupItems.elementAt(i));
                }
            }
        }
        RosterHelper.getInstance().updateRoster();
        needSave();
    }

    private void updateContacts(Group group) {
        RosterHelper.getInstance().updateGroup(this, group);
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
            RosterHelper.getInstance().updateBarProtocols();
            RosterHelper.getInstance().updateRoster();
        } else if (0 == percent) {
            SawimApplication.getInstance().updateConnectionState();
            RosterHelper.getInstance().updateConnectionStatus();
            RosterHelper.getInstance().updateBarProtocols();
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
            if (new Storage(getContactListRS()).exist()) {
                load();
            }
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
            String storage = getContactListRS();
            try {
                SawimApplication.getInstance().recordStoreManager.deleteRecordStore(storage);
            } catch (Exception e) {
            }
            RecordStoreImpl cl = null;
            try {
                cl = SawimApplication.getInstance().recordStoreManager.openRecordStore(storage, true);
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
        Roster roster = new Roster();
        RecordStoreImpl cl = SawimApplication.getInstance().recordStoreManager.openRecordStore(getContactListRS(), false);
        try {
            byte[] buf;
            ByteArrayInputStream bais;
            DataInputStream dis;
            buf = cl.getRecord(1);
            bais = new ByteArrayInputStream(buf);
            dis = new DataInputStream(bais);
            if (!dis.readUTF().equals(SawimApplication.VERSION)) {
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
                                roster.getContactItems().addElement(loadContact(dis));
                                break;
                            case 1:
                                roster.getGroupItems().addElement(loadGroup(dis));
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
        setRoster(roster, false);
    }

    private void save(RecordStoreImpl cl) throws Exception {
        ByteArrayOutputStream baos;
        DataOutputStream dos;
        byte[] buf;
        baos = new ByteArrayOutputStream();
        dos = new DataOutputStream(baos);
        dos.writeUTF(SawimApplication.VERSION);
        buf = baos.toByteArray();
        cl.addRecord(buf, 0, buf.length);
        baos.reset();
        buf = saveProtocolData();
        cl.addRecord(buf, 0, buf.length);
        baos.reset();
        int cItemsCount = roster.getContactItems().size();
        int totalCount = cItemsCount + roster.getGroupItems().size();
        for (int i = 0; i < totalCount; ++i) {
            if (i < cItemsCount) {
                saveContact(dos, roster.getContactItems().elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, roster.getGroupItems().elementAt(i - cItemsCount));
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

    public final void disconnect(boolean user) {
        setConnectingProgress(-1);
        closeConnection();
        if (user) {
            userCloseConnection();
        }
        setStatusesOffline();
        RosterHelper.getInstance().updateBarProtocols();
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
        if (roster.getGroupItems().isEmpty()) {
            return null;
        }
        return new Search(this);
    }

    public final Vector getContactItems() {
        return roster.getContactItems();
    }

    public final Vector getGroupItems() {
        return roster.getGroupItems();
    }

    public final void beginTyping(String uin, boolean type) {
        Contact item = getItemByUIN(uin);
        if (null != item) {
            beginTyping(item, type);
            RosterHelper.getInstance().updateRoster();
            if (RosterHelper.getInstance().getUpdateChatListener() != null)
                RosterHelper.getInstance().getUpdateChatListener().updateChat(item);
        }
    }

    private void beginTyping(Contact item, boolean type) {
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
                //playNotification(Notify.NOTIFY_TYPING);
            }
        }
    }

    protected final void setStatusesOffline() {
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

    public final Contact getItemByUIN(String uid) {
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
        if (save)
            Options.saveAccount(profile);

        setLastStatusChangeTime();
        if (isConnected()) {
            s_updateOnlineStatus();
            RosterHelper.getInstance().updateBarProtocols();
            RosterHelper.getInstance().updateProgressBar();
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
        if (Options.getBoolean(Options.OPTION_SORT_UP_WITH_MSG)) {
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
            RosterHelper.getInstance().getUpdateChatListener().updateChat(contact);
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
        if (-1 != Util.getIndex(roster.getGroupItems(), group)) {
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
        boolean isHighlight = Chat.isHighlight(message.getProcessedText(), contact.getMyName());
        chat.addMessage(message, !message.isWakeUp(), isHighlight);
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
            addMessageNotify(chat, contact, message, isHighlight);
        }
        if (chat.typeNewMessageIcon != chat.getNewMessageIcon() || chat.isVisibleChat()) {
            chat.typeNewMessageIcon = chat.getNewMessageIcon();
            if (contact != RosterHelper.getInstance().getCurrentContact() || !chat.isVisibleChat()) {
                SawimApplication.getInstance().sendNotify(contact.getUserId(), message.getText());
            }
            if (RosterHelper.getInstance().getUpdateChatListener() != null)
                RosterHelper.getInstance().getUpdateChatListener().updateChat(contact);
            RosterHelper.getInstance().updateRoster(contact);
            RosterHelper.getInstance().updateBarProtocols();
        }
    }

    private void addMessageNotify(Chat chat, Contact contact, Message message, boolean isHighlight) {
        boolean isPersonal = contact.isSingleUserContact();
        boolean isBlog = isBlogBot(contact.getUserId());
        boolean isHuman = isBlog || chat.isHuman() || !isPersonal;
        if (isBot(contact)) {
            isHuman = false;
        }
        boolean isMention = false;
        if (!isPersonal && !message.isOffline() && (contact instanceof XmppContact)) {
            String msg = message.getText();
            String myName = contact.getMyName();
            isPersonal = msg.startsWith(myName)
                    && msg.startsWith(" ", myName.length() + 1);
            isMention = isHighlight;
        }
        if (Options.getBoolean(Options.OPTION_ANSWERER)) {
            Answerer.getInstance().checkMessage(this, contact, message);
        }
        String id = contact.getUserId();
        if (message.isOffline()) {
        } else if (isPersonal) {
            if (contact.isAuth() && !contact.isTemp()
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

    public final void playNotification(boolean isSound, int type) {
        Notify.getSound().playSoundNotification(isSound, type);
    }

    public final void playNotification(int type) {
        Notify.getSound().playSoundNotification(type);
    }

    public final void processException(SawimException e) {
        DebugLog.println("process exception: " + e.getMessage());
        RosterHelper.getInstance().activateWithMsg(getUserId() + "\n" + e.getMessage());
        if (!SawimApplication.getInstance().isNetworkAvailable() && Options.getBoolean(Options.OPTION_INSTANT_RECONNECTION)) {
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
                    //playNotification(Notify.NOTIFY_RECONNECT);
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
        PlainMessage plainMsg = new PlainMessage(this, to, SawimApplication.getCurrentGmtTime(), msg);
        if (isConnected()) {
            if (msg.startsWith("/") && !msg.startsWith("/me ") && !msg.startsWith("/wakeup") && (to instanceof XmppContact)) {
                boolean cmdExecuted = ((XmppContact) to).execCommand(this, msg);
                if (!cmdExecuted) {
                    String text = JLocale.getString(R.string.jabber_command_not_found);
                    SystemNotice notice = new SystemNotice(this, SystemNotice.SYS_NOTICE_MESSAGE, to.getUserId(), text);
                    getChat(to).addMessage(notice, false, false);
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
            ChatHistory.instance.registerChat(chat);
        }
        return chat;
    }
}