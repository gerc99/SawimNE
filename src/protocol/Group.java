package protocol;

import ru.sawim.comm.Util;
import ru.sawim.roster.TreeBranch;

import java.util.Vector;


public class Group extends TreeBranch {

    private String name;
    private final Vector<Contact> contacts = new Vector<Contact>();
    private byte mode;
    private String caption = null;
    private int groupId;

    public static final int NOT_IN_GROUP = -1;

    public static final byte MODE_NONE = 0x00;
    public static final byte MODE_REMOVABLE = 0x01;
    public static final byte MODE_EDITABLE = 0x02;
    public static final byte MODE_NEW_CONTACTS = 0x04;
    public static final byte MODE_FULL_ACCESS = 0x0F;

    public static final byte MODE_TOP = 0x10;
    public static final byte MODE_BOTTOM = 0x20;
    public static final byte MODE_BOTTOM2 = 0x40;

    public Group(String name) {
        setName(name);
        caption = name;
        setMode(Group.MODE_FULL_ACCESS);
    }

    public final String getName() {
        return this.name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final void setMode(int newMode) {
        mode = (byte) newMode;
    }

    public final byte getMode() {
        return mode;
    }

    public final boolean hasMode(byte type) {
        return (mode & type) != 0;
    }

    public int getNodeWeight() {
        if (hasMode(MODE_TOP)) return -4;
        if (hasMode(MODE_BOTTOM)) return -2;
        if (hasMode(MODE_BOTTOM2)) return -1;
        return -3;
    }

    public final int getId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public boolean isEmpty() {
        return (0 == contacts.size());
    }

    public final Vector<Contact> getContacts() {
        return contacts;
    }

    public final void updateGroupData() {
        int onlineCount = 0;
        for (int i = 0; i < contacts.size(); ++i) {
            Contact item = contacts.elementAt(i);
            if (item.isOnline()) {
                onlineCount++;
            }
        }
        updateGroupData(contacts.size(), onlineCount);
    }

    public final void updateGroupData(int total, int onlineCount) {
        caption = getName();
        if (0 < total) {
            caption += " (" + onlineCount + "/" + total + ")";
        }
    }

    public final String getText() {
        return caption == null ? "" : caption;
    }

    @Override
    public byte getType() {
        return GROUP;
    }

    public final void sort() {
        if (isExpanded()) {
            Util.sort(contacts);
        }
    }
}