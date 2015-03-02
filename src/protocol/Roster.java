package protocol;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by admin on 09.01.14.
 */
public class Roster {
    private List<Contact> contacts = new CopyOnWriteArrayList<>();
    private List<Group> groups = new CopyOnWriteArrayList<>();

    public Roster(List<Group> groups, List<Contact> contacts) {
        this.groups = groups;
        this.contacts = contacts;
    }

    public Roster() {
        this(new CopyOnWriteArrayList<Group>(), new CopyOnWriteArrayList<Contact>());
    }

    public final List<Contact> getContactItems() {
        return contacts;
    }

    public final List<Group> getGroupItems() {
        return groups;
    }

    public final Contact getItemByUID(String uid) {
        for (int i = contacts.size() - 1; i >= 0; --i) {
            Contact contact = contacts.get(i);
            if (contact.getUserId().equals(uid)) {
                return contact;
            }
        }
        return null;
    }

    public final Group getGroupById(int id) {
        for (int i = groups.size() - 1; 0 <= i; --i) {
            Group group = groups.get(i);
            if (group.getGroupId() == id) {
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
            Group group = groups.get(i);
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
