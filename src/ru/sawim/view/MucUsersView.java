package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import protocol.Protocol;
import protocol.jabber.*;
import ru.sawim.R;
import ru.sawim.models.MucUsersAdapter;
import sawim.ui.TextBoxListener;
import sawim.util.JLocale;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 23.06.13
 * Time: 21:55
 * To change this template use File | Settings | File Templates.
 */
public class MucUsersView implements TextBoxListener {

    private static final int COMMAND_PRIVATE = 0;
    private static final int COMMAND_INFO = 1;
    private static final int COMMAND_STATUS = 2;
    private static final int COMMAND_KICK = 3;
    private static final int COMMAND_BAN = 4;
    private static final int COMMAND_DEVOICE = 5;
    private static final int COMMAND_VOICE = 6;
    private static final int COMMAND_MEMBER = 7;
    private static final int COMMAND_MODER = 8;
    private static final int COMMAND_ADMIN = 9;
    private static final int COMMAND_OWNER = 10;
    private static final int COMMAND_NONE = 11;
    private static final int GATE_COMMANDS = 12;
    private MucUsersAdapter usersAdapter;
    private String currMucNik = "";
    private TextBoxView banTextbox;
    private TextBoxView kikTextbox;
    private Protocol protocol;
    private JabberServiceContact jabberServiceContact;

    public MucUsersView(Protocol protocol, JabberServiceContact jabberServiceContact) {
        this.protocol = protocol;
        this.jabberServiceContact = jabberServiceContact;
    }

