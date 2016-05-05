package protocol.xmpp;

import java.util.concurrent.ConcurrentHashMap;

import protocol.Contact;
import protocol.XStatusInfo;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

/**
 * Created by gerc on 02.01.2016.
 */
public class XStatus {

    private static void sendXStatus(XmppConnection connection, String xstatus, String text) {
        String[] path = Util.explode(Util.xmlEscape(xstatus), ':');
        StringBuilder sb = new StringBuilder();
        String typeUrl = "http://jabber.org/protocol/" + path[0];

        sb.append("<iq type='set' id='").append(XmppConnection.generateId());
        sb.append("'><pubsub xmlns='http://jabber.org/protocol/pubsub'>");
        sb.append("<publish node='").append(typeUrl).append("'><item>");
        sb.append("<").append(path[0]).append(" xmlns='").append(Util.xmlEscape(typeUrl));
        if (1 == path.length) {
            sb.append("'/>");
        } else {
            sb.append("'><").append(path[1]);
            if (2 == path.length) {
                sb.append("/>");
            } else {
                sb.append("><").append(path[2]).append("/></").append(path[1]).append(">");
            }
            if (!StringConvertor.isEmpty(text)) {
                sb.append("<text>").append(Util.xmlEscape(text)).append("</text>");
            }
            sb.append("</").append(path[0]).append(">");
        }
        sb.append("</item></publish></pubsub></iq>");
        connection.putPacketIntoQueue(sb.toString());
    }

    public static String getQipXStatus(XmppConnection connection) {
        int x = connection.getXmpp().getProfile().xstatusIndex;
        if (XStatusInfo.XSTATUS_NONE == x) {
            return "";
        }
        String code = Xmpp.xStatus.getCode(x);
        if ((null == code) || !code.startsWith(XmppXStatus.XSTATUS_START)) {
            return "";
        }
        if (code.equals(XmppXStatus.XSTATUS_TEXT_NONE)) {
            return "";
        }
        String id = code.substring(XmppXStatus.XSTATUS_START.length());
        return "<x xmlns='" + S_FEATURE_XSTATUS + "' id='"
                + Util.xmlEscape(id) + "'><title>"
                + Util.xmlEscape(connection.getXmpp().getProfile().xstatusTitle)
                + "</title></x>";
    }

    public static final String S_FEATURE_XSTATUS = "http://qip.ru/x-status";

    public static void setXStatus(XmppConnection connection) {
        String xstatusCode = Xmpp.xStatus.getCode(connection.getXmpp().getProfile().xstatusIndex);
        if (null == xstatusCode) {
            return;
        }

        setXStatusToIcqTransports(connection);

        if (xstatusCode.startsWith(XmppXStatus.XSTATUS_START)) {
            connection.setStatus(connection.getNativeStatus(connection.getXmpp().getProfile().statusIndex),
                    connection.getXmpp().getProfile().statusMessage, Xmpp.PRIORITY);
            return;
        }
        final String mood = "mood";
        final String activity = "activity";
        if (!xstatusCode.startsWith(mood)) {
            sendXStatus(connection, mood, null);
        }
        if (!xstatusCode.startsWith(activity)) {
            sendXStatus(connection, activity, null);
        }
        if (xstatusCode.startsWith(mood) || xstatusCode.startsWith(activity)) {
            sendXStatus(connection, xstatusCode, connection.getXmpp().getProfile().xstatusTitle);
        }
    }

    public static void setXStatusToIcqTransport(XmppConnection connection, XmppServiceContact gate) {
        String xstatus = Xmpp.xStatus.getIcqXStatus(connection.getXmpp().getProfile().xstatusIndex);
        if (null == xstatus) {
            return;
        }
        String desc = "None".equals(xstatus) ? null : connection.getXmpp().getProfile().xstatusTitle;
        desc = StringConvertor.notNull(desc);
        if (gate.isOnline() && Jid.isPyIcqGate(gate.getUserId())) {
            String out = "<iq type='set' id='" + XmppConnection.generateId() + "' to='"
                    + Util.xmlEscape(gate.getUserId())
                    + "'><command xmlns='http://jabber.org/protocol/commands' node='setxstatus' action='complete'><x xmlns='jabber:x:data' type='submit'><field var='xstatus_desc'><value>"
                    + Util.xmlEscape(desc)
                    + "</value></field><field var='xstatus_name'><value>"
                    + Util.xmlEscape(xstatus)
                    + "</value></field></x></command></iq>";
            connection.putPacketIntoQueue(out);
        }
    }

    private static void setXStatusToIcqTransports(XmppConnection connection) {
        String x = Xmpp.xStatus.getIcqXStatus(connection.getXmpp().getProfile().xstatusIndex);
        if (null == x) {
            return;
        }
        ConcurrentHashMap<String, Contact> contacts = connection.getXmpp().getContactItems();
        for (Contact contact: contacts.values()) {
            XmppContact c = (XmppContact) contact;
            if (c.isOnline() && Jid.isPyIcqGate(c.getUserId())) {
                setXStatusToIcqTransport(connection, (XmppServiceContact) c);
            }
        }
    }

    public static String getXStatus(XmlNode x) {
        return (null == x) ? null : (XmppXStatus.XSTATUS_START + x.getId());
    }

    public static void setConferencesXStatus(XmppConnection connection, String status, String msg, int priority) {
        String xml;
        ConcurrentHashMap<String, Contact> contacts = connection.getXmpp().getContactItems();
        for (Contact c : contacts.values()) {
            XmppContact contact = (XmppContact) c;
            if (contact.isConference() && contact.isOnline()) {
                if (0 <= priority) {
                    xml = "<presence to='" + Util.xmlEscape(contact.getUserId()) + "'>";
                    xml += (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>");
                    xml += (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>");
                    xml += ServerFeatures.getCaps();
                    if (Vcard.myAvatarHash != null) {
                        xml += "<x xmlns='vcard-temp:x:update'><photo>" + Vcard.myAvatarHash + "</photo></x>";
                    }
                    xml += "</presence>";
                    connection.putPacketIntoQueue(xml);

                }
            }
        }
    }
}
