


package protocol.jabber;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import sawim.SawimUI;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.util.JLocale;
import protocol.Contact;
import protocol.MessageEditor;

import java.util.Vector;


public final class ConferenceParticipants  {
    private static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");

    private Jabber protocol;
    private JabberServiceContact conference;
    private Vector contacts = new Vector();

    private final Icon[] leftIcons = new Icon[2];
    private final Icon[] rightIcons = new Icon[1];

    
    private static final int COMMAND_REPLY = 0;
    private static final int COMMAND_PRIVATE = 1;
    private static final int COMMAND_INFO = 2;
    private static final int COMMAND_STATUS = 3;
    private static final int COMMAND_COPY = 4;
    private static final int COMMAND_KICK = 5;
    private static final int COMMAND_BAN = 6;
    private static final int COMMAND_DEVOICE = 7;
    private static final int COMMAND_VOICE = 8;
    private static final int COMMAND_MEMBER    = 10; 
    private static final int COMMAND_MODER     = 11; 
    private static final int COMMAND_ADMIN     = 12; 
	private static final int COMMAND_OWNER     = 13; 
    private static final int COMMAND_NONE      = 14;  
	
	private static final int COMMAND_TIME = 16;
	private static final int COMMAND_IDLE = 17;
	private static final int COMMAND_PING = 18;

	private static final int COMMAND_INVITE = 19;
	
	private static final int GATE_COMMANDS = 30;

    private int myRole;
	private int myAffiliation;
    public ConferenceParticipants(Jabber jabber, JabberServiceContact conf) {
        //super(conf.getName());
        protocol = jabber;
        conference = conf;
		myAffiliation = getAffiliation(conference.getMyName());
        myRole = getRole(conference.getMyName());
        update();
    }

    protected final int getSize() {
        return contacts.size();
    }

    private String getCurrentContact() {
        /*int contactIndex = getCurrItem();
        if ((contactIndex < 0) || (getSize() <= contactIndex)) {
            return null;
        }
        Object o = contacts.elementAt(contactIndex);
        if (o instanceof JabberContact.SubContact) {
            JabberContact.SubContact c = (JabberContact.SubContact)o;
            return c.resource;
        }*/
        return null;
    }

    protected void doSawimAction(int action) {
        /*switch (action) {
            case NativeCanvas.Sawim_SELECT:
                if (conference.canWrite()) {
                    execSawimAction(COMMAND_REPLY);
                }
                return;

            case NativeCanvas.Sawim_BACK:
                back();
                return;

            case NativeCanvas.Sawim_MENU:
                showMenu(getMenu());
                return;
        }
        String nick = getCurrentContact();
        if (null == nick) {
            return;
        }
		JabberContact.SubContact c = conference.getExistSubContact(nick);
		String jid = Jid.realJidToSawimJid(conference.getUserId() + "/" + nick);
        switch (action) {
            case COMMAND_COPY:
                SawimUI.setClipBoardText(nick);
                restore();
                break;

            case COMMAND_REPLY:
                MessageEditor editor = ContactList.getInstance().getMessageEditor();
                if (editor.isActive(conference)) {
                    TextBoxView box = editor.getTextBox();
                    String text = box.getRawString();
                    if (!StringConvertor.isEmpty(text)) {
                        String space = box.getSpace();
                        if (text.endsWith(space)) {
                        } else if (1 == space.length()) {
                            text += space;
                        } else {
                            text += text.endsWith(" ") ? " " : space;
                        }
                        if (text.endsWith("," + space)) {
                            text += nick + "," + space;
                        } else {
                            text += nick + space;
                        }
                        box.setString(text);
                        box.show();
                        return;
                    }
                }
                protocol.getChat(conference).writeMessageTo(nick);
                break;

            case COMMAND_PRIVATE:
                nickSelected(nick);
                break;
			case GATE_COMMANDS:
			    AdHoc adhoc = new AdHoc(protocol, (JabberContact)conference);
				adhoc.setResource(c.resource);
				adhoc.show();
                break;

            case COMMAND_INFO:
                protocol.showUserInfo(getContactForVCard(nick));
                break;

            case COMMAND_STATUS:
                protocol.showStatus(getPrivateContact(nick));
                break;

            case COMMAND_KICK:
                kikField();
                break;

            case COMMAND_BAN:
                banField();
                break;

            case COMMAND_DEVOICE:
                setMucRole(nick, "v" + "isitor");
                update();
                restore();
                break;

            case COMMAND_VOICE:
                setMucRole(nick, "partic" + "ipant");
                update();
                restore();
                break;
            case COMMAND_MEMBER:                                      
                setMucAffiliation(nick, "m" + "ember");    
                update();                                             
				restore();                                            
                break;                                                

            case COMMAND_MODER:                                       
                setMucRole(nick, "m" + "oderator");        
				restore();                                            
                update();                                             
                break;                                                

            case COMMAND_ADMIN:                                       
                setMucAffiliation(nick, "a" + "dmin");     
                update();                                             
				restore();                                            
                break;                                                
				
			case COMMAND_OWNER:                                       
				setMucAffiliation(nick, "o" + "wner");
				update();                                             
				restore();                                            
                break;                                                

            case COMMAND_NONE:                                        
                setMucAffiliation(nick, "n" + "o" + "ne"); 
                update();                                             
				restore();                                            
                break;                                                
        }*/
    }

