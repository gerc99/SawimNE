package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.xmpp.*;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.models.MucUsersAdapter;
import ru.sawim.view.menu.MyMenu;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 23.06.13
 * Time: 21:55
 * To change this template use File | Settings | File Templates.
 */
public class MucUsersView {

    private TextBoxView banTextbox;
    private TextBoxView kikTextbox;
    private boolean isLongClick = false;

    public void show(final Protocol protocol, final XmppServiceContact xmppServiceContact,
                     final ChatView chatView, ListView nickList) {
        final BaseActivity activity = (BaseActivity) chatView.getActivity();
        final MucUsersAdapter usersAdapter = new MucUsersAdapter();
        usersAdapter.init((Xmpp) protocol, xmppServiceContact);
        nickList.setAdapter(usersAdapter);
        nickList.setFastScrollEnabled(true);
        nickList.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    isLongClick = false;
                }
                return false;
            }
        });
        nickList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                if (isLongClick) return;
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
                isLongClick = true;
                final Object o = usersAdapter.getItem(position);
                if (o instanceof String) return false;
                final String nick = usersAdapter.getCurrentSubContact(o);
                final MyMenu menu = new MyMenu();
                final MyMenu roleConfigMenu = getRoleConfigMenu(usersAdapter, xmppServiceContact, nick);
                menu.add(activity.getString(R.string.open_private), ContactMenu.COMMAND_PRIVATE);
                menu.add(activity.getString(R.string.info), ContactMenu.COMMAND_INFO);
                menu.add(activity.getString(R.string.user_statuses), ContactMenu.COMMAND_STATUS);
                //menu.add(activity.getString(R.string.invite), ContactMenu.USER_INVITE);
                menu.add(activity.getString(R.string.adhoc), ContactMenu.GATE_COMMANDS);
                if (roleConfigMenu.getCount() > 0)
                    menu.add(activity.getString(R.string.role_commands), ContactMenu.ROLE_COMMANDS);
                final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(true);
                builder.setTitle(xmppServiceContact.getName());
                builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        chatView.hasBack();
                        XmppContact.SubContact subContact = xmppServiceContact.getExistSubContact(nick);
                        switch (menu.getItem(which).idItem) {
                            case ContactMenu.COMMAND_PRIVATE:
                                String jid = Jid.realJidToSawimJid(xmppServiceContact.getUserId() + "/" + nick);
                                XmppServiceContact c = (XmppServiceContact) protocol.getItemByUID(jid);
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
                                protocol.showUserInfo(activity, xmppServiceContact.getPrivateContact(nick));
                                break;
                            case ContactMenu.COMMAND_STATUS:
                                protocol.showStatus(activity, xmppServiceContact.getPrivateContact(nick));
                                break;
                            case ContactMenu.USER_INVITE:
                                ((Xmpp) protocol).showInviteForm(activity, xmppServiceContact.getUserId() + '/' + subContact.resource);
                                break;
                            case ContactMenu.GATE_COMMANDS:
                                AdHoc adhoc = new AdHoc((Xmpp) protocol, xmppServiceContact);
                                adhoc.setResource(subContact.resource);
                                adhoc.show(activity);
                                break;
                            case ContactMenu.ROLE_COMMANDS:
                                showRoleConfig(xmppServiceContact, roleConfigMenu, nick, chatView);
                                break;
                        }
                    }
                });
                builder.create().show();
                return false;
            }
        });
    }

    private MyMenu getRoleConfigMenu(MucUsersAdapter usersAdapter, XmppServiceContact xmppServiceContact, final String nick) {
        final MyMenu menu = new MyMenu();
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

    private void showRoleConfig(XmppServiceContact xmppServiceContact, final MyMenu menu, final String nick, final ChatView chatView) {
        final BaseActivity activity = (BaseActivity) chatView.getActivity();
        final MucUsersAdapter usersAdapter = chatView.getMucUsersAdapter();
        final TextBoxView.TextBoxListener textBoxListener = new TextBoxView.TextBoxListener() {
            @Override
            public void textboxAction(TextBoxView box, boolean ok) {
                String rzn = (box == banTextbox) ? banTextbox.getString() : kikTextbox.getString();
                String myNick_ = "";
                String myNick = chatView.getCurrentChat().getContact().getMyName();
                String reason = "";
                if (rzn.length() != 0 && rzn.charAt(0) == '!') {
                    rzn = rzn.substring(1);
                } else {
                    myNick_ = (myNick == null) ? myNick : myNick + ": ";
                }
                if (rzn.length() != 0 && myNick != null) {
                    reason = myNick_ + rzn;
                } else {
                    reason = myNick_;
                }
                if (box == banTextbox) {
                    usersAdapter.setMucAffiliationR(nick, "outcast", reason);
                    banTextbox.back();
                    return;
                }
                if (box == kikTextbox) {
                    usersAdapter.setMucRoleR(nick, "none", reason);
                    kikTextbox.back();
                    return;
                }
            }
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(true);
        builder.setTitle(xmppServiceContact.getName());
        builder.setAdapter(menu, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (menu.getItem(which).idItem) {
                    case ContactMenu.COMMAND_KICK:
                        kikTextbox = new TextBoxView();
                        kikTextbox.setTextBoxListener(textBoxListener);
                        kikTextbox.setString("");
                        kikTextbox.show(activity.getSupportFragmentManager(), "message");
                        break;

                    case ContactMenu.COMMAND_BAN:
                        banTextbox = new TextBoxView();
                        banTextbox.setTextBoxListener(textBoxListener);
                        banTextbox.setString("");
                        banTextbox.show(activity.getSupportFragmentManager(), "message");
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

    public void update(MucUsersAdapter usersAdapter) {
        if (usersAdapter != null) {
            usersAdapter.update();
            usersAdapter.notifyDataSetChanged();
        }
    }
}