package ru.sawim.roster;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Toast;
import ru.sawim.listener.OnUpdateChat;
import ru.sawim.listener.OnUpdateRoster;
import protocol.*;
import protocol.icq.Icq;
import protocol.mrim.Mrim;
import protocol.xmpp.Xmpp;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.forms.SmsForm;
import ru.sawim.modules.AutoAbsence;
import ru.sawim.modules.FileTransfer;
import ru.sawim.view.StatusesView;
import ru.sawim.view.XStatusesView;
import ru.sawim.view.menu.MyMenu;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

public final class RosterHelper {

    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME = 2;

    public static final int ALL_CONTACTS = 0;
    public static final int ONLINE_CONTACTS = 1;
    public static final int ACTIVE_CONTACTS = 2;

    private static final RosterHelper instance = new RosterHelper();
    private final StatusView statusView = new StatusView();

    private Contact currentContact;
    private Vector transfers = new Vector();
    private OnUpdateRoster onUpdateRoster;
    private TreeNode selectedItem = null;
    public boolean useGroups;
    private boolean hideOffline;
    private Protocol[] protocolList;
    private int count = 0;

    public static RosterHelper getInstance() {
        return instance;
    }

    public byte getProtocolType(Profile account) {
        for (int i = 0; i < Profile.protocolTypes.length; ++i) {
            if (account.protocolType == Profile.protocolTypes[i]) {
                return account.protocolType;
            }
        }
        return Profile.protocolTypes[0];
    }

    private boolean is(Protocol protocol, Profile profile) {
        Profile exist = protocol.getProfile();
        return exist == profile || (exist.protocolType == profile.protocolType) && exist.userId.equals(profile.userId);
    }

    public void addProtocols(Vector accounts) {
        int count = getProtocolCount();
        Protocol[] protocols = new Protocol[count];
        for (int i = 0; i < count; ++i) {
            protocols[i] = getProtocol(i);
        }
        removeAllProtocols();
        for (int i = 0; i < accounts.size(); ++i) {
            Profile profile = (Profile) accounts.elementAt(i);
            for (int j = 0; j < protocols.length; ++j) {
                Protocol protocol = protocols[j];
                if ((null != protocol) && is(protocol, profile)) {
                    if (protocol.getProfile() != profile) {
                        protocol.setProfile(profile);
                    }
                    protocols[j] = null;
                    profile = null;
                    addProtocol(protocol);
                    break;
                }
            }
            if (null != profile) {
                addProtocol(profile, true);
            }
        }
        for (int i = 0; i < protocols.length; ++i) {
            Protocol protocol = protocols[i];
            if (null != protocol) {
                SawimApplication.getInstance().setStatus(protocol, StatusInfo.STATUS_OFFLINE, "");
                protocol.needSave();
                protocol.dismiss();
            }
        }
    }

