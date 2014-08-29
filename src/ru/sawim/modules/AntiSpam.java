

package ru.sawim.modules;

import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

import java.util.Vector;


public class AntiSpam {
    private static AntiSpam antiSpam = new AntiSpam();

    private Vector validUins = new Vector();
    private Vector uncheckedUins = new Vector();

    private AntiSpam() {
    }

    private void sendHelloMessage(Protocol protocol, Contact contact) {
        validUins.addElement(contact.getUserId());
        uncheckedUins.removeElement(contact.getUserId());
        if (protocol.isMeVisible(contact)) {
            protocol.sendMessage(contact, Options.getString(JLocale.getString(R.string.pref_antispam_hello)), false);
        }
    }

    private void sendQuestion(Protocol protocol, Contact contact) {
        if (uncheckedUins.contains(contact.getUserId())) {
            uncheckedUins.removeElement(contact.getUserId());
            return;
        }
        String message = Options.getString(JLocale.getString(R.string.pref_antispam_msg));
        if (protocol.isMeVisible(contact) && !StringConvertor.isEmpty(message)) {
            protocol.sendMessage(contact, "I don't like spam!\n" + message, false);
            uncheckedUins.addElement(contact.getUserId());
        }
    }

    private boolean isChecked(String uin) {
        if (validUins.contains(uin)) {
            validUins.removeElement(uin);
            return true;
        }
        return false;
    }

    private void denyAuth(Protocol protocol, Message message, boolean isSystem) {
        if (isSystem) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                protocol.autoDenyAuth(message.getSndrUin());
            }
        }
    }

    private boolean containsKeywords(String msg) {
        String opt = Options.getString(JLocale.getString(R.string.pref_antispam_keywords));
        if (0 == opt.length()) return false;
        if (5000 < msg.length()) {
            return true;
        }
        String[] keywords = Util.explode(opt.toLowerCase(), ' ');
        msg = msg.toLowerCase();
        for (int i = 0; i < keywords.length; ++i) {
            if (-1 != msg.indexOf(keywords[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean isSpamMessage(Protocol protocol, Message message, boolean isSystem, boolean isPlain) {
        if (!Options.getBoolean(JLocale.getString(R.string.pref_antispam_enable))) {
            return false;
        }
        String uin = message.getSndrUin();
        if (isChecked(uin)) {
            return false;
        }
        denyAuth(protocol, message, isSystem);
        if (!isPlain) {
            return true;
        }
        String msg = message.getText();
        if (msg.length() < 256) {
            //MagicEye.addAction(protocol, uin, "antispam", msg);
        }
        if (message.isOffline()) {
            return true;
        }
        Contact contact = protocol.createTempContact(uin);

        String[] msgs = Util.explode(Options.getString(JLocale.getString(R.string.pref_antispam_answer)), '\n');
        for (int i = 0; i < msgs.length; ++i) {
            if (msg.equalsIgnoreCase(msgs[i])) {
                sendHelloMessage(protocol, contact);
                return true;
            }
        }
        sendQuestion(protocol, contact);
        return true;
    }

    public static boolean isSpam(Protocol protocol, Message message, boolean isSystem, boolean isPlain) {
        if (Options.getBoolean(JLocale.getString(R.string.pref_antispam_enable)))
            if (antiSpam.containsKeywords(message.getText())) {
                antiSpam.denyAuth(protocol, message, isSystem);
                return true;
            }
        return antiSpam.isSpamMessage(protocol, message, isSystem, isPlain);
    }
}


