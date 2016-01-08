package protocol;

import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 09.01.14.
 */
public class Roster {
    private ConcurrentHashMap<String, Contact> contacts = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Integer, Group> groups = new ConcurrentHashMap<>();

    public Roster(ConcurrentHashMap<Integer, Group> groups, ConcurrentHashMap<String, Contact> contacts) {
        this.groups = groups;
        this.contacts = contacts;
    }

    public Roster() {
        this(new ConcurrentHashMap<Integer, Group>(), new ConcurrentHashMap<String, Contact>());
    }

    public final ConcurrentHashMap<String, Contact> getContactItems() {
        return contacts;
    }

    public final ConcurrentHashMap<Integer, Group> getGroupItems() {
        return groups;
    }

    public final Contact getItemByUID(String uid) {
        return contacts.get(uid);
    }

    public final Group getGroupById(int id) {
        return groups.get(id);
    }

    public final Group getGroup(Contact contact) {
        return getGroupById(contact.getGroupId());
    }

    public final Group getGroup(String name) {
        Enumeration<Group> e = groups.elements();
        while (e.hasMoreElements()) {
            Group group = e.nextElement();
            if (group.getName().equals(name)) {
                return group;
            }
        }
        return null;
    }

    public boolean hasContact(Contact contact) {
        if (contact == null) return false;
        return getContactItems().containsKey(contact.getUserId());
    }

    public void setNull() {
        contacts = null;
        groups = null;
    }
}
