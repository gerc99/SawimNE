package protocol;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextMenu;
import android.widget.Toast;
import protocol.xmpp.Xmpp;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.forms.ManageContactListForm;
import ru.sawim.modules.FileTransfer;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.view.TextBoxView;
import ru.sawim.view.menu.MyMenu;

import java.util.Vector;

public class ContactMenu implements TextBoxView.TextBoxListener {

    public static final int GATE_CONNECT = 0;
    public static final int GATE_DISCONNECT = 1;
    public static final int GATE_REGISTER = 2;
    public static final int GATE_UNREGISTER = 3;
    public static final int GATE_ADD = 4;
    public static final int CONFERENCE_CONNECT = 5;
    public static final int CONFERENCE_OPTIONS = 6;
    public static final int CONFERENCE_OWNER_OPTIONS = 7;
    public static final int CONFERENCE_INFORMATION = 15;
    public static final int CONFERENCE_ADD = 8;
    public static final int COMMAND_TITLE = 9;
    public static final int USER_MENU_CONNECTIONS = 10;
    public static final int USER_MENU_REMOVE_ME = 11;
    public static final int USER_MENU_ADHOC = 12;
    public static final int USER_MENU_SEEN = 13;
    public static final int USER_INVITE = 14;

    public static final int CONFERENCE_AFFILIATION_LIST = 18;
    public static final int CONFERENCE_OWNERS = 19;
    public static final int CONFERENCE_ADMINS = 20;
    public static final int CONFERENCE_MEMBERS = 21;
    public static final int CONFERENCE_INBAN = 22;

    public static final int MENU_COPY_TEXT = 23;

    public static final int ACTION_TO_NOTES = 25;
    public static final int ACTION_QUOTE = 26;

    public static final int COMMAND_PRIVATE = 27;
    public static final int COMMAND_INFO = 28;
    public static final int COMMAND_STATUS = 29;

    public static final int COMMAND_KICK = 30;
    public static final int COMMAND_BAN = 31;
    public static final int COMMAND_DEVOICE = 32;
    public static final int COMMAND_VOICE = 33;
    public static final int COMMAND_MEMBER = 34;
    public static final int COMMAND_MODER = 35;
    public static final int COMMAND_ADMIN = 36;
    public static final int COMMAND_OWNER = 37;
    public static final int COMMAND_NONE = 38;
    public static final int GATE_COMMANDS = 39;

    public static final int USER_MENU_REQU_AUTH = 40;
    public static final int USER_MENU_USER_REMOVE = 41;
    public static final int USER_MENU_RENAME = 42;
    public static final int USER_MENU_USER_INFO = 43;
    public static final int USER_MENU_MOVE = 44;
    public static final int USER_MENU_STATUSES = 45;

    public static final int USER_MENU_ADD_USER = 47;
    public static final int USER_MENU_GRANT_AUTH = 48;
    public static final int USER_MENU_DENY_AUTH = 49;
    public static final int USER_MENU_PS_VISIBLE = 50;
    public static final int USER_MENU_PS_INVISIBLE = 51;
    public static final int USER_MENU_PS_IGNORE = 52;
    public static final int USER_MENU_USERS_LIST = 53;
    public static final int USER_MANAGE_CONTACT = 54;
    public static final int USER_MENU_WAKE = 55;
    public static final int USER_MENU_FILE_TRANS = 56;
    public static final int USER_MENU_CAM_TRANS = 57;

    public static final int USER_MENU_ANNOTATION = 60;
    public static final int CONFERENCE_DISCONNECT = 61;
    public static final int USER_MENU_CLOSE_CHAT = 62;
    public static final int MENU_MULTI_CITATION = 63;
    public static final int ROLE_COMMANDS = 64;
    public static final int USER_MENU_DELETE_HISTORY = 65;

    private Contact contact;
    private Protocol protocol;
    private TextBoxView messageTextbox;

    public ContactMenu(Protocol p, Contact c) {
        contact = c;
        protocol = p;
    }

    public void getContextMenu(ContextMenu menu) {
        contact.initContextMenu(protocol, menu);
    }

