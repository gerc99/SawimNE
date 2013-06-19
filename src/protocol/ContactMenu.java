

package protocol;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.ContextMenu;
import sawim.FileTransfer;
import sawim.Sawim;
import sawim.SawimUI;
import sawim.chat.message.PlainMessage;
import sawim.cl.ContactList;
import sawim.comm.Util;
import sawim.forms.ManageContactListForm;
import sawim.history.HistoryStorage;
import sawim.history.HistoryStorageList;
import sawim.ui.TextBoxListener;
import sawim.util.JLocale;
import protocol.jabber.Jabber;
import ru.sawim.activities.SawimActivity;
import ru.sawim.view.TextBoxView;

import java.util.Vector;

public class ContactMenu implements TextBoxListener {
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

    public void doAction(int cmd) {
        switch (cmd) {
            case Contact.USER_MENU_TRACK: 
                new sawim.modules.tracking.TrackingForm(contact.getUserId()).activate();
                break;
            case Contact.USER_MENU_TRACK_CONF: 
				new sawim.modules.tracking.TrackingForm(contact.getUserId()).activateForConf();
                break;

			case Contact.USER_MENU_ANNOTATION: { 
                messageTextbox = new TextBoxView();
		        messageTextbox.setTextBoxListener(this);
	            messageTextbox.setString(contact.annotations);
                messageTextbox.show(SawimActivity.getInstance().getSupportFragmentManager(), "message");
			    return;
			}
                
            case Contact.USER_MENU_ADD_USER:
                protocol.getSearchForm().show(contact.getUserId());
                break;

            case Contact.USER_MENU_USER_REMOVE:
                HistoryStorage.getHistory(contact).removeHistory();
                protocol.removeContact(contact);
                ContactList.getInstance().activate();
                break;

            case Contact.USER_MENU_STATUSES: 
                protocol.showStatus(contact);
                break;

            case Contact.USER_MENU_WAKE:
                protocol.sendMessage(contact, PlainMessage.CMD_WAKEUP, true);
                protocol.getChat(contact).activate();
                break;

            case Contact.USER_MENU_FILE_TRANS:
                new FileTransfer(protocol, contact).startFileTransfer();
                break;
                
            case Contact.USER_MENU_CAM_TRANS:
                new FileTransfer(protocol, contact).startPhotoTransfer();
                break;

            case Contact.USER_MENU_RENAME:
                new ManageContactListForm(protocol, contact).showContactRename();
                break;

            case Contact.USER_MENU_HISTORY: 
                showHistory();
                break;

            case Contact.USER_MENU_MOVE:
                new ManageContactListForm(protocol, contact).showContactMove();
                break;
            
            case Contact.USER_MENU_USER_INFO:
                protocol.showUserInfo(contact);
                break;
			
			case Jabber.CONFERENCE_AFFILIATION_LIST:
                CharSequence[] items = new CharSequence[4];
                items[0] = JLocale.getString("owners");
                items[1] = JLocale.getString("admins");
                items[2] = JLocale.getString("members");
                items[3] = JLocale.getString("inban");
                AlertDialog.Builder builder = new AlertDialog.Builder(SawimActivity.getInstance());
                builder.setTitle(contact.getName());
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                protocol.doAction(contact, Jabber.CONFERENCE_OWNERS);
                                break;
                            case 1:
                                protocol.doAction(contact, Jabber.CONFERENCE_ADMINS);
                                break;
                            case 2:
                                protocol.doAction(contact, Jabber.CONFERENCE_MEMBERS);
                                break;
                            case 3:
                                protocol.doAction(contact, Jabber.CONFERENCE_INBAN);
                                break;
                        }
                    }
                });
                builder.create().show();
				break;

            case Contact.USER_MENU_REQU_AUTH: 
                protocol.requestAuth(contact.getUserId());
                break;

            case Contact.USER_MENU_GRANT_AUTH:
                protocol.grandAuth(contact.getUserId());
                protocol.getChat(contact).resetAuthRequests();
                break;

            case Contact.USER_MENU_DENY_AUTH:
                protocol.denyAuth(contact.getUserId());
                protocol.getChat(contact).resetAuthRequests();
                break;
                
            default:
                protocol.doAction(contact, cmd);
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
			StringBuffer xml = new StringBuffer();
			xml.append("<iq type='set' id='notes").append(Util.xmlEscape(jid));
			xml.append("'><query xmlns='jabber:iq:private'><storage xmlns='storage:rosternotes'>");
            synchronized (contacts) {
                for (int i = 0; i < size; i++) {
                    find = (Contact)contacts.elementAt(i);
					if (find.annotations == "") find.annotations = null;
				    if (find.annotations != null) {
				        xml.append("<note jid='").append(Util.xmlEscape(find.getUserId()));
						xml.append("'>").append(Util.xmlEscape(find.annotations)).append("</note>");
					}
				}
				xml.append("</storage></query></iq>");
				((Jabber)protocol).saveAnnotations(xml.toString());
			}
		    messageTextbox.back();
			return;
        }
    }
    
    private void showHistory() {
        if (contact.hasHistory()) {
            HistoryStorage history;
            if (contact.hasChat()) {
                history = protocol.getChat(contact).getHistory();
            } else {
                history = HistoryStorage.getHistory(contact);
            }
            new HistoryStorageList().show(history);
            //ru.sawim.activities.SawimActivity.getInstance().showHistory(history);
        }
    }
}