    /*protected final MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        menu.setActionListener(new Binder(this));
        String nick = getCurrentContact();
        if (null == nick) {
            return menu;
        }

        int defaultCode = -1;
        if (conference.canWrite()) {
            menu.addItem("reply", COMMAND_REPLY);
            defaultCode = COMMAND_REPLY;
        }
        menu.addItem("private_chat", COMMAND_PRIVATE);
        menu.addItem("info", COMMAND_INFO);
        menu.addItem("user_statuses", COMMAND_STATUS);
        menu.addItem("copy_text", COMMAND_COPY);
		menu.addSeparator();
		menu.addItem("commands", GATE_COMMANDS);
	    final int role = getRole(nick);
		final int affiliation = getAffiliation(nick);
		if (myAffiliation==JabberServiceContact.AFFILIATION_OWNER) 
            myAffiliation++;
		if (JabberServiceContact.ROLE_MODERATOR == myRole) {
		    if (JabberServiceContact.ROLE_MODERATOR > role) {
                menu.addItem("to_kick", COMMAND_KICK);
			}
			if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN && affiliation < myAffiliation) {
			    menu.addItem("to_ban", COMMAND_BAN);
			}
			if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                if (role == JabberServiceContact.ROLE_VISITOR) {
                    menu.addItem("to_voice", COMMAND_VOICE);
                } else {
                    menu.addItem("to_devoice", COMMAND_DEVOICE);
                }
            }
        }
		if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN) {
            if (affiliation < JabberServiceContact.AFFILIATION_ADMIN) {
                if (role == JabberServiceContact.ROLE_MODERATOR) {
                   menu.addItem("to_voice", COMMAND_VOICE);
                } else {
                   menu.addItem("to_moder", COMMAND_MODER);
                }
            }
            if (affiliation < myAffiliation) {
                if (affiliation != JabberServiceContact.AFFILIATION_NONE) {
                    menu.addItem("to_none", COMMAND_NONE);
                }
                if (affiliation != JabberServiceContact.AFFILIATION_MEMBER) {
                    menu.addItem("to_member", COMMAND_MEMBER);
                }
            }
        }
        if (myAffiliation >= JabberServiceContact.AFFILIATION_OWNER) {
            if (affiliation != JabberServiceContact.AFFILIATION_ADMIN) {
                menu.addItem("to_admin", COMMAND_ADMIN);
            }
            if (affiliation != JabberServiceContact.AFFILIATION_OWNER) {
                menu.addItem("to_owner", COMMAND_OWNER);
            }
        }
		if (myAffiliation >= JabberServiceContact.AFFILIATION_ADMIN) {
			menu.addItem("invite", COMMAND_INVITE);
	    }
        return menu;
    }*/

    private void update() {
        /*super.lock();
        int currentIndex = getCurrItem();
        contacts.removeAllElements();
        final int moderators = getContactCount(JabberServiceContact.ROLE_MODERATOR);
        final int participants = getContactCount(JabberServiceContact.ROLE_PARTICIPANT);
        final int visitors = getContactCount(JabberServiceContact.ROLE_VISITOR);
        
        addLayerToListOfSubcontacts("list_of_moderators", moderators, JabberServiceContact.ROLE_MODERATOR);
        addLayerToListOfSubcontacts("list_of_participants",  participants, JabberServiceContact.ROLE_PARTICIPANT);
        addLayerToListOfSubcontacts("list_of_visitors", visitors, JabberServiceContact.ROLE_VISITOR); 
        setCurrentItemIndex(currentIndex);
        super.unlock();*/
    }

    private int getRole(String nick) {
        JabberContact.SubContact c = conference.getContact(nick);
        int priority = (null == c) ? JabberServiceContact.ROLE_VISITOR : c.priority;
        return priority;
    }
	public final int getAffiliation(String nick) {
        JabberContact.SubContact c = getContact(nick);
        final int priorityA = (null == c) ? JabberServiceContact.AFFILIATION_NONE : c.priorityA;
        return priorityA;
	}
	
