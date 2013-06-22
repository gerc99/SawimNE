package DrawControls.tree;

import sawim.Options;
import sawim.comm.Util;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;

import java.util.List;
import java.util.Vector;

public class ContactListModel {
    private final Protocol[] protocolList;
    private int count = 0;

    protected TreeNode selectedItem = null;

    protected boolean useGroups;
    protected boolean hideOffline;

    public ContactListModel(int maxCount) {
        protocolList = new Protocol[maxCount];
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

    void updateOptions(VirtualContactList vcl) {
        boolean groups = useGroups;
        useGroups = Options.getBoolean(Options.OPTION_USER_GROUPS);
        hideOffline = /*Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)*/vcl.getCurrPage() == 1;
        if (groups && !useGroups) {
            sort();
        }
    }

    Protocol getContactProtocol(Contact c) {
        for (int i = 0; i < getProtocolCount(); ++i) {
            if (getProtocol(i).inContactList(c)) {
                return getProtocol(i);
            }
        }
        return null;
    }

    protected final Protocol getProtocol(Group g) {
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

    public void sort() {
        for (int i = 0; i < getProtocolCount(); ++i) {
            Util.sort(getProtocol(i).getSortedContacts());
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

    public TreeBranch getGroupNode(Group group) {
        return group;
    }
}

