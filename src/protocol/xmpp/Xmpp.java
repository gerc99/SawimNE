package protocol.xmpp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;
import protocol.*;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.ImageList;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.modules.FileTransfer;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.TextBoxView;
import ru.sawim.view.menu.JuickMenu;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Vector;

public final class Xmpp extends Protocol implements FormListener {

    public final static int PRIORITY = 50;
    private XmppConnection connection;
    private Vector rejoinList = new Vector();
    private String resource;
    private ServiceDiscovery disco = null;
    private AffiliationListConf alistc = null;
    private MirandaNotes notes = null;
    public static final XmppXStatus xStatus = new XmppXStatus();
    private final ArrayList<String> bots = new ArrayList<String>();

    public Xmpp() {
    }

    protected void initStatusInfo() {
        bots.add(JuickMenu.JUICK);
        bots.add(JuickMenu.PSTO);
        bots.add(JuickMenu.POINT);

        ImageList icons = ImageList.createImageList("/jabber-status.png");
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
            XmppServiceContact conf = (XmppServiceContact) getItemByUIN(jid);
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

    public void disconnect(boolean user) {
        if (user && null != connection) {
            SawimApplication.getInstance().getXmppSession().clear(connection);
            connection.loggedOut();
        }
        super.disconnect(user);
    }

    protected final void userCloseConnection() {
        //rejoinList.removeAllElements();
    }

    protected final void closeConnection() {
        XmppConnection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    protected void setStatusesOffline() {
        if (connection != null && !isStreamManagementSupported()) {
            super.setStatusesOffline();
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
        if (JLocale.getString(Xmpp.CONFERENCE_GROUP).equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_TOP;
        } else if (JLocale.getString(Xmpp.GATE_GROUP).equals(name)) {
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
            XmppServiceContact c = new XmppServiceContact(jid, name);
            if (c.isConference()) {
                c.setGroup(getOrCreateGroup(c.getDefaultGroupName()));
                c.setMyName(getDefaultName());

            } else if (isConference) {
                XmppServiceContact conf = (XmppServiceContact) getItemByUIN(Jid.getBareJid(jid));
                if (null != conf) {
                    c.setPrivateContactStatus(conf);
                    c.setMyName(conf.getMyName());
                }
            }
            return c;
        }
        return new XmppContact(jid, name);
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
                XmppServiceContact c = (XmppServiceContact) getItemByUIN(jid);
                if (null != c && !c.isOnline()) {
                    connection.sendPresence(c);
                }
            }
        }
    }

    @Override
    protected Contact loadContact(DataInputStream dis) throws Exception {
        XmppContact contact = (XmppContact) super.loadContact(dis);
        if (contact instanceof XmppServiceContact) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
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
                XmppContact.SubContact subContact = new XmppContact.SubContact();
                subContact.status = dis.readByte();
                subContact.client = dis.readByte();
                subContact.priority = dis.readByte();
                subContact.priorityA = dis.readByte();
                subContact.resource = dis.readUTF();
                contact.subcontacts.add(subContact);
            }
        }
        return contact;
    }

    @Override
    protected void saveContact(DataOutputStream dos, Contact contact) throws Exception {
        super.saveContact(dos, contact);
        if (contact instanceof XmppServiceContact) {
            XmppServiceContact serviceContact = (XmppServiceContact) contact;
            if (serviceContact.isConference()) {
                dos.writeUTF(serviceContact.getMyName());
                dos.writeBoolean(serviceContact.isAutoJoin());
                dos.writeBoolean(serviceContact.isPresence());
            }
        }
        if (isStreamManagementSupported()) {
            XmppContact xmppContact = (XmppContact) contact;
            dos.writeInt(xmppContact.subcontacts.size());
            for (XmppContact.SubContact subContact : xmppContact.subcontacts) {
                dos.writeByte(subContact.status);
                dos.writeByte(subContact.client);
                dos.writeByte(subContact.priority);
                dos.writeByte(subContact.priorityA);
                dos.writeUTF(subContact.resource);
            }
        }
    }

    void setConfContactStatus(XmppServiceContact conf, String resource, byte status, String statusText, int role, int priorityA, String roleText) {
        conf.__setStatus(resource, role, priorityA, status, statusText, roleText);
    }

    void setContactStatus(XmppContact c, String resource, byte status, String text, int priority) {
        c.__setStatus(resource, priority, 0, status, text, null);
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
        if (to instanceof XmppServiceContact) {
            return;
        }
        XmppContact c = (XmppContact) to;
        XmppContact.SubContact s = c.getCurrentSubContact();
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
            XmppContact c = (XmppContact) all.elementAt(i);
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
            getConnection().sendPresenceUnavailable(conf.getUserId() + "/" + conf.getMyName());
            conf.nickOffline(this, conf.getMyName(), 0, null, null);

            Vector all = getContactItems();
            String conferenceJid = conf.getUserId() + '/';
            for (int i = all.size() - 1; 0 <= i; --i) {
                XmppContact c = (XmppContact) all.elementAt(i);
                if (c.getUserId().startsWith(conferenceJid) && !c.hasUnreadMessage()) {
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
        connection.sendPresence(c);
        String password = c.getPassword();
        if (Jid.isIrcConference(jid) && !StringConvertor.isEmpty(password)) {
            String nickserv = jid.substring(jid.indexOf('%') + 1) + "/NickServ";
            connection.sendMessage(nickserv, "/quote NickServ IDENTIFY " + password);
            connection.sendMessage(nickserv, "IDENTIFY " + password);
        }
    }

    protected void doAction(BaseActivity activity, Contact c, int cmd) {
        final XmppContact contact = (XmppContact) c;
        switch (cmd) {
            case ContactMenu.GATE_CONNECT:
                getConnection().sendPresence((XmppServiceContact) contact);
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
                join((XmppServiceContact) c);
                break;

            case ContactMenu.CONFERENCE_OPTIONS:
                showOptionsForm(activity, (XmppServiceContact) c);
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

    public void showListOfSubcontacts(BaseActivity activity, final XmppContact c) {
        final Vector items = new Vector();
        int selected = 0;
        for (int i = 0; i < c.subcontacts.size(); ++i) {
            XmppContact.SubContact contact = c.subcontacts.elementAt(i);
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
            XmppServiceContact conference = (XmppServiceContact) getItemByUIN(Jid.getBareJid(realJid));
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
        statusView.showIt();
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

    public String onlineConference(Vector v) {
        String list[] = new String[v.size()];
        String items = "";
        for (int i = 1; i < v.size(); ++i) {
            XmppContact c = (XmppContact) v.elementAt(i);
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
        return SawimApplication.getInstance().getXmppSession()
                .isStreamManagementSupported(getUserId() + '/' + getResource());
    }
}

