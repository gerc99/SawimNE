package protocol;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by admin on 09.01.14.
 */
public class Roster {
    private ConcurrentHashMap<String, Contact> contacts = new ConcurrentHashMap<>();
    private List<Group> groups = new CopyOnWriteArrayList<>();

    public Roster(List<Group> groups, ConcurrentHashMap<String, Contact> contacts) {
        this.groups = groups;
        this.contacts = contacts;
    }

    public Roster() {
        this(new CopyOnWriteArrayList<Group>(), new ConcurrentHashMap<String, Contact>());
    }

    public final ConcurrentHashMap<String, Contact> getContactItems() {
        return contacts;
    }

    public final List<Group> getGroupItems() {
        return groups;
    }

    public final Contact getItemByUID(String uid) {
        return contacts.get(uid);
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
        return getContactItems().contains(contact);
    }

    public void setNull() {
        contacts = null;
        groups = null;
    }
}
