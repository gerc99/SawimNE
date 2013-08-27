package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.jabber.*;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.models.MucUsersAdapter;
import ru.sawim.view.menu.MyMenu;
import sawim.util.JLocale;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 23.06.13
 * Time: 21:55
 * To change this template use File | Settings | File Templates.
 */
public class MucUsersView implements TextBoxView.TextBoxListener {

    private MucUsersAdapter usersAdapter = new MucUsersAdapter();
    private String currMucNik = "";
    private TextBoxView banTextbox;
    private TextBoxView kikTextbox;
    private Protocol protocol;
    private JabberServiceContact jabberServiceContact;

    public void init(Protocol protocol, JabberServiceContact jabberServiceContact) {
        this.protocol = protocol;
        this.jabberServiceContact = jabberServiceContact;
    }

    public void show(final ChatView chatView, ListView nickList) {
        final FragmentActivity activity = chatView.getActivity();
        usersAdapter.init(activity, (Jabber) protocol, jabberServiceContact);
        nickList.setBackgroundColor(Scheme.getInversColor(Scheme.THEME_BACKGROUND));
        nickList.setAdapter(usersAdapter);
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
                final MyMenu menu = new MyMenu(activity);
                menu.add(activity.getString(R.string.open_private), ContactMenu.COMMAND_PRIVATE);
                menu.add(activity.getString(R.string.info), ContactMenu.COMMAND_INFO);
                menu.add(activity.getString(R.string.user_statuses), ContactMenu.COMMAND_STATUS);
                menu.add(activity.getString(R.string.adhoc), ContactMenu.GATE_COMMANDS);
                int myAffiliation = usersAdapter.getAffiliation(jabberServiceContact.getMyName());
                int myRole = usersAdapter.getRole(jabberServiceContact.getMyName());
                final int role = usersAdapter.getRole(nick);
                final int affiliation = usersAdapter.getAffiliation(nick);
                if (myAffiliation == JabberServiceContact.AFFILIATION_OWNER)
                    myAffiliation++;
                if (JabberServiceContact.ROLE_MODERATOR == myRole) {
                    if (JabberServiceContact.ROLE_MODERATOR > role) {
                        menu.add(JLocale.getString("to_kick"), ContactMenu.COMMAND_KICK);
                    }
                    if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN && affiliation < myAffiliation) {
                        menu.add(JLocale.getString("to_ban"), ContactMenu.COMMAND_BAN);
                    }
                    if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                        if (role == JabberServiceContact.ROLE_VISITOR) {
                            menu.add(JLocale.getString("to_voice"), ContactMenu.COMMAND_VOICE);
                        } else {
                            menu.add(JLocale.getString("to_devoice"), ContactMenu.COMMAND_DEVOICE);
                        }
                    }
                }
                if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN) {
                    if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                        if (role == JabberServiceContact.ROLE_MODERATOR) {
                            menu.add(JLocale.getString("to_voice"), ContactMenu.COMMAND_VOICE);
                        } else {
                            menu.add(JLocale.getString("to_moder"), ContactMenu.COMMAND_MODER);
                        }
                    }
                    if (affiliation < myAffiliation) {
                        if (affiliation != JabberServiceContact.AFFILIATION_NONE) {
                            menu.add(JLocale.getString("to_none"), ContactMenu.COMMAND_NONE);
                        }
                        if (affiliation != JabberServiceContact.AFFILIATION_MEMBER) {
                            menu.add(JLocale.getString("to_member"), ContactMenu.COMMAND_MEMBER);
                        }
                    }
                }
                if (myAffiliation >= JabberServiceContact.AFFILIATION_OWNER) {
                    if (affiliation != JabberServiceContact.AFFILIATION_ADMIN) {
                        menu.add(JLocale.getString("to_admin"), ContactMenu.COMMAND_ADMIN);
                    }
                    if (affiliation != JabberServiceContact.AFFILIATION_OWNER) {
                        menu.add(JLocale.getString("to_owner"), ContactMenu.COMMAND_OWNER);
                    }
                }
				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.AlertDialogCustom));
                builder.setCancelable(true);
                builder.setTitle(jabberServiceContact.getName());
                builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int which) {
                                           currMucNik = nick;
                                           switch (menu.getItem(which).idItem) {
                                               case ContactMenu.COMMAND_PRIVATE:
                                                   String jid = Jid.realJidToSawimJid(jabberServiceContact.getUserId() + "/" + nick);
                                                   JabberServiceContact c = (JabberServiceContact) protocol.getItemByUIN(jid);
                                                   if (null == c) {
                                                       c = (JabberServiceContact) protocol.createTempContact(jid);
                                                       protocol.addTempContact(c);
                                                   }
                                                   chatView.pause(chatView.getCurrentChat());
                                                   chatView.resetSpinner();
                                                   chatView.openChat(protocol, c);
                                                   chatView.resume(chatView.getCurrentChat());
                                                   break;
                                               case ContactMenu.COMMAND_INFO:
                                                   protocol.showUserInfo(jabberServiceContact.getPrivateContact(nick));
                                                   break;
                                               case ContactMenu.COMMAND_STATUS:
                                                   protocol.showStatus(jabberServiceContact.getPrivateContact(nick));
                                                   break;
                                               case ContactMenu.GATE_COMMANDS:
                                                   JabberContact.SubContact subContact = jabberServiceContact.getExistSubContact(nick);
                                                   AdHoc adhoc = new AdHoc((Jabber) protocol, jabberServiceContact);
                                                   adhoc.setResource(subContact.resource);
                                                   adhoc.show();
                                                   break;

                                               case ContactMenu.COMMAND_KICK:
                                                   kikTextbox = new TextBoxView();
                                                   kikTextbox.setTextBoxListener(MucUsersView.this);
                                                   kikTextbox.setString("");
                                                   kikTextbox.show(activity.getSupportFragmentManager(), "message");
                                                   break;

                                               case ContactMenu.COMMAND_BAN:
                                                   banTextbox = new TextBoxView();
                                                   banTextbox.setTextBoxListener(MucUsersView.this);
                                                   banTextbox.setString("");
                                                   banTextbox.show(activity.getSupportFragmentManager(), "message");
                                                   break;

                                               case ContactMenu.COMMAND_DEVOICE:
                                                   usersAdapter.setMucRole(nick, "v" + "isitor");
                                                   chatView.updateMucList();
                                                   break;

                                               case ContactMenu.COMMAND_VOICE:
                                                   usersAdapter.setMucRole(nick, "partic" + "ipant");
                                                   chatView.updateMucList();
                                                   break;
                                               case ContactMenu.COMMAND_MEMBER:
                                                   usersAdapter.setMucAffiliation(nick, "m" + "ember");
                                                   chatView.updateMucList();
                                                   break;

                                               case ContactMenu.COMMAND_MODER:
                                                   usersAdapter.setMucRole(nick, "m" + "oderator");
                                                   chatView.updateMucList();
                                                   break;

                                               case ContactMenu.COMMAND_ADMIN:
                                                   usersAdapter.setMucAffiliation(nick, "a" + "dmin");
                                                   chatView.updateMucList();
                                                   break;

                                               case ContactMenu.COMMAND_OWNER:
                                                   usersAdapter.setMucAffiliation(nick, "o" + "wner");
                                                   chatView.updateMucList();
                                                   break;

                                               case ContactMenu.COMMAND_NONE:
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
        if (usersAdapter != null) {
            usersAdapter.update();
            usersAdapter.notifyDataSetChanged();
        }
    }
}