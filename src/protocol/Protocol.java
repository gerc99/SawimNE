package protocol;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.message.*;
import ru.sawim.comm.*;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;
import ru.sawim.io.Storage;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.modules.*;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.ProtocolBranch;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.TextBoxView;
import ru.sawim.view.menu.JuickMenu;

import java.io.*;
import java.util.ArrayList;
import java.util.Vector;

public class Protocol implements FormListener {
    private static final int ROSTER_STORAGE_VERSION = 1;
    private static final int RECONNECT_COUNT = 20;

    private final Object rosterLockObject = new Object();
    protected Roster roster = new Roster();
    protected StatusInfo info;
    protected XStatusInfo xstatusInfo;
    private Profile profile;
    private String password;
    private String userid = "";
    private String rmsName = null;
    private boolean isReconnect;
    private int reconnect_attempts;
    private boolean needSave = false;
    private long lastStatusChangeTime;
    private byte progress = 100;
    private Vector autoGrand = new Vector();
    private Group notInListGroup;

    public final static int PRIORITY = 50;
    private Connection connection;
    private Vector rejoinList = new Vector();
    private String resource;
    private ServiceDiscovery disco = null;
    private AffiliationListConf alistc = null;
    private MirandaNotes notes = null;
    public static final XStatus xStatus = new XStatus();
    private final ArrayList<String> bots = new ArrayList<String>();

    protected void initStatusInfo() {
        bots.add(JuickMenu.JUICK);
        bots.add(JuickMenu.PSTO);
        bots.add(JuickMenu.POINT);

        ImageList icons = ImageList.createImageList("/jabber-status.png");
        final int[] statusIconIndex = {1, 0, 2, 0, -1, -1, -1, -1, -1, 2, -1, 3, -1, -1, 1};
        info = new StatusInfo(icons, statusIconIndex, statuses);
        xstatusInfo = Protocol.xStatus.getInfo();
    }

    private static final byte[] statuses = {
            StatusInfo.STATUS_OFFLINE,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_DND
    };

    public void addRejoin(String jid) {
        if (!rejoinList.contains(jid)) {
            rejoinList.addElement(jid);
        }
    }

    public void removeRejoin(String jid) {
        rejoinList.removeElement(jid);
    }

