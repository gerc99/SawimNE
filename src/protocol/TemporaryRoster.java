package protocol;

import protocol.mrim.MrimPhoneContact;
import protocol.xmpp.XmppServiceContact;

import java.util.Vector;


public class TemporaryRoster {
    private Protocol protocol;
    private Vector oldGroups;
    private Vector oldContacts;
    private Contact[] existContacts;
    private Vector groups = new Vector();
    private Vector contacts = new Vector();

    public TemporaryRoster(Protocol protocol) {
        this.protocol = protocol;
        this.oldGroups = protocol.getGroupItems();
        this.oldContacts = protocol.getContactItems();
        existContacts = new Contact[oldContacts.size()];
        oldContacts.copyInto(existContacts);
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

    private Group getGroup(Vector list, String name) {
        for (int j = list.size() - 1; 0 <= j; --j) {
            Group g = (Group) list.elementAt(j);
            if (name.equals(g.getName())) {
                return g;
            }
        }
        return null;
    }

    public void useOld() {
        groups = oldGroups;
        contacts = oldContacts;
        oldGroups = new Vector();
        oldContacts = new Vector();
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

    public final Vector mergeContacts() {
        Vector newContacts = contacts;
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
                newContacts.addElement(o);
            }
        }
        return newContacts;
    }

    public void addGroup(Group g) {
        groups.addElement(g);
    }

    public void addContact(Contact c) {
        c.setTempFlag(false);
        contacts.addElement(c);
    }

    public Vector getGroups() {
        return groups;
    }

    public Group getGroupById(int groupId) {
        Group group;
        for (int i = oldGroups.size() - 1; 0 <= i; --i) {
            group = (Group) oldGroups.elementAt(i);
            if (group.getId() == groupId) {
                return group;
            }
        }
        return null;
    }
}