    private final int getContactCount(byte priority) {
        int count = 0;
        Vector subcontacts = conference.subcontacts;
        for(int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if((contact.priority) == priority) {
                count++;
            }
        }
        return(count);
    }

    private final JabberContact.SubContact getContact(String nick) {
        if (StringConvertor.isEmpty(nick)) {
            return null;
        }
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if (nick.equals(contact.resource)) {
                return contact;
            }
        }
        return null;
    }

    private void addLayerToListOfSubcontacts(String layer, int size, byte priority) {
        boolean hasLayer = false;
        contacts.addElement(JLocale.getString(layer) + "(" + size + ")");
        Vector subcontacts = conference.subcontacts;
        for (int i = 0; i < subcontacts.size(); ++i) {
            JabberContact.SubContact contact = (JabberContact.SubContact)subcontacts.elementAt(i);
            if (contact.priority == priority) {
                contacts.addElement(contact);
                hasLayer = true;
            }
        }
        if (!hasLayer) {
            contacts.removeElementAt(contacts.size() - 1);
            return;
        }
    }

    private Contact getPrivateContact(String nick) {
        String jid = Jid.realJidToSawimJid(conference.getUserId() + "/" + nick);
        return protocol.createTempContact(jid);
    }
    private Contact getContactForVCard(String nick) {
        String jid = Jid.realJidToSawimJid(conference.getUserId() + "/" + nick);
        return protocol.createTempContact(jid);
    }
    private void nickSelected(String nick) {
        String jid = Jid.realJidToSawimJid(conference.getUserId() + "/" + nick);
        JabberServiceContact c = (JabberServiceContact)protocol.getItemByUIN(jid);
        if (null == c) {
            c = (JabberServiceContact)protocol.createTempContact(jid);
            protocol.addTempContact(c);
        }
        c.activate(protocol);
    }

    public void setMucRole(String nick, String role) {
        protocol.getConnection().setMucRole(conference.getUserId(), nick, role);
    }
    public void setMucAffiliation(String nick, String affiliation) {
        JabberContact.SubContact c = conference.getExistSubContact(nick);
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliation(conference.getUserId(),
                c.realJid, affiliation);
    }
	public void setMucRoleR(String role, String setReason) {
        protocol.getConnection().setMucRoleR(conference.getUserId(), getCurrentContact(), role, setReason);
    }
	public void setMucAffiliationR(String affiliation, String setReason) {
        JabberContact.SubContact c = conference.getExistSubContact(getCurrentContact());
        if ((null == c) || (null == c.realJid)) {
            return;
        }
        protocol.getConnection().setMucAffiliationR(conference.getUserId(),
                c.realJid, affiliation, setReason);
    }
	
	
	
	private void showContactMenu(String nick) {
        String jid = Jid.realJidToSawimJid(conference.getUserId() + '/' + nick);
        JabberServiceContact c = (JabberServiceContact)protocol.createTempContact(jid);
        //MenuModel m = c.getContextMenu(protocol);
        //if (null != m) {
        //    new Select(m).show();
        //}
    }

	/*private TextBoxView banTextbox;
	private TextBoxView kikTextbox;
	private void banField() {
	    banTextbox = new TextBoxView().create("message", 1000);
		banTextbox.setTextBoxListener(this);
	    banTextbox.setString("");
        banTextbox.show();
	}
	private void kikField() {
	    kikTextbox = new TextBoxView().create("message", 1000);
		kikTextbox.setTextBoxListener(this);
	    kikTextbox.setString("");
        kikTextbox.show();
	}
	public void textboxAction(TextBoxView box, boolean ok) {
		String rzn = (box == banTextbox) ? banTextbox.getString() : kikTextbox.getString();
        String Nick = "";
		String myNick = conference.getMyName();
		String reason = "";
        if (rzn.charAt(0) == '!') {
            rzn=rzn.substring(1);
        } else {
            Nick = (myNick == null) ? myNick : myNick + ": ";
        }
        if (rzn.length() != 0 && myNick != null) {
            reason = Nick + rzn;
        } else {
            reason = Nick;
        }
        if ((box == banTextbox)) {
		    setMucAffiliationR("o" + "utcast",  reason);
		    banTextbox.back();
			return;
        }
		if ((box == kikTextbox)) {
		    setMucRoleR("n" + "o" + "ne", reason);
		    kikTextbox.back();
			return;
        }
    }*/
}


