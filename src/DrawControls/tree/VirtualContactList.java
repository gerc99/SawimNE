package DrawControls.tree;

import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.comm.Util;

import java.util.List;
import java.util.Vector;

public final class VirtualContactList {

    private OnUpdateRoster onUpdateRoster;
    private int currentProtocol = 0;
    private int currPage = 0;

    private final Protocol[] protocolList;
    private int count = 0;

    private TreeNode selectedItem = null;

    private boolean useGroups;
    private boolean hideOffline;

    public VirtualContactList() {
        protocolList = new Protocol[10];
    }

    public void update(TreeNode node) {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public final void update() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public void updateBarProtocols() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateBarProtocols();
    }

    public void setOnUpdateRoster(OnUpdateRoster l) {
        onUpdateRoster = l;
    }

    public void setCurrProtocol(int currentProtocol) {
        this.currentProtocol = currentProtocol;
    }

    public int getCurrProtocol() {
        return currentProtocol;
    }

    public final Protocol getCurrentProtocol() {
        Protocol p = getProtocol(currentProtocol);
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

    public void updateOptions(VirtualContactList vcl, int currProtocol) {
        boolean groups = useGroups;
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS);
        hideOffline = /*Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)*/vcl.getCurrPage() == 1;
        if (groups && !useGroups) {
            Util.sort(getProtocol(currProtocol).getSortedContacts());
        }
    }

    /*public Protocol getContactProtocol(Contact c) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).inContactList(c)) {
                return getProtocol(i);
            }
        }
        return null;
    }*/

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

    public void buildFlatItems(int currProtocol, List<TreeNode> items) {
        Protocol p = getProtocol(currProtocol);
        if (p == null) return;
        synchronized (p.getRosterLockObject()) {
            if (useGroups) {
                rebuildFlatItemsWG(p, items);
            } else {
                rebuildFlatItemsWOG(p, items);
            }
        }
    }

    private void rebuildFlatItemsWG(Protocol p, List<TreeNode> drawItems) {
        Vector contacts;
        Group g;
        Contact c;
        int contactCounter;
        boolean all = !hideOffline;
        Vector groups = p.getSortedGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            g = (Group) groups.elementAt(groupIndex);
            contactCounter = 0;
            drawItems.add(g);
            contacts = g.getContacts();
            for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
                c = (Contact) contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                    if (g.isExpanded()) {
                        drawItems.add(c);
                    }
                    contactCounter++;
                }
            }
            if (hideOffline && (0 == contactCounter)) {
                drawItems.remove(drawItems.size() - 1);
            }
        }

        g = p.getNotInListGroup();
        drawItems.add(g);
        contacts = g.getContacts();
        contactCounter = 0;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                if (g.isExpanded()) {
                    drawItems.add(c);
                }
                contactCounter++;
            }
        }
        if (0 == contactCounter) {
            drawItems.remove(drawItems.size() - 1);
        }
    }

    private void rebuildFlatItemsWOG(Protocol p, List<TreeNode> drawItems) {
        boolean all = !hideOffline;
        Contact c;
        Vector contacts = p.getSortedContacts();
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.add(c);
            }
        }
    }

    public void updateGroup(Group group) {
        if (useGroups) {
            group.updateGroupData();
            group.sort();
        } else {
            Util.sort(getProtocol(group).getSortedContacts());
        }
    }

    public void removeFromGroup(Group g, Contact c) {
        if (g.getContacts().removeElement(c)) {
            g.updateGroupData();
        }
    }

    public void addToGroup(Group group, Contact contact) {
        group.getContacts().addElement(contact);
    }

    public void updateGroupData(Group group) {
        group.updateGroupData();
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

    public void putIntoQueue(Group group) {
        if (onUpdateRoster != null)
            onUpdateRoster.putIntoQueue(group);
    }

    /*private Protocol getProtocol(Group g) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            Protocol p = getProtocol(i);
            if (-1 != Util.getIndex(p.getGroupItems(), g)) {
                return p;
            }
        }
        return getProtocol(0);
    }*/

    public final void setActiveContact(Contact cItem) {
        if (onUpdateRoster != null) {
            onUpdateRoster.setCurrentNode(cItem);
            onUpdateRoster.updateRoster();
        }
    }

    public String getStatusMessage(Contact contact) {
        String message;
        Protocol protocol = /*getContactProtocol(contact)*/getCurrentProtocol();
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
        String status = protocol.getStatusInfo().getName(contact.getStatusIndex());
        return (status == null) ? "" : status;
    }

    public interface OnUpdateRoster {
        void updateRoster();

        void updateBarProtocols();

        void putIntoQueue(Group g);

        void setCurrentNode(TreeNode cItem);
    }
}