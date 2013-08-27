package sawim.roster;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import protocol.*;
import protocol.icq.Icq;
import protocol.jabber.Jabber;
import protocol.mrim.Mrim;
import ru.sawim.General;
import ru.sawim.SawimApplication;
import ru.sawim.models.RosterAdapter;
import sawim.FileTransfer;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.AutoAbsence;

import java.util.List;
import java.util.Vector;

public final class Roster {
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME = 2;

    private static final Roster instance = new Roster();
    private final StatusView statusView = new StatusView();

    private Contact currentContact;
    private Vector transfers = new Vector();
    private OnUpdateRoster onUpdateRoster;
    private int currentItemProtocol = 0;
    private int currPage = 0;
    private TreeNode selectedItem = null;
    public boolean useGroups;
    private boolean hideOffline;
    private Protocol[] protocolList;
    private int count = 0;

    public static Roster getInstance() {
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
        if (exist == profile) {
            return true;
        }
        return (exist.protocolType == profile.protocolType)
                && exist.userId.equals(profile.userId);
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
                protocol.disconnect(true);
                protocol.needSave();
                protocol.dismiss();
            }
        }
    }

    public void setCurrentProtocol() {
        Roster cl = Roster.getInstance();
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
        cl.addProtocols(listOfProfiles);
        update();
    }

    public void initAccounts() {
        protocolList = new Protocol[10];
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
            case Profile.PROTOCOL_VK:
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
                protocol = new Jabber();
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
            if (p.getProfile() == profile) {
                return p;
            }
        }
        return null;
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

    public Protocol getCurrProtocol() {
        return getCurrentProtocol();
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
        if (null != c) {
            setActiveContact(c);
        }
        setAlwaysVisibleNode(c);
        update();
    }

    public void activateWithMsg(final String message) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SawimApplication.getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
        update();
    }

    public void autoConnect() {
        if (!SawimApplication.getInstance().isNetworkAvailable())
            return;
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = getProtocol(i);
            if (!"".equals(p.getPassword()) && p.getProfile().isConnected()) {
                p.connect();
            }
        }
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

    /*public void collapseAll() {
        int count = contactList.getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getProtocol(i);
            Vector groups = p.getGroupItems();
            for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                ((TreeBranch) groups.elementAt(groupIndex)).setExpandFlag(false);
            }
            p.getNotInListGroup().setExpandFlag(false);
        }
        contactList.update();
    }*/

    public final void markMessages(Contact contact) {
        SawimApplication.getInstance().updateAppIcon();
        if (General.getInstance().getUpdateChatListener() != null)
            General.getInstance().getUpdateChatListener().updateChat(contact);
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
        AutoAbsence.instance.updateTime();
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

    public void update(TreeNode node) {
        update();
    }

    public final void update() {
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

    public void setCurrentItemProtocol(int currentItemProtocol) {
        this.currentItemProtocol = currentItemProtocol;
    }

    public int getCurrentItemProtocol() {
        return currentItemProtocol;
    }

    public final Protocol getCurrentProtocol() {
        Protocol p = getProtocol(currentItemProtocol);
        if (getProtocolCount() == 0 || null == p) {
            p = getProtocol(0);
        }
        return p;
    }

    public void setCurrPage(int curr) {
        currPage = curr;
    }

    public int getCurrPage() {
        return currPage;
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
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS) && getCurrPage() == RosterAdapter.ALL_CONTACTS;
        hideOffline = /*Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)*/getCurrPage() == RosterAdapter.ONLINE_CONTACTS;
    }

    public final Protocol getProtocol(Group g) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (-1 < Util.getIndex(getProtocol(i).getGroupItems(), g)) {
                return getProtocol(i);
            }
            if (getProtocol(i).getNotInListGroup() == g) {
                return getProtocol(i);
            }
        }
        return null;
    }

    public void rebuildFlatItemsWG(Protocol p, List<TreeNode> drawItems) {
        Vector contacts;
        Group g;
        Contact c;
        int contactCounter;
        int onlineContactCounter;
        boolean all = !hideOffline;
        Vector groups = p.getSortedGroups();
        Util.sort(groups);
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            g = (Group) groups.elementAt(groupIndex);
            g.sort();
            contactCounter = 0;
            onlineContactCounter = 0;
            drawItems.add(g);
            contacts = g.getContacts();
            int contactsSize = contacts.size();
            for (int contactIndex = 0; contactIndex < contactsSize; ++contactIndex) {
                c = (Contact) contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                    if (g.isExpanded()) {
                        drawItems.add(c);
                    }
                    contactCounter++;
                }
                if (c.isOnline())
                    ++onlineContactCounter;
            }
            if (hideOffline && (0 == contactCounter)) {
                drawItems.remove(drawItems.size() - 1);
            }
            g.updateGroupData(contactsSize, onlineContactCounter);
        }

        g = p.getNotInListGroup();
        g.sort();
        drawItems.add(g);
        contacts = g.getContacts();
        contactCounter = 0;
        onlineContactCounter = 0;
        int contactsSize = contacts.size();
        for (int contactIndex = 0; contactIndex < contactsSize; ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                if (g.isExpanded()) {
                    drawItems.add(c);
                }
                contactCounter++;
            }
            if (c.isOnline())
                ++onlineContactCounter;
        }
        if (0 == contactCounter) {
            drawItems.remove(drawItems.size() - 1);
        }
        g.updateGroupData(contactsSize, onlineContactCounter);
    }

    public void rebuildFlatItemsWOG(Protocol p, List<TreeNode> drawItems) {
        boolean all = !hideOffline;
        Contact c;
        Vector contacts = p.getSortedContacts();
        Util.sort(contacts);
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.add(c);
            }
        }
    }

    public void updateGroup(Protocol protocol, Group group) {
        Vector allItems = protocol.getContactItems();
        Vector groupItems = group.getContacts();
        groupItems.removeAllElements();
        int size = allItems.size();
        int groupId = group.getId();
        for (int i = 0; i < size; ++i) {
            Contact item = (Contact) allItems.elementAt(i);
            if (item.getGroupId() == groupId) {
                groupItems.addElement(item);
            }
        }
        group.updateGroupData();
        group.sort();
    }

    public void updateGroup(Group group) {
        if (group == null) return;
        if (useGroups) {
            group.updateGroupData();
            group.sort();
        } else {
            Util.sort(getProtocol(group).getSortedContacts());
        }
    }

    public void removeFromGroup(Group g, Contact c) {
        if (g.getContacts().removeElement(c))
            g.updateGroupData();
    }

    public void addToGroup(Group group, Contact contact) {
        group.getContacts().addElement(contact);
    }

    public void putIntoQueue(Group group) {
        if (onUpdateRoster != null)
            onUpdateRoster.putIntoQueue(group);
    }

    public final void setActiveContact(Contact cItem) {
        if (onUpdateRoster != null)
            onUpdateRoster.setCurrentNode(cItem);
    }

    public String getStatusMessage(Contact contact) {
        String message;
        Protocol protocol = getCurrentProtocol();
        if (protocol == null) return "";
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            message = contact.getXStatusText();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
            message = protocol.getXStatusInfo().getName(contact.getXStatusIndex());
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
        }
        message = contact.getStatusText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        message = protocol.getStatusInfo().getName(contact.getStatusIndex());
        return (message == null) ? "" : message;
    }

    public interface OnUpdateRoster {
        void updateRoster();

        void updateBarProtocols();

        void updateProgressBar();

        void putIntoQueue(Group g);

        void setCurrentNode(TreeNode cItem);
    }
}