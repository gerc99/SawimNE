package protocol.xmpp;

import android.support.v4.util.Pair;
import protocol.*;
import protocol.net.ClientConnection;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.Config;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.listener.OnMoreMessagesLoaded;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.RosterHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public final class XmppConnection extends ClientConnection {

    private static final String LOG_TAG = XmppConnection.class.getSimpleName();
    public static final boolean DEBUGLOG = true;
    private static final int MESSAGE_COUNT_AFTER_CONNECT = 20;

    public Socket socket;
    private Xmpp protocol;

    String fullJid_;
    public String domain_ = "";
    public String resource;
    private byte[] pingPacket = new byte[]{' '};
    private byte[] forPongPacket = StringConvertor.stringToByteArrayUtf8(
            "<iq type='get'><ping xmlns='urn:xmpp:ping'/></iq>");

    private final LinkedBlockingQueue<String> packets = new LinkedBlockingQueue<>();
    private final Map<String, Pair<XmlNode, OnIqReceived>> packetCallbacks = new Hashtable<>();

    private boolean rosterLoaded = false;

    public UserInfo singleUserInfo;
    private String autoSubscribeDomain;

    private IBBFileTransfer ibb;

    private ServiceDiscovery serviceDiscovery = null;
    private AdHoc adhoc;
    public MessageArchiveManagement messageArchiveManagement = new MessageArchiveManagement();
    private ServerFeatures serverFeatures = new ServerFeatures();
    private Auth auth = new Auth();

    private XmppForm xmppForm;

    public byte nativeStatus2StatusIndex(String rawStatus) {
        rawStatus = StringConvertor.notNull(rawStatus);
        for (byte i = 0; i < XmlConstants.statusCodes.length; ++i) {
            if (rawStatus.equals(XmlConstants.statusCodes[i])) {
                return i;
            }
        }
        return StatusInfo.STATUS_ONLINE;
    }

    public String getNativeStatus(byte statusIndex) {
        return XmlConstants.statusCodes[statusIndex];
    }

    static boolean isTrue(String val) {
        return XmlConstants.S_TRUE.equals(val) || "1".equals(val);
    }

    public XmppConnection() {
    }

    public void setXmpp(Xmpp xmpp) {
        protocol = xmpp;
        resource = xmpp.getResource();
        fullJid_ = xmpp.getUserId() + '/' + resource;
        domain_ = Jid.getDomain(fullJid_);

        XmppSession xmppSession = xmpp.getXmppSession();
        if (!xmppSession.isEmpty() && !xmppSession.isSessionFor(fullJid_)) {
            DebugLog.println("[SESSION] Oops");
        }
    }

    public void setProgress(int percent) {
        getXmpp().setConnectingProgress(percent);
    }

    public Xmpp getXmpp() {
        return protocol;
    }

    public XmppSession getXmppSession() {
        return protocol.getXmppSession();
    }

    private void write(byte[] data) throws SawimException {
        try {
            socket.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectTo(String host, int port) throws SawimException {
        socket = new Socket();
        socket.connectTo(host, port);
    }

    public final void disconnect() {
        connect = false;
        protocol = null;
    }

    protected final void ping() throws SawimException {
        write(pingPacket);
    }

    protected final void pingForPong() throws SawimException {
        write(forPongPacket);
    }

    public void putPacketIntoQueue(String packet) {
        if (DEBUGLOG)
            DebugLog.println("[PUT PACKET]:\n" + packet);
        packets.add(packet);
    }

    public synchronized void putPacketIntoQueue(final XmlNode packet, final OnIqReceived callback) {
        if (packet.getId() == null) {
            packet.putAttribute(XmlNode.S_ID, generateId());
        }
        if (callback != null) {
            packetCallbacks.put(packet.getId(), new Pair<>(packet, callback));
        }
        requestSetIq(packet.toString());
    }

    private boolean hasOutPackets() {
        return !packets.isEmpty();
    }

    private void sendPacket() throws SawimException {
        String packet = null;
        try {
            packet = packets.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        writePacket(packet);
    }

    protected final boolean processPacket() throws SawimException {
        if (hasOutPackets()) {
            sendPacket();
            return true;
        }
        if (processInPacket()) {
            updateTimeout();
            return true;
        }
        return false;
    }

    protected final void closeSocket() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    protected final void loggedOut() {
        try {
            write("<presence type='unavailable'><status>Logged out</status></presence>");
            write("</stream:stream>");
        } catch (Exception ignored) {
        }
    }

    protected Protocol getProtocol() {
        return protocol;
    }

    public void write(String xml) throws SawimException {
        if (DEBUGLOG)
            DebugLog.println("[OUT]:\n" + xml);
        write(StringConvertor.stringToByteArrayUtf8(xml));
    }

    void writePacket(String packet) throws SawimException {
        write(packet);
    }

    public XmlNode readXmlNode(boolean notEmpty) throws SawimException {
        XmlNode node = socket.readNode(notEmpty);
        if (DEBUGLOG && node != null)
            DebugLog.println("[IN]:\n" + node.toString());
        return node;
    }

    public void sendRequest(String request) throws SawimException {
        write(request);
    }

    XmlNode newAccountConnect(String domain, String server, int port) throws SawimException {
        domain = Util.xmlEscape(domain);
        connectTo(server, port);
        write(getOpenStreamXml(domain));
        readXmlNode(true);
        XmlNode features = readXmlNode(true);
        if (!features.contains("register")) {
            return null;
        }
        write("<iq type='get' to='" + domain
                + "' id='1'><query xmlns='jabber:iq:register'/></iq>");
        return readXmlNode(true);
    }

    XmlNode newAccountRegister(String xml) throws SawimException {
        write(xml);
        XmlNode x = readXmlNode(true);
        socket.close();
        return x;
    }

    private String[] getHostAndPort(String server, String defaultServer) {
        // TODO: legacy SSL
        String[] url = Util.explode(server, ':');
        String[] socketUrl = new String[2];
        final String S_SOCKET = "socket";
        final String S_SSL = "ssl";
        final String S_5222 = "5222";
        if (3 == url.length) {
            // skip socket/ssl, get host and port
            socketUrl[0] = url[1];
            socketUrl[1] = url[2];
        } else if (2 == url.length) {
            // get host and port
            socketUrl[0] = url[0];
            socketUrl[1] = url[1];
        } else if (1 == url.length) {
            // default port
            socketUrl[0] = url[0];
            socketUrl[1] = S_5222;
        }
        if (null != defaultServer) {
            socketUrl[0] = defaultServer;
        }
        return socketUrl;
    }

    protected final void connect() throws SawimException {
        connect = true;
        setProgress(0);
        packetCallbacks.clear();
        serverFeatures.initFeatures();
        protocol.net.SrvResolver resolver = new protocol.net.SrvResolver();
        String server = Config.getConfigValue(domain_, "/jabber-servers.txt");
        String defaultServer = domain_;
        if (StringConvertor.isEmpty(server) && (null == defaultServer)) {
            server = resolver.getXmpp(domain_);
        }
        if (StringConvertor.isEmpty(server)) {
            server = domain_;
        }
        String[] hostAndPort = getHostAndPort(server, defaultServer);
        connectTo(hostAndPort[0], Integer.parseInt(hostAndPort[1]));

        write(getOpenStreamXml(domain_));
        setProgress(10);

        readXmlNode(true);
        resolver.close();
        auth.parseAuth(this, readXmlNode(true));
        while (!auth.authorized_) {
            auth.loginParse(this, readXmlNode(true));
        }

        setProgress(30);
        socket.start();
		if (getXmppSession().isRestored()) {
            usePong();
            setProgress(100);
        } else {
            requestDiscoServerItems();
            try {
                write(XmlConstants.GET_ROSTER_XML);
            } catch (SawimException e) {
                e.printStackTrace();
            }
			setProgress(50);
			usePong();
			setProgress(60);
        }
    }

    private boolean processInPacket() throws SawimException {
        XmlNode x = null;
        try {
            x = readXmlNode(false);
            if (null == x) {
                return false;
            }
            parse(x);
            x = null;
        } catch (SawimException e) {
            throw e;
        } catch (Exception e) {
            DebugLog.panic("Xmpp parse", e);
            if (null != x) {
                DebugLog.println("xml: " + x.toString());
            }
        }
        return true;
    }

    public void parse(XmlNode x) throws SawimException {
        if (x.is("iq")) {
            parseIq(x);

        } else if (x.is("presence")) {
            Presences.parsePresence(this, x);

        } else if (x.is("message")) {
            Messages.parseMessage(this, x);

        } else if (x.is("stream:error")) {
            auth.setAuthStatus(this, false);

            XmlNode err = (null == x.childAt(0)) ? x : x.childAt(0);
            DebugLog.systemPrintln("[INFO-JABBER] Stream error!: " + err.name + "," + err.value);
        }
    }

    public static String generateId(String key) {
        return key + Util.uniqueValue();
    }

    public static String generateId() {
        return "sawim" + Util.uniqueValue();
    }

    private boolean isNoAutorized(String subscription) {
        return XmlConstants.S_NONE.equals(subscription) || XmlConstants.S_FROM.equals(subscription);
    }

    public byte getIqType(XmlNode iq) {
        String iqType = iq.getAttribute(XmlConstants.S_TYPE);
        if (XmlConstants.S_RESULT.equals(iqType)) {
            return XmlConstants.IQ_TYPE_RESULT;
        }
        if (XmlConstants.S_GET.equals(iqType)) {
            return XmlConstants.IQ_TYPE_GET;
        }
        if (XmlConstants.S_SET.equals(iqType)) {
            return XmlConstants.IQ_TYPE_SET;
        }
        return XmlConstants.IQ_TYPE_ERROR;
    }

    private void parseIqError(XmlNode iqNode, String from) throws SawimException {
        XmlNode errorNode = iqNode.getFirstNode(XmlConstants.S_ERROR);
        iqNode.removeNode(XmlConstants.S_ERROR);

        if (null == errorNode) {
            DebugLog.println("Error without description is stupid");
        } else {
            DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(XmlConstants.S_CODE) + " " +
                    "Value=" + getError(errorNode));
        }
        XmlNode query = iqNode.childAt(0);
        if (null == query) {
        } else if (Vcard.S_VCARD.equals(query.name)) {
            Vcard.loadVCard(this, null, from);
        } else if (XmlConstants.S_QUERY.equals(query.name)) {
            String xmlns = query.getXmlns();
            if ("jabber:iq:register".equals(xmlns) && (null != xmppForm)) {
                xmppForm.error(getError(errorNode));
                xmppForm = null;
            }
        }
    }

    private void sendIqError(String query, String xmlns, String from, String id) {
        putPacketIntoQueue("<iq type='error' to='"
                + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                + "<" + query + " xmlns='" + Util.xmlEscape(xmlns) + "'/>"
                + "<error type='cancel'>"
                + "<feature-not-implemented xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>"
                + "</error>"
                + "</iq>");
    }

    private boolean isMy(String from) {
        return StringConvertor.isEmpty(from) || getXmpp().getUserId().equals(Jid.getBareJid(from));
    }

    private void processEmptyId(String id, byte iqType, String from) {
        if (XmlConstants.IQ_TYPE_RESULT != iqType) {
            return;
        }
        if (id.startsWith(Vcard.S_VCARD)) {
            Vcard.loadVCard(this, null, from);
        }
        if ((null != xmppForm) && xmppForm.getId().equals(id)) {
            xmppForm.success();
            xmppForm = null;
        }
    }

    private void parseIq(XmlNode iq) throws SawimException {

        String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
        byte iqType = getIqType(iq);
        String id = iq.getId();
        if (id != null && packetCallbacks.containsKey(id)) {
            String myJid = Jid.getBareJid(fullJid_);
            final Pair<XmlNode, OnIqReceived> packetCallbackDuple = packetCallbacks.get(id);
            packetCallbackDuple.second.onIqReceived(iq);
            packetCallbacks.remove(id);
            return;
        }
        if (StringConvertor.isEmpty(id)) {
            id = generateId();
        }
        if (null != ibb) {
            boolean processed = processIbb(iq, iqType, id);
            if (processed) {
                return;
            }
        }
        if (XmlConstants.IQ_TYPE_ERROR == iqType) {
            parseIqError(iq, from);
            return;
        }
        XmlNode iqQuery = iq.childAt(0);
        if (null == iqQuery) {
            processEmptyId(id, iqType, from);
            return;
        }
        String queryName = iqQuery.name;
        final Xmpp xmpp = getXmpp();
        if (XmlConstants.S_QUERY.equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("jabber:iq:roster".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                if ((XmlConstants.IQ_TYPE_RESULT == iqType) && !rosterLoaded) {
                    rosterLoaded = true;
                    while (0 < iqQuery.childrenCount()) {
                        final XmlNode itemNode = iqQuery.popChildNode();
                        final String jid = itemNode.getAttribute(XmlNode.S_JID);
                        /*requestIq(Jid.getDomain(jid), "http://jabber.org/protocol/disco#info", null, new OnIqReceived() {
                            @Override
                            public void onIqReceived(XmlNode iq) {
                                String id = iq.getId();
                                XmlNode iqQuery = iq.childAt(0);
                                String from = StringConvertor.notNull(iq.getAttribute(S_FROM));
                                byte iqType = getIqType(iq);
                                while (0 < iqQuery.childrenCount()) {
                                    XmlNode featureNode = iqQuery.popChildNode();
                                    String feature = featureNode.getAttribute("var");
                                    if (feature != null) {
                                        switch (feature) {
                                            case "http://jabber.org/protocol/muc":
                                                if (!rosterConferenceServers.contains(from)) {
                                                    rosterConferenceServers.add(from);
                                                }
                                                Contact c = xmpp.getItemByUID(jid);
                                                c.setConference(true);
                                                c.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                                                c.setTempFlag(false);
                                                XmppServiceContact conference = (XmppServiceContact) c;
                                                if (conference.isAutoJoin()) {
                                                    xmpp.join(conference);
                                                }
                                                xmpp.rejoin();

                                                xmpp.safeSave();
                                                RosterHelper.getInstance().updateGroup(xmpp, xmpp.getNotInListGroup());
                                                RosterHelper.getInstance().updateRoster();
                                                break;
                                        }
                                    }
                                }
                            }
                        });*/
                        Contact contact = xmpp.getItemByUID(jid);
                        if (contact == null) {
                            contact = xmpp.createContact(jid, jid, false);
                        }
                        contact.setName(itemNode.getAttribute(XmlNode.S_NAME));
                        String groupName = itemNode.getFirstNodeValue(XmlConstants.S_GROUP);
                        if (StringConvertor.isEmpty(groupName)) {
                            groupName = contact.getDefaultGroupName();
                        }
                        Group g = xmpp.getOrCreateGroup(groupName);
                        contact.setGroup(g);

                        if (!xmpp.inContactList(contact)) {
                            xmpp.getContactItems().put(contact.getUserId(), contact);
                        }
                        RosterHelper.getInstance().updateGroup(xmpp, g);
                        String subscription = itemNode.getAttribute("subscription");
                        contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                        getXmpp().getStorage().save(getXmpp(), contact, g);
                    }
                    enableMessageArchiveManager();
                    RosterHelper.getInstance().updateGroup(xmpp, xmpp.getNotInListGroup());
                    RosterHelper.getInstance().updateRoster();
                    setProgress(70);
                    if (!isConnected()) {
                        return;
                    }
                    Contact selfContact = xmpp.getItemByUID(xmpp.getUserId());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        xmpp.ui_updateContact(selfContact);
                    }
                    setProgress(80);
                    getXmpp().s_updateOnlineStatus();
                    Vcard.requestVCard(this, fullJid_, "-", "---");
                    String xcode = Xmpp.xStatus.getCode(xmpp.getProfile().xstatusIndex);
                    if ((null != xcode) && !xcode.startsWith(XmppXStatus.XSTATUS_START)) {
                        XStatus.setXStatus(this);
                    }
                    setProgress(90);
                    Muc.getBookmarks(this);
                    putPacketIntoQueue("<iq type='get' id='getnotes'><query xmlns='jabber:iq:private'><storage xmlns='storage:rosternotes'/></query></iq>");
					setProgress(100);

                    if (serverFeatures.hasMessageArchiveManagement()) {
                        ////messageArchiveManagement.catchup(this);
                        //messageArchiveManagement.query(this);
                    }
                } else if (XmlConstants.IQ_TYPE_SET == iqType) {
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = iqQuery.popChildNode();
                        String subscription = itemNode.getAttribute("subscription");
                        String jid = itemNode.getAttribute(XmlNode.S_JID);
                        if ((XmlConstants.S_REMOVE).equals(subscription)) {
                            xmpp.removeLocalContact(xmpp.getItemByUID(jid));
                        } else {
                            String name = itemNode.getAttribute(XmlNode.S_NAME);
                            Contact contact = xmpp.createTempContact(jid, false);
                            String group = itemNode.getFirstNodeValue(XmlConstants.S_GROUP);
                            if (StringConvertor.isEmpty(group)) {
                                group = contact.getDefaultGroupName();
                            }
                            contact.setName(name);
                            RosterHelper.getInstance().removeFromGroup(xmpp, xmpp.getGroup(contact), contact);
                            contact.setGroup(xmpp.getOrCreateGroup(group));
                            contact.setTempFlag(false);
                            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                            xmpp.addLocalContact(contact);
                        }
                    }
                    Contact selfContact = xmpp.getItemByUID(xmpp.getUserId());
                    if (null != selfContact) {
                        selfContact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
                        xmpp.ui_updateContact(selfContact);
                    }
                }
                return;
            } else if ("http://jabber.org/protocol/disco#info".equals(xmlns)) {
                if (XmlConstants.IQ_TYPE_GET == iqType) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<iq type='result' to='")
                            .append(Util.xmlEscape(from))
                            .append("' id='").append(Util.xmlEscape(id)).append("'>");
                    sb.append("<query xmlns='http://jabber.org/protocol/disco#info'>");
                    sb.append(serverFeatures.getFeatureList());
                    sb.append("</query></iq>");
                    write(sb.toString());
                    return;
                }
            } else if ("http://jabber.org/protocol/muc#admin".equals(xmlns)) {
                if (XmlConstants.IQ_TYPE_GET == iqType) {
                    sendIqError(XmlConstants.S_QUERY, xmlns, from, id);
                    return;
                }
                AffiliationListConf uslist = Muc.affListConf;
                if (null == uslist) {
                    return;
                }
                uslist.setTotalCount();
                while (0 < iqQuery.childrenCount()) {
                    XmlNode item = iqQuery.popChildNode();
                    String jid = item.getAttribute(XmlNode.S_JID);
                    String affiliation = item.getAttribute(XmlNode.S_AFFILIATION);
                    String reasone = item.getFirstNodeValue("reason");
                    uslist.setAffiliation(affiliation);
                    uslist.addItem(reasone, jid);
                }
                return;
            } else if ("jabber:iq:private".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                XmlNode storage = iqQuery.getFirstNode("storage", "storage:bookmarks");
                if (null != storage) {
                    loadBookmarks(storage);
                } else {
                    storage = iqQuery.getFirstNode("storage", "http://miranda-im.org/storage#notes");
                    if (null != storage) {
                        MirandaNotes.loadMirandaNotes(getXmpp(), storage);
                    }
                }
                if (XmlConstants.IQ_TYPE_RESULT == iqType && ("getnotes").equals(id)) {
                    storage = iqQuery.getFirstNode("storage", "storage:rosternotes");
                    if (null != storage) {
                        while (0 < storage.childrenCount()) {
                            XmlNode item = storage.popChildNode();
                            String jid = item.getAttribute(XmlNode.S_JID);
                            String note = item.value;
                            Contact contact = xmpp.getItemByUID(jid);
                            if (contact != null) {
                                contact.annotations = note;
                            }
                        }
                    }
                }
                return;
            } else if ("jabber:iq:version".equals(xmlns)) {
                String platform = Util.xmlEscape(SawimApplication.PHONE);
                if (XmlConstants.IQ_TYPE_GET == iqType) {
                    putPacketIntoQueue("<iq type='result' to='"
                            + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:version'><name>"
                            + Util.xmlEscape(SawimApplication.NAME)
                            + "</name><version>"
                            + Util.xmlEscape(SawimApplication.VERSION)
                            + "</version><os>"
                            + platform
                            + "</os></query></iq>");

                    //String jid = getXmpp().getItemByUID(Jid.getBareJid(from)).isConference() ? from : Jid.getBareJid(from);
                    //MagicEye.addAction(xmpp, jid, "get_version");
                }
                return;
            } else if ("jabber:iq:last".equals(xmlns)) {
                if (XmlConstants.IQ_TYPE_GET == iqType) {
                    String jid = getXmpp().getItemByUID(Jid.getBareJid(from)).isConference() ? from : Jid.getBareJid(from);
                    //MagicEye.addAction(xmpp, jid, "last_activity_request");

                    long time = SawimApplication.getCurrentGmtTime() / 1000 - xmpp.getLastStatusChangeTime();
                    putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                            + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:last' seconds='"
                            + time
                            + "'/></iq>");
                }
                if (XmlConstants.IQ_TYPE_RESULT == iqType) {
                    String jid = getXmpp().getItemByUID(Jid.getBareJid(from)).isConference() ? from : Jid.getBareJid(from);
                    String time = iqQuery.getAttribute("seconds");
                    time = Util.secDiffToDate(Integer.parseInt(time));
                    getXmpp().addMessage(new SystemNotice(getXmpp(), (byte) -1, Jid.getBareJid(from), time));
                }
                return;

            } else if ("http://jabber.org/protocol/muc#owner".equals(xmlns)) {

            }

        } else if (XmlConstants.S_TIME.equals(queryName)) {
            if (XmlConstants.IQ_TYPE_GET != iqType) {
                return;
            }
            String jid = getXmpp().getItemByUID(Jid.getBareJid(from)).isConference() ? from : Jid.getBareJid(from);
            //MagicEye.addAction(xmpp, jid, "get_time");

            /*putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                    + "' id='" + Util.xmlEscape(id) + "'>"
                    + "<time xmlns='urn:xmpp:time'><tzo>"
                    + (0 <= SawimApplication.gmtOffset ? "+" : "-") + Util.makeTwo(Math.abs(SawimApplication.gmtOffset)) + ":00"
                    + "</tzo><utc>"
                    + Util.getUtcDateString(SawimApplication.getCurrentGmtTime())
                    + "</utc></time></iq>");*/
            return;
        } else if (("ping").equals(queryName)) {
            writePacket("<iq to='" + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' type='result'/>");
            return;

        } else if (("pubsub").equals(queryName)) {
            if (!isMy(from)) {
                return;
            }
            loadBookmarks(iqQuery.getFirstNodeRecursive("storage"));
            return;

        } else if (Vcard.S_VCARD.equals(queryName)) {
            if (XmlConstants.IQ_TYPE_RESULT == iqType) {
                Vcard.loadVCard(this, iqQuery, from);
            }
            return;

        } else if ("x".equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("http://jabber.org/protocol/rosterx".equals(xmlns)) {
                if (Jid.isGate(from)) {
                    Contact c = getXmpp().getItemByUID(from);
                    if ((null != c) && !c.isTemp() && c.isAuth()) {
                        putPacketIntoQueue("<iq type='result' to='"
                                + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "' />");
                        parseRosterExchange(iqQuery, '@' + from);
                        return;
                    }
                }
            }

        } else if (("command").equals(queryName)) {
            if (null != adhoc) {
                adhoc.loadCommandXml(iq, id);
            }
        } else if (("alarm").equals(queryName)) {
            String jid = getXmpp().getItemByUID(Jid.getBareJid(from)).isConference() ? from : Jid.getBareJid(from);
            PlainMessage message = new PlainMessage(jid, getProtocol(),
                    SawimApplication.getCurrentGmtTime(), PlainMessage.CMD_WAKEUP, false);
            getXmpp().addMessage(message);
        }
        if (XmlConstants.IQ_TYPE_GET == iqType) {
            sendIqError(iqQuery.name, iqQuery.getXmlns(), from, id);
        }
    }

    private void loadBookmarks(XmlNode storage) {
        if ((null == storage) || (0 == storage.childrenCount())) {
            return;
        }
        if (!"storage:bookmarks".equals(storage.getXmlns())) {
            return;
        }
        Xmpp xmpp = getXmpp();
        Group group = xmpp.getOrCreateGroup(JLocale.getString(Xmpp.CONFERENCE_GROUP));
        ConcurrentHashMap<String, Contact> contacts = xmpp.getContactItems();
        while (0 < storage.childrenCount()) {
            XmlNode item = storage.popChildNode();
            String jid = item.getAttribute(XmlNode.S_JID);
            if ((null == jid)) {
                continue;
            }
            String name = item.getAttribute(XmlNode.S_NAME);
            if (null == name) {
                name = Jid.getNick(jid);
            }
            String nick = item.getFirstNodeValue(XmlConstants.S_NICK);
            boolean autojoin = isTrue(item.getAttribute("autojoin"));
            String password = item.getAttribute("password");

            Contact contact = xmpp.getItemByUID(jid);
            XmppServiceContact conference;
            if (contact != null && contact.isConference()) {
                conference = (XmppServiceContact) contact;
            } else {
                conference = new XmppServiceContact(jid, name, true, false);
            }
            conference.setName(name);
            conference.setMyName(nick);
            conference.setTempFlag(false);
            conference.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            conference.setAutoJoin(autojoin);
            conference.setPassword(password);
            conference.setGroup(group);
            contacts.put(conference.getUserId(), conference);
            getXmpp().getStorage().save(getXmpp(), conference, group);
            Vcard.requestVCard(this, conference.getUserId(), null, conference.avatarHash);
        }
        xmpp.setContactListAddition(group);
        for (Contact contact : contacts.values()) {
            if (contact.isConference()) {
                XmppServiceContact conference = (XmppServiceContact) contact;
                if (conference.isAutoJoin()) {
                    xmpp.join(conference);
                }
            }
        }
        xmpp.rejoin();
    }

    public String getNickFromNode(XmlNode x) {
        String name = x.getFirstNodeValueRecursive("nickname");
        return (null == name) ? x.getFirstNodeValue(XmlConstants.S_NICK) : name;
    }

    public void autoRenameContact(Contact contact, XmlNode x) {
        if (null == contact) {
            return;
        }
        String name = getNickFromNode(x);
        if (null == name) {
            return;
        }
        if (contact.getUserId().equals(contact.getName())) {
            getXmpp().renameContact(contact, name);
        }
    }

    public void autoMoveContact(Contact contact, XmlNode presence) {
        if (null == contact) {
            return;
        }
        XmlNode x = presence.getXNode("http:/" + "/delx.cjb.net/protocol/roster-subsync");
        if (null == x) {
            return;
        }
        x = x.childAt(0);
        if (null == x) {
            return;
        }
        Group g = getXmpp().getOrCreateGroup(x.getFirstNodeValue(XmlConstants.S_GROUP));
        String name = x.getAttribute(XmlNode.S_NAME);
        boolean update = false;
        if (null != g) {
            contact.setGroup(g);
            update = true;
        }
        if (StringConvertor.isEmpty(name)) {
            contact.setName(name);
            update = true;
        }
        if (update) {
            updateContact((XmppContact) contact);
        }
    }

    String getError(XmlNode errorNode) {
        if (null == errorNode) {
            return XmlConstants.S_ERROR;
        }
        String errorText = errorNode.getFirstNodeValue(XmlConstants.S_TEXT);
        if (null == errorText) {
            errorText = errorNode.value;
        }
        if (null == errorText) {
            errorText = "error " + errorNode.getAttribute(XmlConstants.S_CODE);
            if (null != errorNode.childAt(0)) {
                errorText += ": " + errorNode.childAt(0).name;
            }
        }
        return errorText;
    }

    public void updateConfPrivate(XmppServiceContact conf, String resource) {
        String privateJid = Jid.realJidToSawimJid(conf.getUserId() + '/' + resource);
        Contact privateContact = getXmpp().getItemByUID(privateJid);
        if (null != privateContact) {
            ((XmppServiceContact) privateContact).setPrivateContactStatus(conf);
            getXmpp().ui_changeContactStatus(privateContact);
        }
    }

    public void updateContacts(ConcurrentHashMap<String, Contact> contacts) {
        StringBuilder xml = new StringBuilder();

        int itemCount = 0;
        xml.append("<iq type='set' id='").append(generateId())
                .append("'><query xmlns='jabber:iq:roster'>");
        for (Contact contact1 : contacts.values()) {
            XmppContact contact = (XmppContact) contact1;
            if (contact.isConference()) {
                continue;
            }
            itemCount++;
            xml.append("<item name='");
            xml.append(Util.xmlEscape(contact.getName()));
            xml.append("' jid='");
            xml.append(Util.xmlEscape(contact.getUserId()));
            Group group = getProtocol().getGroup(contact);
            if (null != group) {
                xml.append("'><group>");
                xml.append(Util.xmlEscape(group.getName()));
                xml.append("</group></item>");
            } else {
                xml.append("'/>");
            }
        }
        xml.append("</query></iq>");
        if (0 < itemCount) {
            putPacketIntoQueue(xml.toString());
        }
    }

    private void parseRosterExchange(XmlNode x, String domain) {
        StringBuilder xml = new StringBuilder();
        Xmpp j = protocol;
        List<XmppContact> subscribes = new ArrayList<>();
        for (int i = 0; i < x.childrenCount(); ++i) {
            XmlNode item = x.childAt(i);
            String jid = item.getAttribute(XmlNode.S_JID);
            if (!jid.endsWith(domain)) {
                continue;
            }
            boolean isDelete = item.getAttribute("action").equals("delete");
            boolean isModify = item.getAttribute("action").equals("modify");

            XmppContact contact = (XmppContact) j.getItemByUID(jid);
            if (null == contact) {
                if (isModify || isDelete) {
                    continue;
                }
                contact = (XmppContact) j.createTempContact(jid, false);
                contact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            }
            String group = item.getFirstNodeValue(XmlConstants.S_GROUP);
            if (!isDelete) {
                contact.setName(item.getAttribute(XmlNode.S_NAME));
                if (StringConvertor.isEmpty(group)) {
                    group = contact.getDefaultGroupName();
                }
                contact.setGroup(j.getOrCreateGroup(group));
                if ((null != group) && group.equals(contact.getDefaultGroupName())) {
                    group = null;
                }
                contact.setTempFlag(false);
                if (!contact.isAuth()) {
                    subscribes.add(contact);
                }
            }

            xml.append("<item jid='").append(Util.xmlEscape(jid));
            if (isDelete) {
                xml.append("' subscription='remove'/>");
                continue;
            } else if (!isModify) {
                xml.append("' ask='subscribe");
            }
            xml.append("' name='");
            xml.append(Util.xmlEscape(contact.getName()));
            if (null != group) {
                xml.append("'><group>")
                        .append(Util.xmlEscape(group))
                        .append("</group></item>");
            } else {
                xml.append("'/>");
            }
        }
        if (0 < xml.length()) {
            putPacketIntoQueue("<iq type='set' id='" + generateId()
                    + "'><query xmlns='jabber:iq:roster'>"
                    + xml.toString() + "</query></iq>");
            xml = new StringBuilder();
            for (XmppContact subscribe : subscribes) {
                xml.append("<presence type='subscribe' to='")
                        .append(Util.xmlEscape(subscribe.getUserId()))
                        .append("'/>");
            }
            if (0 < xml.length()) {
                putPacketIntoQueue(xml.toString());
            }
        }
    }

    public void removeGateContacts(String gate) {
        if (StringConvertor.isEmpty(gate)) {
            return;
        }
        gate = "@" + gate;
        ConcurrentHashMap<String, Contact> contacts = getXmpp().getContactItems();
        StringBuilder xml = new StringBuilder();

        xml.append("<iq type='set' id='").append(generateId())
                .append("'><query xmlns='jabber:iq:roster'>");
        for (Contact c : contacts.values()) {
            XmppContact contact = (XmppContact) c;
            if (!contact.getUserId().endsWith(gate)) {
                continue;
            }

            xml.append("<item subscription='remove' jid='");
            xml.append(Util.xmlEscape(contact.getUserId()));
            xml.append("'/>");
        }
        xml.append("</query></iq>");

        putPacketIntoQueue(xml.toString());
    }

    public void updateContact(XmppContact contact) {
        if (contact.isConference()) {
            contact.setTempFlag(false);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            String groupName = contact.getDefaultGroupName();
            Group group = getXmpp().getOrCreateGroup(groupName);
            contact.setGroup(group);
            Muc.saveConferences(this);
            return;
        }

        Group g = getProtocol().getGroup(contact);
        if (contact.isConference()) {
            g = getXmpp().getOrCreateGroup(contact.getDefaultGroupName());

        } else if (g != null && g.getName().equals(contact.getDefaultGroupName())) {
            g = null;
        }

        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item name='" + Util.xmlEscape(contact.getName())
                + "' jid='" + Util.xmlEscape(contact.getUserId()) + "'>"
                + (null == g ? "" : "<group>" + Util.xmlEscape(g.getName()) + "</group>")
                + "</item>"
                + "</query></iq>");
    }

    public void removeContact(Contact contact) {
        if (contact.isConference()) {
            Muc.saveConferences(this);
        }
        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item subscription='remove' jid='" + Util.xmlEscape(contact.getUserId()) + "'/>"
                + "</query></iq>");
    }

    public String getOpenStreamXml(String server) {
        return "<?xml version='1.0'?>"
                + "<stream:stream xmlns='jabber:client' "
                + "xmlns:stream='http:/" + "/etherx.jabber.org/streams' "
                + "version='1.0' "
                + "to='" + server + "'"
                + " xml:lang='" + JLocale.getSystemLanguage() + "'>";
    }

    public String getChatStateTag(String state) {
        return "<" + state + " xmlns='http://jabber.org/protocol/chatstates'/>";
    }

    void setStatus(byte statusIndex, String msg, int priority) {
        setStatus(getNativeStatus(statusIndex), msg, priority);
    }

    void setStatus(String status, String msg, int priority) {
        String xXml = XStatus.getQipXStatus(this);
        if (0 != xXml.length() && getXmpp().getProfile() != null) {
            msg = getXmpp().getProfile().xstatusTitle;
            String descr = getXmpp().getProfile().xstatusDescription;
            if (!StringConvertor.isEmpty(descr)) {
                msg += " " + descr;
            }
        }

        String xml = "<presence>"
                + (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>")
                + (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>")
                + (0 < priority ? "<priority>" + priority + "</priority>" : "")
                + ServerFeatures.getCaps();
        if (Vcard.myAvatarHash != null) {
            xml += "<x xmlns='vcard-temp:x:update'><photo>" + Vcard.myAvatarHash + "</photo></x>";
        }
        xml += xXml + "</presence>";
        putPacketIntoQueue(xml);
        //if (!AutoAbsence.getInstance().isChangeStatus())
        //setConferencesXStatus(status, msg, priority);
    }

    public void sendSubscribed(String jid) {
        requestPresence(jid, "subscribed");
    }

    public void sendUnsubscribed(String jid) {
        requestPresence(jid, "unsubscribed");
    }

    public void requestSubscribe(String jid) {
        requestPresence(jid, "subscribe");
    }

    private void requestPresence(String jid, String type) {
        putPacketIntoQueue("<presence type='" + Util.xmlEscape(type) + "' to='" + Util.xmlEscape(jid) + "'/>");
    }

    public void requestIq(String jid, String xmlns, String id, OnIqReceived iqReceivedListener) {
        XmlNode xmlNode = new XmlNode(XmlConstants.S_IQ);
        xmlNode.putAttribute(XmlConstants.S_TYPE, XmlConstants.S_GET);
        xmlNode.putAttribute(XmlConstants.S_TO, Util.xmlEscape(jid));
        if (id == null) {
            xmlNode.putAttribute(XmlNode.S_ID, generateId());
        } else {
            xmlNode.putAttribute(XmlNode.S_ID, id);
        }
        xmlNode.value = XmlNode.addXmlns(XmlConstants.S_QUERY, xmlns).toString();
        packetCallbacks.put(xmlNode.getId(), new Pair<>(xmlNode, iqReceivedListener));
        putPacketIntoQueue(xmlNode.toString());
    }

    private void requestSetIq(String xmlns) {
        putPacketIntoQueue("<iq type='set' from='" + Util.xmlEscape(fullJid_)
                + "' id='" + Util.xmlEscape(generateId()) + "'>" + xmlns + "'/></iq>");
    }

    private void requestIq(String jid, String xmlns, OnIqReceived iqReceivedListener) {
        requestIq(jid, xmlns, generateId(), iqReceivedListener);
    }

    public void requestClientVersion(String jid) {
        requestIq(jid, "jabber:iq:version", new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                String name = iqQuery.getFirstNodeValue(XmlNode.S_NAME);
                String ver = iqQuery.getFirstNodeValue("version");
                String os = iqQuery.getFirstNodeValue("os");
                name = Util.notUrls(name);
                ver = Util.notUrls(ver);
                os = Util.notUrls(os);
                String jid = getXmpp().getItemByUID(Jid.getBareJid(from)).isConference() ? from : Jid.getBareJid(from);

                DebugLog.println("ver " + jid + " " + name + " " + ver + " in " + os);

                StatusView sv = RosterHelper.getInstance().getStatusView();
                Contact c = sv.getContact();

                if ((null != c) && c.getUserId().equals(jid)) {
                    sv.setClientVersion(name + " " + ver + " " + os);
                    getXmpp().updateStatusView(sv, c);
                }
            }
        });
    }

    private void requestServerFeatures(final String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#info", generateId(), new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                serverFeatures.parseServerFeatures(iqQuery, iq.getId());
                enableCarbons();
            }
        });
    }

    public void requestConferenceInfo(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#info", new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                String name = iqQuery.getFirstNodeAttribute("identity", XmlNode.S_NAME);
                getXmpp().setConferenceInfo(from, name);
            }
        });
    }

    private void requestDiscoServerItems() {
        requestIq(domain_, "http://jabber.org/protocol/disco#items", new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                byte iqType = getIqType(iq);
                String xmlns = iqQuery.getXmlns();
                if (XmlConstants.IQ_TYPE_ERROR == iqType) {
                    XmlNode errorNode = iq.getFirstNode(XmlConstants.S_ERROR);
                    iq.removeNode(XmlConstants.S_ERROR);

                    if (null == errorNode) {
                        DebugLog.println("Error without description is stupid");
                    } else {
                        DebugLog.systemPrintln(
                                "[INFO-JABBER] <IQ> error received: " +
                                        "Code=" + errorNode.getAttribute(XmlConstants.S_CODE) + " " +
                                        "Value=" + getError(errorNode));
                    }
                    return;
                }
                while (0 < iqQuery.childrenCount()) {
                        String jid = iqQuery.popChildNode().getAttribute(XmlNode.S_JID);
                        requestServerFeatures(jid);
                    }
                    requestServerFeatures(domain_);
                }
        });
    }

    public void requestDiscoItems(String server) {
        serviceDiscovery = getXmpp().getServiceDiscovery();
        requestIq(server, "http://jabber.org/protocol/disco#items", new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                byte iqType = getIqType(iq);
                String xmlns = iqQuery.getXmlns();
                if (XmlConstants.IQ_TYPE_ERROR == iqType) {
                    XmlNode errorNode = iq.getFirstNode(XmlConstants.S_ERROR);
                    iq.removeNode(XmlConstants.S_ERROR);

                    if (null == errorNode) {
                        DebugLog.println("Error without description is stupid");
                    } else {
                        DebugLog.systemPrintln(
                                "[INFO-JABBER] <IQ> error received: " +
                                        "Code=" + errorNode.getAttribute(XmlConstants.S_CODE) + " " +
                                        "Value=" + getError(errorNode));
                    }
                    ServiceDiscovery disco = serviceDiscovery;
                    if (null != disco) {
                        serviceDiscovery = null;
                        disco.setError(getError(errorNode));
                    }
                    AdHoc commands = adhoc;
                    if ((null != commands) && commands.getJid().equals(from)) {
                        adhoc = null;
                        commands.addItems(null);
                    }
                    return;
                }
                ServiceDiscovery disco = serviceDiscovery;
                if (null != disco) {
                    serviceDiscovery = null;
                    disco.setTotalCount(iqQuery.childrenCount());
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode item = iqQuery.popChildNode();
                        String name = item.getAttribute(XmlNode.S_NAME);
                        String jid = item.getAttribute(XmlNode.S_JID);
                        disco.addItem(name, jid);
                    }
                    disco.update();
                    return;
                }
                AdHoc commands = adhoc;
                if ((null != commands) && commands.getJid().equals(from)) {
                    adhoc = null;
                    commands.addItems(iqQuery);
                }
            }
        });
    }

    public void enableMessageArchiveManager() {
        if (serverFeatures.hasMessageArchiveManagement()) {
            XmlNode xmlNode = new XmlNode(XmlConstants.S_IQ);
            xmlNode.putAttribute(XmlConstants.S_TYPE, XmlConstants.S_SET);
            xmlNode.putAttribute(XmlNode.S_ID, generateId());

            XmlNode prefsNode = XmlNode.addXmlns("prefs", "urn:xmpp:mam:0");
            prefsNode.putAttribute("default", "roster");
            prefsNode.addSubTag("always");
            xmlNode.addNode(prefsNode);
            putPacketIntoQueue(xmlNode.toString());
        }
    }

    private void enableCarbons() {
        if (!serverFeatures.hasCarbon() || serverFeatures.isCarbonsEnabled()) return;
        XmlNode xmlNode = XmlNode.addXmlns("enable", "urn:xmpp:carbons:2");
        putPacketIntoQueue(xmlNode, new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode xmlNode) {
                XmlNode errorNode = xmlNode.getFirstNode(XmlConstants.S_ERROR);
                serverFeatures.setCarbonsEnabled(errorNode == null);
            }
        });
    }

    void requestRawXml(String xml) {
        putPacketIntoQueue(xml);
    }

    UserInfo getUserInfo(Contact contact) {
        singleUserInfo = new UserInfo(getXmpp(), contact.getUserId());
        Vcard.getVCard(this, contact.getUserId());
        return singleUserInfo;
    }

    void register2(XmppForm form, String rawXml, String jid) {
        xmppForm = form;
        autoSubscribeDomain = jid;
        requestRawXml(rawXml);
    }

    public boolean isAutoGateContact(String jid) {
        return !StringConvertor.isEmpty(autoSubscribeDomain)
                && (jid.equals(autoSubscribeDomain) || jid.endsWith('@' + autoSubscribeDomain));
    }

    XmppForm register(String jid) {
        xmppForm = new XmppForm(XmppForm.TYPE_REGISTER, getXmpp(), jid);
        requestIq(jid, "jabber:iq:register", xmppForm.getId(), new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                String id = iq.getId();
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                byte iqType = getIqType(iq);
                String xmlns = iqQuery.getXmlns();
                if ((null != xmppForm) && xmppForm.getId().equals(id)) {
                    if (xmppForm.isWaiting()) {
                        xmppForm.loadFromXml(iqQuery, iqQuery);
                        xmppForm = null;

                    } else {
                        processEmptyId(id, iqType, from);
                    }
                }
            }
        });
        return xmppForm;
    }

    XmppForm requestOwnerForm(String jid) {
        xmppForm = new XmppForm(XmppForm.TYPE_OWNER, getXmpp(), jid);
        requestIq(jid, "http://jabber.org/protocol/muc#owner", xmppForm.getId(), new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode iqQuery = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                byte iqType = getIqType(iq);
                String xmlns = iqQuery.getXmlns();
                if (null != xmppForm) {
                    xmppForm.loadFromXml(iqQuery, iq);
                    xmppForm = null;
                }
            }
        });
        return xmppForm;
    }

    void unregister(String jid) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='unreg1'><query xmlns='jabber:iq:register'><remove/></query></iq>");
    }

    void showContactSeen(String jid) {
        putPacketIntoQueue("<iq to='" + Util.xmlEscape(jid) + "' type='get' id='last_seen'><query xmlns='jabber:iq:last'/></iq>");
    }

    void resetAdhoc() {
        adhoc = null;
    }

    void requestCommand(AdHoc adhoc, String node) {
        this.adhoc = adhoc;
        putPacketIntoQueue("<iq to='" + Util.xmlEscape(adhoc.getJid())
                + "' type='set' id='" + Util.xmlEscape(generateId()) + "'>"
                + "<command xmlns='http://jabber.org/protocol/commands' "
                + "node='" + Util.xmlEscape(node) + "'/></iq>");
    }

    void requestCommandList(AdHoc adhoc) {
        this.adhoc = adhoc;
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(adhoc.getJid())
                + "' id='" + Util.xmlEscape(generateId()) + "'><query xmlns='"
                + "http://jabber.org/protocol/disco#items"
                + "' node='http://jabber.org/protocol/commands'/></iq>");
    }


    void setIBB(IBBFileTransfer transfer) {
        ibb = transfer;
        ibb.setProgress(0);
        putPacketIntoQueue(ibb.getRequest());
    }

    private boolean processIbb(XmlNode iq, byte type, String id) {
        id = StringConvertor.notNull(id);
        if (!id.startsWith("Sawimibb_")) {
            return false;
        }
        if (XmlConstants.IQ_TYPE_RESULT != type) {

            ibb.setProgress(-1);
            ibb.destroy();
            ibb = null;
            return true;
        }
        if ("Sawimibb_si".equals(id)) {
            ibb.setProgress(10);
            putPacketIntoQueue(ibb.initTransfer());
            return true;
        }

        if ("Sawimibb_close".equals(id)) {
            return true;
        }
        if (ibb.isCanceled()) {
            putPacketIntoQueue(ibb.close());
            ibb.destroy();
            ibb = null;
            return true;
        }

        ibb.setProgress(ibb.getPercent());
        String stanza = ibb.nextBlock();
        if (null == stanza) {
            stanza = ibb.close();
            ibb.setProgress(100);
            ibb.destroy();
            ibb = null;
        }
        putPacketIntoQueue(stanza);
        return true;
    }

    public void queryMessageArchiveManagement(Contact contact, OnMoreMessagesLoaded moreMessagesLoadedListener) {
    /*    if (!serverFeatures.hasMessageArchiveManagement()) return;
        if (!contact.isHasMessagesLeftOnServer()) return;
        if (getMessageArchiveManagement().queryInProgress(contact, moreMessagesLoadedListener)) return;
        MessageArchiveManagement.Query query = messageArchiveManagement.prev(this, contact);
        if (query != null) {
            query.setOnMoreMessagesLoaded(moreMessagesLoadedListener);
        }*/
    }

    public MessageArchiveManagement getMessageArchiveManagement() {
        return messageArchiveManagement;
    }
}
