package DrawControls.tree.alloy;

import DrawControls.tree.ContactListModel;
import DrawControls.tree.TreeBranch;
import sawim.comm.Util;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.SawimApplication;
import ru.sawim.R;

import java.util.Vector;
/*

public class AlloyContactListModel extends ContactListModel {
    private Vector groups = new Vector();
    private Vector contacts = new Vector();
    private Vector prevGroups = new Vector();
    private Vector prevContacts = new Vector();
    private Group notInListGroup;

    public AlloyContactListModel(int maxCount) {
        super(maxCount);

        notInListGroup = new Group(SawimApplication.getContext().getString(R.string.group_not_in_list));
        notInListGroup.setMode(Group.MODE_NONE);
        notInListGroup.setGroupId(Group.NOT_IN_GROUP);
    }

    public void buildFlatItems(Vector items) {

        prevContacts = contacts;
        prevGroups = groups;
        groups = new Vector();
        contacts = new Vector();
        notInListGroup.getContacts().removeAllElements();
        for (int i = 0; i < getProtocolCount(); ++i) {
            putProtocol(getProtocol(i));
        }

        if (useGroups) {
            for (int i = 0; i < groups.size(); ++i) {
                ((Group) groups.elementAt(i)).updateGroupData();
                ((Group) groups.elementAt(i)).sort();
            }
            Util.sort(groups);
        } else {
            Util.sort(contacts);
        }

        if (useGroups) {
            rebuildFlatItemsWG(items);
        } else {
            rebuildFlatItemsWOG(items);
        }
    }


    private void rebuildFlatItemsWG(Vector drawItems) {
        Vector contacts;
        Group g;
        Contact c;
        int contactCounter;
        boolean all = !hideOffline;
        Vector groups = this.groups;
        for (int groupIndex = 0; groupIndex < groups.size(); ++groupIndex) {
            g = (Group) groups.elementAt(groupIndex);
            contactCounter = 0;
            drawItems.addElement(g);
            contacts = g.getContacts();
            for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
                c = (Contact) contacts.elementAt(contactIndex);
                if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                    if (g.isExpanded()) {
                        drawItems.addElement(c);
                    }
                    contactCounter++;
                }
            }
            if (hideOffline && (0 == contactCounter)) {
                drawItems.removeElementAt(drawItems.size() - 1);
            }
        }

        g = notInListGroup;
        drawItems.addElement(g);
        contacts = g.getContacts();
        contactCounter = 0;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                if (g.isExpanded()) {
                    drawItems.addElement(c);
                }
                contactCounter++;
            }
        }
        if (0 == contactCounter) {
            drawItems.removeElementAt(drawItems.size() - 1);
        }
    }

    private void rebuildFlatItemsWOG(Vector drawItems) {
        boolean all = !hideOffline;
        Contact c;
        Vector contacts = this.contacts;
        for (int contactIndex = 0; contactIndex < contacts.size(); ++contactIndex) {
            c = (Contact) contacts.elementAt(contactIndex);
            if (all || c.isVisibleInContactList() || (c == selectedItem)) {
                drawItems.addElement(c);
            }
        }
    }

    public void updateGroup(Group g) {
        if (useGroups) {
            Group group = getGroup(g);
            if (null == group) {
                group = createGroup(g);
            }
            group.updateGroupData();
            group.sort();
        } else {
            Util.sort(getProtocol(g).getSortedContacts());
        }
    }


    private void putProtocol(Protocol p) {
        Vector gs = p.getGroupItems();
        for (int i = 0; i < gs.size(); ++i) {
            putGroup((Group) gs.elementAt(i));
        }
        Vector cs = p.getContactItems();
        for (int i = 0; i < cs.size(); ++i) {
            putContact(p, (Contact) cs.elementAt(i));
        }
    }

    private void putGroup(Group g) {
        Group group = getGroup(g.getName());
        if (null == group) {
            group = getGroup(prevGroups, g.getName());
            if (null != group) {
                groups.addElement(group);
                group.getContacts().removeAllElements();
                return;
            }
            group = createGroup(g);
        }
    }

    private Group createGroup(Group g) {
        Group group = new Group(g.getName());
        group.setMode(g.getMode());
        groups.addElement(group);
        return group;
    }

    private Group getGroup(Vector groups, String name) {
        Group g;
        for (int i = 0; i < groups.size(); ++i) {
            g = (Group) groups.elementAt(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    private Group getGroup(Group g) {
        return getGroup(g.getName());
    }

    private Group getGroup(String name) {
        Group g;
        for (int i = 0; i < groups.size(); ++i) {
            g = (Group) groups.elementAt(i);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    private void putContact(Protocol p, Contact contact) {
        if (-1 == Util.getIndex(contacts, contact)) {
            contacts.addElement(contact);
            if (Group.NOT_IN_GROUP != contact.getGroupId()) {
                getGroup(p.getGroupById(contact.getGroupId()).getName()).getContacts().addElement(contact);
            } else {
                notInListGroup.getContacts().addElement(contact);
            }
        }
    }

    public TreeBranch getGroupNode(Group group) {
        return getGroup(group);
    }
}

 */