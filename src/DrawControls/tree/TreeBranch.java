


package DrawControls.tree;


public abstract class TreeBranch implements TreeNode {

    public TreeBranch() {
    }

    private boolean expanded = false;

    public final boolean isExpanded() {
        return expanded;
    }

    public final void setExpandFlag(boolean value) {
        expanded = value;
        sort();
    }

    public void sort() {
    }

    public abstract boolean isEmpty();
}