    public void doAction(final BaseActivity activity, int cmd) {
        switch (cmd) {
            case USER_MENU_ANNOTATION: {
                messageTextbox = new TextBoxView();
                messageTextbox.setTextBoxListener(this);
                messageTextbox.setString(contact.annotations);
                messageTextbox.show(activity.getSupportFragmentManager(), "message");
                return;
            }

            case USER_MENU_ADD_USER:
                protocol.getSearchForm().show(activity, contact.getUserId(), false);
                break;

            case USER_MENU_USER_REMOVE:
                HistoryStorage.getHistory(protocol.getUserId(), contact.getUserId()).removeHistory();
                protocol.removeContact(contact);
                break;

            case USER_MENU_STATUSES:
                protocol.showStatus(activity, contact);
                break;

            case USER_MANAGE_CONTACT:
                final MyMenu menu = new MyMenu();
                contact.initManageContactMenu(protocol, menu);
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setCancelable(true);
                builder.setTitle(SawimApplication.getContext().getString(R.string.manage_contact_list));
                builder.setAdapter(menu, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doAction(activity, menu.getItem(which).idItem);
                    }
                });
                builder.create().show();
                break;

            case USER_MENU_CLOSE_CHAT:
                ChatHistory.instance.unregisterChat(protocol.getChat(contact));
                Toast.makeText(activity, activity.getString(R.string.messages_are_deleted), Toast.LENGTH_LONG).show();
                break;

            case USER_MENU_WAKE:
                SawimApplication.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        protocol.sendMessage(contact, PlainMessage.CMD_WAKEUP, true);
                    }
                });
                protocol.getChat(contact).activate();
                break;

            case USER_MENU_DELETE_HISTORY:
                SawimApplication.getExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        protocol.getChat(contact).getHistory().removeHistory();
                    }
                });
                Toast.makeText(activity, activity.getString(R.string.messages_are_deleted), Toast.LENGTH_LONG).show();
                break;

            case USER_MENU_FILE_TRANS:
                new FileTransfer(protocol, contact).startFileTransfer();
                break;

            case USER_MENU_CAM_TRANS:
                new FileTransfer(protocol, contact).startPhotoTransfer();
                break;

            case USER_MENU_RENAME:
                new ManageContactListForm(protocol, contact).showContactRename(activity);
                break;

            case USER_MENU_MOVE:
                new ManageContactListForm(protocol, contact).showContactMove(activity);
                break;

            case USER_MENU_USER_INFO:
                protocol.showUserInfo(activity, contact);
                break;

            case CONFERENCE_AFFILIATION_LIST:
                CharSequence[] items = new CharSequence[4];
                items[0] = JLocale.getString(R.string.owners);
                items[1] = JLocale.getString(R.string.admins);
                items[2] = JLocale.getString(R.string.members);
                items[3] = JLocale.getString(R.string.inban);
                AlertDialog.Builder b = new AlertDialog.Builder(activity);
                b.setTitle(contact.getName());
                b.setCancelable(true);
                b.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                protocol.doAction(activity, contact, CONFERENCE_OWNERS);
                                break;
                            case 1:
                                protocol.doAction(activity, contact, CONFERENCE_ADMINS);
                                break;
                            case 2:
                                protocol.doAction(activity, contact, CONFERENCE_MEMBERS);
                                break;
                            case 3:
                                protocol.doAction(activity, contact, CONFERENCE_INBAN);
                                break;
                        }
                    }
                });
                b.create().show();
                break;

            case USER_MENU_REQU_AUTH:
                protocol.requestAuth(contact.getUserId());
                break;

            case USER_MENU_GRANT_AUTH:
                protocol.grandAuth(contact.getUserId());
                protocol.getChat(contact).resetAuthRequests();
                break;

            case USER_MENU_DENY_AUTH:
                protocol.denyAuth(contact.getUserId());
                protocol.getChat(contact).resetAuthRequests();
                break;

            default:
                protocol.doAction(activity, contact, cmd);
        }
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        if ((box == messageTextbox)) {
            Contact find = null;
            Contact current = contact;
            current.annotations = messageTextbox.getString();
            Vector contacts = protocol.getContactItems();
            int size = contacts.size();
            String jid = current.getUserId();
            StringBuilder xml = new StringBuilder();
            xml.append("<iq type='set' id='notes").append(Util.xmlEscape(jid));
            xml.append("'><query xmlns='jabber:iq:private'><storage xmlns='storage:rosternotes'>");
            synchronized (contacts) {
                for (int i = 0; i < size; i++) {
                    find = (Contact) contacts.elementAt(i);
                    //if (find.annotations.equals("")) find.annotations = null;
                    if (find.annotations != null) {
                        xml.append("<note jid='").append(Util.xmlEscape(find.getUserId()));
                        xml.append("'>").append(Util.xmlEscape(find.annotations)).append("</note>");
                    }
                }
                xml.append("</storage></query></iq>");
                ((Xmpp) protocol).saveAnnotations(xml.toString());
            }
            messageTextbox.back();
            return;
        }
    }
}