package protocol.xmpp;

import android.view.ContextMenu;
import android.view.Menu;
import protocol.*;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.comm.*;
import ru.sawim.io.RosterStorage;
import ru.sawim.view.menu.MyMenu;

import java.util.ArrayList;
import java.util.List;


public class XmppContact extends Contact {

    public XmppContact(String jid, String name) {
        this.userId = jid;
        this.setName((null == name) ? jid : name);
        setOfflineStatus();
    }

    protected String currentResource;

    public boolean isConference() {
        return false;
    }

    public String getDefaultGroupName() {
        return JLocale.getString(Xmpp.GENERAL_GROUP);
    }

    public void addChatMenuItems(ContextMenu model) {
        if (isOnline()) {
            if (Options.getBoolean(JLocale.getString(R.string.pref_alarm)) && isSingleUserContact()) {
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
        if (this instanceof XmppServiceContact) {
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

        XmppConnection xmppConnection = ((Xmpp) protocol).getConnection();

        String jid = Jid.SawimJidToRealJid(getUserId());
        String fullJid = jid;
        if (isConference()) {
            fullJid = Jid.SawimJidToRealJid(getUserId() + '/' + getMyName());
        }

        xml = xml.replace("${sawim.caps}", xmppConnection.getCaps());
        xml = xml.replace("${c.jid}", Util.xmlEscape(jid));
        xml = xml.replace("${c.fulljid}", Util.xmlEscape(fullJid));
        xml = xml.replace("${param.full}", Util.xmlEscape(param));
        xml = xml.replace("${param.res}", Util.xmlEscape(resource));
        xml = xml.replace("${param.msg}", Util.xmlEscape(newMessage));
        xml = xml.replace("${param.res.realjid}",
                Util.xmlEscape(getSubContactRealJid(resource)));
        xml = xml.replace("${param.full.realjid}",
                Util.xmlEscape(getSubContactRealJid(param)));

        xmppConnection.requestRawXml(xml);
        return true;
    }

    private String getSubContactRealJid(String resource) {
        SubContact c = getExistSubContact(resource);
        return StringConvertor.notNull((null == c) ? null : c.realJid);
    }

    public static class SubContact implements Sortable {
        public String avatarHash;
        public String resource;
        public String statusText;
        public String roleText;
        public String realJid;

        public short client = ClientInfo.CLI_NONE;

        public byte status;
        public byte priority;
        public byte priorityA;

        @Override
        public String getText() {
            return resource;
        }

        @Override
        public int getNodeWeight() {
            return 0;
        }
    }

    public final List<SubContact> subcontacts = new ArrayList<>();

    private void removeSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = subcontacts.get(i);
            if (c.resource.equals(resource)) {
                c.status = StatusInfo.STATUS_OFFLINE;
                c.statusText = null;
                subcontacts.remove(i);
                return;
            }
        }
    }

    public SubContact getExistSubContact(String resource) {
        for (int i = subcontacts.size() - 1; i >= 0; --i) {
            SubContact c = subcontacts.get(i);
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
        c.avatarHash = RosterStorage.getAvatarHash(getUserId() + "/" + resource);
        subcontacts.add(c);
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
            currentContact = subcontacts.get(0);
            byte maxPriority = currentContact.priority;
            for (int i = 1; i < subcontacts.size(); ++i) {
                SubContact contact = subcontacts.get(i);
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

    void updateMainStatus(Xmpp xmpp) {
        if (isSingleUserContact()) {
            SubContact c = getCurrentSubContact();
            if (null == c) {
                setOfflineStatus();

            } else if (this instanceof XmppServiceContact) {
                setStatus(c.status, c.statusText);

            } else {
                xmpp.setContactStatus(this, c.status, c.statusText);
            }
        }
    }

    public void setClient(String resource, String caps) {
        SubContact c = getExistSubContact(resource);
        if (null != c) {
            c.client = XmppClient.createClient(caps);
        }
        SubContact cur = getCurrentSubContact();
        setClient((null == cur) ? ClientInfo.CLI_NONE : cur.client, null);
    }

    public void setXStatus(String id, String text) {
        setXStatus(Xmpp.xStatus.createXStatus(id), text);
    }

    public final void setOfflineStatus() {
        subcontacts.clear();
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
}
