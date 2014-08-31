package ru.sawim.forms;

import android.app.AlertDialog;
import android.content.DialogInterface;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.modules.search.Search;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.TextBoxView;
import ru.sawim.view.menu.MyMenu;

import java.util.ArrayList;
import java.util.Vector;


public final class ManageContactListForm implements FormListener, TextBoxView.TextBoxListener {
    private static final int ADD_USER = 0;
    private static final int ADD_CONFERENCE = 1;
    private static final int SEARCH_USER = 2;
    private static final int ADD_GROUP = 3;
    private static final int RENAME_GROUP = 4;
    private static final int DEL_GROUP = 5;

    private static final int GROUP = 25;
    private static final int GROUP_NEW_NAME = 26;

    private Protocol protocol;
    private Group group;
    private TextBoxView groupName;
    private Contact contact;
    private TextBoxView renameContactTextbox;
    private int action;

    public ManageContactListForm(Protocol protocol) {
        this(protocol, (Group) null);
    }

    public ManageContactListForm(Protocol protocol, Group group) {
        this.protocol = protocol;
        this.group = group;
    }

    public ManageContactListForm(Protocol protocol, Contact contact) {
        this.protocol = protocol;
        this.contact = contact;
    }

    public void showContactRename(BaseActivity a) {
        renameContactTextbox = new TextBoxView();
        renameContactTextbox.setString(contact.getName());
        renameContactTextbox.setTextBoxListener(this);
        renameContactTextbox.show(a.getSupportFragmentManager(), "rename");
    }

    public void showContactMove(BaseActivity a) {
        Vector groups = protocol.getGroupItems();
        Group myGroup = protocol.getGroup(contact);
        Vector items = new Vector();
        final ArrayList<Integer> itemsId = new ArrayList<Integer>();
        for (int i = 0; i < groups.size(); ++i) {
            Group g = (Group) groups.elementAt(i);
            if ((myGroup != g) && g.hasMode(Group.MODE_NEW_CONTACTS)) {
                items.add(g.getName());
                itemsId.add(g.getId());
            }
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setCancelable(true);
        builder.setTitle(contact.getName());
        builder.setItems(Util.vectorToArray(items), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                protocol.moveContactTo(contact, protocol.getGroupById(itemsId.get(which)));
                RosterHelper.getInstance().updateRoster();
            }
        });
        builder.create().show();
    }

    public void showMenu(final BaseActivity a) {
        final MyMenu menu = new MyMenu();
        boolean canAdd = !protocol.getGroupItems().isEmpty()
                && ((null == group) || group.hasMode(Group.MODE_NEW_CONTACTS));
        if (canAdd) {
            menu.add(R.string.add_user, ADD_USER);
            if ((protocol instanceof protocol.xmpp.Xmpp)) {
                menu.add(R.string.add_conference, ADD_CONFERENCE);
            } else {
                menu.add(R.string.search_user, SEARCH_USER);
            }
        }
        menu.add(R.string.add_group, ADD_GROUP);
        if (null != group) {
            if (group.hasMode(Group.MODE_EDITABLE)) {
                menu.add(R.string.rename_group, RENAME_GROUP);
            }
            if (group.getContacts().isEmpty() && group.hasMode(Group.MODE_REMOVABLE)) {
                menu.add(R.string.del_group, DEL_GROUP);
            }
        } else {
            menu.add(R.string.rename_group, RENAME_GROUP);
            menu.add(R.string.del_group, DEL_GROUP);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(a);
        builder.setCancelable(true);
        builder.setTitle(R.string.manage_contact_list);
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                select(a, menu.getItem(which).idItem);
            }
        });
        builder.create().show();
    }

    private void select(BaseActivity a, int cmd) {
        action = cmd;
        Search search = protocol.getSearchForm();
        switch (cmd) {
            case ADD_USER:
                search.putToGroup(group);
                search.show(a, "", false);
                break;

            case ADD_CONFERENCE:
                search.putToGroup(group);
                search.show(a, "", true);
                break;

            case SEARCH_USER:
                search.putToGroup(group);
                search.show(a);
                break;

            case ADD_GROUP:
                showTextBox(a, R.string.add_group, null);
                break;

            case RENAME_GROUP:
                if (null == group) {
                    Forms form = new Forms(R.string.rename_group, this, true);
                    addGroup(form, getGroups(Group.MODE_EDITABLE));
                    form.addTextField(GROUP_NEW_NAME, R.string.new_group_name, "");
                    form.show(a);
                } else {
                    showTextBox(a, R.string.rename_group, group.getName());
                }
                break;

            case DEL_GROUP:
                if (null == group) {
                    Forms form = new Forms(R.string.del_group, this, true);
                    addGroup(form, getGroups(Group.MODE_REMOVABLE));
                    form.show(a);
                } else {
                    protocol.removeGroup(group);
                }
                break;
        }
    }

    private Vector getGroups(byte mode) {
        Vector all = protocol.getGroupItems();
        Vector groups = new Vector();
        for (int i = 0; i < all.size(); ++i) {
            Group g = (Group) all.elementAt(i);
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
            form.addSelector(GROUP, R.string.group, list, def);
        } else {
            form.addString(JLocale.getString(R.string.no_groups_available));
        }
    }

    private void showTextBox(BaseActivity a, int caption, String text) {
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
                RosterHelper.getInstance().updateRoster();
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
            RosterHelper.getInstance().updateRoster();
            return;
        }
        switch (action) {
            case ADD_GROUP:
                if (!isExist) {
                    protocol.addGroup(protocol.createGroup(groupName_));
                    RosterHelper.getInstance().updateRoster();
                }
                break;

            case RENAME_GROUP:
                boolean isMyName = group.getName().equals(groupName_);
                if (isMyName) {
                    RosterHelper.getInstance().updateRoster();

                } else if (!isExist) {
                    protocol.renameGroup(group, groupName_);
                    RosterHelper.getInstance().updateRoster();
                }
                break;
        }
    }

    public void formAction(BaseActivity activity, Forms form, boolean apply) {
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
                    form.addString(JLocale.getString(R.string.group_already_exist));
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