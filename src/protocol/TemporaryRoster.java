package protocol;

import protocol.mrim.MrimPhoneContact;
import protocol.xmpp.XmppServiceContact;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;


public class TemporaryRoster {
    private Protocol protocol;
    private List<Group> oldGroups;
    private List<Contact> oldContacts;
    private Contact[] existContacts;
    private List<Group> groups = new CopyOnWriteArrayList<>();
    private List<Contact> contacts = new CopyOnWriteArrayList<>();

    public TemporaryRoster(Protocol protocol) {
        this.protocol = protocol;
        this.oldGroups = protocol.getGroupItems();
        this.oldContacts = protocol.getContactItems();
        existContacts = new Contact[oldContacts.size()];
        System.arraycopy(existContacts, 0, oldContacts.toArray(), 0, oldContacts.size());
    }

    public Contact makeContact(String userId) {
        Contact c;
        for (int i = 0; i < existContacts.length; ++i) {
            c = existContacts[i];
            if ((null != c) && userId.equals(c.getUserId())) {
                existContacts[i] = null;
                return c;
            }
        }
        return protocol.createContact(userId, userId);
    }

    private Group getGroup(List list, String name) {
        for (int j = list.size() - 1; 0 <= j; --j) {
            Group g = (Group) list.get(j);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    public void useOld() {
        groups = oldGroups;
        contacts = oldContacts;
        oldGroups = new CopyOnWriteArrayList<>();
        oldContacts = new CopyOnWriteArrayList<>();
        existContacts = new Contact[0];
    }

    public Group makeGroup(String name) {
        if (null == name) {
            return null;
        }
        Group g = getGroup(oldGroups, name);
        return (null == g) ? protocol.createGroup(name) : g;
    }

    public Group getGroup(String name) {
        return (null == name) ? null : getGroup(groups, name);
    }

    public Group getOrCreateGroup(String name) {
        if (null == name) {
            return null;
        }
        Group g = getGroup(name);
        if (null == g) {
            g = makeGroup(name);
            addGroup(g);
        }
        return g;
    }

    public final List<Contact> mergeContacts() {
        List<Contact> newContacts = contacts;
        boolean sync = protocol.isReconnect();
//        sync |= Options.getBoolean(Options.OPTION_SAVE_TEMP_CONTACT);
        if (sync) {
            Group g;
            Contact o;
            for (int i = existContacts.length - 1; 0 <= i; --i) {
                o = existContacts[i];
                if (null == o) {
                    continue;
                }
                if (o instanceof MrimPhoneContact) {
                    continue;
                }
                g = null;
                if (o instanceof XmppServiceContact) {
                    if (o.isSingleUserContact()) {
                        continue;
                    }
                    g = getOrCreateGroup(o.getDefaultGroupName());
                }
                o.setGroup(g);
                o.setTempFlag(true);
                o.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                newContacts.add(o);
            }
        }
        return newContacts;
    }

    public void addGroup(Group g) {
        groups.add(g);
    }

    public void addContact(Contact c) {
        c.setTempFlag(false);
        contacts.add(c);
    }

    public List<Group> getGroups() {
        return groups;
    }

    public Group getGroupById(int groupId) {
        Group group;
        for (int i = oldGroups.size() - 1; 0 <= i; --i) {
            group = oldGroups.get(i);
            if (group.getGroupId() == groupId) {
                return group;
            }
        }
        return null;
    }
}