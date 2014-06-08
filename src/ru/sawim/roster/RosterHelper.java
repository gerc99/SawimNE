package ru.sawim.roster;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import protocol.*;
import protocol.AdHoc;
import protocol.Jid;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimNotification;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.listener.OnUpdateChat;
import ru.sawim.listener.OnUpdateRoster;
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
    private Protocol protocol;

    public static RosterHelper getInstance() {
        return instance;
    }

    private boolean is(Protocol protocol, Profile profile) {
        Profile exist = protocol.getProfile();
        return exist == profile && exist.userId.equals(profile.userId);
    }

    public void addProtocols(Profile profile) {
        if ((null != protocol) && is(protocol, profile)) {
            protocol.setProfile(profile);
            setProtocol(protocol);
            return;
        }
        if (null != profile) {
            addProtocol(profile, true);
        }
    }

    public void setCurrentProtocol() {
        Profile p = Options.getAccount();
        addProtocols(p);
        updateRoster();
    }

    public void initAccounts() {
        Profile p = Options.getAccount();
        addProtocol(p, false);
    }

    public void loadAccounts() {
        if (protocol != null)
            protocol.safeLoad();
    }

    private void addProtocol(Profile account, boolean load) {
        Protocol protocol = new Protocol();
        protocol.setProfile(account);
        protocol.init();
        if (load) {
            protocol.safeLoad();
        }
        setProtocol(protocol);
    }

    public void setProtocol(Protocol p) {
        protocol = p;
    }

    public void activate(Contact c) {
        setAlwaysVisibleNode(c);
        updateRoster();
    }

    public void activateWithMsg(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
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
        Protocol p = getProtocol();
        if (!"".equals(p.getPassword()) && p.getProfile().isConnected()) {
            p.connect();
        }
    }

    public boolean isConnected() {
        Protocol p = getProtocol();
        if (p.isConnected() && !p.isConnecting()) {
            return true;
        }
        return false;
    }

    public boolean isConnecting() {
        Protocol p = getProtocol();
        if (p.isConnecting()) {
            return true;
        }
        return false;
    }

    public boolean disconnect() {
        boolean disconnecting = false;
        Protocol p = getProtocol();
        if (p.isConnected()) {
            p.disconnect(false);
            disconnecting = true;
        }
        return disconnecting;
    }

    public void safeSave() {
        getProtocol().safeSave();
    }

    public final void markMessages(Contact contact) {
        SawimNotification.clear(SawimNotification.NOTIFY_ID);
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

    public void updateBarProtocols() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateBarProtocols();
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

    public final void setAlwaysVisibleNode(TreeNode node) {
        selectedItem = node;
    }

    public final Protocol getProtocol() {
        return protocol;
    }

    public void updateOptions() {
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS) && getCurrPage() != ACTIVE_CONTACTS;
        hideOffline = getCurrPage() == ONLINE_CONTACTS;
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
            Protocol p = getProtocol();
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
            protocol = getProtocol();
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
    public static final int MENU_ADHOC = 17;
    public static final int MENU_NOTES = 18;
    public static final int MENU_GROUPS = 19;
    public static final int MENU_MYSELF = 20;

    public void showProtocolMenu(final BaseActivity activity, final Protocol p) {
        if (p != null) {
            final MyMenu menu = new MyMenu(activity);
            menu.add(p.isConnected() || p.isConnecting() ? R.string.disconnect : R.string.connect, MENU_CONNECT);
            menu.add(R.string.status, MENU_STATUS);
            if (p.getXStatusInfo() != null)
                menu.add(R.string.xstatus, MENU_XSTATUS);
            Protocol pr = RosterHelper.getInstance().getProtocol();
            if (pr.isConnected()) {
                //menu.add(R.string.send_sms, MENU_SEND_SMS);
            }
            if (p.isConnected()) {
                menu.add(R.string.service_discovery, MENU_DISCO);
                menu.add(R.string.account_settings, MENU_ADHOC);
                menu.add(R.string.manage_contact_list, MENU_GROUPS);
                menu.add(R.string.notes, MENU_NOTES);
                if (p.hasVCardEditor())
                    menu.add(R.string.myself, MENU_MYSELF);
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
                new StatusesView(p, StatusesView.ADAPTER_STATUS).show(activity.getSupportFragmentManager(), "change-status");
                return true;
            case RosterHelper.MENU_XSTATUS:
                new XStatusesView(p).show(activity.getSupportFragmentManager(), "change-xstatus");
                return true;
            case RosterHelper.MENU_PRIVATE_STATUS:
                new StatusesView(p, StatusesView.ADAPTER_PRIVATESTATUS).show(activity.getSupportFragmentManager(), "change-private-status");
                return true;
            case RosterHelper.MENU_SEND_SMS:

                return true;
            case RosterHelper.MENU_DISCO:
                p.getServiceDiscovery().showIt();
                return true;
            case RosterHelper.MENU_ADHOC:
                String serverAddress = Jid.getDomain(p.getUserId());
                Contact serverContact = p.createTempContact(serverAddress);
                AdHoc adhoc = new AdHoc(p, serverContact);
                adhoc.show(activity);
                return true;
            case RosterHelper.MENU_NOTES:
                p.getMirandaNotes().showIt();
                return true;
            case RosterHelper.MENU_GROUPS:
                new ManageContactListForm(p).showMenu(activity);
                return true;
            case RosterHelper.MENU_MYSELF:
                p.showUserInfo(activity, p.createTempContact(p.getUserId(), p.getNick()));
                return true;
        }
        return false;
    }

    private HashMap<String, Long> lastMessagesTimeMap = new HashMap<String, Long>();
    private HashMap<String, String> subjectsMap = new HashMap<String, String>();
    private HashMap<String, Boolean> isPresencesFlagsMap = new HashMap<String, Boolean>();

    public boolean isPresence(String id) {
        if (isPresencesFlagsMap.containsKey(id)) return isPresencesFlagsMap.get(id);
        else return false;
    }

    public void setPresencesFlag(String id, boolean flag) {
        isPresencesFlagsMap.put(id, flag);
    }

    public String getSubject(String id) {
        if (subjectsMap.containsKey(id)) return subjectsMap.get(id);
        else return null;
    }

    public void setSubject(String id, String subject) {
        subjectsMap.put(id, subject);
    }

    public long getLastMessageTime(String id) {
        if (lastMessagesTimeMap.containsKey(id)) return lastMessagesTimeMap.get(id);
        else return 0;
    }

    public void setLastMessageTime(String id, long time) {
        lastMessagesTimeMap.put(id, time);
    }

    private OnUpdateChat updateChatListener;

    public void setOnUpdateChat(OnUpdateChat l) {
        updateChatListener = l;
    }

    public OnUpdateChat getUpdateChatListener() {
        return updateChatListener;
    }

}