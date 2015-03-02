package ru.sawim.roster;

import ru.sawim.comm.Sortable;

public interface TreeNode extends Sortable {
    public static final byte PROTOCOL = 0;
    public static final byte GROUP = 1;
    public static final byte CONTACT = 2;
    public static final byte LAYER = 3;

    public byte getType();
    public int getGroupId();
}