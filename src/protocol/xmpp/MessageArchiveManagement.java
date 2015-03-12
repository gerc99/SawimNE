package protocol.xmpp;

import android.util.Log;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.comm.Util;
import ru.sawim.listener.OnMoreMessagesLoaded;
import ru.sawim.roster.RosterHelper;

import java.util.HashSet;
import java.util.List;

/**
 * Created by gerc on 05.03.2015.
 */
public class MessageArchiveManagement {

    private static final long MILLISECONDS_IN_DAY = 24 * 60 * 60 * 1000;
    public static final long MAX_CATCHUP = MILLISECONDS_IN_DAY * 7;

    private final HashSet<Query> queries = new HashSet<>();

    public String getQueryMessageArchiveManagement(Contact contact, Query query) {
        String iq = "<iq ";
        if (contact != null && contact.isConference()) {
            iq += "to='" + contact.getUserId() + "'";
        }
        iq += "type='set' id='" + XmppConnection.generateId() + "'>"
                + "<query xmlns='urn:xmpp:mam:0' queryid='" + query.queryId + "'>"
                + "<x xmlns='jabber:x:data'>"
                + "<field var='FORM_TYPE'>"
                + "<value>urn:xmpp:mam:0</value>"
                + "</field>";
        if (query.getStart() > 0) {
            iq += "<field var='start'>";
            iq += "<value>" + Util.getTimestamp(query.getStart()) + "</value>";
            iq += "</field>";
        }
        if (query.getEnd() > 0) {
            iq += "<field var='end'>";
            iq += "<value>" + Util.getTimestamp(query.getEnd()) + "</value>";
            iq += "</field>";
        }
        if (query.withJid != null) {
            iq += "<field var='with'>"
                    + "<value>" + query.withJid + "</value>"
                    + "</field>";
        }
        iq += "</x>";
        if (query.getReference() != null) {
            iq += "<set xmlns='http://jabber.org/protocol/rsm'>"
                    /*+ "<max>10</max>"*/;
            if (query.getPagingOrder() == PagingOrder.REVERSE) {
                iq += "<before>" + query.getReference() + "</before>";
            } else {
                iq += "<after>" + query.getReference() + "</after>";
            }
            iq += "</set>";
        }
        iq += "</query></iq>";
        return iq;
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
        } else if (endCatchup - startCatchup >= MAX_CATCHUP) {
            startCatchup = endCatchup - MAX_CATCHUP;
            List<Contact> contacts = connection.getProtocol().getContactItems();
            for (Contact contact : contacts) {
                long lastMessageTransmitted = contact.getLastMessageTransmitted();
                if (!contact.isConference() && startCatchup > lastMessageTransmitted) {
                    query(connection, contact, startCatchup);
                }
            }
        }
        final Query query = new Query(connection.getXmpp().getUserId(), null, startCatchup, endCatchup);
        queries.add(query);
        queryMessageArchiveManagement(connection, query);
    }

    private long getLastMessageTransmitted(XmppConnection connection) {
        long timestamp = 0;
        for (Contact contact : connection.getProtocol().getContactItems()) {
            long lastMessageTransmitted = contact.getLastMessageTransmitted();
            if (lastMessageTransmitted > timestamp) {
                timestamp = lastMessageTransmitted;
            }
        }
        return timestamp;
    }

    public Query query(XmppConnection connection) {
        long endCatchup = connection.getLastSessionEstablished();
        long startCatchup = endCatchup - MAX_CATCHUP;
        List<Contact> contacts = connection.getProtocol().getContactItems();
        for (Contact contact : contacts) {
            long lastMessageTransmitted = contact.getLastMessageTransmitted();
            if (!contact.isConference() && startCatchup > lastMessageTransmitted) {
                //query(connection, contact, endCatchup);
                final Query query = new Query(connection.getXmpp().getUserId(), contact.getUserId(), startCatchup, endCatchup);
                queries.add(query);
                queryMessageArchiveManagement(connection, query);
            }
        }
        return null;
    }

    public Query query(XmppConnection connection, final Contact contact) {
        return query(connection, contact, connection.getLastSessionEstablished());
    }

    public Query query(XmppConnection connection, final Contact contact, long end) {
        long lastMessageTransmitted = contact.getLastMessageTransmitted();
        return query(connection, contact, lastMessageTransmitted, end);
    }

    public Query query(XmppConnection connection, Contact contact, long start, long end) {
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

    public void processFin(XmppConnection connection, XmlNode fin) {
        if (fin == null) {
            return;
        }
        Query query = findQuery(fin.getAttribute("queryid"));
        if (query == null) {
            return;
        }
        boolean complete = XmppConnection.isTrue(fin.getAttribute("complete"));
        XmlNode set = fin.getFirstNode("set", "http://jabber.org/protocol/rsm");
        String last = set == null ? null : set.getAttribute("last");
        String first = set == null ? null : set.getAttribute("first");
        String relevant = query.getPagingOrder() == PagingOrder.NORMAL ? last : first;
        String count = fin.getAttribute("count");
        if (count != null) {
            query.setMessageCount(Integer.valueOf(count));
        }
        if (complete || relevant == null) {
            finalizeQuery(connection.getProtocol(), query);
            Log.d("MAM", "finished mam after " + query.getMessageCount() + " messages");
        } else {
            final Query nextQuery;
            if (query.getPagingOrder() == PagingOrder.NORMAL) {
                nextQuery = query.next(last);
            } else {
                nextQuery = query.prev(first);
            }
            queryMessageArchiveManagement(connection, nextQuery);
            finalizeQuery(connection.getProtocol(), query);
            synchronized (queries) {
                queries.add(nextQuery);
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
            boolean hasMessages = query.getMessageCount() > 0;
            if (hasMessages) {
                query.messagesLoaded();
            }
            RosterHelper.getInstance().updateRoster(contact);
            if (RosterHelper.getInstance().getUpdateChatListener() != null) {
                RosterHelper.getInstance().getUpdateChatListener().updateChat();
            }
        } else {
            for (Contact c : protocol.getContactItems()) {
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
        private int messageCount = 0;
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

        public void setMessageCount(int messageCount) {
            this.messageCount = messageCount;
        }

        public int getMessageCount() {
            return messageCount;
        }

        public void setOnMoreMessagesLoaded(OnMoreMessagesLoaded onMoreMessagesLoaded) {
            this.onMoreMessagesLoaded = onMoreMessagesLoaded;
        }

        public void messagesLoaded() {
            if (onMoreMessagesLoaded != null) {
                onMoreMessagesLoaded.onLoaded();
            }
        }
    }

    public enum PagingOrder {
        NORMAL,
        REVERSE
    };
}
