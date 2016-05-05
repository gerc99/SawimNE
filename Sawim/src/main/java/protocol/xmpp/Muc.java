package protocol.xmpp;

import java.util.concurrent.ConcurrentHashMap;

import protocol.Contact;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

/**
 * Created by gerc on 02.01.2016.
 */
public class Muc {

    private static final boolean xep0048 = false;
    public static AffiliationListConf affListConf = null;

    public static String getConferenceStorage(XmppConnection connection) {
        StringBuilder xml = new StringBuilder();
        ConcurrentHashMap<String, Contact> contacts = connection.getXmpp().getContactItems();
        xml.append("<storage xmlns='storage:bookmarks'>");
        for (Contact contact1 : contacts.values()) {
            XmppContact contact = (XmppContact) contact1;
            if (!contact.isConference() || contact.isTemp()) {
                continue;
            }
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);

            XmppServiceContact conf = (XmppServiceContact) contact;
            xml.append("<conference autojoin='");
            xml.append(conf.isAutoJoin() ? XmlConstants.S_TRUE : XmlConstants.S_FALSE);
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUserId()));
            if (!StringConvertor.isEmpty(conf.getPassword())) {
                xml.append("' password='");
                xml.append(Util.xmlEscape(conf.getPassword()));
            }
            xml.append("'><nick>");
            xml.append(Util.xmlEscape(conf.getMyName()));
            xml.append("</nick></conference>");
        }
        xml.append("</storage>");
        return xml.toString();
    }

    public static void getBookmarks(XmppConnection connection) {
        connection.putPacketIntoQueue("<iq type='get' id='0'><query xmlns='jabber:iq:private'><storage xmlns='storage:bookmarks'/></query></iq>");
        if (xep0048) {
            connection.putPacketIntoQueue("<iq type='get' id='1'><pubsub xmlns='http://jabber.org/protocol/pubsub'><items node='storage:bookmarks'/></pubsub></iq>");
        }
    }

    public static void saveConferences(XmppConnection connection) {
        StringBuilder xml = new StringBuilder();
        String storage = getConferenceStorage(connection);
        xml.append("<iq type='set'><query xmlns='jabber:iq:private'>");
        xml.append(storage);
        xml.append("</query></iq>");
        if (xep0048) {
            xml.append("<iq type='set'>");
            xml.append("<pubsub xmlns='http://jabber.org/protocol/pubsub'>");
            xml.append("<publish node='storage:bookmarks'><item id='current'>");
            xml.append(storage);
            xml.append("</item></publish></pubsub></iq>");
        }
        connection.putPacketIntoQueue(xml.toString());
    }

    public static void requestAffiliationListConf(XmppConnection connection, String jidConference, String affiliation) {
        connection.putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jidConference)
                + "' id='" + Util.xmlEscape(affiliation)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation) + "'/></query></iq>");
        affListConf = connection.getXmpp().getAffiliationListConf();
    }

    public static void setAffiliationListConf(XmppConnection connection, String jidConference, String jidItem, String setAffiliation, String setReason) {
        connection.putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jidConference)
                + "' id='admin_modify'><query xmlns='http://jabber.org/protocol/muc#admin'><item jid='"
                + Util.xmlEscape(jidItem) + "' affiliation='" + Util.xmlEscape(setAffiliation)
                + "'><reason>" + Util.xmlEscape(setReason) + "</reason></item></query></iq>");
        affListConf = connection.getXmpp().getAffiliationListConf();
    }

    public static void sendInvite(XmppConnection connection, String jidMessTo, String jidInviteTo, String setReason) {
        connection.putPacketIntoQueue("<message to='" + Util.xmlEscape(jidMessTo)
                + "'><x xmlns='http://jabber.org/protocol/muc#user'><invite to='"
                + Util.xmlEscape(jidInviteTo) + "'><reason>" + Util.xmlEscape(setReason) + "</reason></invite></x></message>");
    }

    public static void setMucRole(XmppConnection connection, String jid, String nick, String role) {
        connection.putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'/></query></iq>");
    }

    public static void setMucAffiliation(XmppConnection connection, String jid, String userJid, String affiliation) {
        connection.putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'/></query></iq>");
    }

    public static void setMucRoleR(XmppConnection connection, String jid, String nick, String role, String setReason) {
        connection.putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='itemmuc'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'><reason>" + Util.xmlEscape(setReason) + "</reason></item>"
                + "</query></iq>");
    }

    public static void setMucAffiliationR(XmppConnection connection, String jid, String userJid, String affiliation, String setReason) {
        connection.putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='itemmuc'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'><reason>" + Util.xmlEscape(setReason) + "</reason></item>"
                + "</query></iq>");
    }
}
