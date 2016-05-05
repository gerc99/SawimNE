package ru.sawim.roster;

import ru.sawim.comm.Sortable;

public interface TreeNode extends Sortable {
    byte PROTOCOL = 0;
    byte GROUP = 1;
    byte CONTACT = 2;
    byte LAYER = 3;

    byte getType();
    int getGroupId();
}