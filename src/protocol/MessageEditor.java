

package protocol;

import sawim.Options;
import sawim.cl.ContactList;


public class MessageEditor {
    /*private TextBoxView messageTextbox;
    private Protocol protocol = null;
    private Contact toContact = null;

    
    public MessageEditor() {
        createTextBox();
    }
    private void createTextBox() {
        int size = 5000;
        
        size = 10000;
        
        messageTextbox = new TextBoxView().create("message", size, "send");
        messageTextbox.setCancelNotify(true);
    }
    public void writeMessage(Protocol p, Contact to, String message) {
        boolean recreate = Options.getBoolean(Options.OPTION_RECREATE_TEXTBOX);
        String prevText = null;
        if (recreate) {
            prevText = messageTextbox.getRawString();
            createTextBox();

        } else {
            messageTextbox.updateCommands();
        }

        
        if (null != message) {
            messageTextbox.setString(message);

        
        } else if (toContact != to) {
            messageTextbox.setString(null);

        } else if (recreate) {
            messageTextbox.setString(prevText);
        }

        if (toContact != to) {
            protocol = p;
            toContact = to;
            
            messageTextbox.setCaption(" " + to.getName());
        }
        protocol.sendTypingNotify(to, true);

        messageTextbox.setTextBoxListener(this);
        messageTextbox.show();
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        protocol.sendTypingNotify(toContact, false);
        if (ok) {
            String text = messageTextbox.getString();
            if (!toContact.isSingleUserContact() && text.endsWith(", ")) {
                text = "";
            }
            protocol.sendMessage(toContact, text, true);
            if (toContact.hasChat()) {
                protocol.getChat(toContact).activate();
            } else {
                ContactList.getInstance().activate();
            }
            messageTextbox.setString(null);
            return;
        }
    }
    public void insert(String text) {

    }
    public TextBoxView getTextBox() {
        return messageTextbox;
    }
    public boolean isActive(Contact c) {
        return c == toContact;
    }*/
}

