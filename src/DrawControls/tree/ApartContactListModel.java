package DrawControls.tree;

import protocol.Protocol;

import java.util.List;


public class ApartContactListModel extends ContactListModel {

    public ApartContactListModel(int maxCount) {
        super(maxCount);
    }

    public void buildFlatItems(int currProtocol, List<TreeNode> items) {
        Protocol p = getProtocol(currProtocol);
        synchronized (p.getRosterLockObject()) {
            if (useGroups) {
                rebuildFlatItemsWG(p, items);
            } else {
                rebuildFlatItemsWOG(p, items);
            }
        }
    }
}