    public void setCurrentProtocol() {
        Vector listOfProfiles = new Vector();
        for (int i = 0; i < Options.getAccountCount(); ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                listOfProfiles.addElement(p);
            }
        }
        /*if (listOfProfiles.isEmpty()) {
            Profile p = Options.getAccount(0);
            p.isActive = true;
            listOfProfiles.addElement(p);
        }*/
        addProtocols(listOfProfiles);
        updateRoster();
    }

    public void initAccounts() {
        protocolList = new Protocol[Options.getMaxAccountCount()];
        int count = Math.max(1, Options.getAccountCount());
        for (int i = 0; i < count; ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                addProtocol(p, false);
            }
        }
    }

    public void loadAccounts() {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol protocol = getProtocol(i);
            protocol.safeLoad();
        }
    }

    private byte getRealType(byte type) {
        switch (type) {
            case Profile.PROTOCOL_GTALK:
            case Profile.PROTOCOL_FACEBOOK:
            case Profile.PROTOCOL_LJ:
            case Profile.PROTOCOL_YANDEX:
            case Profile.PROTOCOL_QIP:
            case Profile.PROTOCOL_ODNOKLASSNIKI:
                return Profile.PROTOCOL_JABBER;
        }
        return type;
    }

    private void addProtocol(Profile account, boolean load) {
        Protocol protocol = null;
        byte type = getProtocolType(account);
        switch (getRealType(type)) {
            case Profile.PROTOCOL_ICQ:
                protocol = new Icq();
                break;

            case Profile.PROTOCOL_MRIM:
                protocol = new Mrim();
                break;

            case Profile.PROTOCOL_JABBER:
                protocol = new Xmpp();
                break;

            case Profile.PROTOCOL_VK_API:
                protocol = new protocol.vk.Vk();
                break;
        }
        if (null == protocol) {
            return;
        }
        protocol.setProfile(account);
        protocol.init();
        if (load) {
            protocol.safeLoad();
        }
        addProtocol(protocol);
    }

    public Protocol getProtocol(Profile profile) {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (p.getProfile() != null)
                if (p.getProfile().protocolType == profile.protocolType)
                    return p;
        }
        return null;
    }

    public int getProtocol(Protocol protocol) {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (protocol == p) {
                return i;
            }
        }
        return 0;
    }

    public Protocol getProtocol(String account) {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (p.getUserId().equals(account)) {
                return p;
            }
        }
        return null;
    }

    public Protocol[] getProtocols() {
        Protocol[] all = new Protocol[getProtocolCount()];
        for (int i = 0; i < all.length; ++i) {
            all[i] = getProtocol(i);
        }
        return all;
    }

    public Protocol getProtocol(Contact c) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).inContactList(c)) {
                return getProtocol(i);
            }
        }
        return null;
    }

    public void activate(Contact c) {
        setAlwaysVisibleNode(c);
        updateRoster();
    }

    public void activateWithMsg(final String message) {
        SawimApplication.getInstance().getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SawimApplication.getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
        updateRoster();
    }

    public void autoConnect() {
        if (!SawimApplication.getInstance().isNetworkAvailable())
            return;
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Profile profile = Options.getAccount(i);
            Protocol protocol = getProtocol(profile);
            if (!"".equals(protocol.getPassword()) && profile.isConnected()) {
                protocol.connect();
            }
        }
    }

    public boolean isConnected() {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (p.isConnected() && !p.isConnecting()) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnecting() {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (p.isConnecting()) {
                return true;
            }
        }
        return false;
    }

    public boolean disconnect() {
        boolean disconnecting = false;
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (p.isConnected()) {
                p.disconnect(false);
                disconnecting = true;
            }
        }
        return disconnecting;
    }

    public void safeSave() {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            p.safeSave();
        }
    }

    public final void markMessages(Contact contact) {
        SawimApplication.getInstance().updateAppIcon();
        if (getUpdateChatListener() != null)
            getUpdateChatListener().updateChat();
    }

    public void updateConnectionStatus() {
        SawimApplication.getInstance().updateAppIcon();
    }

    public final Contact getCurrentContact() {
        return currentContact;
    }

    public final void setCurrentContact(Contact contact) {
        currentContact = contact;
    }

    public StatusView getStatusView() {
        return statusView;
    }

    private int contactListSaveDelay = 0;

    public final void needRosterSave() {
        contactListSaveDelay = 60 * 4 /* * 250 = 60 sec */;
    }

    public final void timerAction() {
        AutoAbsence.getInstance().updateTime();
        /*if (0 < contactListSaveDelay) {
            contactListSaveDelay--;
            if (0 == contactListSaveDelay) {
                int count = contactList.getProtocolCount();
                for (int i = 0; i < count; ++i) {
                    Protocol p = contactList.getProtocol(i);
                    p.safeSave();
                }
            }
        }*/
    }

    public void addTransfer(FileTransfer ft) {
        transfers.addElement(ft);
    }

    public void removeTransfer(boolean cancel) {
        for (int i = 0; i < transfers.size(); ++i) {
            FileTransfer ft = (FileTransfer) transfers.elementAt(i);
            transfers.removeElementAt(i);
            if (cancel) {
                ft.cancel();
            }
            return;
        }
    }

    public void updateRoster(TreeNode node) {
        updateRoster();
    }

    public final void updateRoster() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public void updateProgressBar() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateProgressBar();
    }

    public void setOnUpdateRoster(OnUpdateRoster l) {
        onUpdateRoster = l;
    }

    public void setCurrPage(int curr) {
        if (Options.getInt(Options.OPTION_CURRENT_PAGE) != curr) {
            Options.setInt(Options.OPTION_CURRENT_PAGE, curr);
            Options.safeSave();
        }
    }

    public int getCurrPage() {
        return Options.getInt(Options.OPTION_CURRENT_PAGE);
    }

    public void removeAllProtocols() {
        count = 0;
        for (int i = 0; i < protocolList.length; ++i) {
            protocolList[i] = null;
        }
    }

    public void removeProtocol(int i) {
        protocolList[i] = null;
    }

    public void addProtocol(Protocol prot) {
        if ((count < protocolList.length) && (null != prot)) {
            protocolList[count] = prot;
            count++;
        }
    }

    public final void setAlwaysVisibleNode(TreeNode node) {
        selectedItem = node;
    }

    public final Protocol getProtocol(int accountIndex) {
        return protocolList[accountIndex];
    }

    public final int getProtocolCount() {
        return count;
    }

    public void updateOptions() {
        useGroups = Options.getBoolean(JLocale.getString(R.string.pref_user_groups)) && getCurrPage() != ACTIVE_CONTACTS;
        hideOffline = getCurrPage() == ONLINE_CONTACTS;
    }

    public final Protocol getProtocol(Group g) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (-1 < getProtocol(i).getGroupItems().indexOf(g)) {
                return getProtocol(i);
            }
            if (getProtocol(i).getNotInListGroup() == g) {
                return getProtocol(i);
            }
        }
        return null;
    }

    public void rebuildFlatItemsWG(Protocol p, List<Object> list) {
        Vector contacts;
        Group g;
        Contact c;
        int contactCounter;
        int onlineContactCounter;
        boolean all = !hideOffline;
        Vector groups = p.getGroupItems();
        Util.sort(groups);
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            g = (Group) groups.elementAt(groupIndex);
            g.sort();
            contactCounter = 0;
            onlineContactCounter = 0;
            list.add(g);
            contacts = g.getContacts();
            int contactsSize = contacts.size();
            for (int contactIndex = 0; contactIndex < contactsSize; contactIndex++) {
                c = (Contact) contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList()/* || (c == selectedItem)*/) {
                    if (g.isExpanded()) {
                        list.add(c);
                    }
                    contactCounter++;
                }
                if (c.isOnline())
                    ++onlineContactCounter;
            }
            if (hideOffline && (0 == contactCounter)) {
                list.remove(list.size() - 1);
            }
            g.updateGroupData(contactsSize, onlineContactCounter);
        }

        g = p.getNotInListGroup();
        g.sort();
        list.add(g);
        contacts = g.getContacts();
        contactCounter = 0;
        onlineContactCounter = 0;
        int contactsSize = contacts.size();
        for (int contactIndex = 0; contactIndex < contactsSize; contactIndex++) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList()/* || (c == selectedItem)*/) {
                if (g.isExpanded()) {
                    list.add(c);
                }
                contactCounter++;
            }
            if (c.isOnline())
                ++onlineContactCounter;
        }
        if (0 == contactCounter) {
            list.remove(list.size() - 1);
        }
        g.updateGroupData(contactsSize, onlineContactCounter);
    }

    public void rebuildFlatItemsWOG(Protocol p, List<Object> list) {
        boolean all = !hideOffline;
        Contact c;
        Vector contacts = p.getContactItems();
        Util.sort(contacts);
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList()/* || (c == selectedItem)*/) {
                list.add(c);
            }
        }
    }

    public void updateGroup(Protocol protocol, Group group, Contact contact) {
        if (group == null) return;
        Vector allItems = protocol.getContactItems();
        Vector groupItems = group.getContacts();
        groupItems.removeAllElements();
        int size = allItems.size();
        int groupId = group.getId();
        for (int i = 0; i < size; ++i) {
            Contact item = (Contact) allItems.elementAt(i);
            if (item.getGroupId() == groupId && contact != item) {
                groupItems.addElement(item);
            }
        }
        group.updateGroupData();
        //group.sort();
    }

    public void updateGroup(Protocol protocol, Group group) {
        updateGroup(protocol, group, null);
    }

    public void updateGroup(Group group) {
        if (useGroups) {
            group.updateGroupData();
            group.sort();
        } else {
            Protocol p = getProtocol(group);
            if (p != null)
                Util.sort(p.getContactItems());
        }
    }

    public void removeFromGroup(Protocol protocol, Group g, Contact c) {
        if (g == null) return;
        if (g.getContacts().removeElement(c))
            updateGroup(protocol, g, c);
    }

    public void addToGroup(Group group, Contact contact) {
        group.getContacts().addElement(contact);
    }

    public void putIntoQueue(Group group) {
        if (onUpdateRoster != null)
            onUpdateRoster.putIntoQueue(group);
    }

    public String getStatusMessage(Protocol protocol, Contact contact) {
        String message;
        if (getCurrPage() == RosterHelper.ACTIVE_CONTACTS)
            protocol = contact.getProtocol();
        if (protocol == null || contact == null) return "";
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            message = contact.getXStatusText();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
            XStatusInfo xStatusInfo = protocol.getXStatusInfo();
            if (xStatusInfo != null)
                return JLocale.getString(xStatusInfo.getName(contact.getXStatusIndex()));
        }
        message = contact.getStatusText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        return protocol.getStatusInfo().getName(contact.getStatusIndex());
    }

    public static final int MENU_CONNECT = 0;
    public static final int MENU_STATUS = 1;
    public static final int MENU_XSTATUS = 2;
    public static final int MENU_PRIVATE_STATUS = 3;
    public static final int MENU_SEND_SMS = 5;
    public static final int MENU_DISCO = 16;
    public static final int MENU_NOTES = 17;
    public static final int MENU_GROUPS = 18;
    public static final int MENU_MYSELF = 19;
    public static final int MENU_MICROBLOG = 20;

    public void showProtocolMenu(final BaseActivity activity, final Protocol p) {
        if (p != null) {
            final MyMenu menu = new MyMenu();
            menu.add(p.isConnected() || p.isConnecting() ? R.string.disconnect : R.string.connect, MENU_CONNECT);
            menu.add(R.string.status, MENU_STATUS);
            if (p.getXStatusInfo() != null)
                menu.add(R.string.xstatus, MENU_XSTATUS);
            if ((p instanceof Icq) || (p instanceof Mrim))
                menu.add(R.string.private_status, MENU_PRIVATE_STATUS);
            for (int i = 0; i < count; ++i) {
                Protocol pr = RosterHelper.getInstance().getProtocol(i);
                if (pr instanceof Mrim && pr.isConnected()) {
                    menu.add(R.string.send_sms, MENU_SEND_SMS);
                }
            }
            if (p.isConnected()) {
                if (p instanceof Xmpp) {
                    if (((Xmpp) p).hasS2S()) {
                        menu.add(R.string.service_discovery, MENU_DISCO);
                    }
                }
                menu.add(R.string.manage_contact_list, MENU_GROUPS);
                if (p instanceof Icq) {
                    menu.add(R.string.myself, MENU_MYSELF);
                } else {
                    if (p instanceof Xmpp) {
                        menu.add(R.string.notes, MENU_NOTES);
                    }
                    if (p.hasVCardEditor())
                        menu.add(R.string.myself, MENU_MYSELF);
                    if (p instanceof Mrim)
                        menu.add(R.string.microblog, MENU_MICROBLOG);
                }
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(true);
            builder.setTitle(p.getUserId());
            builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    protocolMenuItemSelected(activity, p, menu.getItem(which).idItem);
                }
            });
            builder.create().show();
        }
    }

    public boolean protocolMenuItemSelected(BaseActivity activity, Protocol p, int idItem) {
        switch (idItem) {
            case RosterHelper.MENU_CONNECT:
                SawimApplication.getInstance().setStatus(p, (p.isConnected() || p.isConnecting())
                        ? StatusInfo.STATUS_OFFLINE : StatusInfo.STATUS_ONLINE, "");
                return true;
            case RosterHelper.MENU_STATUS:
                new StatusesView().init(p, StatusesView.ADAPTER_STATUS).show(activity.getSupportFragmentManager(), "change-status");
                return true;
            case RosterHelper.MENU_XSTATUS:
                new XStatusesView(p).show(activity.getSupportFragmentManager(), "change-xstatus");
                return true;
            case RosterHelper.MENU_PRIVATE_STATUS:
                new StatusesView().init(p, StatusesView.ADAPTER_PRIVATESTATUS).show(activity.getSupportFragmentManager(), "change-private-status");
                return true;
            case RosterHelper.MENU_SEND_SMS:
                new SmsForm(null, null).show(activity);
                return true;
            case RosterHelper.MENU_DISCO:
                ((Xmpp) p).getServiceDiscovery().showIt(activity);
                return true;
            case RosterHelper.MENU_NOTES:
                ((Xmpp) p).getMirandaNotes().showIt(activity);
                return true;
            case RosterHelper.MENU_GROUPS:
                new ManageContactListForm(p).showMenu(activity);
                return true;
            case RosterHelper.MENU_MYSELF:
                p.showUserInfo(activity, p.createTempContact(p.getUserId(), p.getNick()));
                return true;
            case RosterHelper.MENU_MICROBLOG:
                ((Mrim) p).getMicroBlog().activate(activity);
                return true;
        }
        return false;
    }

    private HashMap<String, String> subjectsMap = new HashMap<String, String>();

    public String getSubject(String id) {
        if (subjectsMap.containsKey(id)) return subjectsMap.get(id);
        else return null;
    }

    public void setSubject(String id, String subject) {
        subjectsMap.put(id, subject);
    }

    private OnUpdateChat updateChatListener;

    public void setOnUpdateChat(OnUpdateChat l) {
        updateChatListener = l;
    }

    public OnUpdateChat getUpdateChatListener() {
        return updateChatListener;
    }
}