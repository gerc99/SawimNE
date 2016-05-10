package protocol.xmpp;

import android.text.TextUtils;

import protocol.Contact;
import protocol.XStatusInfo;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.chat.message.*;
import ru.sawim.comm.*;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by gerc on 02.01.2016.
 */
public class Messages {

    public static void sendMessage(XmppConnection connection, String to, String msg, String type, boolean notify, String id) {
        if (TextUtils.isEmpty(id)) id = XmppConnection.generateId();
        to = Jid.SawimJidToRealJid(to);
        boolean buzz = msg.startsWith(PlainMessage.CMD_WAKEUP) && XmlConstants.S_CHAT.equals(type);
        if (buzz) {
            type = XmlConstants.S_HEADLINE;
            notify = false;
            if (!connection.getXmpp().isContactOverGate(to)) {
                msg = msg.substring(PlainMessage.CMD_WAKEUP.length()).trim();
                if (StringConvertor.isEmpty(msg)) {
                    msg = "/me " + JLocale.getString(R.string.wake_you_up);
                }
            }
        }

        String chatState = "";
        if (XmlConstants.S_CHAT.equals(type)) {
            chatState = connection.getChatStateTag(XmlConstants.S_ACTIVE);
        }
        connection.putPacketIntoQueue("<message to='" + Util.xmlEscape(to) + "'"
                + " type='" + type + "' id='" + Util.xmlEscape(id) + "'>"
                + (buzz ? "<attention xmlns='urn:xmpp:attention:0'/>" : "")
                + "<body>" + Util.xmlEscape(msg) + "</body>"
                + (notify ? "<request xmlns='urn:xmpp:receipts'/><x xmlns='jabber:x:event'><offline/><delivered/></x>" : "")
                + chatState
                + "</message>");
    }

    public static void sendMessage(XmppConnection connection, String to, String msg) {
        String type = XmlConstants.S_CHAT;
        if (connection.getXmpp().getItemByUID(Jid.getBareJid(to)).isConference() && (-1 == to.indexOf('/'))) {
            type = XmlConstants.S_GROUPCHAT;
        }
        sendMessage(connection, to, msg, type, false, XmppConnection.generateId());
    }

    public static void sendMessage(XmppConnection connection, PlainMessage message) {
        String to = message.getRcvrUin();
        XmppContact toContact = (XmppContact) connection.getXmpp().getItemByUID(to);
        if (null != toContact) {
            to = toContact.getReciverJid();
        }
        String type = XmlConstants.S_CHAT;
        if (connection.getXmpp().getItemByUID(Jid.getBareJid(to)).isConference() && (-1 == to.indexOf('/'))) {
            type = XmlConstants.S_GROUPCHAT;
        }

        sendMessage(connection, to, message.getText(), type, XmlConstants.S_CHAT.equals(type),
                message.getMessageId());
    }

    public static void sendTypingNotify(XmppConnection connection, String to, boolean composing) {
        String tag = connection.getChatStateTag(composing ? XmlConstants.S_COMPOSING : XmlConstants.S_ACTIVE/*S_PAUSED*/);
        connection.putPacketIntoQueue("<message to='" + Util.xmlEscape(to)
                + "' id='0'>" + tag + "</message>");
    }

    private static void prepareFirstPrivateMessage(XmppConnection connection, String jid) {
        final XmppServiceContact conf =
                (XmppServiceContact) connection.getXmpp().getItemByUID(Jid.getBareJid(jid));
        if (null == conf) {
            return;
        }

        XmppContact.SubContact sub = conf.getExistSubContact(Jid.getResource(jid, ""));
        if (null == sub) {
            return;
        }
        if (XmppServiceContact.ROLE_MODERATOR == sub.priority) {
            connection.getXmpp().addTempContact(connection.getXmpp().createTempContact(jid, false));
        }
    }

