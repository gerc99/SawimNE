package protocol.xmpp;

import protocol.Contact;
import protocol.XStatusInfo;
import ru.sawim.R;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

/**
 * Created by gerc on 02.01.2016.
 */
public class Presences {

    public static void sendPresence(XmppConnection connection, XmppServiceContact conf) {
        String to = conf.getUserId();
        String xml = "";
        if (conf.isConference()) {
            to += "/" + conf.getMyName();
            String xNode = "";
            String password = conf.getPassword();
            if (!StringConvertor.isEmpty(password)) {
                xNode += "<password>" + Util.xmlEscape(password) + "</password>";
            }
            /*long time = conf.getLastMessageTransmitted();
            if (0 != time)
                xNode += "<history since='" + time + "'/>";*/
            HistoryStorage historyStorage = connection.getXmpp().getChat(conf).getHistory();
            //long time = historyStorage.getMessageTime(true);
            /*if (0 != time)
                xNode += "<history maxstanzas='"
                        + MESSAGE_COUNT_AFTER_CONNECT
                        + "' seconds='" + (SawimApplication.getCurrentGmtTime() / 1000 - time / 1000) + "'/>";*/

            if (!StringConvertor.isEmpty(xNode)) {
                xml += "<x xmlns='http://jabber.org/protocol/muc'>" + xNode + "</x>";
            }
            connection.queryMessageArchiveManagement(conf, null);
        }
        if (connection.getXmpp().getProfile() != null) {
            String status = connection.getNativeStatus(connection.getXmpp().getProfile().statusIndex);
            if (!StringConvertor.isEmpty(status)) {
                xml += "<show>" + status + "</show>";
            }
        }
        String xstatusTitle = null;
        if (connection.getXmpp().getProfile() != null) {
            xstatusTitle = connection.getXmpp().getProfile().xstatusTitle;
            String descr = connection.getXmpp().getProfile().xstatusDescription;
            if (!StringConvertor.isEmpty(descr)) {
                xstatusTitle += " " + descr;
            }
        }
        xml += (StringConvertor.isEmpty(xstatusTitle) ? "" : "<status>" + Util.xmlEscape(xstatusTitle) + "</status>");

        xml = "<presence to='" + Util.xmlEscape(to) + "'>" + xml + ServerFeatures.getCaps();
        if (Vcard.myAvatarHash != null) {
            xml += "<x xmlns='vcard-temp:x:update'><photo>" + Vcard.myAvatarHash + "</photo></x>";
        }
        xml += "</presence>";
        connection.putPacketIntoQueue(xml);
    }

    public static void sendPresenceUnavailable(XmppConnection connection, String to) {
        connection.putPacketIntoQueue("<presence type='unavailable' to='" + Util.xmlEscape(to)
                + "'><status>I'll be back</status></presence>");
    }

