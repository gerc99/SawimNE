

package DrawControls.tree;

import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.DebugLog;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.XStatusInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import android.os.Handler;


public final class VirtualContactList {

    //public List<TreeNode> drawItems = new ArrayList<TreeNode>();
    private ContactListListener clListener;
    private boolean useGroups;
    private int useAccounts;
    private ContactListModel model;
    private int currentProtocol = 0;
    private TreeNode currentNode = null;

    //private Vector[] listOfContactList = new Vector[]{new Vector(), new Vector()};
    private int visibleListIndex = 0;
    private int currPage;

    public VirtualContactList() {
        //if (Options.getInt(Options.OPTION_USER_ACCOUNTS) == 1) {
        //    model = new ContactListModel(10);
        //} else if (Options.getInt(Options.OPTION_USER_ACCOUNTS) == 2) {
            model = new ApartContactListModel(10);
        //} else {
        //    model = new AlloyContactListModel(10);
        //}
        updateOption();
    }

    public ContactListModel getModel() {
        return model;
    }

    public void setCLListener(ContactListListener listener) {
        clListener = listener;
    }

    //protected final int getSize() {
    //    return drawItems.size();
    //}

    public void update(TreeNode node) {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public final void update() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public void updateTree() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateRoster();
    }

    public void updateBarProtocols() {
        if (onUpdateRoster != null)
            onUpdateRoster.updateBarProtocols();
    }

    OnUpdateRoster onUpdateRoster;

    public void setOnUpdateRoster(OnUpdateRoster l) {
        onUpdateRoster = l;
    }

    public void setCurrPage(int currPage) {
        this.currPage = currPage;
    }

    public int getCurrPage() {
        return currPage;
    }

    public void putIntoQueue(Group group) {
        if (onUpdateRoster != null)
            onUpdateRoster.putIntoQueue(group);
    }

    public interface OnUpdateRoster {
        void updateRoster();
        void updateBarProtocols();
        void putIntoQueue(Group g);
    }

    private Protocol getProtocol(Group g) {
        for (int i = 0; i < getModel().getProtocolCount(); ++i) {
            Protocol p = getModel().getProtocol(i);
            if (-1 != Util.getIndex(p.getGroupItems(), g)) {
                return p;
            }
        }
        return getModel().getProtocol(0);
    }

    /*public TreeNode getDrawItem(int index) {
        return drawItems.get(index);
    }

    private TreeNode getCurrentNode() {
        return getSafeNode(0);
    }

    public TreeNode getSafeNode(int index) {
        if ((index < drawItems.size()) && (index >= 0)) {
            return getDrawItem(index);
        }
        return null;
    }*/

    public void updateOption() {
        useGroups = /*Options.getBoolean(Options.OPTION_USER_GROUPS)*/getCurrPage() == 0;

        int oldUseAccounts = useAccounts;
        useAccounts = Options.getInt(Options.OPTION_USER_ACCOUNTS);
        if (oldUseAccounts != useAccounts) {
            ContactListModel oldModel = model;
                model = new ApartContactListModel(10);
            for (int i = 0; i < oldModel.getProtocolCount(); ++i) {
                model.addProtocol(oldModel.getProtocol(i));
            }
        }
        model.updateOptions(this);
    }

    private void expandNodePath(TreeNode node) {
        if ((node instanceof Contact) && useGroups) {
            Contact c = (Contact) node;
            Protocol p = model.getContactProtocol(c);
            if (null != p) {
                Group group = p.getGroupById(c.getGroupId());
                if (null == group) {
                    group = p.getNotInListGroup();
                }
                model.getGroupNode(group).setExpandFlag(true);
            }
        }
    }
    private void setCurrentNode(TreeNode node) {
        if (null != node) {
            currentNode = node;
        }
    }

    public void setExpandFlag(TreeBranch node, boolean value) {
        //setCurrentNode(getCurrentNode());
        node.setExpandFlag(value);
        updateTree();
    }

    private void itemSelected(TreeNode item) {
        if (null == item) {
            return;
        }
        if (item instanceof Contact) {
            ((Contact) item).activate(model.getContactProtocol((Contact) item));

        } else if (item instanceof Group) {
            Group group = (Group) item;
            setExpandFlag(group, !group.isExpanded());

        } else if (item instanceof TreeBranch) {
            TreeBranch root = (TreeBranch) item;
            setExpandFlag(root, !root.isExpanded());
        }
    }

    public final void setActiveContact(Contact cItem) {
        setCurrentNode(cItem);
        updateTree();
    }

    public void setCurrProtocol(int currentProtocol) {
        this.currentProtocol = currentProtocol;
    }

    public int getCurrProtocol() {
        return currentProtocol;
    }

    /*private Protocol getProtocol(TreeNode node) {
        if (Options.getInt(Options.OPTION_USER_ACCOUNTS) == 2) {
            Protocol p = model.getProtocol(getCurrProtocol());
            if (model.getProtocolCount() == 0 || null == p) {
                p = model.getProtocol(0);
            }
            return p;
        }
        if (node instanceof ProtocolBranch) {
            return ((ProtocolBranch) node).getProtocol();
        }
        if (node instanceof Contact) {
            return model.getContactProtocol((Contact) node);
        }

        Protocol last = null;
        for (int i = 0; i < drawItems.size(); ++i) {
            if (drawItems.get(i) instanceof ProtocolBranch) {
                last = ((ProtocolBranch) drawItems.get(i)).getProtocol();

            } else if (drawItems.get(i) == node) {
                return last;
            }
        }

        return model.getProtocol(0);
    }*/

    public final Protocol getCurrentProtocol() {
        //if (Options.getInt(Options.OPTION_USER_ACCOUNTS) == 2) {
            Protocol p = model.getProtocol(getCurrProtocol());
            if (model.getProtocolCount() == 0 || null == p) {
                p = model.getProtocol(0);
            }
            return p;
        /*} else {
            Protocol protocol = getProtocol(getCurrentNode());
            if ((null != protocol) && (null == protocol.getProfile())) {
                protocol = model.getProtocol(0);
            }
            return protocol;
        }*/
    }

    public String getStatusMessage(Contact contact) {
        String message;
        //Protocol protocol = getProtocol(contact);
        Protocol protocol = model.getContactProtocol(contact);
        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            message = contact.getXStatusText();
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
            message = protocol.getXStatusInfo().getName(contact.getXStatusIndex());
            if (!StringConvertor.isEmpty(message)) {
                return message;
            }
        }

        message = contact.getStatusText();
        if (!StringConvertor.isEmpty(message)) {
            return message;
        }
        return protocol.getStatusInfo().getName(contact.getStatusIndex());
    }
}

