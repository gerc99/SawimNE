package DrawControls.tree;


import DrawControls.icons.Icon;

public abstract class TreeNode {
    public static final int GROUP = 0;
    public static final int CONTACT = 1;

    protected abstract String getText();

    public boolean isGroup() {
        return getType() == GROUP;
    }

    public boolean isContact() {
        return getType() == CONTACT;
    }

    protected abstract Icon getLeftIcon();

    protected abstract int getType();
}