

package protocol.jabber;

import DrawControls.icons.ImageList;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import ru.sawim.view.TextBoxView;
import sawim.FileTransfer;
import sawim.chat.message.PlainMessage;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.search.Search;
import sawim.search.UserInfo;
import sawim.ui.TextBoxListener;
import sawim.util.JLocale;
import protocol.*;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.activities.SawimActivity;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;

import java.util.Vector;


public final class Jabber extends Protocol implements FormListener {

    private JabberXml connection;
    private Vector rejoinList = new Vector();
    private String resource;
    private ServiceDiscovery disco = null;
	private AffiliationListConf alistc = null;
	private MirandaNotes notes = null;
    
    public static final JabberXStatus xStatus = new JabberXStatus();
    
    private final Vector bots = new Vector();

    public Jabber() {
    }

    protected void initStatusInfo() {
        bots.addElement("juick@juick.com");
        bots.addElement("psto@psto.net");

        byte type = getProfile().protocolType;
        ImageList icons = createStatusIcons(type);
        final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, 6, -1, 5, -1, -1, 1};
        info = new StatusInfo(icons, statusIconIndex, statuses);
        xstatusInfo = Jabber.xStatus.getInfo();
        clientInfo = JabberClient.get();
    }

    private static final byte[] statuses = {
            StatusInfo.STATUS_OFFLINE,
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_XA,
            StatusInfo.STATUS_DND
    };

    private ImageList createStatusIcons(byte type) {
        
        String file = "jabber";
        switch (type) {
            case Profile.PROTOCOL_GTALK:
                file = "gtalk";
                break;
            case Profile.PROTOCOL_FACEBOOK:
                file = "facebook";
                break;
            case Profile.PROTOCOL_LJ:
                file = "livejournal";
                break;
            case Profile.PROTOCOL_YANDEX:
                file = "ya";
                break;
            case Profile.PROTOCOL_VK:
                file = "vk";
                break;
            case Profile.PROTOCOL_QIP:
                file = "qip";
                break;
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                file = "o" + "k";
                break;
        }
        ImageList icons = ImageList.createImageList("/" + file + "-status.png");
        if (0 < icons.size()) {
            return icons;
        }
        
        return ImageList.createImageList("/jabber-status.png");
    }

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
            JabberServiceContact conf = (JabberServiceContact) getItemByUIN(jid);
            if ((null != conf) && !conf.isOnline()) {
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

    public final boolean isBlogBot(String jid) {
        return -1 < bots.indexOf(jid);
    }

    protected void startConnection() {
        connection = new JabberXml();
        connection.setJabber(this);
        connection.start();
    }

    public JabberXml getConnection() {
        return connection;
    }

    public boolean hasS2S() {
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return false;
        }
        return true;
    }

    public boolean hasVCardEditor() {
        
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_VK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return false;
        }
        
        return true;
    }

    protected final void userCloseConnection() {
        rejoinList.removeAllElements();
    }

    protected final void closeConnection() {
        Log.e("Jabber", "closeConnection()");
        JabberXml c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    private int getNextGroupId() {
        while (true) {
            int id = Util.nextRandInt() % 0x1000;
            for (int i = groups.size() - 1; i >= 0; --i) {
                Group group = (Group) groups.elementAt(i);
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
    public static final String GENERAL_GROUP = SawimApplication.getContext().getString(R.string.group_general);
    public static final String GATE_GROUP = SawimApplication.getContext().getString(R.string.group_transports);
    public static final String CONFERENCE_GROUP = SawimApplication.getContext().getString(R.string.group_conferences);

    public final Group createGroup(String name) {
        Group group = new Group(name);
        group.setGroupId(getNextGroupId());
        int mode = Group.MODE_FULL_ACCESS;
        if (JLocale.getString(Jabber.CONFERENCE_GROUP).equals(name)) {
            mode &= ~Group.MODE_EDITABLE;
            mode |= Group.MODE_TOP;
        } else if (JLocale.getString(Jabber.GATE_GROUP).equals(name)) {
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
            JabberServiceContact c = new JabberServiceContact(jid, name);
            if (c.isConference()) {
                c.setGroup(getOrCreateGroup(c.getDefaultGroupName()));
                c.setMyName(getDefaultName());

            } else if (isConference ) {
                JabberServiceContact conf = (JabberServiceContact) getItemByUIN(Jid.getBareJid(jid));
                if (null != conf) {
                    c.setPrivateContactStatus(conf);
                    c.setMyName(conf.getMyName());
                }
            }
            return c;
        }

        return new JabberContact(jid, name);
    }

    private String getDefaultName() {
        
        String nick = getProfile().nick;
        if (StringConvertor.isEmpty(nick)) {
            return Jid.getNick(getUserId());
        }
        return nick;
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
    public final static int PRIORITY = 50;

    protected void s_updateOnlineStatus() {
        connection.setStatus(getProfile().statusIndex, "", PRIORITY);
        for (int i = 0; i < rejoinList.size(); ++i) {
            String jid = (String) rejoinList.elementAt(i);
            JabberServiceContact c = (JabberServiceContact) getItemByUIN(jid);
            if (null != c) {
                connection.sendPresence(c);
            }
        }
    }

    void setConfContactStatus(JabberServiceContact conf, String resource, byte status, String statusText, int role, int priorityA) {
        conf.__setStatus(resource, role, priorityA, status, statusText);
    }

    void setContactStatus(JabberContact c, String resource, byte status, String text, int priority) {
        c.__setStatus(resource, priority, 0, status, text);
    }

    protected final void s_addedContact(Contact contact) {
        connection.updateContact((JabberContact) contact);
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
        connection.updateContacts(contacts);
    }

    protected final void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        connection.updateContact((JabberContact) contact);
    }

    protected final void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        connection.updateContact((JabberContact) contact);
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
    String getDefaultServer(String domain) {
        
        switch (getProfile().protocolType) {
            case Profile.PROTOCOL_GTALK:
                return "talk.google.com";
            case Profile.PROTOCOL_FACEBOOK:
                return "chat.facebook.com";
            case Profile.PROTOCOL_LJ:
                return "livejournal.com";
            case Profile.PROTOCOL_YANDEX:
                return getYandexDomain(domain);
            case Profile.PROTOCOL_VK:
                return "vkmessenger.com";
            case Profile.PROTOCOL_QIP:
                return "webim.qip.ru";
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return "xmpp.odnoklassniki.ru";
        }
        
        if ("jabber.ru".equals(domain)) {
            return domain;
        }
        if ("xmpp.ru".equals(domain)) {
            return domain;
        }
        if ("ya.ru".equals(domain)) {
            return "xmpp.yandex.ru";
        }
        if ("gmail.com".equals(domain)) {
            return "talk.google.com";
        }
        if ("qip.ru".equals(domain)) {
            return "webim.qip.ru";
        }
        if ("livejournal.com".equals(domain)) {
            return "livejournal.com";
        }
        if ("api.com".equals(domain)) {
            return "vkmessenger.com";
        }
        if ("vkontakte.ru".equals(domain)) {
            return "vkmessenger.com";
        }
        if ("chat.facebook.com".equals(domain)) {
            return domain;
        }
        return null;
    }

    protected String processUin(String uin) {
        resource = Jid.getResource(uin, "Sawim");
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

    protected void s_updateXStatus() {
        connection.setXStatus();
    }

    public void saveUserInfo(UserInfo userInfo) {
        if (isConnected()) {
            getConnection().saveVCard(userInfo);
        }
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to instanceof JabberServiceContact) {
            return;
        }
        if (Profile.PROTOCOL_LJ == getProfile().protocolType) {
            return;
        }
        JabberContact c = (JabberContact) to;
        JabberContact.SubContact s = c.getCurrentSubContact();
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
            JabberContact c = (JabberContact) all.elementAt(i);
            if (Jid.isGate(c.getUserId())) {
                if (jid.endsWith(c.getUserId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public void leave(JabberServiceContact conf) {
        if (conf.isOnline()) {
            getConnection().sendPresenceUnavailable(conf.getUserId() + "/" + conf.getMyName());
            conf.nickOffline(this, conf.getMyName(), 0, null, null);

            Vector all = getContactItems();
            String conferenceJid = conf.getUserId() + '/';
            for (int i = all.size() - 1; 0 <= i; --i) {
                JabberContact c = (JabberContact) all.elementAt(i);
                if (c.getUserId().startsWith(conferenceJid) && !c.hasUnreadMessage()) {
                    removeContact(c);
                }
            }
        }
    }

    public void join(JabberServiceContact c) {
        String jid = c.getUserId();
        if (!c.isOnline()) {
            setContactStatus(c, c.getMyName(), StatusInfo.STATUS_ONLINE, "", 0);
            c.doJoining();
        }

        connection.sendPresence(c);
        String password = c.getPassword();
        if (Jid.isIrcConference(jid) && !StringConvertor.isEmpty(password)) {
            String nickserv = jid.substring(jid.indexOf('%') + 1) + "/NickServ";
            connection.sendMessage(nickserv, "/quote NickServ IDENTIFY " + password);
            connection.sendMessage(nickserv, "IDENTIFY " + password);
        }
    }
	public static final int CONFERENCE_AFFILIATION_LIST = 11;
	public static final int CONFERENCE_OWNERS = 25;
	public static final int CONFERENCE_ADMINS = 21;
	public static final int CONFERENCE_MEMBERS = 22;
	public static final int CONFERENCE_INBAN = 23;

    protected void doAction(Contact c, int cmd) {
        final JabberContact contact = (JabberContact) c;
        switch (cmd) {
            case JabberServiceContact.GATE_CONNECT:
                getConnection().sendPresence((JabberServiceContact) contact);
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.GATE_DISCONNECT:
                getConnection().sendPresenceUnavailable(c.getUserId());
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.GATE_REGISTER:
                getConnection().register(c.getUserId());
                break;

            case JabberServiceContact.GATE_UNREGISTER:
                getConnection().unregister(c.getUserId());
                getConnection().removeGateContacts(c.getUserId());
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.GATE_ADD:
                Search s = this.getSearchForm();
                s.setJabberGate(c.getUserId());
                s.show("");
                break;

            case JabberServiceContact.USER_MENU_USERS_LIST:
                if (contact.isOnline() || !isConnected()) {
                    //new ConferenceParticipants(this, (JabberServiceContact) c).show();
                } else {
                    ServiceDiscovery sd = getServiceDiscovery();
                    sd.setServer(contact.getUserId());
                    sd.showIt();
                }
                break;
			case JabberServiceContact.COMMAND_TITLE:
                TextBoxView textbox = new TextBoxView();
                textbox.setString("/title " + c.getStatusText());
                textbox.setTextBoxListener(new TextBoxListener() {
                    @Override
                    public void textboxAction(TextBoxView box, boolean ok) {
                        sendMessage(contact, box.getString(), true);
                    }
                });
                textbox.show(SawimActivity.getInstance().getSupportFragmentManager(), "title_conf");
                break;

            case JabberServiceContact.CONFERENCE_CONNECT:
                join((JabberServiceContact) c);
                break;

            case JabberServiceContact.CONFERENCE_OPTIONS:
                showOptionsForm((JabberServiceContact) c);
                break;

            case JabberServiceContact.CONFERENCE_OWNER_OPTIONS:
                connection.requestOwnerForm(c.getUserId());
                break;
			case CONFERENCE_OWNERS:
			    AffiliationListConf alc = getAffiliationListConf();
                alc.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "ow" + "ner");
                alc.showIt();
				break;
			case CONFERENCE_ADMINS:
			    AffiliationListConf al = getAffiliationListConf();
                al.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "ad" + "min");
                al.showIt();
				break;
			case CONFERENCE_MEMBERS:
			    AffiliationListConf a = getAffiliationListConf();
                a.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "mem" + "ber");
                a.showIt();
				break;
			case CONFERENCE_INBAN:
			    AffiliationListConf aff = getAffiliationListConf();
                aff.setServer(c.getUserId(), c.getMyName());
                getConnection().requestAffiliationListConf(c.getUserId(), "ou" + "tcast");
                aff.showIt();
				break;

            case JabberServiceContact.CONFERENCE_DISCONNECT:
                leave((JabberServiceContact) c);
                ContactList.getInstance().activate();
                break;

            case JabberServiceContact.CONFERENCE_ADD:
                addContact(c);
                ContactList.getInstance().activate();
                break;

            case JabberContact.USER_MENU_CONNECTIONS:
                showListOfSubcontacts(contact);
                break;
			case JabberContact.USER_INVITE:
			    try {
	                showInviteForm(c.getUserId() + '/' + ((JabberContact)c).getCurrentSubContact().resource);
		        } catch (Exception e) {
		            
		        }
                break;

			case JabberContact.USER_MENU_SEEN:
                getConnection().showContactSeen(c.getUserId());
				ContactList.getInstance().activate();
                break;

            case JabberContact.USER_MENU_ADHOC:
                AdHoc adhoc = new AdHoc(this, contact);
                adhoc.show();
                break;

            case JabberContact.USER_MENU_REMOVE_ME:
                removeMe(c.getUserId());
                ContactList.getInstance().activate();
                break;

        }
    }

    public void showListOfSubcontacts(final JabberContact c) {
        final Vector items = new Vector();
        int selected = 0;
        for (int i = 0; i < c.subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact) c.subcontacts.elementAt(i);
            items.add(contact.resource);
            if (contact.resource.equals(c.currentResource)) {
                selected = i;
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(SawimActivity.getInstance());
        builder.setTitle(c.getName());
        builder.setSingleChoiceItems(Util.vectorToArray(items), selected, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                c.setActiveResource((String) items.get(which));
                ContactList.getInstance().activate();
            }
        });
        builder.create().show();
    }

    public void showUserInfo(Contact contact) {
        if (!contact.isSingleUserContact()) {
            doAction(contact, JabberContact.USER_MENU_USERS_LIST);
            return;
        }

        String realJid = contact.getUserId();
        if (Jid.isConference(realJid) && (-1 != realJid.indexOf('/'))) {
            JabberServiceContact conference = (JabberServiceContact) getItemByUIN(Jid.getBareJid(realJid));
            String r = conference.getRealJid(Jid.getResource(realJid, ""));
            if (!StringConvertor.isEmpty(r)) {
                realJid = r;
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
        data.showProfile();
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
        statusView.update();
    }

    public void showStatus(Contact contact) {
        StatusView statusView = ContactList.getInstance().getStatusView();
        try {
            if (contact.isOnline() && contact.isSingleUserContact()) {
                String jid = contact.getUserId();
                if (!(contact instanceof JabberServiceContact)) {
                    jid += '/' + ((JabberContact) contact).getCurrentSubContact().resource;
                }
                getConnection().requestClientVersion(jid);
            }
        } catch (Exception ignored) {
        }

        statusView.init(this, contact);
        updateStatusView(statusView, contact);
        statusView.showIt();
    }

    public String getUniqueUserId(Contact c) {
        String jid = c.getUserId();
        if (isContactOverGate(jid)) {
            return Jid.getNick(jid).replace('%', '@');
        }
        return jid.replace('%', '@');
    }
	public void saveAnnotations(String xml) {
	    getConnection().requestRawXml(xml);
	}
    
    private static Forms enterData = null;
    private static JabberServiceContact enterConf = null;
    private static final int NICK = 0;
    private static final int PASSWORD = 1;
    private static final int AUTOJOIN = 2;
    private static final int OLD_GATE = 3;


	private static Forms enterDataInvite = null;
	private static final int JID_MESS_TO = 7;
	private static final int JID_INVITE_TO = 8;
	private static final int REASON_INVITE = 9;
    public String onlineConference(Vector v) {
		String list[] = new String[v.size()];
		String items = "";
        for (int i = 1; i < v.size(); ++i) {
			JabberContact c = (JabberContact)v.elementAt(i);
		    if (c.isConference() && c.isOnline()) {
			    list[i] = c.getUserId();
		        items += "|" + list[i];
			}
        }
		return items.substring(1);
	}

	final void showInviteForm(String jid) {
	    enterDataInvite = Forms.getInstance();
        enterDataInvite.init("invite", this);
	    enterDataInvite.addSelector(JID_MESS_TO, "conference", onlineConference(getContactItems()), 1); 
		enterDataInvite.addTextField(JID_INVITE_TO, "jid", jid);
        enterDataInvite.addTextField(REASON_INVITE, "reason", "");
        enterDataInvite.show();
	}
    void showOptionsForm(JabberServiceContact c) {
        enterConf = c;
        enterData = Forms.getInstance();
        enterData.init("conference", this);
        enterData.addTextField(NICK, "nick", c.getMyName());
        enterData.addTextField(PASSWORD, "password", c.getPassword());
        if (!c.isTemp()) {
            enterData.addCheckBox(AUTOJOIN, "autojoin", c.isAutoJoin());
        }
        enterData.show();
        if (!Jid.isIrcConference(c.getUserId())) {
            getConnection().requestConferenceInfo(c.getUserId());
        }
    }

    void setConferenceInfo(String jid, String description) {
        if ((null != enterData) && enterConf.getUserId().equals(jid)) {
            enterData.addString("description", description);
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

                    boolean needUpdate = !enterConf.isTemp();
                    if (needUpdate) {
                        getConnection().saveConferences();
                    }
                    if (enterConf.isOnline() && !oldNick.equals(enterConf.getMyName())) {
                        join(enterConf);
                    }
                }
                ContactList.getInstance().activate();

            } else {
                enterData.back();
            }
            enterData = null;
            enterConf = null;
        }
		if (enterDataInvite == form) {
            if (apply) {
				String[] onlineConferenceI = Util.explode(onlineConference(getContactItems()), '|');
				getConnection().sendInvite(onlineConferenceI[enterDataInvite.getSelectorValue(JID_MESS_TO)], enterDataInvite.getTextFieldValue(JID_INVITE_TO), enterDataInvite.getTextFieldValue(REASON_INVITE));
				ContactList.getInstance().activate();
			} else {
                enterDataInvite.back();
            }
			enterDataInvite = null;
        }
    }
}