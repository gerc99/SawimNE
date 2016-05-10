package protocol.xmpp;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.provider.Settings;
import android.widget.Toast;
import protocol.*;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.gcm.Preferences;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.ImageList;
import ru.sawim.listener.OnMoreMessagesLoaded;
import ru.sawim.ui.adapter.form.FormListener;
import ru.sawim.ui.adapter.form.Forms;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.FileTransfer;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.RosterHelper;
import ru.sawim.ui.fragment.TextBoxDialogFragment;
import ru.sawim.ui.fragment.menu.JuickMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public final class Xmpp extends Protocol implements FormListener {

    public final static int PRIORITY = 50;
    private XmppConnection connection;
    private List<String> rejoinList = new ArrayList<>();
    private String resource;
    private ServiceDiscovery disco = null;
    private AffiliationListConf alistc = null;
    private MirandaNotes notes = null;
    public static final XmppXStatus xStatus = new XmppXStatus();
    private final ArrayList<String> bots = new ArrayList<String>();

    private XmppSession xmppSession;

    public Xmpp() {
        xmppSession = new XmppSession();
        if (xmppSession.isEmpty()) {
            DebugLog.systemPrintln("[SESSION] Session not found");
        } else {
            DebugLog.systemPrintln(
                    String.format("[SESSION] Load session with id=%s and user=%s",
                            xmppSession.getSessionId(), xmppSession.getUserId()));
        }
    }

    XmppSession getXmppSession() {
        return xmppSession;
    }

    protected void initStatusInfo() {
        bots.add(JuickMenu.JUICK);
        bots.add(JuickMenu.PSTO);
        bots.add(JuickMenu.POINT);

        ImageList icons = createStatusIcons();
        final int[] statusIconIndex = {1, 0, 2, 0, -1, -1, -1, -1, -1, 2, -1, 3, -1, -1, 1};
        info = new StatusInfo(icons, statusIconIndex, statuses);
        xstatusInfo = Xmpp.xStatus.getInfo();
        clientInfo = XmppClient.get();
    }

    private static final byte[] statuses = {
            StatusInfo.STATUS_OFFLINE,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_DND
    };

    private static final byte[] statusesOther = {
            StatusInfo.STATUS_OFFLINE,
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_XA,
            StatusInfo.STATUS_DND
    };

    private ImageList createStatusIcons() {
        String file = "jabber";
        ImageList icons = ImageList.createImageList("/" + file + "-status.png");
        if (0 < icons.size()) {
            return icons;
        }

        return ImageList.createImageList("/jabber-status.png");
    }

    public void addRejoin(String jid) {
        if (!rejoinList.contains(jid)) {
            rejoinList.add(jid);
        }
    }

    public void removeRejoin(String jid) {
        rejoinList.remove(jid);
    }

    public void rejoin() {
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = rejoinList.get(i);
            XmppServiceContact conf = (XmppServiceContact) getItemByUID(jid);
            if (null != conf) {
                join(conf);
            }
        }
    }

    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
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

    @Override
    public String getUniqueUserId(String userId) {
        if (isContactOverGate(userId)) {
            return Jid.getNick(userId).replace('%', '@').replace("\\40", "@");
        }
        return userId.replace('%', '@');
    }

    public void startConnection() {
        connection = new XmppConnection();
        connection.setXmpp(this);
        connection.start();
    }

    public XmppConnection getConnection() {
        return connection;
    }

    public boolean hasS2S() {
        return true;
    }

    public boolean hasVCardEditor() {
        return true;
    }

    protected final void userCloseConnection() {
        //rejoinList.removeAllElements();
        if (null != connection) {
            connection.loggedOut();
            connection.getXmppSession().resetSessionData();
        }
    }

    protected final void closeConnection() {
        XmppConnection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    protected void setStatusesOffline() {
        if (!xmppSession.isEmpty()) {
            DebugLog.systemPrintln("[SESSION] Keeping, don't drop statuses");
            return;
        }
        DebugLog.systemPrintln("[SESSION] Not keeping, drop statuses");

        super.setStatusesOffline();
    }

    public final void ui_changeContactStatus(Contact contact) {
        super.ui_changeContactStatus(contact);
        getStorage().save(this, contact, getGroup(contact));
    }

    public final void addContact(Contact contact) {
        super.addContact(contact);
        getStorage().save(this, contact, getGroup(contact));
    }

    public static final int GENERAL_GROUP = R.string.group_general;
    public static final int GATE_GROUP = R.string.group_transports;
    public static final int CONFERENCE_GROUP = R.string.group_conferences;

    public final Group createGroup(String groupName) {
        Group group = new Group(groupName);
        group.setGroupId(generateGroupId(groupName));
        int mode = Group.MODE_FULL_ACCESS;
        if (JLocale.getString(Xmpp.CONFERENCE_GROUP).equals(groupName) || "Conferences".equals(groupName)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_TOP;
        } else if (JLocale.getString(Xmpp.GATE_GROUP).equals(groupName)) {
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
        Group group = getGroupById(generateGroupId(groupName));
        if (null == group) {
            group = createGroup(groupName);
            addGroup(group);
        }
        return group;
    }

    public final Contact createContact(String jid, String name, boolean isConference) {
        name = (null == name) ? jid : name;
        jid = Jid.realJidToSawimJid(jid);
        boolean isGate = !jid.contains("@");
        boolean isPrivate = jid.contains("/");
        if (isConference) {
            XmppServiceContact c = new XmppServiceContact(jid, name, true, false);
            c.setGroup(getOrCreateGroup(c.getDefaultGroupName()));
            c.setMyName(Jid.getNick(getUserId()));
            return c;
        }
        if (isGate) {
            XmppServiceContact c = new XmppServiceContact(jid, name, false, true);
            return c;
        }
        if (isPrivate) {
            XmppServiceContact conf = (XmppServiceContact) getItemByUID(Jid.getBareJid(jid));
            if (null != conf) {
                XmppServiceContact c = new XmppServiceContact(jid, name, true, false);
                c.setPrivateContactStatus(conf);
                c.setMyName(conf.getMyName());
                return c;
            }
        }
        Contact contact = new XmppContact(jid, name);
        return contact;
    }

    protected void sendSomeMessage(PlainMessage msg) {
        Messages.sendMessage(getConnection(), msg);
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
        if (isReconnect()) {
            for (int i = 0; i < rejoinList.size(); ++i) {
                String jid = rejoinList.get(i);
                XmppServiceContact c = (XmppServiceContact) getItemByUID(jid);
                if (null != c && !c.isOnline()) {
                    Presences.sendPresence(connection, c);
                }
            }
        }
    }

    void setConfContactStatus(XmppServiceContact conf, String resource, byte status, String statusText, int role, int priorityA, String roleText) {
        conf.__setStatus(this, resource, role, priorityA, status, statusText, roleText);
    }

    void setContactStatus(XmppContact c, String resource, byte status, String text, int priority) {
        c.__setStatus(this, resource, priority, 0, status, text, null);
    }

    protected final void s_addedContact(Contact contact) {
        connection.updateContact((XmppContact) contact);
    }

    protected final void s_addGroup(Group group) {
    }

    protected final void s_removeGroup(Group group) {
    }

    protected final void s_removedContact(Contact contact) {
        if (!contact.isTemp()) {
            boolean unregister = Jid.isGate(contact.getUserId())
                    && !Jid.getDomain(getUserId()).equals(contact.getUserId());
            if (unregister) {
                getConnection().unregister(contact.getUserId());
            }
            connection.removeContact(contact);
            if (unregister) {
                getConnection().removeGateContacts(contact.getUserId());
            }
        }
        if (contact.isOnline() && !contact.isSingleUserContact()) {
            Presences.sendPresenceUnavailable(getConnection(), contact.getUserId());
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
        connection.updateContact((XmppContact) contact);
    }

    protected final void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((XmppContact) contact);
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

    protected String processUin(String uin) {
        String androidId = Settings.Secure.getString(SawimApplication.getContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        resource = Jid.getResource(uin, "Sawim" + " (" + androidId + ")");
        return Jid.getBareJid(uin);
    }

    public String getResource() {
        return resource;
    }

    protected void s_setPrivateStatus() {
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

    public void queryMessageArchiveManagement(Contact contact, OnMoreMessagesLoaded moreMessagesLoadedListener) {
        if (getConnection() != null) {
            getConnection().queryMessageArchiveManagement(contact, moreMessagesLoadedListener);
        }
    }

    protected void s_updateXStatus() {
        XStatus.setXStatus(connection);
    }

    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            Vcard.saveVCard(getConnection(), userInfo);
        }
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to instanceof XmppServiceContact) {
            return;
        }
        XmppContact c = (XmppContact) to;
        XmppContact.SubContact s = c.getCurrentSubContact();
        if (null != s) {
            Messages.sendTypingNotify(connection, to.getUserId() + "/" + s.resource, isTyping);
        }
    }

    public boolean isContactOverGate(String jid) {
        if (Jid.isGate(jid)) {
            return false;
        }
        for (Contact contact : getContactItems().values()) {
            XmppContact c = (XmppContact) contact;
            if (Jid.isGate(c.getUserId())) {
                if (jid.endsWith(c.getUserId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void leave(XmppServiceContact conf) {
        if (conf.isOnline()) {
            Presences.sendPresenceUnavailable(getConnection(), conf.getUserId() + "/" + conf.getMyName());
            conf.nickOffline(this, conf.getMyName(), 0, null, null);

            String conferenceJid = conf.getUserId() + '/';
            for (Contact contact : getContactItems().values()) {
                XmppContact c = (XmppContact) contact;
                if (c.getUserId().startsWith(conferenceJid) && getChat(c).getAllUnreadMessageCount() == 0) {
                    removeContact(c);
                }
            }
        }
    }

    public void join(XmppServiceContact c) {
        String jid = c.getUserId();
        if (!c.isOnline()) {
            setContactStatus(c, c.getMyName(), StatusInfo.STATUS_ONLINE, "", 0);
            c.doJoining();
        }
        if (connection == null) return;
        Presences.sendPresence(connection, c);
        String password = c.getPassword();
        if (Jid.isIrcConference(jid) && !StringConvertor.isEmpty(password)) {
            String nickserv = jid.substring(jid.indexOf('%') + 1) + "/NickServ";
            Messages.sendMessage(connection, nickserv, "/quote NickServ IDENTIFY " + password);
            Messages.sendMessage(connection, nickserv, "IDENTIFY " + password);
        }
    }

    protected void doAction(BaseActivity activity, Contact c, int cmd) {
        final XmppContact contact = (XmppContact) c;
        switch (cmd) {
            case ContactMenu.GATE_CONNECT:
                Presences.sendPresence(getConnection(), (XmppServiceContact) contact);
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.GATE_DISCONNECT:
                Presences.sendPresenceUnavailable(getConnection(), c.getUserId());
                RosterHelper.getInstance().updateRoster();
                break;

            case ContactMenu.GATE_REGISTER:
                getConnection().register(c.getUserId()).show(activity);
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
                    sd.showIt(activity);
                }
                break;
            case ContactMenu.COMMAND_TITLE:
                TextBoxDialogFragment textbox = new TextBoxDialogFragment();
                textbox.setString(c.getStatusText());
                textbox.setTextBoxListener(new TextBoxDialogFragment.TextBoxListener() {
                    @Override
                    public void textboxAction(final TextBoxDialogFragment box, boolean ok) {
                        SawimApplication.getExecutor().execute(new Runnable() {
                            @Override
                            public void run() {
                                sendMessage(contact, "/title " + box.getString(), true);
                            }
                        });

                    }
                });
                textbox.show(activity.getSupportFragmentManager(), "title_conf");
                break;

            case ContactMenu.CONFERENCE_CONNECT:
                join((XmppServiceContact) c);
                break;

            case ContactMenu.CONFERENCE_OPTIONS:
                showOptionsForm(activity, (XmppServiceContact) c);
                break;

            case ContactMenu.CONFERENCE_INFORMATION:
                XmppServiceContact xmppServiceContact = (XmppServiceContact) contact;
                UserInfo data = getConnection().getUserInfo(xmppServiceContact);
                XmppContact.SubContact my = xmppServiceContact.getContact(xmppServiceContact.getMyName());
                if (null != my) {
                    if (XmppServiceContact.AFFILIATION_OWNER == my.priorityA) {
                        data.setEditable();
                    }
                }
                data.uin = c.getUserId();
                data.createProfileView(contact.getName());
                data.setProfileViewToWait();
                data.showProfile(activity);
                break;

            case ContactMenu.CONFERENCE_OWNER_OPTIONS:
                connection.requestOwnerForm(c.getUserId()).show(activity);
                break;
            case ContactMenu.CONFERENCE_OWNERS:
                AffiliationListConf alc = getAffiliationListConf();
                alc.setServer(c.getUserId(), c.getMyName());
                Muc.requestAffiliationListConf(getConnection(), c.getUserId(), "owner");
                alc.showIt(activity);
                break;
            case ContactMenu.CONFERENCE_ADMINS:
                AffiliationListConf al = getAffiliationListConf();
                al.setServer(c.getUserId(), c.getMyName());
                Muc.requestAffiliationListConf(getConnection(), c.getUserId(), "admin");
                al.showIt(activity);
                break;
            case ContactMenu.CONFERENCE_MEMBERS:
                AffiliationListConf affiliationListConf = getAffiliationListConf();
                affiliationListConf.setServer(c.getUserId(), c.getMyName());
                Muc.requestAffiliationListConf(getConnection(), c.getUserId(), "member");
                affiliationListConf.showIt(activity);
                break;
            case ContactMenu.CONFERENCE_INBAN:
                AffiliationListConf aff = getAffiliationListConf();
                aff.setServer(c.getUserId(), c.getMyName());
                Muc.requestAffiliationListConf(getConnection(), c.getUserId(), "outcast");
                aff.showIt(activity);
                break;

            case ContactMenu.CONFERENCE_DISCONNECT:
                leave((XmppServiceContact) c);
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
                    showInviteForm(activity, c.getUserId() + '/' + ((XmppContact) c).getCurrentSubContact().resource);
                } catch (Exception ignored) {
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

    public void showListOfSubcontacts(BaseActivity activity, final XmppContact c) {
        final Vector items = new Vector();
        int selected = 0;
        int i = 0;
        for (XmppContact.SubContact contact : c.subcontacts.values()) {
            ++i;
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
        if (contact.isConference() && (-1 != realJid.indexOf('/'))) {
            XmppServiceContact conference = (XmppServiceContact) getItemByUID(Jid.getBareJid(realJid));
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
        if (contact instanceof XmppServiceContact) {
            if (contact.isOnline() && contact.isSingleUserContact()) {
                XmppServiceContact xmppServiceContact = (XmppServiceContact) contact;
                statusView.addContactRole(xmppServiceContact.getCurrentSubContact().roleText);
            }
        }
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

    public void showStatus(BaseActivity activity, Contact contact) {
        StatusView statusView = RosterHelper.getInstance().getStatusView();
        try {
            if (contact.isOnline() && contact.isSingleUserContact()) {
                String jid = contact.getUserId();
                if (!(contact instanceof XmppServiceContact)) {
                    jid += '/' + ((XmppContact) contact).getCurrentSubContact().resource;
                }
                if (contact instanceof XmppServiceContact) {
                    statusView.setUserRole(((XmppServiceContact) contact).getCurrentSubContact().roleText);
                }
                getConnection().requestClientVersion(jid);
            }
        } catch (Exception ignored) {
        }

        statusView.init(this, contact);
        updateStatusView(statusView, contact);
        statusView.showIt(activity);
    }

    public void saveAnnotations(String xml) {
        getConnection().requestRawXml(xml);
    }

    private Forms enterData = null;
    private XmppServiceContact enterConf = null;
    private static final int NICK = 0;
    private static final int PASSWORD = 1;
    private static final int AUTOJOIN = 2;
    private static final int IS_PRESENCES = 3;

    private Forms enterDataInvite = null;
    private static final int JID_MESS_TO = 7;
    private static final int JID_INVITE_TO = 8;
    private static final int REASON_INVITE = 9;

    public String onlineConference(ConcurrentHashMap<String, Contact> contacts) {
        String list[] = new String[contacts.size()];
        String items = "";
        for (int i = 1; i < contacts.values().size(); ++i) {
            XmppContact c = (XmppContact) contacts.values().iterator().next();
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

    void showOptionsForm(BaseActivity activity, XmppServiceContact c) {
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

    public void formAction(BaseActivity activity, Forms form, boolean apply) {
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
                            Muc.saveConferences(getConnection());
                        }
                        if (enterConf.isOnline() && !oldNick.equals(enterConf.getMyName())) {
                            join(enterConf);
                        }
                    }
                    getStorage().save(this, enterConf, getGroup(enterConf));
                }
            }
            enterData.back();
            enterData = null;
            enterConf = null;
        }
        if (enterDataInvite == form) {
            if (apply) {
                String[] onlineConferenceI = Util.explode(onlineConference(getContactItems()), '|');
                Muc.sendInvite(getConnection(), onlineConferenceI[enterDataInvite.getSelectorValue(JID_MESS_TO)], enterDataInvite.getTextFieldValue(JID_INVITE_TO), enterDataInvite.getTextFieldValue(REASON_INVITE));
                Toast.makeText(SawimApplication.getContext(), R.string.invitation_sent, Toast.LENGTH_LONG).show();
            }
            enterDataInvite.back();
            enterDataInvite = null;
        }
    }
}

