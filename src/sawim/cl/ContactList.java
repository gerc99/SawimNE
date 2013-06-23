

package sawim.cl;

import DrawControls.tree.ContactListModel;
import DrawControls.tree.TreeBranch;
import DrawControls.tree.VirtualContactList;
import android.util.Log;
import android.widget.Toast;
import protocol.Contact;
import protocol.Profile;
import protocol.Protocol;
import protocol.StatusView;
import protocol.icq.Icq;
import protocol.jabber.Jabber;
import protocol.mrim.Mrim;
import ru.sawim.General;
import ru.sawim.activities.SawimActivity;
import sawim.FileTransfer;
import sawim.Options;
import sawim.modules.AutoAbsence;

import java.util.Vector;

public final class ContactList {
    final static public int SORT_BY_STATUS = 0;
    final static public int SORT_BY_ONLINE = 1;
    final static public int SORT_BY_NAME = 2;
    private static final ContactList instance = new ContactList();
    private final StatusView statusView = new StatusView();
    private VirtualContactList contactList;
    private Contact currentContact;
    private Vector transfers = new Vector();

    public static ContactList getInstance() {
        return instance;
    }

    public void initUI() {
        contactList = new VirtualContactList();
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
        int count = contactList.getModel().getProtocolCount();
        Protocol[] protocols = new Protocol[count];
        for (int i = 0; i < count; ++i) {
            protocols[i] = contactList.getModel().getProtocol(i);
        }
        contactList.getModel().removeAllProtocols();
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
                    contactList.getModel().addProtocol(protocol);
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

    public void initAccounts() {
        int count = Math.max(1, Options.getAccountCount());
        for (int i = 0; i < count; ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                addProtocol(p, false);
            }
        }
    }

    public void loadAccounts() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol protocol = contactList.getModel().getProtocol(i);
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
        }
        if (null == protocol) {
            return;
        }
        protocol.setProfile(account);
        protocol.init();
        if (load) {
            protocol.safeLoad();
        }
        contactList.getModel().addProtocol(protocol);
    }

    public Protocol getProtocol(Profile profile) {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.getProfile() == profile) {
                return p;
            }
        }
        return null;
    }

    public Protocol getProtocol(String account) {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.getUserId().equals(account)) {
                return p;
            }
        }
        return null;
    }

    public Protocol[] getProtocols() {
        ContactListModel model = contactList.getModel();
        Protocol[] all = new Protocol[model.getProtocolCount()];
        for (int i = 0; i < all.length; ++i) {
            all[i] = model.getProtocol(i);
        }
        return all;
    }

    public Protocol getProtocol(int i) {
        ContactListModel model = contactList.getModel();
        return model.getProtocol(i);
    }

    public Protocol getCurrProtocol() {
        return contactList.getModel().getProtocol(contactList.getCurrProtocol());
    }

    public Protocol getProtocol(Contact c) {
        ContactListModel model = contactList.getModel();
        for (int i = 0; i < model.getProtocolCount(); ++i) {
            if (model.getProtocol(i).inContactList(c)) {
                return model.getProtocol(i);
            }
        }
        return null;
    }

    public void activate() {
        contactList.update();
    }

    public void _setActiveContact(Contact c) {
        if (null != c) {
            contactList.setActiveContact(c);
        }
        contactList.getModel().setAlwaysVisibleNode(c);
    }

    public void activate(Contact c) {
        _setActiveContact(c);
        activate();
    }

    public void activateWithMsg(String message) {
        activate();
        Toast.makeText(SawimActivity.getInstance(), message, Toast.LENGTH_LONG);
    }

    public void autoConnect() {
        if (!SawimActivity.getInstance().isNetworkAvailable()) {
            return;
        }
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
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
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.isConnected() && !p.isConnecting()) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnecting() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.isConnecting()) {
                return true;
            }
        }
        return false;
    }

    public boolean disconnect() {
        boolean disconnecting = false;
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            if (p.isConnected()) {
                p.disconnect(false);
                disconnecting = true;
            }
        }
        return disconnecting;
    }

    public void safeSave() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            p.safeSave();
        }
    }

    public void collapseAll() {
        int count = contactList.getModel().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = contactList.getModel().getProtocol(i);
            Vector groups = p.getGroupItems();
            for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
                ((TreeBranch) groups.elementAt(groupIndex)).setExpandFlag(false);
            }
            p.getNotInListGroup().setExpandFlag(false);
        }
        contactList.update();
    }

    public VirtualContactList getManager() {
        return contactList;
    }

    public void setActiveContact(Contact contact) {
        contactList.setActiveContact(contact);
    }

    public final void receivedMessage(Contact contact) {
        markMessages(contact);
    }

    public final void markMessages(Contact contact) {
        if (General.getInstance().getUpdateChatListener() != null)
            General.getInstance().getUpdateChatListener().updateChat();
        SawimActivity.getInstance().updateAppIcon();
    }

    public void updateConnectionStatus() {
        SawimActivity.getInstance().updateAppIcon();
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
        if (0 < contactListSaveDelay) {
            contactListSaveDelay--;
            if (0 == contactListSaveDelay) {
                int count = contactList.getModel().getProtocolCount();
                for (int i = 0; i < count; ++i) {
                    Protocol p = contactList.getModel().getProtocol(i);
                    p.safeSave();
                }
            }
        }
    }
}