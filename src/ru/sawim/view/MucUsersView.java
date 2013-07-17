package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
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

    public void init(Protocol protocol, JabberServiceContact jabberServiceContact) {
        this.protocol = protocol;
        this.jabberServiceContact = jabberServiceContact;
    }

    public void show(final FragmentActivity activity, ListView nickList, ImageView usersImage, final ChatView chatView) {
        usersAdapter = new MucUsersAdapter(activity, (Jabber) protocol, jabberServiceContact);
        nickList.setCacheColorHint(0x00000000);
        nickList.setBackgroundColor(Scheme.getInversColor(Scheme.THEME_BACKGROUND));
        nickList.setAdapter(usersAdapter);
        usersImage.setVisibility(View.VISIBLE);
        nickList.setVisibility(View.VISIBLE);
        nickList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Object o = usersAdapter.getItem(position);
                SawimApplication.getInstance().runOnUiThread(new Runnable() {
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
                menu.add(activity.getString(R.string.open_private), COMMAND_PRIVATE);
                menu.add(activity.getString(R.string.info), COMMAND_INFO);
                menu.add(activity.getString(R.string.user_statuses), COMMAND_STATUS);
                menu.add(activity.getString(R.string.adhoc), GATE_COMMANDS);
                int myAffiliation = usersAdapter.getAffiliation(jabberServiceContact.getMyName());
                int myRole = usersAdapter.getRole(jabberServiceContact.getMyName());
                final int role = usersAdapter.getRole(nick);
                final int affiliation = usersAdapter.getAffiliation(nick);
                if (myAffiliation == JabberServiceContact.AFFILIATION_OWNER)
                    myAffiliation++;
                if (JabberServiceContact.ROLE_MODERATOR == myRole) {
                    if (JabberServiceContact.ROLE_MODERATOR > role) {
                        menu.add(JLocale.getString("to_kick"), COMMAND_KICK);
                    }
                    if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN && affiliation < myAffiliation) {
                        menu.add(JLocale.getString("to_ban"), COMMAND_BAN);
                    }
                    if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                        if (role == JabberServiceContact.ROLE_VISITOR) {
                            menu.add(JLocale.getString("to_voice"), COMMAND_VOICE);
                        } else {
                            menu.add(JLocale.getString("to_devoice"), COMMAND_DEVOICE);
                        }
                    }
                }
                if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN) {
                    if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                        if (role == JabberServiceContact.ROLE_MODERATOR) {
                            menu.add(JLocale.getString("to_voice"), COMMAND_VOICE);
                        } else {
                            menu.add(JLocale.getString("to_moder"), COMMAND_MODER);
                        }
                    }
                    if (affiliation < myAffiliation) {
                        if (affiliation != JabberServiceContact.AFFILIATION_NONE) {
                            menu.add(JLocale.getString("to_none"), COMMAND_NONE);
                        }
                        if (affiliation != JabberServiceContact.AFFILIATION_MEMBER) {
                            menu.add(JLocale.getString("to_member"), COMMAND_MEMBER);
                        }
                    }
                }
                if (myAffiliation >= JabberServiceContact.AFFILIATION_OWNER) {
                    if (affiliation != JabberServiceContact.AFFILIATION_ADMIN) {
                        menu.add(JLocale.getString("to_admin"), COMMAND_ADMIN);
                    }
                    if (affiliation != JabberServiceContact.AFFILIATION_OWNER) {
                        menu.add(JLocale.getString("to_owner"), COMMAND_OWNER);
                    }
                }
				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, R.style.AlertDialogCustom));
                builder.setTitle(jabberServiceContact.getName());
                builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                                       @Override
                                       public void onClick(DialogInterface dialog, int which) {
                                           currMucNik = nick;
                                           switch (menu.getItem(which).idItem) {
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
        if (usersAdapter != null) {
            usersAdapter.update();
            usersAdapter.notifyDataSetChanged();
        }
    }
}