package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.xmpp.*;
import ru.sawim.SawimApplication;
import ru.sawim.R;
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
    private XmppServiceContact xmppServiceContact;

    public void init(Protocol protocol, XmppServiceContact xmppServiceContact) {
        this.protocol = protocol;
        this.xmppServiceContact = xmppServiceContact;
    }

    public void show(final ChatView chatView, ListView nickList) {
        final FragmentActivity activity = SawimApplication.getCurrentActivity();
        usersAdapter.init(activity, (Xmpp) protocol, xmppServiceContact);
        nickList.setAdapter(usersAdapter);
        nickList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                final Object o = usersAdapter.getItem(position);
                chatView.hasBack();
                if (o instanceof XmppContact.SubContact) {
                    XmppContact.SubContact c = (XmppContact.SubContact) o;
                    chatView.insert(c.resource + ", ");
                    chatView.showKeyboard();
                }
            }
        });
        nickList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, final int position, long l) {
                final Object o = usersAdapter.getItem(position);
                if (o instanceof String) return false;
                final String nick = usersAdapter.getCurrentSubContact(o);
                final MyMenu menu = new MyMenu(activity);
                final MyMenu roleConfigMenu = getRoleConfigMenu(nick);
                menu.add(activity.getString(R.string.open_private), ContactMenu.COMMAND_PRIVATE);
                menu.add(activity.getString(R.string.info), ContactMenu.COMMAND_INFO);
                menu.add(activity.getString(R.string.user_statuses), ContactMenu.COMMAND_STATUS);
                //menu.add(activity.getString(R.string.invite), ContactMenu.USER_INVITE);
                menu.add(activity.getString(R.string.adhoc), ContactMenu.GATE_COMMANDS);
                if (roleConfigMenu.getCount() > 0)
                    menu.add(activity.getString(R.string.role_commands), ContactMenu.ROLE_COMMANDS);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(true);
                builder.setTitle(xmppServiceContact.getName());
                builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        currMucNik = nick;
                        chatView.hasBack();
                        XmppContact.SubContact subContact = xmppServiceContact.getExistSubContact(nick);
                        switch (menu.getItem(which).idItem) {
                            case ContactMenu.COMMAND_PRIVATE:
                                String jid = Jid.realJidToSawimJid(xmppServiceContact.getUserId() + "/" + nick);
                                XmppServiceContact c = (XmppServiceContact) protocol.getItemByUIN(jid);
                                if (null == c) {
                                    c = (XmppServiceContact) protocol.createTempContact(jid);
                                    protocol.addTempContact(c);
                                }
                                chatView.pause(chatView.getCurrentChat());
                                chatView.openChat(protocol, c);
                                chatView.resume(chatView.getCurrentChat());
                                activity.supportInvalidateOptionsMenu();
                                break;
                            case ContactMenu.COMMAND_INFO:
                                protocol.showUserInfo(xmppServiceContact.getPrivateContact(nick));
                                break;
                            case ContactMenu.COMMAND_STATUS:
                                protocol.showStatus(xmppServiceContact.getPrivateContact(nick));
                                break;
                            case ContactMenu.USER_INVITE:
                                try {
                                    ((Xmpp) protocol).showInviteForm(xmppServiceContact.getUserId() + '/' + subContact.resource);
                                } catch (Exception e) {
                                }
                                break;
                            case ContactMenu.GATE_COMMANDS:
                                AdHoc adhoc = new AdHoc((Xmpp) protocol, xmppServiceContact);
                                adhoc.setResource(subContact.resource);
                                adhoc.show();
                                break;
                            case ContactMenu.ROLE_COMMANDS:
                                showRoleConfig(roleConfigMenu, nick, chatView);
                                break;
                        }
                    }
                });
                builder.create().show();
                return false;
            }
        });
    }

    public void destroy(ListView nickList) {
        nickList.setAdapter(null);
        nickList.setOnItemClickListener(null);
        nickList.setOnItemLongClickListener(null);
    }

    private MyMenu getRoleConfigMenu(final String nick) {
        final MyMenu menu = new MyMenu(SawimApplication.getCurrentActivity());
        int myAffiliation = usersAdapter.getAffiliation(xmppServiceContact.getMyName());
        int myRole = usersAdapter.getRole(xmppServiceContact.getMyName());
        final int role = usersAdapter.getRole(nick);
        final int affiliation = usersAdapter.getAffiliation(nick);
        if (myAffiliation == XmppServiceContact.AFFILIATION_OWNER)
            myAffiliation++;
        if (XmppServiceContact.ROLE_MODERATOR == myRole) {
            if (XmppServiceContact.ROLE_MODERATOR > role) {
                menu.add(R.string.to_kick, ContactMenu.COMMAND_KICK);
            }
            if (myAffiliation >= XmppServiceContact.AFFILIATION_ADMIN && affiliation < myAffiliation) {
                menu.add(R.string.to_ban, ContactMenu.COMMAND_BAN);
            }
            if (affiliation < XmppServiceContact.AFFILIATION_ADMIN) {
                if (role == XmppServiceContact.ROLE_VISITOR) {
                    menu.add(R.string.to_voice, ContactMenu.COMMAND_VOICE);
                } else {
                    menu.add(R.string.to_devoice, ContactMenu.COMMAND_DEVOICE);
                }
            }
        }
        if (myAffiliation >= XmppServiceContact.AFFILIATION_ADMIN) {
            if (affiliation < XmppServiceContact.AFFILIATION_ADMIN) {
                if (role == XmppServiceContact.ROLE_MODERATOR) {
                    menu.add(R.string.to_voice, ContactMenu.COMMAND_VOICE);
                } else {
                    menu.add(R.string.to_moder, ContactMenu.COMMAND_MODER);
                }
            }
            if (affiliation < myAffiliation) {
                if (affiliation != XmppServiceContact.AFFILIATION_NONE) {
                    menu.add(R.string.to_none, ContactMenu.COMMAND_NONE);
                }
                if (affiliation != XmppServiceContact.AFFILIATION_MEMBER) {
                    menu.add(R.string.to_member, ContactMenu.COMMAND_MEMBER);
                }
            }
        }
        if (myAffiliation >= XmppServiceContact.AFFILIATION_OWNER) {
            if (affiliation != XmppServiceContact.AFFILIATION_ADMIN) {
                menu.add(R.string.to_admin, ContactMenu.COMMAND_ADMIN);
            }
            if (affiliation != XmppServiceContact.AFFILIATION_OWNER) {
                menu.add(R.string.to_owner, ContactMenu.COMMAND_OWNER);
            }
        }
        return menu;
    }

    private void showRoleConfig(final MyMenu menu, final String nick, final ChatView chatView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(SawimApplication.getCurrentActivity());
        builder.setCancelable(true);
        builder.setTitle(xmppServiceContact.getName());
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (menu.getItem(which).idItem) {
                    case ContactMenu.COMMAND_KICK:
                        kikTextbox = new TextBoxView();
                        kikTextbox.setTextBoxListener(MucUsersView.this);
                        kikTextbox.setString("");
                        kikTextbox.show(SawimApplication.getCurrentActivity().getSupportFragmentManager(), "message");
                        break;

                    case ContactMenu.COMMAND_BAN:
                        banTextbox = new TextBoxView();
                        banTextbox.setTextBoxListener(MucUsersView.this);
                        banTextbox.setString("");
                        banTextbox.show(SawimApplication.getCurrentActivity().getSupportFragmentManager(), "message");
                        break;

                    case ContactMenu.COMMAND_DEVOICE:
                        usersAdapter.setMucRole(nick, "visitor");
                        chatView.updateMucList();
                        break;

                    case ContactMenu.COMMAND_VOICE:
                        usersAdapter.setMucRole(nick, "participant");
                        chatView.updateMucList();
                        break;
                    case ContactMenu.COMMAND_MEMBER:
                        usersAdapter.setMucAffiliation(nick, "member");
                        chatView.updateMucList();
                        break;

                    case ContactMenu.COMMAND_MODER:
                        usersAdapter.setMucRole(nick, "moderator");
                        chatView.updateMucList();
                        break;

                    case ContactMenu.COMMAND_ADMIN:
                        usersAdapter.setMucAffiliation(nick, "admin");
                        chatView.updateMucList();
                        break;

                    case ContactMenu.COMMAND_OWNER:
                        usersAdapter.setMucAffiliation(nick, "owner");
                        chatView.updateMucList();
                        break;

                    case ContactMenu.COMMAND_NONE:
                        usersAdapter.setMucAffiliation(nick, "none");
                        chatView.updateMucList();
                        break;
                }
            }
        });
        builder.create().show();
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        String rzn = (box == banTextbox) ? banTextbox.getString() : kikTextbox.getString();
        String Nick = "";
        String myNick = xmppServiceContact.getMyName();
        String reason = "";
        if (rzn.length() != 0 && rzn.charAt(0) == '!') {
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
            usersAdapter.setMucAffiliationR(currMucNik, "outcast", reason);
            banTextbox.back();
            return;
        }
        if ((box == kikTextbox)) {
            usersAdapter.setMucRoleR(currMucNik, "none", reason);
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