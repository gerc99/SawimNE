package sawim.forms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.*;
import android.widget.BaseAdapter;
import android.widget.TextView;
import ru.sawim.SawimApplication;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.search.Search;
import sawim.ui.TextBoxListener;
import sawim.util.JLocale;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.view.TextBoxView;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public final class ManageContactListForm implements FormListener, TextBoxListener {
    private static final int ADD_USER     = 20;
    private static final int SEARCH_USER  = 21;
    private static final int ADD_GROUP    = 22;
    private static final int RENAME_GROUP = 23;
    private static final int DEL_GROUP    = 24;

    private static final int GROUP    = 25;
    private static final int GROUP_NEW_NAME = 26;

    private Protocol protocol;
    private Group group;
    private TextBoxView groupName;
    private Contact contact;
    private TextBoxView renameContactTextbox;
    private int action;

    public ManageContactListForm(Protocol protocol) {
        this(protocol, (Group)null);
    }
    public ManageContactListForm(Protocol protocol, Group group) {
        this.protocol = protocol;
        this.group = group;
    }
    public ManageContactListForm(Protocol protocol, Contact contact) {
        this.protocol = protocol;
        this.contact = contact;
    }
    public void showContactRename(FragmentActivity a) {
        renameContactTextbox = new TextBoxView();
        renameContactTextbox.setString(contact.getName());
        renameContactTextbox.setTextBoxListener(this);
        renameContactTextbox.show(a.getSupportFragmentManager(), "rename");
    }
    public void showContactMove(FragmentActivity a) {
        Vector groups = protocol.getGroupItems();
        Group myGroup = protocol.getGroup(contact);
        Vector items = new Vector();
        final ArrayList<Integer> itemsId = new ArrayList<Integer>();
        for (int i = 0; i < groups.size(); ++i) {
            Group g = (Group)groups.elementAt(i);
            if ((myGroup != g) && g.hasMode(Group.MODE_NEW_CONTACTS)) {
                items.add(g.getName());
                itemsId.add(g.getId());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(contact.getName());
        builder.setItems(Util.vectorToArray(items), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                protocol.moveContactTo(contact, protocol.getGroupById(itemsId.get(which)));
                ContactList.getInstance().activate();
            }
        });
        builder.create().show();
    }

    private class MenuItem {
        String nameItem;
        int idItem;
        public void addItem(String name, int id) {
            nameItem = name;
            idItem = id;
        }
    }

    private class ItemWrapper {
        final View item;
        TextView text;

        public ItemWrapper(View item) {
            this.item = item;
        }
    }

    public void showMenu(final FragmentActivity a) {
        final List<MenuItem> menuItems = new ArrayList<MenuItem>();
        MenuItem menuItem;
        boolean canAdd = !protocol.getGroupItems().isEmpty()
                && ((null == group) || group.hasMode(Group.MODE_NEW_CONTACTS));
        if (canAdd) {
            menuItem = new MenuItem();
            menuItem.addItem(SawimApplication.getContext().getString(R.string.add_user), ADD_USER);
            menuItems.add(menuItem);
            if (!(protocol instanceof protocol.jabber.Jabber)) {
                menuItem = new MenuItem();
                menuItem.addItem(SawimApplication.getContext().getString(R.string.search_user), SEARCH_USER);
                menuItems.add(menuItem);
            }
        }
        menuItem = new MenuItem();
        menuItem.addItem(SawimApplication.getContext().getString(R.string.add_group), ADD_GROUP);
        menuItems.add(menuItem);
        if (null != group) {
            if (group.hasMode(Group.MODE_EDITABLE)) {
                menuItem = new MenuItem();
                menuItem.addItem(SawimApplication.getContext().getString(R.string.rename_group), RENAME_GROUP);
                menuItems.add(menuItem);
            }
            if (group.getContacts().isEmpty() && group.hasMode(Group.MODE_REMOVABLE)) {
                menuItem = new MenuItem();
                menuItem.addItem(SawimApplication.getContext().getString(R.string.del_group), DEL_GROUP);
                menuItems.add(menuItem);
            }
        } else {
            menuItem = new MenuItem();
            menuItem.addItem(SawimApplication.getContext().getString(R.string.rename_group), RENAME_GROUP);
            menuItems.add(menuItem);
            menuItem = new MenuItem();
            menuItem.addItem(SawimApplication.getContext().getString(R.string.del_group), DEL_GROUP);
            menuItems.add(menuItem);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setTitle(SawimApplication.getContext().getString(R.string.manage_contact_list));
        builder.setAdapter(new BaseAdapter() {
                               @Override
                               public int getCount() {
                                   return menuItems.size();
                               }

                               @Override
                               public MenuItem getItem(int i) {
                                   return menuItems.get(i);
                               }

                               @Override
                               public long getItemId(int i) {
                                   return i;
                               }

                               @Override
                               public View getView(int i, View convertView, ViewGroup viewGroup) {
                                   View row = convertView;
                                   ItemWrapper wr;
                                   if (row == null) {
                                       LayoutInflater inf = (LayoutInflater.from(a));
                                       row = inf.inflate(R.layout.menu_item, null);
                                       wr = new ItemWrapper(row);
                                       row.setTag(wr);
                                   } else {
                                       wr = (ItemWrapper) row.getTag();
                                   }
                                   wr.text = (TextView) row.findViewById(R.id.menuTextView);
                                   wr.text.setText(getItem(i).nameItem);
                                   return row;
                               }
                           }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                select(a, menuItems.get(which).idItem);
            }
        });
        builder.create().show();
    }

    private void select(FragmentActivity a, int cmd) {
        action = cmd;
        switch (cmd) {
            case ADD_USER:
                Search search = protocol.getSearchForm();
                search.putToGroup(group);
                search.show("");
                break;

            case SEARCH_USER:
                Search searchUser = protocol.getSearchForm();
                searchUser.putToGroup(group);
                searchUser.show();
                break;

            case ADD_GROUP:
                showTextBox(a, "add_group", null);
                break;

            case RENAME_GROUP:
                if (null == group) {
                    Forms form = Forms.getInstance();
                    form.init("rename_group", this);
                    addGroup(form, getGroups(Group.MODE_EDITABLE));
                    form.addTextField(GROUP_NEW_NAME, "new_group_name", "");
                    form.show();
                } else {
                    showTextBox(a, "rename_group", group.getName());
                }
                break;

            case DEL_GROUP:
                if (null == group) {
                    Forms form = Forms.getInstance();
                    form.init("del_group", this);
                    addGroup(form, getGroups(Group.MODE_REMOVABLE));
                    form.show();
                } else {
                    protocol.removeGroup(group);
                    //ContactList.getInstance().activate();
                }
                break;
        }
    }

    private Vector getGroups(byte mode) {
        Vector all = protocol.getGroupItems();
        Vector groups = new Vector();
        for (int i = 0; i < all.size(); ++i) {
            Group g = (Group)all.elementAt(i);
            if (g.hasMode(mode)) {
                if ((Group.MODE_REMOVABLE == mode) && !g.isEmpty()) continue;
                groups.addElement(g);
            }
        }
        return groups;
    }
    private void addGroup(Forms form, Vector groups) {
        if (!groups.isEmpty()) {
            String[] list = new String[groups.size()];
            int def = 0;
            for (int i = 0; i < groups.size(); ++i) {
                Group g = (Group) groups.elementAt(i);
                list[i] = g.getName();
                if (g == group) {
                    def = i;
                }
            }
            form.addSelector(GROUP, "group", list, def);
        } else {
            form.addString(JLocale.getString("no_groups_available"));
        }
    }
    
    private void showTextBox(FragmentActivity a, String caption, String text) {
        groupName = new TextBoxView();
        groupName.setCaption(JLocale.getString(caption));
        groupName.setString(text);
        groupName.setTextBoxListener(this);
        groupName.show(a.getSupportFragmentManager(), "group_name");
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        if (!ok) {
            return;
        }
        if (null != contact) {
            if (renameContactTextbox == box) {
                protocol.renameContact(contact, renameContactTextbox.getString());
                ContactList.getInstance().activate();
                renameContactTextbox.setString(null);
            }
            return;
        }
        if (groupName != box) {
            return;
        }
        
        String groupName_ = groupName.getString();
        boolean isExist = null != protocol.getGroup(groupName_);
        if (0 == groupName_.length()) {
            ContactList.getInstance().activate();
            return;
        }
        switch (action) {
            case ADD_GROUP:
                if (!isExist) {
                    protocol.addGroup(protocol.createGroup(groupName_));
                    ContactList.getInstance().activate();
                }
                break;

            case RENAME_GROUP:
                boolean isMyName = group.getName().equals(groupName_);
                if (isMyName) {
                    ContactList.getInstance().activate();

                } else if (!isExist) {
                    protocol.renameGroup(group, groupName_);
                    ContactList.getInstance().activate();
                }
                break;
        }
    }

    public void formAction(Forms form, boolean apply) {
        if (!apply) {
            form.back();
            return;
        }
        if (!form.hasControl(GROUP)) {
            form.back();
            return;
        }
        switch (action) {
            case RENAME_GROUP: {
                Vector groups = getGroups(Group.MODE_EDITABLE);
                Group g = (Group) groups.elementAt(form.getSelectorValue(GROUP));
                String oldGroupName = form.getSelectorString(GROUP);
                String newGroupName = form.getTextFieldValue(GROUP_NEW_NAME);
                boolean isExist = null != protocol.getGroup(newGroupName);
                boolean isMyName = oldGroupName.equals(newGroupName);
                if (!oldGroupName.equals(g.getName())) {
                    form.back();
                } else if (StringConvertor.isEmpty(newGroupName)) {
                    form.back();
                } else if (isMyName) {
                    form.back();
                } else if (isExist) {
                    form.addString(JLocale.getString("group_already_exist"));
                } else {
                    protocol.renameGroup(g, newGroupName);
                    form.back();
                }
                break;
            }
            case DEL_GROUP: {
                Vector groups = getGroups(Group.MODE_REMOVABLE);
                Group g = (Group) groups.elementAt(form.getSelectorValue(GROUP));
                String oldGroupName = form.getSelectorString(GROUP);
                if (oldGroupName.equals(g.getName())) {
                    protocol.removeGroup(g);
                }
                form.back();
                break;
            }
        }
    }
}