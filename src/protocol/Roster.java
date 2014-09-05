package protocol;

import java.util.Vector;

/**
 * Created by admin on 09.01.14.
 */
public class Roster {
    private Vector<Contact> contacts = new Vector<Contact>();
    private Vector<Group> groups = new Vector<Group>();

    public Roster(Vector<Group> groups, Vector<Contact> contacts) {
        this.groups = groups;
        this.contacts = contacts;
    }

    public Roster() {
        this(new Vector<Group>(), new Vector<Contact>());
    }

    public final Vector<Contact> getContactItems() {
        return contacts;
    }

    public final Vector<Group> getGroupItems() {
        return groups;
    }

    public final Contact getItemByUID(String uid) {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = contacts.elementAt(i);
            if (contact.getUserId().equals(uid)) {
                return contact;
            }
        }
        return null;
    }

    public final Group getGroupById(int id) {
        for (int i = groups.size() - 1; 0 <= i; --i) {
            Group group = groups.elementAt(i);
            if (group.getId() == id) {
                return group;
            }
        }
        return null;
    }

    public final Group getGroup(Contact contact) {
        return getGroupById(contact.getGroupId());
    }

    public final Group getGroup(String name) {
        for (int i = groups.size() - 1; 0 <= i; --i) {
            Group group = groups.elementAt(i);
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    public boolean hasContact(Contact contact) {
        return -1 != contacts.indexOf(contact);
    }

    public void setNull() {
        contacts = null;
        groups = null;
    }
}