    public void rejoin() {
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = (String) rejoinList.elementAt(i);
            ServiceContact conf = (ServiceContact) getItemByUIN(jid);
            if (null != conf) {
                join(conf);
            }
        }
    }

    public boolean isEmpty() {
        return StringConvertor.isEmpty(userid) || (getUserId().indexOf('@') <= 0);
    }

    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    public boolean isBlogBot(String jid) {
        return bots.contains(getUniqueUserId(jid));
    }

    public String getUniqueUserId(Contact c) {
        return getUniqueUserId(c.getUserId());
    }

    public String getUniqueUserId(String userId) {
        if (isContactOverGate(userId)) {
            return Jid.getNick(userId).replace('%', '@').replace("\\40", "@");
        }
        return userId.replace('%', '@');
    }

    public void startConnection() {
        connection = new Connection();
        connection.setXmpp(this);
        connection.start();
    }

    public Connection getConnection() {
        return connection;
    }

    public void disconnect(boolean user) {
        if (user && null != connection) {
            SawimApplication.getInstance().getSession().clear(connection);
            connection.loggedOut();
        }
        setConnectingProgress(-1);
        if (user) {
            userCloseConnection();
        }
        setStatusesOffline();
        closeConnection();
        RosterHelper.getInstance().updateBarProtocols();
        RosterHelper.getInstance().updateProgressBar();
        RosterHelper.getInstance().updateRoster();
        SawimApplication.getInstance().updateConnectionState();
        RosterHelper.getInstance().updateConnectionStatus();
        if (user) {
            DebugLog.println("disconnect " + getUserId());
        }
    }

    protected final void userCloseConnection() {
        //rejoinList.removeAllElements();
    }

    protected final void closeConnection() {
        Connection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    protected void setStatusesOffline() {
        if (connection != null && !isStreamManagementSupported()) {
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
    }

    private int getNextGroupId() {
        while (true) {
            int id = Util.nextRandInt() % 0x1000;
            for (int i = getGroupItems().size() - 1; i >= 0; --i) {
                Group group = (Group) getGroupItems().elementAt(i);
                if (group.getId() == id) {
                    id = -1;
                    break;
                }
            }
            if (0 <= id) {
                return id;
            }
        }
    }

    public static final int GENERAL_GROUP = R.string.group_general;
    public static final int GATE_GROUP = R.string.group_transports;
    public static final int CONFERENCE_GROUP = R.string.group_conferences;

    public final Group createGroup(String name) {
        Group group = new Group(name);
        group.setGroupId(getNextGroupId());
        int mode = Group.MODE_FULL_ACCESS;
        if (JLocale.getString(Protocol.CONFERENCE_GROUP).equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_TOP;
        } else if (JLocale.getString(Protocol.GATE_GROUP).equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_BOTTOM;
        }
        group.setMode(mode);
        return group;
    }

    public final Group getOrCreateGroup(String groupName) {
        if (StringConvertor.isEmpty(groupName)) {
            return null;
        }
        Group group = getGroup(groupName);
        if (null == group) {
            group = createGroup(groupName);
            addGroup(group);
        }
        return group;
    }

    protected final Contact createContact(String jid, String name) {
        name = (null == name) ? jid : name;
        jid = Jid.realJidToSawimJid(jid);

        boolean isGate = (-1 == jid.indexOf('@'));
        boolean isConference = Jid.isConference(jid);
        if (isGate || isConference) {
            ServiceContact c = new ServiceContact(jid, name);
            if (c.isConference()) {
                c.setGroup(getOrCreateGroup(c.getDefaultGroupName()));
                c.setMyName(getDefaultName());

            } else if (isConference) {
                ServiceContact conf = (ServiceContact) getItemByUIN(Jid.getBareJid(jid));
                if (null != conf) {
                    c.setPrivateContactStatus(conf);
                    c.setMyName(conf.getMyName());
                }
            }
            return c;
        }
        return new Contact(jid, name);
    }

    private String getDefaultName() {
        return Jid.getNick(getUserId());
    }

    protected void sendSomeMessage(PlainMessage msg) {
        getConnection().sendMessage(msg);
    }

    protected final void s_searchUsers(Search cont) {
        UserInfo userInfo = new UserInfo(this);
        userInfo.uin = cont.getSearchParam(Search.UIN);
        if (null != userInfo.uin) {
            cont.addResult(userInfo);
        }
        cont.finished();
    }

    protected void s_updateOnlineStatus() {
        connection.setStatus(getProfile().statusIndex, "", PRIORITY);
        if (isStreamManagementSupported()) return;
        if (isReconnect()) {
            for (int i = 0; i < rejoinList.size(); ++i) {
                String jid = (String) rejoinList.elementAt(i);
                ServiceContact c = (ServiceContact) getItemByUIN(jid);
                if (null != c && !c.isOnline()) {
                    connection.sendPresence(c);
                }
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
        if (isStreamManagementSupported()) {
            contact.setStatus(dis.readByte(), dis.readUTF());
            contact.setXStatus(dis.readByte(), dis.readUTF());
        }
        if (contact instanceof ServiceContact) {
            ServiceContact serviceContact = (ServiceContact) contact;
            if (serviceContact.isConference()) {
                String userNick = dis.readUTF();
                boolean isAutoJoin = dis.readBoolean();
                serviceContact.setMyName(userNick);
                serviceContact.setAutoJoin(isAutoJoin);
                if (isAutoJoin) {
                    addRejoin(serviceContact.getUserId());
                }
                serviceContact.setPresencesFlag(dis.readBoolean());
            }
        }
        if (isStreamManagementSupported()) {
            int subContactSize = dis.readInt();
            for (int i = 0; i < subContactSize; ++i) {
                Contact.SubContact subContact = new Contact.SubContact();
                subContact.status = dis.readByte();
                subContact.priority = dis.readByte();
                subContact.priorityA = dis.readByte();
                subContact.resource = dis.readUTF();
                contact.subcontacts.add(subContact);
            }
        }
        return contact;
    }

    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        out.writeByte(0);
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
        if (isStreamManagementSupported()) {
            out.writeByte(contact.getStatusIndex());
            out.writeUTF(StringConvertor.notNull(contact.getStatusText()));
            out.writeByte(contact.getXStatusIndex());
            out.writeUTF(StringConvertor.notNull(contact.getXStatusText()));
        }
        if (contact instanceof ServiceContact) {
            ServiceContact serviceContact = (ServiceContact) contact;
            if (serviceContact.isConference()) {
                out.writeUTF(serviceContact.getMyName());
                out.writeBoolean(serviceContact.isAutoJoin());
                out.writeBoolean(serviceContact.isPresence());
            }
        }
        if (isStreamManagementSupported()) {
            Contact xmppContact = contact;
            out.writeInt(xmppContact.subcontacts.size());
            for (Contact.SubContact subContact : xmppContact.subcontacts) {
                out.writeByte(subContact.status);
                out.writeByte(subContact.priority);
                out.writeByte(subContact.priorityA);
                out.writeUTF(subContact.resource);
            }
        }
    }

    void setConfContactStatus(ServiceContact conf, String resource, byte status, String statusText, int role, int priorityA, String roleText) {
        conf.__setStatus(resource, role, priorityA, status, statusText, roleText);
    }

    void setContactStatus(Contact c, String resource, byte status, String text, int priority) {
        c.__setStatus(resource, priority, 0, status, text, null);
    }

    protected final void s_addedContact(Contact contact) {
        connection.updateContact((Contact) contact);
    }

    protected final void s_removedContact(Contact contact) {
        if (!contact.isTemp()) {
            boolean unregister = Jid.isGate(contact.getUserId())
                    && !Jid.getDomain(getUserId()).equals(contact.getUserId());
            if (unregister) {
                getConnection().unregister(contact.getUserId());
            }
            connection.removeContact(contact.getUserId());
            if (unregister) {
                getConnection().removeGateContacts(contact.getUserId());
            }
        }
        if (contact.isOnline() && !contact.isSingleUserContact()) {
            getConnection().sendPresenceUnavailable(contact.getUserId());
        }
    }

    protected final void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.updateContacts(getContactItems());
    }

    protected final void s_moveContact(Contact contact, Group to) {
        Group fromGroup = getGroup(contact);
        RosterHelper.getInstance().removeFromGroup(this, fromGroup, contact);
        contact.setGroup(to);
        connection.updateContact((Contact) contact);
    }

    protected final void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((Contact) contact);
    }

    public void grandAuth(String uin) {
        connection.sendSubscribed(uin);
    }

    public void denyAuth(String uin) {
        connection.sendUnsubscribed(uin);
    }

    public void autoDenyAuth(String uin) {
        denyAuth(uin);
    }

    public void requestAuth(String uin) {
        connection.requestSubscribe(uin);
    }

    private String getYandexDomain(String domain) {
        boolean nonPdd = "ya.ru".equals(domain)
                || "narod.ru".equals(domain)
                || domain.startsWith("yandex.");
        return nonPdd ? "xmpp.yandex.ru" : "domain-xmpp.ya.ru";
    }

    protected String processUin(String uin) {
        resource = Jid.getResource(uin, "Sawim");
        return Jid.getBareJid(uin);
    }

    public String getResource() {
        return resource;
    }

    void removeMe(String uin) {
        connection.sendUnsubscribed(uin);
    }

    public ServiceDiscovery getServiceDiscovery() {
        if (null == disco) {
            disco = new ServiceDiscovery();
        }
        disco.init(this);
        return disco;
    }

    public AffiliationListConf getAffiliationListConf() {
        if (null == alistc) {
            alistc = new AffiliationListConf();
        }
        alistc.init(this);
        return alistc;
    }

    public MirandaNotes getMirandaNotes() {
        if (null == notes) {
            notes = new MirandaNotes();
        }
        notes.init(this);
        return notes;
    }

    public String getUserIdName() {
        return "JID";
    }

    public void sendFile(FileTransfer transfer, String filename, String description) {
        getConnection().setIBB(new IBBFileTransfer(filename, description, transfer));
    }

    protected void s_updateXStatus() {
        connection.setXStatus();
    }

    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            getConnection().saveVCard(userInfo);
        }
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to instanceof ServiceContact) {
            return;
        }
        Contact c = (Contact) to;
        Contact.SubContact s = c.getCurrentSubContact();
        if (null != s) {
            connection.sendTypingNotify(to.getUserId() + "/" + s.resource, isTyping);
        }
    }

    public boolean isContactOverGate(String jid) {
        if (Jid.isGate(jid)) {
            return false;
        }
        Vector all = getContactItems();
        for (int i = all.size() - 1; 0 <= i; --i) {
            Contact c = (Contact) all.elementAt(i);
            if (Jid.isGate(c.getUserId())) {
                if (jid.endsWith(c.getUserId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void leave(ServiceContact conf) {
        if (conf.isOnline()) {
            getConnection().sendPresenceUnavailable(conf.getUserId() + "/" + conf.getMyName());
            conf.nickOffline(this, conf.getMyName(), 0, null, null);

            Vector all = getContactItems();
            String conferenceJid = conf.getUserId() + '/';
            for (int i = all.size() - 1; 0 <= i; --i) {
                Contact c = (Contact) all.elementAt(i);
                if (c.getUserId().startsWith(conferenceJid) && !c.hasUnreadMessage()) {
                    removeContact(c);
                }
            }
        }
    }

    public void join(ServiceContact c) {
        String jid = c.getUserId();
        if (!c.isOnline()) {
            setContactStatus(c, c.getMyName(), StatusInfo.STATUS_ONLINE, "", 0);
            c.doJoining();
        }
        if (connection == null) return;
        connection.sendPresence(c);
        String password = c.getPassword();
        if (Jid.isIrcConference(jid) && !StringConvertor.isEmpty(password)) {
            String nickserv = jid.substring(jid.indexOf('%') + 1) + "/NickServ";
            connection.sendMessage(nickserv, "/quote NickServ IDENTIFY " + password);
            connection.sendMessage(nickserv, "IDENTIFY " + password);
        }
    }

    protected void doAction(BaseActivity activity, Contact c, int cmd) {
        final Contact contact = (Contact) c;
        switch (cmd) {
            case ContactMenu.GATE_CONNECT:
                getConnection().sendPresence((ServiceContact) contact);
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.GATE_DISCONNECT:
                getConnection().sendPresenceUnavailable(c.getUserId());
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.GATE_REGISTER:
                getConnection().register(c.getUserId());
                break;

            case ContactMenu.GATE_UNREGISTER:
                getConnection().unregister(c.getUserId());
                getConnection().removeGateContacts(c.getUserId());
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.GATE_ADD:
                Search s = this.getSearchForm();
                s.setXmppGate(c.getUserId());
                s.show(activity, "", false);
                break;

            case ContactMenu.USER_MENU_USERS_LIST:
                if (contact.isOnline() || !isConnected()) {
                } else {
                    ServiceDiscovery sd = getServiceDiscovery();
                    sd.setServer(contact.getUserId());
                    sd.isMucUsers(true);
                    sd.showIt();
                }
                break;
            case ContactMenu.COMMAND_TITLE:
                TextBoxView textbox = new TextBoxView();
                textbox.setString("/title " + c.getStatusText());
                textbox.setTextBoxListener(new TextBoxView.TextBoxListener() {
                    @Override
                    public void textboxAction(TextBoxView box, boolean ok) {
                        sendMessage(contact, box.getString(), true);
                    }
                });
                textbox.show(activity.getSupportFragmentManager(), "title_conf");
                break;

            case ContactMenu.CONFERENCE_CONNECT:
                join((ServiceContact) c);
                break;

            case ContactMenu.CONFERENCE_OPTIONS:
                showOptionsForm(activity, (ServiceContact) c);
                break;

            case ContactMenu.CONFERENCE_OWNER_OPTIONS:
                connection.requestOwnerForm(c.getUserId());
                break;
            case ContactMenu.CONFERENCE_OWNERS:
                AffiliationListConf alc = getAffiliationListConf();
                alc.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "owner");
                alc.showIt();
                break;
            case ContactMenu.CONFERENCE_ADMINS:
                AffiliationListConf al = getAffiliationListConf();
                al.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "admin");
                al.showIt();
                break;
            case ContactMenu.CONFERENCE_MEMBERS:
                AffiliationListConf affiliationListConf = getAffiliationListConf();
                affiliationListConf.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "member");
                affiliationListConf.showIt();
                break;
            case ContactMenu.CONFERENCE_INBAN:
                AffiliationListConf aff = getAffiliationListConf();
                aff.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "outcast");
                aff.showIt();
                break;

            case ContactMenu.CONFERENCE_DISCONNECT:
                leave((ServiceContact) c);
                break;

            case ContactMenu.CONFERENCE_ADD:
                addContact(c);
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.USER_MENU_CONNECTIONS:
                showListOfSubcontacts(activity, contact);
                break;
            case ContactMenu.USER_INVITE:
                try {
                    showInviteForm(activity, c.getUserId() + '/' + c.getCurrentSubContact().resource);
                } catch (Exception e) {
                }
                break;

            case ContactMenu.USER_MENU_SEEN:
                getConnection().showContactSeen(c.getUserId());
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.USER_MENU_ADHOC:
                AdHoc adhoc = new AdHoc(this, contact);
                adhoc.show(activity);
                break;

            case ContactMenu.USER_MENU_REMOVE_ME:
                removeMe(c.getUserId());
                RosterHelper.getInstance().updateRoster();
                break;

        }
    }

    public void showListOfSubcontacts(BaseActivity activity, final Contact c) {
        final Vector items = new Vector();
        int selected = 0;
        for (int i = 0; i < c.subcontacts.size(); ++i) {
            Contact.SubContact contact = c.subcontacts.elementAt(i);
            items.add(contact.resource);
            if (contact.resource.equals(c.currentResource)) {
                selected = i;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle(c.getName());
        builder.setSingleChoiceItems(Util.vectorToArray(items), selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                c.setActiveResource((String) items.get(which));
                RosterHelper.getInstance().updateRoster();
                dialog.dismiss();
            }
        });
        builder.create().show();
    }

    public void showUserInfo(BaseActivity activity, Contact contact) {
        if (!contact.isSingleUserContact()) {
            doAction(activity, contact, ContactMenu.USER_MENU_USERS_LIST);
            return;
        }

        String realJid = contact.getUserId();
        if (Jid.isConference(realJid) && (-1 != realJid.indexOf('/'))) {
            ServiceContact conference = (ServiceContact) getItemByUIN(Jid.getBareJid(realJid));
            if (conference != null) {
                String r = conference.getRealJid(Jid.getResource(realJid, ""));
                if (!StringConvertor.isEmpty(r)) {
                    realJid = r;
                }
            }
        }
        UserInfo data;
        if (isConnected()) {
            data = getConnection().getUserInfo(contact);
            data.uin = realJid;
            data.createProfileView(contact.getName());
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(this, contact.getUserId());
            data.uin = realJid;
            data.nick = contact.getName();
            data.createProfileView(contact.getName());
            data.updateProfileView();
        }
        data.showProfile(activity);
    }

    public void updateStatusView(StatusView statusView, Contact contact) {
        if (statusView.getContact() != contact) {
            return;
        }
        String statusMessage = contact.getStatusText();

        String xstatusMessage = "";
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            xstatusMessage = contact.getXStatusText();
            String s = StringConvertor.notNull(statusMessage);
            if (!StringConvertor.isEmpty(xstatusMessage)
                    && s.startsWith(xstatusMessage)) {
                xstatusMessage = statusMessage;
                statusMessage = null;
            }
        }

        statusView.initUI();
        statusView.addContactStatus();
        statusView.addStatusText(statusMessage);

        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            statusView.addXStatus();
            statusView.addStatusText(xstatusMessage);
        }
        if (contact.isSingleUserContact()) {
            statusView.addClient();
        }
        statusView.addTime();
    }

    public void showStatus(Contact contact) {
        StatusView statusView = RosterHelper.getInstance().getStatusView();
        try {
            if (contact.isOnline() && contact.isSingleUserContact()) {
                String jid = contact.getUserId();
                if (!(contact instanceof ServiceContact)) {
                    jid += '/' + contact.getCurrentSubContact().resource;
                }
                if (contact instanceof ServiceContact) {
                    statusView.setUserRole(contact.getCurrentSubContact().roleText);
                }
                getConnection().requestClientVersion(jid);
            }
        } catch (Exception ignored) {
        }

        statusView.init(this, contact);
        updateStatusView(statusView, contact);
        statusView.showIt();
    }

    public void saveAnnotations(String xml) {
        getConnection().requestRawXml(xml);
    }

    private Forms enterData = null;
    private ServiceContact enterConf = null;
    private static final int NICK = 0;
    private static final int PASSWORD = 1;
    private static final int AUTOJOIN = 2;
    private static final int IS_PRESENCES = 3;

    private Forms enterDataInvite = null;
    private static final int JID_MESS_TO = 7;
    private static final int JID_INVITE_TO = 8;
    private static final int REASON_INVITE = 9;

    public String onlineConference(Vector v) {
        String list[] = new String[v.size()];
        String items = "";
        for (int i = 1; i < v.size(); ++i) {
            Contact c = (Contact) v.elementAt(i);
            if (c.isConference() && c.isOnline()) {
                list[i] = c.getUserId();
                items += "|" + list[i];
            }
        }
        return items.substring(1);
    }

    public final void showInviteForm(BaseActivity activity, String jid) {
        enterDataInvite = new Forms(R.string.invite, this, true);
        enterDataInvite.addSelector(JID_MESS_TO, R.string.conference, onlineConference(getContactItems()), 1);
        enterDataInvite.addTextField(JID_INVITE_TO, R.string.jid, jid);
        enterDataInvite.addTextField(REASON_INVITE, R.string.reason, "");
        enterDataInvite.show(activity);
    }

    void showOptionsForm(BaseActivity activity, ServiceContact c) {
        enterConf = c;
        enterData = new Forms(R.string.conference, this, false);
        enterData.addTextField(NICK, R.string.nick, c.getMyName());
        enterData.addTextField(PASSWORD, R.string.password, c.getPassword());
        if (!c.isTemp()) {
            enterData.addCheckBox(AUTOJOIN, R.string.autojoin, c.isAutoJoin());
        }
        enterData.addCheckBox(IS_PRESENCES, R.string.notice_presence, c.isPresence());
        enterData.show(activity);
        if (isConnected() && !Jid.isIrcConference(c.getUserId())) {
            getConnection().requestConferenceInfo(c.getUserId());
        }
    }

    void setConferenceInfo(String jid, String description) {
        if (null != enterData && enterConf.getUserId().equals(jid)) {
            enterData.addString(R.string.description, description);
            enterData.invalidate(true);
        }
    }

    public void formAction(Forms form, boolean apply) {
        if (enterData == form) {
            if (apply) {
                if (enterConf.isConference()) {
                    String oldNick = enterConf.getMyName();
                    enterConf.setMyName(enterData.getTextFieldValue(NICK));
                    enterConf.setAutoJoin(!enterConf.isTemp() && enterData.getCheckBoxValue(AUTOJOIN));
                    enterConf.setPassword(enterData.getTextFieldValue(PASSWORD));
                    enterConf.setPresencesFlag(enterData.getCheckBoxValue(IS_PRESENCES));

                    if (isConnected()) {
                        boolean needUpdate = !enterConf.isTemp();
                        if (needUpdate) {
                            getConnection().saveConferences();
                        }
                        if (enterConf.isOnline() && !oldNick.equals(enterConf.getMyName())) {
                            join(enterConf);
                        }
                    }
                }
            }
            enterData.back();
            enterData = null;
            enterConf = null;
        }
        if (enterDataInvite == form) {
            if (apply) {
                String[] onlineConferenceI = Util.explode(onlineConference(getContactItems()), '|');
                getConnection().sendInvite(onlineConferenceI[enterDataInvite.getSelectorValue(JID_MESS_TO)], enterDataInvite.getTextFieldValue(JID_INVITE_TO), enterDataInvite.getTextFieldValue(REASON_INVITE));
                Toast.makeText(SawimApplication.getContext(), R.string.invitation_sent, Toast.LENGTH_LONG).show();
            }
            enterDataInvite.back();
            enterDataInvite = null;
        }
    }

    public boolean isStreamManagementSupported() {
        return SawimApplication.getInstance().getSession()
                .isStreamManagementSupported(getUserId() + '/' + getResource());
    }

    public final String getUserId() {
        return userid;
    }

    protected final void setUserId(String userId) {
        userid = userId;
    }

    public final String getNick() {
        return JLocale.getString(R.string.me);
    }

    public final Profile getProfile() {
        return profile;
    }

    public final void setProfile(Profile account) {
        profile = account;
        String rawUin = StringConvertor.notNull(account.userId);
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
        if (!Options.hasAccount()) return;
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
                if (-1 == roster.getContactItems().indexOf(groupItems.elementAt(i))) {
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
                saveContact(dos, roster.getContactItems().elementAt(i));
            } else {
                dos.writeByte(1);
                saveGroup(dos, roster.getGroupItems().elementAt(i - cItemsCount));
            }
            if ((baos.size() >= 4000) || (i == totalCount - 1)) {
                buf = baos.toByteArray();
                storage.addRecord(buf);
                baos.reset();
            }
        }
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

    protected void saveGroup(DataOutputStream out, Group group) throws Exception {
        out.writeInt(group.getId());
        out.writeUTF(group.getName());
        out.writeBoolean(group.isExpanded());
    }

    public final void removeContact(Contact contact) {
        if (contact.isTemp()) {
        } else if (isConnected()) {
        } else {
            return;
        }
        removeLocalContact(contact);
        RosterHelper.getInstance().updateRoster();
    }

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

    public final void moveContactTo(Contact contact, Group to) {
        s_moveContact(contact, to);
        cl_moveContact(contact, to);
    }

    public final void addContact(Contact contact) {
        contact.setTempFlag(false);
        cl_addContact(contact);
        needSave();
        s_addedContact(contact);
    }

    public final void addTempContact(Contact contact) {
        cl_addContact(contact);
    }

    public final void removeGroup(Group group) {
        cl_removeGroup(group);
        needSave();
    }

    public final void renameGroup(Group group, String name) {
        s_renameGroup(group, name);
        group.setName(name);
        cl_renameGroup(group);
        needSave();
    }

    public final void addGroup(Group group) {
        cl_addGroup(group);
        needSave();
    }

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
        Contact contact = getItemByUIN(message.getSndrUin());
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
        if (!silent) {
            if (Options.getBoolean(Options.OPTION_ANSWERER)) {
                Answerer.getInstance().checkMessage(this, contact, message);
            }
            if (!isPersonal && !message.isOffline()) {
                String msg = message.getText();
                String myName = contact.getMyName();
                isPersonal = msg.startsWith(myName)
                        && msg.startsWith(" ", myName.length() + 1);
                isMention = isHighlight;
            }
            if (message.isOffline()) {
            } else if (isPersonal) {
                if (contact.isAuth() && !contact.isTemp()
                        && message.isWakeUp() && Options.getBoolean(Options.OPTION_ALARM)) {
                    SawimNotification.alarm(message.getProcessedText());
                } else {
                    //playNotification(Notify.NOTIFY_MESSAGE);
                    notifyMessage = true;
                }
            } else if (isMention) {
                //playNotification(Notify.NOTIFY_MULTIMESSAGE);
            }
        }
        boolean isNewMessageIcon = chat.typeNewMessageIcon != chat.getNewMessageIcon();
        if ((isNewMessageIcon || (isNewMessageIcon && SawimApplication.isManyPane()))
                && isPersonal || isMention) {
            SawimApplication.getInstance().sendNotify(contact.getUserId(), message.getText(), notifyMessage);
        }
        if (isNewMessageIcon) {
            chat.typeNewMessageIcon = chat.getNewMessageIcon();
            RosterHelper.getInstance().updateRoster(contact);
            RosterHelper.getInstance().updateBarProtocols();
        }
        if (RosterHelper.getInstance().getUpdateChatListener() != null)
            RosterHelper.getInstance().getUpdateChatListener().updateChat(contact);
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
        ChatHistory.instance.unregisterChats();
        safeSave();
        profile = null;
        roster.setNull();
        roster = null;
    }

    public boolean isMeVisible(Contact to) {
        return true;
    }

    public final void sendTypingNotify(Contact to, boolean isTyping) {
        if (isConnected() && isMeVisible(to)
                && (1 < Options.getInt(R.array.typing_array, Options.OPTION_TYPING_MODE))) {
            s_sendTypingNotify(to, isTyping);
        }
    }

    public final void sendMessage(Contact to, String msg, boolean addToChat) {
        msg = StringConvertor.trim(msg);
        if (StringConvertor.isEmpty(msg)) {
            return;
        }
        PlainMessage plainMsg = new PlainMessage(this, to, SawimApplication.getCurrentGmtTime(), msg);
        if (isConnected()) {
            if (msg.startsWith("/") && !msg.startsWith("/me ") && !msg.startsWith("/wakeup")) {
                boolean cmdExecuted = to.execCommand(this, msg);
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
