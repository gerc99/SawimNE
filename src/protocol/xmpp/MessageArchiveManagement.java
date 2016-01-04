package protocol.xmpp;

import android.util.Log;;

import protocol.Contact;
import protocol.Protocol;
import ru.sawim.comm.Util;
import ru.sawim.io.RosterStorage;
import ru.sawim.listener.OnMoreMessagesLoaded;
import ru.sawim.roster.RosterHelper;

import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by gerc on 05.03.2015.
 */
public class MessageArchiveManagement {

    private static final long MILLISECONDS_IN_DAY = 24 * 60 * 60 * 1000;
    public static final long MAX_CATCHUP = MILLISECONDS_IN_DAY * 7;
    public static final long MAX_MESSAGES = 20;

    private final HashSet<Query> queries = new HashSet<>();

    private String getQueryMessageArchiveManagement(Contact contact, Query query) {
        XmlNode xmlNode = new XmlNode(XmlConstants.S_IQ);
        xmlNode.putAttribute(XmlConstants.S_TYPE, XmlConstants.S_SET);
        if (contact != null && contact.isConference()) {
            xmlNode.putAttribute(XmlConstants.S_TO, Util.xmlEscape(contact.getUserId()));
        }
        xmlNode.putAttribute(XmlNode.S_ID, XmppConnection.generateId());

        XmlNode queryNode = new XmlNode(XmlConstants.S_QUERY);
        queryNode.putAttribute(XmlNode.S_XMLNS, "urn:xmpp:mam:0");
        queryNode.putAttribute("queryid", query.queryId);

        XmlNode xNode = new XmlNode("x");
        xNode.putAttribute(XmlNode.S_XMLNS, "jabber:x:data");
        xNode.putAttribute("type", "submit");

        XmlNode formTypeNode = new XmlNode("field");
        formTypeNode.putAttribute("var", "FORM_TYPE");
        formTypeNode.putAttribute("type", "hidden");
        formTypeNode.setValue("value", "urn:xmpp:mam:0");
        xNode.addNode(formTypeNode);
    /*    if (query.getStart() > 0) {
            XmlNode startNode = new XmlNode("field");
            startNode.putAttribute("var", "start");
            startNode.setValue("value", Util.getTimestamp(query.getStart()));
            xNode.addNode(startNode);
        }
        if (query.getEnd() > 0) {
            XmlNode endNode = new XmlNode("field");
            endNode.putAttribute("var", "end");
            endNode.setValue("value", Util.getTimestamp(query.getEnd()));
            xNode.addNode(endNode);
        }*/
        if (query.withJid != null && contact != null && !contact.isConference()) {
            XmlNode withNode = new XmlNode("field");
            withNode.putAttribute("var", "with");
            withNode.setValue("value", query.withJid);
            xNode.addNode(withNode);
        }

        XmlNode setNode = XmlNode.addXmlns("set", "http://jabber.org/protocol/rsm");
        setNode.setValue("max", String.valueOf(MAX_MESSAGES));
        if (query.getPagingOrder() == PagingOrder.REVERSE) {
            setNode.setValue("before", query.getReference());
        } else {
            setNode.setValue("after", query.getReference());
        }
        queryNode.addNode(setNode);
        queryNode.addNode(xNode);
        xmlNode.addNode(queryNode);
        return xmlNode.toString();
    }

    private void queryMessageArchiveManagement(XmppConnection connection, Query query) {
        Contact contact = null;
        if (query.getWith() != null) {
            contact = connection.getProtocol().getItemByUID(query.getWith());
        }
        connection.putPacketIntoQueue(getQueryMessageArchiveManagement(contact, query));
    }

    public void catchup(XmppConnection connection) {
        long startCatchup = getLastMessageTransmitted(connection);
        long endCatchup = connection.getLastSessionEstablished();
        if (startCatchup == 0) {
            return;
        } else {
            ConcurrentHashMap<String, Contact> contacts = connection.getProtocol().getContactItems();
            for (Contact contact : contacts.values()) {
                queryReverse(connection, contact, startCatchup);
            }
        }
        final Query query = new Query(connection.getXmpp().getUserId(), null, startCatchup, endCatchup);
        queries.add(query);
        queryMessageArchiveManagement(connection, query);
    }

    private long getLastMessageTransmitted(XmppConnection connection) {
        long timestamp = 0;
        for (Contact contact : connection.getProtocol().getContactItems().values()) {
            long lastMessageTransmitted = contact.getLastMessageTransmitted();
            if (lastMessageTransmitted > timestamp) {
                timestamp = lastMessageTransmitted;
            }
        }
        return timestamp;
    }

    public Query queryReverse(XmppConnection connection, final Contact contact) {
        return queryReverse(connection, contact, connection.getLastSessionEstablished());
    }

    public Query queryReverse(XmppConnection connection, final Contact contact, long end) {
        long lastMessageTransmitted = contact.getLastMessageTransmitted();
        return queryReverse(connection, contact, lastMessageTransmitted, end);
    }

    public Query queryReverse(XmppConnection connection, Contact contact, long start, long end) {
        synchronized (queries) {
            if (start > end) {
                return null;
            }
            final Query query = new Query(connection.getXmpp().getUserId(), contact.getUserId(),
                    start, end, PagingOrder.REVERSE);
            queries.add(query);
            queryMessageArchiveManagement(connection, query);
            return query;
        }
    }

