package protocol.jabber;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.SubMenu;
import sawim.Options;
import sawim.comm.Config;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.util.JLocale;
import protocol.ClientInfo;
import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.R;
import java.util.Vector;


public class JabberContact extends Contact {
    
    public JabberContact(String jid, String name) {
        this.userId = jid;
        this.setName((null == name) ? jid : name);
        setOfflineStatus();
    }

    protected String currentResource;

    public boolean isConference() {
        return false;
    }

    public String getDefaultGroupName() {
        return JLocale.getString(Jabber.GENERAL_GROUP);
    }

    public static final int USER_MENU_CONNECTIONS = 10;
    public static final int USER_MENU_REMOVE_ME   = 11;
    public static final int USER_MENU_ADHOC       = 12;
	public static final int USER_MENU_SEEN   = 14;
	public static final int USER_INVITE = 29;
    public void addChatMenuItems(ContextMenu model) {
        if (isOnline() && !(this instanceof JabberServiceContact)) {
            if (Options.getBoolean(Options.OPTION_ALARM)) {
                model.add(Menu.FIRST, USER_MENU_WAKE, 2, R.string.wake);
            }
        }
    }
    protected void initContextMenu(Protocol protocol, ContextMenu contactMenu) {
        addChatItems(contactMenu);

		if (!isOnline() && isAuth() && !isTemp()) {
            contactMenu.add(Menu.FIRST, USER_MENU_SEEN, 2, R.string.contact_seen);
		}
		if (isOnline() && isAuth()) {
            contactMenu.add(Menu.FIRST, USER_INVITE, 2, R.string.invite);
		}
        contactMenu.add(Menu.FIRST, USER_MENU_ANNOTATION, 2, R.string.notes);
        if (0 < subcontacts.size()) {
            contactMenu.add(Menu.FIRST, USER_MENU_CONNECTIONS, 2, R.string.list_of_connections);
        }
        addGeneralItems(protocol, contactMenu);
    }
    protected void initManageContactMenu(Protocol protocol, SubMenu menu) {
        if (protocol.isConnected()) {
            if (isOnline()) {
                menu.add(Menu.FIRST, USER_MENU_ADHOC, 2, R.string.adhoc);
            }
            if (isTemp()) {
                menu.add(Menu.FIRST, USER_MENU_ADD_USER, 2, R.string.add_user);

            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.add(Menu.FIRST, USER_MENU_MOVE, 2, R.string.move_to_group);
                }
                if (!isAuth()) {
                    menu.add(Menu.FIRST, USER_MENU_REQU_AUTH, 2, R.string.requauth);
                }
            }
            if (!isTemp()) {
                menu.add(Menu.FIRST, USER_MENU_RENAME, 2, R.string.rename);
            }
        }
        if (protocol.isConnected() || (isTemp() && protocol.inContactList(this))) {
            if (protocol.isConnected()) {
                menu.add(Menu.FIRST, USER_MENU_REMOVE_ME, 2, R.string.remove_me);
            }
            if (protocol.inContactList(this)) {
                menu.add(Menu.FIRST, USER_MENU_USER_REMOVE, 2, R.string.remove);
            }
        }
    }
    
    String getReciverJid() {
        if (this instanceof JabberServiceContact) {
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
        final String on = "o" + "n";
        final String off = "o" + "f" + "f";
        if (on.equals(param) || off.equals(param)) {
            xml = Config.getConfigValue(cmd + ' ' + param, "/jabber-commands.txt");
        }
        if (null == xml) {
            xml = Config.getConfigValue(cmd, "/jabber-commands.txt");
        }
        if (null == xml) {
            return false;
        }

        JabberXml jabberXml = ((Jabber)protocol).getConnection();

        String jid = Jid.SawimJidToRealJid(getUserId());
        String fullJid = jid;
        if (isConference()) {
            String nick = ((JabberServiceContact)this).getMyName();
            fullJid = Jid.SawimJidToRealJid(getUserId() + '/' + nick);
        }

    	xml = Util.replace(xml, "${sawim.caps}", jabberXml.getCaps());
        xml = Util.replace(xml, "${c.jid}", Util.xmlEscape(jid));
        xml = Util.replace(xml, "${c.fulljid}", Util.xmlEscape(fullJid));
    	xml = Util.replace(xml, "${param.full}", Util.xmlEscape(param));
        xml = Util.replace(xml, "${param.res}", Util.xmlEscape(resource));
        xml = Util.replace(xml, "${param.msg}", Util.xmlEscape(newMessage));
        xml = Util.replace(xml, "${param.res.realjid}",
    		Util.xmlEscape(getSubContactRealJid(resource)));
        xml = Util.replace(xml, "${param.full.realjid}",
    		Util.xmlEscape(getSubContactRealJid(param)));

        jabberXml.requestRawXml(xml);
        return true;
    }
    private String getSubContactRealJid(String resource) {
        SubContact c = getExistSubContact(resource);
        return StringConvertor.notNull((null == c) ? null : c.realJid);
    }

    public static class SubContact {
        public String resource;
        public String statusText;
        public String realJid;
        
        public short client = ClientInfo.CLI_NONE;
        
        public byte status;
        public byte priority;
		public byte priorityA;
    }
    public Vector subcontacts = new Vector();
    private void removeSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = (SubContact)subcontacts.elementAt(i);
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
            SubContact c = (SubContact)subcontacts.elementAt(i);
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
    SubContact getCurrentSubContact() {
        if ((0 == subcontacts.size()) || isConference()) {
            return null;
        }
        SubContact currentContact = getExistSubContact(currentResource);
        if (null != currentContact) {
            return currentContact;
        }
        try {
            currentContact = (SubContact)subcontacts.elementAt(0);
            byte maxPriority = currentContact.priority;
            for (int i = 1; i < subcontacts.size(); ++i) {
                SubContact contact = (SubContact)subcontacts.elementAt(i);
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
	        return (byte)subcontacts.size();
		}
		return (byte)0;
	}
    public void __setStatus(String resource, int priority, int priorityA, byte index, String statusText) {
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
            c.priority = (byte)priority;
			c.priorityA = (byte)priorityA;
            c.status = index;
            c.statusText = statusText;
        }
    }
    void updateMainStatus(Jabber jabber) {
        if (isSingleUserContact()) {
            SubContact c = getCurrentSubContact();
            if (null == c) {
                setOfflineStatus();

            } else if (this instanceof JabberServiceContact) {
                setStatus(c.status, c.statusText);

            } else {
                jabber.setContactStatus(this, c.status, c.statusText);
            }
        }
    }

    public void setClient(String resource, String caps) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.client = JabberClient.createClient(caps);
        }
        SubContact cur = getCurrentSubContact();
        setClient((null == cur) ? ClientInfo.CLI_NONE : cur.client, null);
    }

    public void setXStatus(String id, String text) {
        setXStatus(Jabber.xStatus.createXStatus(id), text);
    }

    public final void setOfflineStatus() {
        subcontacts.removeAllElements();
        super.setOfflineStatus();
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
}