    public static void parseMessage(XmppConnection connection, XmlNode msg) {
        msg.removeNode("html");
        if (parseMamMessage(connection, msg)) {
            return;
        }
        if (parseCarbonMessage(connection, msg)) {
            return;
        }
        String fullJid = msg.getAttribute(XmlConstants.S_FROM);
        String type = msg.getAttribute(XmlConstants.S_TYPE);
        boolean isGroupchat = ("groupchat").equals(type);
        boolean isError = XmlConstants.S_ERROR.equals(type);

        final Contact contact = connection.getXmpp().getItemByUID(Jid.getBareJid(fullJid));
        boolean isConference = contact != null && contact.isConference();
        if (!isGroupchat) {
            fullJid = Jid.realJidToSawimJid(fullJid);
        }
        String from = Jid.getBareJid(fullJid);
        if (from.equals(connection.getXmpp().getUserId())) {
            XmlNode addresses = msg.getFirstNode("addresses");
            if (null != addresses) {
                String ofrom = null;
                while (0 < addresses.childrenCount()) {
                    XmlNode address = addresses.popChildNode();
                    if ("ofrom".equals(address.getAttribute(XmlConstants.S_TYPE))) {
                        ofrom = address.getAttribute(XmlNode.S_JID);
                        break;
                    }
                }
                if (null != ofrom) {
                    fullJid = ofrom;
                    if (!isGroupchat) {
                        fullJid = Jid.realJidToSawimJid(fullJid);
                    }
                    from = Jid.getBareJid(fullJid);
                    isConference = connection.getXmpp().getItemByUID(Jid.getBareJid(fullJid)).isConference();
                }
            }
        }

        if (isConference && !isGroupchat) {
            from = fullJid;
        }
        String fromRes = Jid.getResource(fullJid, null);
        String subject = msg.getFirstNodeValue(XmlConstants.S_SUBJECT);
        String text = msg.getFirstNodeValue(XmlConstants.S_BODY);
        if ((null != subject) && (null == text)) {
            text = "";
        }
        if ("jubo@nologin.ru".equals(from) && msg.contains("juick")) {
            parseBlogMessage(connection, "juick@juick.com", msg, text, "JuBo");
            return;
        }
        if (connection.getXmpp().isBlogBot(from)) {
            parseBlogMessage(connection, from, msg, text, null);
            return;
        }

        if (!isConference) {
            if (msg.contains("attention")) {
                type = XmlConstants.S_CHAT;
                text = PlainMessage.CMD_WAKEUP;
                subject = null;
            }
        }

        final String date = getDate(msg);
        final boolean isOnlineMessage = (null == date);

        if (!(isConference && isGroupchat) && isOnlineMessage) {
            parseChatState(connection, msg, from);
        }

        if (null == text) {
            XmlNode received = msg.getFirstNode("received");
            if (null != received) {
                String id = received.getId();
                if (null == id) {
                    id = msg.getId();
                }
                setMessageSended(connection, Jid.getBareJid(fullJid), id, PlainMessage.NOTIFY_FROM_CLIENT);
                return;
            }
            if (!isConference && !isError) {
                parseMessageEvent(connection, Jid.getBareJid(fullJid), msg.getXNode("jabber:x:event"), from);
                parseEvent(connection, msg.getFirstNode("event"), fullJid);
            }
            return;
        }

        msg.removeNode(XmlConstants.S_SUBJECT);
        msg.removeNode(XmlConstants.S_BODY);
        if ((null != subject) && (!text.contains(subject))) {
            text = subject + "\n\n" + text;
        }
        text = StringConvertor.trim(text);

        Message message = null;
        long time = isOnlineMessage ? SawimApplication.getCurrentGmtTime() : /*new Delay().getTime(date)*/parseTimestamp(date);
        final XmppContact c = (XmppContact) connection.getXmpp().getItemByUID(from);
        if (msg.contains(XmlConstants.S_ERROR)) {
            final String errorText = connection.getError(msg.getFirstNode(XmlConstants.S_ERROR));
            if (null != errorText) {
                text = errorText + "\n-------\n" + text;
            }

        } else {
            if ((null != c) && msg.contains("captcha")) {
                final XmppForm form = new XmppForm(XmppForm.TYPE_CAPTCHA, connection.getXmpp(), from);
                form.showCaptcha(msg);
                return;
            }

            final XmlNode oobNode = msg.getXNode("jabber:x:oob");
            if (null != oobNode) {
                String url = oobNode.getFirstNodeValue(XmlConstants.S_URL);
                if (null != url) {
                    text += "\n\n" + url;
                    String desc = oobNode.getFirstNodeValue(XmlConstants.S_DESC);
                    if (null != desc) {
                        text += "\n" + desc;
                    }
                    msg.removeNode(XmlConstants.S_URL);
                    msg.removeNode(XmlConstants.S_DESC);
                }
            }

            if (!isGroupchat && msg.contains("request") && (null != msg.getId())) {
                connection.putPacketIntoQueue("<message to='" + Util.xmlEscape(fullJid)
                        + "' id='" + Util.xmlEscape(msg.getId())
                        + "'><received xmlns='urn:xmpp:receipts' id='"
                        + Util.xmlEscape(msg.getId()) + "'/></message>");
            }

            if (c instanceof XmppServiceContact) {
                XmppServiceContact conference = (XmppServiceContact) c;
                isConference = conference.isConference();
                XmlNode xMuc = msg.getXNode("http://jabber.org/protocol/muc#user");
                if (null != xMuc) {
                    int code = Util.strToIntDef(xMuc.getFirstNodeAttribute(XmlConstants.S_STATUS, XmlConstants.S_CODE), 0);
                    if (code == 100) {
                        return;
                    }
                }
                if (null != subject && isConference && isGroupchat) {
                    String prevSubject = StringConvertor.notNull(conference.getSubject());
                    conference.setSubject(subject);
                    connection.getXmpp().ui_changeContactStatus(conference);
                    fromRes = null;
                    boolean isOldSubject = prevSubject.equals(subject);
                    if (isOldSubject) {
                        return;
                    } else if (!StringConvertor.isEmpty(prevSubject) && !isOldSubject) {
                        message = new SystemNotice(connection.getXmpp(), SystemNotice.SYS_NOTICE_CUSTOM, from, JLocale.getString(R.string.new_subject), text);
                    }
                }
            }
        }
        if (StringConvertor.isEmpty(text)) {
            return;
        }
        if (subject == null) {
            message = new PlainMessage(from, connection.getXmpp(), time, text, !isOnlineMessage);
        } else if (!isGroupchat) {
            message = new SystemNotice(connection.getXmpp(), SystemNotice.SYS_NOTICE_MESSAGE, from, text);
        }
        if (null == c) {
            if (isConference && !isGroupchat) {
                prepareFirstPrivateMessage(connection, from);
            }
        } else {
            if (isConference) {
                final XmppServiceContact conf = (XmppServiceContact) c;
                if (isGroupchat && (null != fromRes)) {
                    if (isOnlineMessage && fromRes.equals(conf.getMyName())) {
                        if (HistoryStorage.isMessageExist(msg.getId())) {
                            setMessageSended(connection, conf.getUserId(), msg.getId(),
                                    PlainMessage.NOTIFY_FROM_CLIENT);
                            return;
                        }
                        if (Jid.isIrcConference(fullJid)) {
                            return;
                        }
                    }
                    if (message != null)
                        message.setName(conf.getNick(fromRes));
                }
            } else {
                c.setActiveResource(fromRes);
            }
        }
        String xmlns = msg.getXmlns();
        if (xmlns == null || !xmlns.equals("p1:pushed")) {
            if (subject == null && isConference && !isOnlineMessage) {
                if (HistoryStorage.getHistory(connection.getXmpp().getUserId(), c.getUserId()).hasLastMessage(connection.getXmpp().getChat(c), message)) {
                    return;
                }
            }
        }
        if (message != null) {
            connection.getXmpp().addMessage(message, XmlConstants.S_HEADLINE.equals(type), isConference);
        }
    }