    public Query prev(XmppConnection connection, Contact contact) {
        synchronized (queries) {
            Query query = new Query(connection.getXmpp().getUserId(), contact.getUserId(), 0, 0)
                        .prev(contact.firstServerMsgId);
            queries.add(query);
            queryMessageArchiveManagement(connection, query);
            return query;
        }
    }

    public void processFin(XmppConnection connection, XmlNode fin) {
        Query query = findQuery(fin.getAttribute("queryid"));
        if (query == null) {
            return;
        }
        boolean complete = XmppConnection.isTrue(fin.getAttribute("complete"));
        XmlNode set = fin.getFirstNode("set", "http://jabber.org/protocol/rsm");
        String last = set == null ? null : set.getFirstNodeValue("last");
        String first = set == null ? null : set.getFirstNodeValue("first");
        String relevant = query.getPagingOrder() == PagingOrder.NORMAL ? last : first;
        String count = set == null ? null : set.getFirstNodeValue("count");
        if (count != null) {
            query.setAllMessageCount(Integer.valueOf(count));
        }
        if (relevant != null) {
            Contact contact = null;
            if (query.getWith() != null) {
                contact = connection.getProtocol().getItemByUID(query.getWith());
            }
            contact.firstServerMsgId = first;
            RosterStorage.updateFirstServerMsgId(contact);
        }
        if (complete || relevant == null) {
            finalizeQuery(connection.getProtocol(), query);
            Log.d("MAM", "finished mam after " + query.getAllMessagesCount() + " messages");
        } else {
            final Query nextQuery;
            if (query.getPagingOrder() == PagingOrder.NORMAL) {
                nextQuery = query.next(last);
            } else {
                nextQuery = query.prev(first);
            }
        //    queryMessageArchiveManagement(connection, nextQuery);
            finalizeQuery(connection.getProtocol(), query);
            synchronized (queries) {
            //    queries.add(nextQuery);
            }
        }
    }

    public boolean queryInProgress(Contact contact, OnMoreMessagesLoaded moreMessagesLoadedListener) {
        synchronized (queries) {
            for (Query query : queries) {
                if (query.getWith().equals(contact.getUserId())) {
                    if (query.onMoreMessagesLoaded == null && moreMessagesLoadedListener != null) {
                        query.setOnMoreMessagesLoaded(moreMessagesLoadedListener);
                    }
                    return true;
                }
            }
            return false;
        }
    }

    private void finalizeQuery(Protocol protocol, Query query) {
        synchronized (queries) {
            queries.remove(query);
        }
        Contact contact = null;
        if (query.getWith() != null) {
            contact = protocol.getItemByUID(query.getWith());
        }
        if (contact != null) {
            if (contact.setLastMessageTransmitted(query.getEnd())) {
            }
            contact.setHasMessagesLeftOnServer(query.getMessagesCount() > 0);
            query.messagesLoaded(query.getMessagesCount());
            RosterHelper.getInstance().updateRoster(contact);
            if (RosterHelper.getInstance().getUpdateChatListener() != null) {
                RosterHelper.getInstance().getUpdateChatListener().updateChat();
            }
        } else {
            for (Contact c : protocol.getContactItems().values()) {
                if (c.setLastMessageTransmitted(query.getEnd())) {
                }
            }
        }
    }

    public Query findQuery(String id) {
        if (id == null) {
            return null;
        }
        synchronized (queries) {
            for(Query query : queries) {
                if (query.getQueryId().equals(id)) {
                    return query;
                }
            }
            return null;
        }
    }

    public class Query {
        private int allMessagesCount = 0;
        private int messagesCount = 0;
        private long start;
        private long end;
        private String queryId;
        private String reference = null;
        private PagingOrder pagingOrder = PagingOrder.NORMAL;
        private String accJid;
        private String withJid;
        private OnMoreMessagesLoaded onMoreMessagesLoaded;

        public Query(String accJid, String withJid, long start, long end, PagingOrder order) {
            this(accJid, withJid, start, end);
            this.pagingOrder = order;
        }

        public Query(String accJid, String withJid, long start, long end) {
            this.accJid = accJid;
            this.withJid = withJid;
            this.start = start;
            this.end = end;
            this.queryId = XmppConnection.generateId();
        }

        private Query page(String reference) {
            Query query = new Query(accJid, withJid, start, end);
            query.reference = reference;
            query.onMoreMessagesLoaded = onMoreMessagesLoaded;
            return query;
        }

        public Query next(String reference) {
            Query query = page(reference);
            query.pagingOrder = PagingOrder.NORMAL;
            return query;
        }

        public Query prev(String reference) {
            Query query = page(reference);
            query.pagingOrder = PagingOrder.REVERSE;
            return query;
        }

        public String getReference() {
            return reference;
        }

        public PagingOrder getPagingOrder() {
            return pagingOrder;
        }

        public String getQueryId() {
            return queryId;
        }

        public String getWith() {
            return withJid;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }

        public void setAllMessageCount(int messageCount) {
            this.allMessagesCount = messageCount;
        }

        public int getAllMessagesCount() {
            return allMessagesCount;
        }

        public void incrementMessageCount() {
            messagesCount++;
        }

        public int getMessagesCount() {
            return messagesCount;
        }

        public void setOnMoreMessagesLoaded(OnMoreMessagesLoaded onMoreMessagesLoaded) {
            this.onMoreMessagesLoaded = onMoreMessagesLoaded;
        }

        public void messagesLoaded(int messagesCount) {
            if (onMoreMessagesLoaded != null) {
                onMoreMessagesLoaded.onLoaded(messagesCount);
            }
        }
    }

    public enum PagingOrder {
        NORMAL,
        REVERSE
    };
}