    public void show(final FragmentActivity activity, ListView nickList, ImageView usersImage, final ChatView chatView) {
        usersAdapter = new MucUsersAdapter(activity, (Jabber) protocol, jabberServiceContact);
        nickList.setAdapter(usersAdapter);
        usersImage.setVisibility(View.VISIBLE);
        nickList.setVisibility(View.VISIBLE);
        nickList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Object o = usersAdapter.getItem(position);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (o instanceof JabberContact.SubContact) {
                            JabberContact.SubContact c = (JabberContact.SubContact) o;
                            chatView.insert(c.resource + ", ");
                            chatView.showKeyboard();
                        }
                    }
                });
            }
        });
        nickList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {
                final Object o = usersAdapter.getItem(position);
                final String nick = usersAdapter.getCurrentSubContact(o);
                if (o instanceof String) return false;
                final List<MucMenuItem> menuItems = new ArrayList<MucMenuItem>();
                MucMenuItem menuItem = new MucMenuItem();
                menuItem.addItem(activity.getString(R.string.open_private), COMMAND_PRIVATE);
                menuItems.add(menuItem);
                menuItem = new MucMenuItem();
                menuItem.addItem(activity.getString(R.string.info), COMMAND_INFO);
                menuItems.add(menuItem);
                menuItem = new MucMenuItem();
                menuItem.addItem(activity.getString(R.string.user_statuses), COMMAND_STATUS);
                menuItems.add(menuItem);
                menuItem = new MucMenuItem();
                menuItem.addItem(activity.getString(R.string.adhoc), GATE_COMMANDS);
                menuItems.add(menuItem);
                int myAffiliation = usersAdapter.getAffiliation(jabberServiceContact.getMyName());
                int myRole = usersAdapter.getRole(jabberServiceContact.getMyName());
                final int role = usersAdapter.getRole(nick);
                final int affiliation = usersAdapter.getAffiliation(nick);
                if (myAffiliation == JabberServiceContact.AFFILIATION_OWNER)
                    myAffiliation++;
                if (JabberServiceContact.ROLE_MODERATOR == myRole) {
                    if (JabberServiceContact.ROLE_MODERATOR > role) {
                        menuItem = new MucMenuItem();
                        menuItem.addItem(JLocale.getString("to_kick"), COMMAND_KICK);
                        menuItems.add(menuItem);
                    }
                    if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN && affiliation < myAffiliation) {
                        menuItem = new MucMenuItem();
                        menuItem.addItem(JLocale.getString("to_ban"), COMMAND_BAN);
                        menuItems.add(menuItem);
                    }
                    if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                        if (role == JabberServiceContact.ROLE_VISITOR) {
                            menuItem = new MucMenuItem();
                            menuItem.addItem(JLocale.getString("to_voice"), COMMAND_VOICE);
                            menuItems.add(menuItem);
                        } else {
                            menuItem = new MucMenuItem();
                            menuItem.addItem(JLocale.getString("to_devoice"), COMMAND_DEVOICE);
                            menuItems.add(menuItem);
                        }
                    }
                }
                if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN) {
                    if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                        if (role == JabberServiceContact.ROLE_MODERATOR) {
                            menuItem = new MucMenuItem();
                            menuItem.addItem(JLocale.getString("to_voice"), COMMAND_VOICE);
                            menuItems.add(menuItem);
                        } else {
                            menuItem = new MucMenuItem();
                            menuItem.addItem(JLocale.getString("to_moder"), COMMAND_MODER);
                            menuItems.add(menuItem);
                        }
                    }
                    if (affiliation < myAffiliation) {
                        if (affiliation != JabberServiceContact.AFFILIATION_NONE) {
                            menuItem = new MucMenuItem();
                            menuItem.addItem(JLocale.getString("to_none"), COMMAND_NONE);
                            menuItems.add(menuItem);
                        }
                        if (affiliation != JabberServiceContact.AFFILIATION_MEMBER) {
                            menuItem = new MucMenuItem();
                            menuItem.addItem(JLocale.getString("to_member"), COMMAND_MEMBER);
                            menuItems.add(menuItem);
                        }
                    }
                }
                if (myAffiliation >= JabberServiceContact.AFFILIATION_OWNER) {
                    if (affiliation != JabberServiceContact.AFFILIATION_ADMIN) {
                        menuItem = new MucMenuItem();
                        menuItem.addItem(JLocale.getString("to_admin"), COMMAND_ADMIN);
                        menuItems.add(menuItem);
                    }
                    if (affiliation != JabberServiceContact.AFFILIATION_OWNER) {
                        menuItem = new MucMenuItem();
                        menuItem.addItem(JLocale.getString("to_owner"), COMMAND_OWNER);
                        menuItems.add(menuItem);
                    }
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(jabberServiceContact.getName());
                builder.setAdapter(new BaseAdapter() {
                                       @Override
                                       public int getCount() {
                                           return menuItems.size();
                                       }

                                       @Override
                                       public MucMenuItem getItem(int i) {
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
                                               LayoutInflater inf = LayoutInflater.from(activity);
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
                                           currMucNik = nick;
                                           switch (menuItems.get(which).idItem) {
                                               case COMMAND_PRIVATE:
                                                   String jid = Jid.realJidToSawimJid(jabberServiceContact.getUserId() + "/" + nick);
                                                   JabberServiceContact c = (JabberServiceContact) protocol.getItemByUIN(jid);
                                                   if (null == c) {
                                                       c = (JabberServiceContact) protocol.createTempContact(jid);
                                                       protocol.addTempContact(c);
                                                   }
                                                   chatView.openChat(protocol, c);
                                                   break;
                                               case COMMAND_INFO:
                                                   protocol.showUserInfo(usersAdapter.getContactForVCard(nick));
                                                   break;
                                               case COMMAND_STATUS:
                                                   protocol.showStatus(usersAdapter.getPrivateContact(nick));
                                                   break;
                                               case GATE_COMMANDS:
                                                   JabberContact.SubContact subContact = jabberServiceContact.getExistSubContact(nick);
                                                   AdHoc adhoc = new AdHoc((Jabber) protocol, jabberServiceContact);
                                                   adhoc.setResource(subContact.resource);
                                                   adhoc.show();
                                                   break;

                                               case COMMAND_KICK:
                                                   kikTextbox = new TextBoxView();
                                                   kikTextbox.setTextBoxListener(MucUsersView.this);
                                                   kikTextbox.setString("");
                                                   kikTextbox.show(activity.getSupportFragmentManager(), "message");
                                                   break;

                                               case COMMAND_BAN:
                                                   banTextbox = new TextBoxView();
                                                   banTextbox.setTextBoxListener(MucUsersView.this);
                                                   banTextbox.setString("");
                                                   banTextbox.show(activity.getSupportFragmentManager(), "message");
                                                   break;

                                               case COMMAND_DEVOICE:
                                                   usersAdapter.setMucRole(nick, "v" + "isitor");
                                                   chatView.updateMucList();
                                                   break;

                                               case COMMAND_VOICE:
                                                   usersAdapter.setMucRole(nick, "partic" + "ipant");
                                                   chatView.updateMucList();
                                                   break;
                                               case COMMAND_MEMBER:
                                                   usersAdapter.setMucAffiliation(nick, "m" + "ember");
                                                   chatView.updateMucList();
                                                   break;

                                               case COMMAND_MODER:
                                                   usersAdapter.setMucRole(nick, "m" + "oderator");
                                                   chatView.updateMucList();
                                                   break;

                                               case COMMAND_ADMIN:
                                                   usersAdapter.setMucAffiliation(nick, "a" + "dmin");
                                                   chatView.updateMucList();
                                                   break;

                                               case COMMAND_OWNER:
                                                   usersAdapter.setMucAffiliation(nick, "o" + "wner");
                                                   chatView.updateMucList();
                                                   break;

                                               case COMMAND_NONE:
                                                   usersAdapter.setMucAffiliation(nick, "n" + "o" + "ne");
                                                   chatView.updateMucList();
                                                   break;
                                           }
                                       }
                                   }
                );
                builder.create().show();
                return false;
            }
        });
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        String rzn = (box == banTextbox) ? banTextbox.getString() : kikTextbox.getString();
        String Nick = "";
        String myNick = jabberServiceContact.getMyName();
        String reason = "";
        if (rzn.charAt(0) == '!') {
            rzn = rzn.substring(1);
        } else {
            Nick = (myNick == null) ? myNick : myNick + ": ";
        }
        if (rzn.length() != 0 && myNick != null) {
            reason = Nick + rzn;
        } else {
            reason = Nick;
        }
        if ((box == banTextbox)) {
            usersAdapter.setMucAffiliationR(currMucNik, "o" + "utcast", reason);
            banTextbox.back();
            return;
        }
        if ((box == kikTextbox)) {
            usersAdapter.setMucRoleR(currMucNik, "n" + "o" + "ne", reason);
            kikTextbox.back();
            return;
        }
    }

    public void update() {
        if (usersAdapter != null)
            usersAdapter.notifyDataSetChanged();
    }

    private class MucMenuItem {
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
}
