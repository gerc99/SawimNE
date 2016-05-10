package ru.sawim.roster;

import android.support.v7.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import ru.sawim.SawimException;
import ru.sawim.ui.activity.AccountsListActivity;
import ru.sawim.ui.activity.SawimActivity;
import ru.sawim.comm.Util;
import ru.sawim.listener.OnAccountsLoaded;
import ru.sawim.listener.OnUpdateChat;
import ru.sawim.listener.OnUpdateRoster;
import protocol.*;
import protocol.xmpp.Xmpp;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.modules.FileTransfer;
import ru.sawim.ui.fragment.SearchConferenceFragment;
import ru.sawim.ui.fragment.StatusesFragment;
import ru.sawim.ui.fragment.XStatusesFragment;
import ru.sawim.ui.fragment.menu.MyMenu;

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

    Protocol protocol;

    private Contact currentContact;
    private List<FileTransfer> transfers = new ArrayList<FileTransfer>();
    private OnUpdateRoster updateRoster;
    private OnAccountsLoaded accountsLoaded;
    public boolean useGroups;

    public static RosterHelper getInstance() {
        return instance;
    }

    private boolean is(Protocol protocol, Profile profile) {
        Profile exist = protocol.getProfile();
        return exist == profile || exist.userId.equals(profile.userId);
    }

    public boolean isAccountsLoaded;
    public void loadAccounts() {
        Protocol protocol = getProtocol();
        if (protocol != null) {
            protocol.load();
            isAccountsLoaded = true;
            if (accountsLoaded != null) {
                accountsLoaded.onAccountsLoaded();
            }
        }
    }

    public Protocol getProtocol() {
        if (protocol == null) {
            protocol = new Xmpp();
            protocol.setProfile(Options.getAccount(0));
            protocol.init();
            protocol.safeLoad();
        }
        return protocol;
    }

    public Group getGroupWithContacts(Group g) {
        Group group = g;
        Protocol protocol = RosterHelper.getInstance().getProtocol();
        if (protocol != null && protocol.getGroupItems() != null) {
            g = protocol.getGroupItems().get(g.getGroupId());
        }
        if (g == null) g = group;
        return g;
    }

    public void activate(Contact c) {
        updateRoster();
    }

    public void activateWithMsg(final Protocol protocol, final SawimException e) {
        if (e.getErrorCode() == 111) {
            showLoginWindow();
        }
        SawimApplication.getInstance().getUiHandler().post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SawimApplication.getContext(), protocol.getUserId() + "\n" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
        updateRoster();
    }

    public void showLoginWindow() {
        Intent intent = new Intent(SawimApplication.getContext(), AccountsListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setAction(SawimActivity.ACTION_SHOW_LOGIN_WINDOW);
        SawimApplication.getContext().startActivity(intent);
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
        Protocol p = getProtocol();
        SawimApplication.getInstance().setStatus(p, StatusInfo.STATUS_ONLINE, "");
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

    public final void markMessages(Contact contact) {
        SawimApplication.getInstance().updateAppIcon();
        if (getUpdateChatListener() != null)
            getUpdateChatListener().updateChat();
    }

    public void updateConnectionStatus() {
        SawimApplication.getInstance().updateAppIcon();
    }

    public final Contact getCurrentContact() {
        currentContact = getProtocol().getItemByUID(Options.getString(Options.OPTIONS_CURR_CONTACT));
        return currentContact;
    }

    public final void setCurrentContact(Contact contact) {
        Options.setString(Options.OPTIONS_CURR_CONTACT, contact.getUserId());
        Options.safeSave();
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

    public void updateOptions() {
        useGroups = Options.getBoolean(JLocale.getString(R.string.pref_user_groups));
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
    public static final int MENU_FIND_CONF = 1;
    public static final int MENU_STATUS = 2;
    public static final int MENU_XSTATUS = 3;
    public static final int MENU_DISCO = 16;
    public static final int MENU_NOTES = 17;
    public static final int MENU_GROUPS = 18;
    public static final int MENU_MYSELF = 19;

    public MyMenu getMenu(final Protocol p) {
        final MyMenu menu = new MyMenu();
        menu.add(p.isConnected() || p.isConnecting() ? R.string.disconnect : R.string.connect, MENU_CONNECT);
        menu.add(R.string.find_conference, MENU_FIND_CONF);
        menu.add(R.string.status, MENU_STATUS);
        if (p.getXStatusInfo() != null)
            menu.add(R.string.xstatus, MENU_XSTATUS);

        if (p.isConnected()) {
            if (p instanceof Xmpp) {
                if (((Xmpp) p).hasS2S()) {
                    menu.add(R.string.service_discovery, MENU_DISCO);
                }
            }
            menu.add(R.string.manage_contact_list, MENU_GROUPS);

                if (p instanceof Xmpp) {
                    menu.add(R.string.notes, MENU_NOTES);
                }
                if (p.hasVCardEditor())
                    menu.add(R.string.myself, MENU_MYSELF);
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
                //if (checkPassword(p)) {
                SawimApplication.getInstance().setStatus(p, (p.isConnected() || p.isConnecting())
                        ? StatusInfo.STATUS_OFFLINE : StatusInfo.STATUS_ONLINE, "");
                //}
                return true;
            case MENU_FIND_CONF:
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SearchConferenceFragment(), SearchConferenceFragment.TAG)
                        .addToBackStack(null)
                        .commit();
                return true;
            case RosterHelper.MENU_STATUS:
                new StatusesFragment().init(p, StatusesFragment.ADAPTER_STATUS).show(activity.getSupportFragmentManager(), "change-status");
                return true;
            case RosterHelper.MENU_XSTATUS:
                new XStatusesFragment().init(p).show(activity.getSupportFragmentManager(), "change-xstatus");
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

    public Protocol getProtocol(Profile acc) {
        return getProtocol();
    }
    public Protocol getProtocol(int i) {
        return getProtocol();
    }
}