    public static void parsePresence(XmppConnection connection, XmlNode x) {
        final String fromFull = x.getAttribute(XmlConstants.S_FROM);
        final String from = Jid.getBareJid(fromFull);
        final String fromRes = Jid.getResource(fromFull, "");
        String type = x.getAttribute(XmlConstants.S_TYPE);

        if (XmlConstants.S_ERROR.equals(type)) {
            XmlNode errorNode = x.getFirstNode(XmlConstants.S_ERROR);

            /*DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + errorNode.getFirstNodeValue(S_TEXT));
            */

            boolean showError = Jid.isGate(from);
            XmppContact conf = (XmppContact) connection.getXmpp().getItemByUID(from);
            if (null != conf && conf.isConference()) {
                int code = Util.strToIntDef(errorNode.getAttribute(XmlConstants.S_CODE), -1);
                ((XmppServiceContact) conf).nickError(connection.getXmpp(), fromRes, code, connection.getError(errorNode));
                return;
            }
            if (showError) {
                connection.getXmpp().addMessage(new SystemNotice(connection.getXmpp(),
                        SystemNotice.SYS_NOTICE_ERROR, from, connection.getError(errorNode)));
            }

            Contact c = connection.getXmpp().getItemByUID(from);
            if (null == c) {
                return;
            }
            c.setOfflineStatus();
            return;
        }

        if (("subscribe").equals(type)) {
            if (connection.isAutoGateContact(from)) {
                connection.sendSubscribed(from);
                connection.requestSubscribe(from);
            } else {
                connection.getXmpp().addMessage(new SystemNotice(connection.getXmpp(), SystemNotice.SYS_NOTICE_AUTHREQ, from, null));
            }
            Contact c = connection.getXmpp().getItemByUID(from);
            connection.autoRenameContact(c, x);
            connection.autoMoveContact(c, x);
            return;
        }
        if (("subscribed").equals(type)) {
            if (!connection.isAutoGateContact(from)) {
                connection.getXmpp().setAuthResult(from, true);
            }
            connection.autoRenameContact(connection.getXmpp().getItemByUID(from), x);
            return;
        }
        if (("unsubscribed").equals(type)) {
            connection.getXmpp().setAuthResult(from, false);
            return;
        }
        if (null == type) {
            type = x.getFirstNodeValue("show");
        }
        if (null == type) {
            type = "";
        }

        XmppContact contact = (XmppContact) connection.getXmpp().getItemByUID(from);
        if (null == contact) {
            String fullJid = Jid.realJidToSawimJid(fromFull);
            contact = (XmppContact) connection.getXmpp().getItemByUID(fullJid);
            if (null == contact) {
                return;
            }
        }

        XmlNode vcardUpdate = x.getXNode("vcard-temp:x:update");
        String newAvatarHash = null;
        if (vcardUpdate != null) {
            newAvatarHash = vcardUpdate.getFirstNodeValue("photo");
        }
        int priority = Util.strToIntDef(x.getFirstNodeValue("priority"), 0);
        String statusString = x.getFirstNodeValue(XmlConstants.S_STATUS);
        if (contact.isConference()) {
            XmlNode xMuc = x.getXNode("http://jabber.org/protocol/muc#user");
            int code = 0;
            if (null != xMuc) {
                code = Util.strToIntDef(xMuc.getFirstNodeAttribute(XmlConstants.S_STATUS, XmlConstants.S_CODE), 0);
            }
            XmlNode item = (null == xMuc) ? null : xMuc.getFirstNode(XmlConstants.S_ITEM);
            XmppServiceContact conf = (XmppServiceContact) contact;
            String reasone = null;
            String rangVoice = null;
            String roleVoice = null;
            priority = 0;
            int priorityA = 0;

            if (null != item) {
                String affiliation = item.getAttribute(XmlNode.S_AFFILIATION);
                if (("member").equals(affiliation)) {
                    priorityA = XmppServiceContact.AFFILIATION_MEMBER;
                    //rangVoice = JLocale.getString("member");
                } else if (("owner").equals(affiliation)) {
                    priorityA = XmppServiceContact.AFFILIATION_OWNER;
                    rangVoice = JLocale.getString(R.string.owner);
                } else if (("admin").equals(affiliation)) {
                    priorityA = XmppServiceContact.AFFILIATION_ADMIN;
                    rangVoice = JLocale.getString(R.string.admin);
                } else {
                    priorityA = XmppServiceContact.AFFILIATION_NONE;
                    rangVoice = JLocale.getString(R.string.none);
                }
                String role = item.getAttribute(XmlNode.S_ROLE);
                if (("moderator").equals(role)) {
                    priority = XmppServiceContact.ROLE_MODERATOR;
                    roleVoice = JLocale.getString(R.string.moderator);
                } else if (("participant").equals(role)) {
                    priority = XmppServiceContact.ROLE_PARTICIPANT;
                    roleVoice = JLocale.getString(R.string.participant);
                } else if (XmlConstants.S_NONE.equals(role)) {
                    reasone = item.getFirstNodeValue("reason");
                    item = null;
                    roleVoice = JLocale.getString(R.string.reason);
                } else {
                    priority = XmppServiceContact.ROLE_VISITOR;
                    roleVoice = JLocale.getString(R.string.visitor);
                }
            }
            if (rangVoice != null) rangVoice += "/" + roleVoice;
            else rangVoice = roleVoice;
            connection.getXmpp().setConfContactStatus(conf, fromRes,
                    connection.nativeStatus2StatusIndex(type), statusString, priority, priorityA, rangVoice);
            if (null != item) {
                XmppContact.SubContact sc = conf.getExistSubContact(fromRes);
                if (sc != null) {
                    String id = conf.getUserId() + "/" + fromRes;
                    Vcard.requestVCard(connection, id, newAvatarHash, sc.avatarHash);
                }
                String newNick = item.getAttribute(XmlNode.S_NICK);
                String realJid = item.getAttribute(XmlNode.S_JID);
                if (null != newNick) {
                    connection.getXmpp().setConfContactStatus(conf, newNick,
                            connection.nativeStatus2StatusIndex(""), "", priority, priorityA, rangVoice);
                    conf.nickChainged(connection.getXmpp(), fromRes, newNick);
                } else {
                    StringBuilder s = new StringBuilder();
                    if (statusString != null) {
                        s.append('(').append(statusString).append(") ");
                    }
                    s.append(StringConvertor.notNull(reasone));
                    if (null != realJid) {
                        s.append(Jid.getBareJid(realJid)).append(" ");
                    }
                    conf.nickOnline(connection.getXmpp(), fromRes, s.toString());
                }
                if (null != realJid) {
                    conf.setRealJid(fromRes, Jid.getBareJid(realJid));
                }
                contact.setClient(fromRes, x.getFirstNodeAttribute("c", XmlConstants.S_NODE));
                if (303 == code) {
                    conf.addPresence(connection.getXmpp(), fromRes, ": " + JLocale.getString(R.string.change_nick) + " " + StringConvertor.notNull(newNick));
                }
            } else {
                conf.nickOffline(connection.getXmpp(), fromRes, code, StringConvertor.notNull(reasone), StringConvertor.notNull(statusString));
            }
            if (conf.getMyName().equals(fromRes)) {
                connection.getXmpp().ui_changeContactStatus(conf);
            }
            connection.updateConfPrivate(conf, fromRes);
            if (RosterHelper.getInstance().getUpdateChatListener() != null)
                RosterHelper.getInstance().getUpdateChatListener().updateMucList();
        } else {
            Vcard.requestVCard(connection, contact.getUserId(), newAvatarHash, contact.avatarHash);
            if (!("unavailable").equals(type)) {
                if ((XStatusInfo.XSTATUS_NONE == contact.getXStatusIndex())
                        || !Xmpp.xStatus.isPep(contact.getXStatusIndex())) {
                    XmlNode xNode = x.getXNode(XStatus.S_FEATURE_XSTATUS);
                    String id = XStatus.getXStatus(xNode);

                    String xtext = null;
                    if (null != id) {
                        xtext = xNode.getFirstNodeValue(XmlConstants.S_TITLE);
                        String s = StringConvertor.notNull(statusString);
                        if (StringConvertor.isEmpty(xtext)) {
                            xtext = null;
                        } else if (s.startsWith(xtext)) {
                            xtext = statusString;
                            statusString = null;
                        }
                    }
                    contact.setXStatus(id, xtext);
                }
                if (Jid.isPyIcqGate(from)) {
                    XStatus.setXStatusToIcqTransport(connection, (XmppServiceContact) contact);
                }
            }
            connection.getXmpp().setContactStatus(contact, fromRes, connection.nativeStatus2StatusIndex(type), statusString, priority);
            contact.updateMainStatus(connection.getXmpp());
            if (contact.isOnline()) {
                contact.setClient(fromRes, x.getFirstNodeAttribute("c", XmlConstants.S_NODE));
            }
            if (contact.getUserId().equals(contact.getName())) {
                connection.getXmpp().renameContact(contact, connection.getNickFromNode(x));
            }
            connection.getXmpp().ui_changeContactStatus(contact);
        }
    }
}