    private static boolean parseMamMessage(XmppConnection connection, XmlNode msg) {
        XmlNode finMamXmlNode = msg.getFirstNode("fin", "urn:xmpp:mam:0");
        if (finMamXmlNode != null) {
            connection.messageArchiveManagement.processFin(connection, finMamXmlNode);
            return true;
        }
        XmlNode mamXmlNode = msg.getFirstNode("result", "urn:xmpp:mam:0");
        if (mamXmlNode != null) {
            String serverMsgId = mamXmlNode.getId();
            final MessageArchiveManagement.Query query = connection.messageArchiveManagement.findQuery(mamXmlNode.getAttribute("queryid"));
            XmlNode forwardedXmlNode = mamXmlNode.getFirstNode("forwarded", "urn:xmpp:forward:0");
            final String date = getDate(forwardedXmlNode);
            final boolean isOnlineMessage = (null == date);
            long time = isOnlineMessage ? SawimApplication.getCurrentGmtTime() : parseTimestamp(date);
            msg = forwardedXmlNode.getFirstNode("message");

            String text = msg.getFirstNodeValue(XmlConstants.S_BODY);
            String type = msg.getAttribute(XmlConstants.S_TYPE);
            String from = msg.getAttribute(XmlConstants.S_FROM);
            String to = msg.getAttribute(XmlConstants.S_TO);
            boolean isGroupchat = ("groupchat").equals(type);
            String fromRes = Jid.getResource(from, null);
            String title;
            boolean isIncoming = false;
            if (from != null && to != null && Jid.getBareJid(from).equals(Jid.getBareJid(connection.fullJid_))) {
                isIncoming = false;
                title = to;
            } else if (from != null && to != null) {
                isIncoming = true;
                title = from;
            } else {
                //Log.e("MAM ERROR MEASSAGE", from + " " + to);
                title = from;
            }
            from = Jid.getBareJid(title);
            XmppContact c = (XmppContact) connection.getXmpp().getItemByUID(from);
            if (c == null) {
                c = (XmppContact) connection.getXmpp().createTempContact(from, false);
                connection.getXmpp().addLocalContact(c);
            }
            Message message = new PlainMessage(from, isIncoming ? connection.getXmpp().getUserId() : from, time, text, !isOnlineMessage, !isIncoming);
            message.setServerMsgId(serverMsgId);
            if (isGroupchat && fromRes != null) {
                final XmppServiceContact conf = (XmppServiceContact) c;
                message.setName(conf.getNick(fromRes));
            }
            HistoryStorage.getHistory(connection.getProtocol().getUserId(), c.getUserId()).addMessageToHistory(c, message, Chat.getFrom(c, connection.getProtocol(), message), false);
            if (query != null) {
                query.incrementMessageCount();
            }
            if (!c.isConference()) {
                if (query == null) {
                    connection.messageArchiveManagement.queryReverse(connection, c);
                } else {
                    if (query.getWith() == null) {
                        connection.messageArchiveManagement.queryReverse(connection, c, query.getStart());
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static boolean parseCarbonMessage(XmppConnection connection, XmlNode carbonMsg) {
        XmlNode forwarded;
        XmlNode receivedNode = carbonMsg.getFirstNode("received", "urn:xmpp:carbons:2");
        XmlNode sentNode = carbonMsg.getFirstNode("sent", "urn:xmpp:carbons:2");
        if (receivedNode != null) {
            forwarded = receivedNode.getFirstNode("forwarded", "urn:xmpp:forward:0");
        } else if (sentNode != null) {
            forwarded = sentNode.getFirstNode("forwarded", "urn:xmpp:forward:0");
        } else {
            return false;
        }
        if (forwarded == null) {
            return false;
        }
        XmlNode msg = forwarded.getFirstNode("message");
        if (msg == null) {
            return false;
        }
        String type = msg.getAttribute(XmlConstants.S_TYPE);
        if (msg.getFirstNode("x","http://jabber.org/protocol/muc#user") != null
                && "chat".equals(type)) {
            return false;
        }

        String fullJid;
        boolean isIncoming = receivedNode != null;
        if (isIncoming) {
            fullJid = msg.getAttribute(XmlConstants.S_FROM);
        } else {
            fullJid = msg.getAttribute(XmlConstants.S_TO);
        }
        if (fullJid == null) {
            return false;
        }

        final String date = getDate(carbonMsg);
        final boolean isOnlineMessage = (null == date);

        if (isIncoming && isOnlineMessage) {
            parseChatState(connection, msg, Jid.getBareJid(fullJid));
        }

        String text = msg.getFirstNodeValue(XmlConstants.S_BODY);
        if (text == null) {
            return false;
        }

        final Contact contact = connection.getXmpp().getItemByUID(Jid.getBareJid(fullJid));
        boolean isConference = contact != null && contact.isConference();
        if (!isConference) {
            fullJid = Jid.getBareJid(fullJid);
        }

        long time = isOnlineMessage ? SawimApplication.getCurrentGmtTime() : parseTimestamp(date);

        XmppContact c = (XmppContact) connection.getXmpp().getItemByUID(fullJid);
        if (null == c) {
            if (isConference) {
                prepareFirstPrivateMessage(connection, fullJid);
            }
        }

        Message message = new PlainMessage(fullJid, isIncoming ? connection.getXmpp().getUserId() : fullJid, time, text, !isOnlineMessage, isIncoming);
        connection.getXmpp().addMessage(message, XmlConstants.S_HEADLINE.equals(type), isConference);
        return true;
    }

    private static void parseBlogMessage(XmppConnection connection, String to, XmlNode msg, String text, String botNick) {
        text = StringConvertor.notNull(text);
        String userNick = connection.getNickFromNode(msg);
        if (null == userNick) {
            XmlNode juick = msg.getFirstNode("juick", "http://juick.com/message");
            if (juick != null) {
                XmlNode user = juick.getFirstNode("user", "http://juick.com/user");
                if (user != null) {
                    userNick = user.getAttribute("uname");
                }
            }
        }
        if (StringConvertor.isEmpty(userNick)) {
            userNick = null;
        }
        String nick = userNick;
        if (null != botNick) {
            nick = StringConvertor.notNull(userNick) + "@" + botNick;
        }
        if (null != userNick) {
            userNick += ':';
            int nickPos = text.indexOf(userNick);
            if (0 == nickPos) {
                text = text.substring(userNick.length() + 1);
            }
        }

        text = StringConvertor.trim(text);
        if (StringConvertor.isEmpty(text)) {
            return;
        }

        String date = getDate(msg);
        long time = (null == date) ? SawimApplication.getCurrentGmtTime() : parseTimestamp(date);
        PlainMessage message = new PlainMessage(to, connection.getXmpp(), time, text, false);
        if (null != nick) {
            message.setName(('@' == nick.charAt(0)) ? nick.substring(1) : nick);
        }
        connection.getXmpp().addMessage(message, true, false);
    }

    private static void parseMessageEvent(XmppConnection connection, String contactId, XmlNode messageEvent, String from) {
        if (null == messageEvent) {
            return;
        }
        if (messageEvent.contains("offline")) {
            setMessageSended(connection, contactId, messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_SERVER);
            return;
        }
        if (messageEvent.contains("delivered")) {
            setMessageSended(connection, contactId, messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_CLIENT);
            return;
        }
        connection.getXmpp().beginTyping(from, messageEvent.contains(XmlConstants.S_COMPOSING));
    }

    private static void parseChatState(XmppConnection connection, XmlNode message, String from) {
        if (message.contains(XmlConstants.S_ACTIVE)
                || message.contains(XmlConstants.S_GONE)
                || message.contains(XmlConstants.S_PAUSED)
                || message.contains(XmlConstants.S_INACTIVE)) {
            connection.getXmpp().beginTyping(from, false);
        } else if (message.contains(XmlConstants.S_COMPOSING)) {
            connection.getXmpp().beginTyping(from, true);
        }
    }

    private static String getDate(XmlNode message) {
        XmlNode offline = message.getXNode("jabber:x:delay");
        if (null == offline) {
            offline = message.getFirstNode("delay");
        }
        return (null == offline) ? null : offline.getAttribute("stamp");
    }

    private static void parseEvent(XmppConnection connection, XmlNode eventNode, String fullJid) {
        if (null == eventNode) {
            return;
        }
        XmppContact contact = (XmppContact) connection.getXmpp().getItemByUID(Jid.getBareJid(fullJid));
        if (null == contact) {
            return;
        }
        XmlNode statusNode = eventNode.getFirstNode(XmlConstants.S_ITEMS);
        String eventType = "";
        if (null != statusNode) {
            eventType = statusNode.getAttribute(XmlConstants.S_NODE);
            int start = eventType.lastIndexOf('/');
            if (-1 != start) {
                eventType = eventType.substring(start + 1);
            }
            statusNode = statusNode.getFirstNode(XmlConstants.S_ITEM);
        }
        if (null != statusNode) {
            statusNode = statusNode.childAt(0);
        }
        if (!"|mood|activity|tune".contains(eventType)) {
            return;
        }
        if ((null == statusNode) || (0 == statusNode.childrenCount())) {
            if ((XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex())
                    && Xmpp.xStatus.isType(contact.getXStatusIndex(), eventType)) {
                contact.setXStatus("", "");
            }
            return;
        }
        String text = statusNode.getFirstNodeValue(XmlConstants.S_TEXT);
        statusNode.removeNode(XmlConstants.S_TEXT);
        StringBuilder status = new StringBuilder();
        while (null != statusNode) {
            status.append(':').append(statusNode.name);
            statusNode = statusNode.childAt(0);
        }
        status.deleteCharAt(0);
        if ((XStatusInfo.XSTATUS_NONE == contact.getXStatusIndex())
                || Xmpp.xStatus.isPep(contact.getXStatusIndex())) {
            contact.setXStatus(status.toString(), text);
        }
    }

    private static void setMessageSended(XmppConnection connection, String contactId, String id, int state) {
        Contact contact = connection.getXmpp().getItemByUID(contactId);
        HistoryStorage historyStorage = connection.getXmpp().getChat(contact).getHistory();
        historyStorage.updateState(id, state);
        if (RosterHelper.getInstance().getUpdateChatListener() != null)
            RosterHelper.getInstance().getUpdateChatListener().updateMessage(contact, id, state);
    }

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
    public static long parseTimestamp(String timestamp) {
        timestamp = timestamp.replace("Z", "+0000");
        timestamp = timestamp.substring(0, 19) + timestamp.substring(timestamp.length() - 5, timestamp.length());
        try {
            return dateFormat.parse(timestamp).getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return System.currentTimeMillis();
        }
    }
}
