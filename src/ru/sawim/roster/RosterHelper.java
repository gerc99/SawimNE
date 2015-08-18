package ru.sawim.roster;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.widget.Toast;
import ru.sawim.activities.SawimActivity;
import ru.sawim.comm.Util;
import ru.sawim.listener.OnAccountsLoaded;
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
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.forms.SmsForm;
import ru.sawim.modules.FileTransfer;
import ru.sawim.view.StatusesView;
import ru.sawim.view.XStatusesView;
import ru.sawim.view.menu.MyMenu;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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
    private List<FileTransfer> transfers = new ArrayList<FileTransfer>();
    private OnUpdateRoster updateRoster;
    private OnAccountsLoaded accountsLoaded;
    public boolean useGroups;
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
        /*for (int i = 0; i < protocols.length; ++i) {
            Protocol protocol = protocols[i];
            if (null != protocol) {
                SawimApplication.getInstance().setStatus(protocol, StatusInfo.STATUS_OFFLINE, "");
                protocol.dismiss();
            }
        }*/
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
        SawimApplication.getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                int count = getProtocolCount();
                for (int i = 0; i < count; ++i) {
                    Protocol protocol = getProtocol(i);
                    protocol.load();
                    autoConnect(i);
                }
                if (accountsLoaded != null) {
                    accountsLoaded.onAccountsLoaded();
                }
                SawimApplication.actionQueue.put(SawimActivity.ACTION_ACC_LOADED, SawimActivity.ACTION_ACC_LOADED);
            }
        });
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
                if (is(p, profile))
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

    public final Protocol getProtocol(int accountIndex) {
        if (protocolList == null) return null;
        return protocolList[accountIndex];
    }

    public final Protocol getProtocol(Group treeNode) {
        if (treeNode == null) return null;
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).getGroupItems().get(treeNode.getGroupId()) != null) {
                return getProtocol(i);
            }
            if (getProtocol(i).getNotInListGroup().getGroupId() == treeNode.getGroupId()) {
                return getProtocol(i);
            }
        }
        return null;
    }

    public Protocol getProtocol(Contact c) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).inContactList(c)) {
                return getProtocol(i);
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

    public Group getGroupWithContacts(Group g) {
        Group group = g;
        Protocol protocol = RosterHelper.getInstance().getProtocol(g);
        if (protocol != null && protocol.getGroupItems() != null) {
            g = protocol.getGroupItems().get(g.getGroupId());
        }
        if (g == null) g = group;
        return g;
    }

    public void activate(Contact c) {
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

    public boolean checkPassword(Protocol protocol) {
        if (protocol == null) return false;
        if (protocol.getPassword() == null || "".equals(protocol.getPassword())) {
            Profile profile = protocol.getProfile();
            int editAccountNum = Options.getAccountIndex(profile);
            return false;
        }
        return true;
    }

    public void autoConnect() {
        int count = getProtocolCount();
        for (int i = 0; i < count; ++i) {
            autoConnect(i);
        }
    }

    public void autoConnect(int i) {
        Profile profile = Options.getAccount(i);
        Protocol protocol = getProtocol(profile);
        if (checkPassword(protocol)) {
            if (profile.isConnected()) {
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

    public void addTransfer(FileTransfer ft) {
        transfers.add(ft);
    }

    public void removeTransfer(int id, boolean cancel) {
        for (int i = 0; i < transfers.size(); ++i) {
            FileTransfer ft = transfers.get(i);
            if (ft.getProgressText().equals(id)) {
                transfers.remove(i);
                if (cancel) {
                    ft.cancel();
                }
            }
            return;
        }
    }

    public FileTransfer getFileTransfer(int id) {
        for (int i = 0; i < transfers.size(); ++i) {
            FileTransfer ft = transfers.get(i);
            if (ft.getId() == id) {
                return ft;
            }
        }
        return null;
    }

    public void updateRoster(TreeNode node) {
        updateRoster();
    }

    public final void updateRoster() {
        if (updateRoster != null)
            updateRoster.updateRoster();
    }

    public void updateProgressBar() {
        if (updateRoster != null)
            updateRoster.updateProgressBar();
    }

    public void setOnUpdateRoster(OnUpdateRoster l) {
        updateRoster = l;
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

    public final int getProtocolCount() {
        return count;
    }

    public void updateOptions() {
        useGroups = Options.getBoolean(JLocale.getString(R.string.pref_user_groups)) && getCurrPage() != ACTIVE_CONTACTS;
    }

    public static <T extends TreeNode> void sort(List<T> subnodes, final ConcurrentHashMap<Integer, Group> groups) {
        Collections.sort(subnodes, new Comparator<T>() {
            @Override
            public int compare(T node1, T node2) {
                if (node1.getType() == TreeNode.PROTOCOL || node2.getType() == TreeNode.PROTOCOL) {
                    return node1.getNodeWeight() - node1.getNodeWeight();
                }
                if (groups != null) {
                    if (node1.getGroupId() == node2.getGroupId()) {
                        return Util.compareNodes(node1, node2);
                    } else {
                        Group group1 = groups.get(node1.getGroupId());
                        Group group2 = groups.get(node2.getGroupId());
                        if (group1 != null && group2 != null) {
                            return Util.compareNodes(group1, group2);
                        }
                    }
                } else {
                    return Util.compareNodes(node1, node2);
                }
                return 0;
            }
        });
    }

    public void updateGroup(Protocol protocol, Group group, Contact contact) {
        if (group == null) return;
        List<Contact> groupItems = group.getContacts();
        groupItems.clear();
        int groupId = group.getGroupId();
        for (Contact item : protocol.getContactItems().values()) {
            if (item.getGroupId() == groupId && contact != item) {
                groupItems.add(item);
            }
        }
        group.updateGroupData();
        //group.sort();
    }

    public void updateGroup(Protocol protocol, Group group) {
        updateGroup(protocol, group, null);
    }

    public void removeFromGroup(Protocol protocol, Group g, Contact c) {
        if (g == null) return;
        if (g.getContacts().remove(c))
            updateGroup(protocol, g, c);
    }

    public void putIntoQueue(Group group) {
        if (updateRoster != null)
            updateRoster.putIntoQueue(group);
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

    public MyMenu getMenu(final Protocol p) {
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
        return menu;
    }

    public void showProtocolMenu(final BaseActivity activity, final Protocol p) {
        if (p != null) {
            final MyMenu menu = getMenu(p);
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
                if (checkPassword(p)) {
                    SawimApplication.getInstance().setStatus(p, (p.isConnected() || p.isConnecting())
                            ? StatusInfo.STATUS_OFFLINE : StatusInfo.STATUS_ONLINE, "");
                }
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
                p.showUserInfo(activity, p.createTempContact(p.getUserId(), p.getNick(), false));
                return true;
            case RosterHelper.MENU_MICROBLOG:
                ((Mrim) p).getMicroBlog().activate(activity);
                return true;
        }
        return false;
    }

    private OnUpdateChat updateChatListener;

    public void setOnUpdateChat(OnUpdateChat l) {
        updateChatListener = l;
    }

    public OnUpdateChat getUpdateChatListener() {
        return updateChatListener;
    }

    public void setOnAccountsLoaded(OnAccountsLoaded accountsLoaded) {
        this.accountsLoaded = accountsLoaded;
    }
}