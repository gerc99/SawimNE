package protocol.xmpp;

import protocol.*;
import protocol.net.ClientConnection;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.Config;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.ImageCache;
import ru.sawim.io.FileSystem;
import ru.sawim.io.RosterStorage;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.crypto.MD5;
import ru.sawim.modules.crypto.SHA1;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.RosterHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;


public final class XmppConnection extends ClientConnection {

    public static final boolean DEBUGLOG = false;
    private static final int MESSAGE_COUNT_AFTER_CONNECT = 20;

    private static final String[] statusCodes = {
            "unavailable",
            "",
            "away",
            "chat",
            "",
            "",
            "",
            "",
            "",
            "xa",
            "",
            "dnd",
            "",
            ""
    };

    private static final String S_TEXT = "text";
    private static final String S_FROM = "from";
    private static final String S_TYPE = "type";
    private static final String S_ERROR = "error";
    private static final String S_NONE = "none";
    private static final String S_NODE = "node";
    private static final String S_NICK = "nick";
    private static final String S_SET = "set";
    private static final String S_REMOVE = "remove";
    private static final String S_RESULT = "result";
    private static final String S_GROUP = "group";
    private static final String S_ITEM = "item";
    private static final String S_ITEMS = "items";
    private static final String S_TRUE = "true";
    private static final String S_FALSE = "false";
    private static final String S_GET = "get";
    private static final String S_TIME = "time";
    private static final String S_TITLE = "title";
    private static final String S_CODE = "code";
    private static final String S_QUERY = "query";
    private static final String S_STATUS = "status";
    private static final String S_VCARD = "vCard";
    private static final String S_SUBJECT = "subject";
    private static final String S_BODY = "body";
    private static final String S_URL = "url";
    private static final String S_DESC = "desc";
    private static final String S_COMPOSING = "composing";
    private static final String S_ACTIVE = "active";
    private static final String S_PAUSED = "paused";
    private static final String S_CHAT = "chat";
    private static final String S_GROUPCHAT = "groupchat";
    private static final String S_HEADLINE = "headline";

    private static final String GET_ROSTER_XML = "<iq type='get' id='roster'>"
            + "<query xmlns='jabber:iq:roster'/>"
            + "</iq>";

    private static final byte IQ_TYPE_RESULT = 0;
    private static final byte IQ_TYPE_GET = 1;
    private static final byte IQ_TYPE_SET = 2;
    private static final byte IQ_TYPE_ERROR = 3;

    private Socket socket;
    private Xmpp protocol;

    String fullJid_;
    private String domain_ = "";
    private String resource;
    private final boolean xep0048 = false;
    private byte[] pingPacket = new byte[]{' '};
    private byte[] forPongPacket = StringConvertor.stringToByteArrayUtf8(
            "<iq type='get'><ping xmlns='urn:xmpp:ping'/></iq>");

    private String verHash = "";
    private String featureList = "";

    private final Vector packets = new Vector();

    private boolean isGTalk_ = false;
    private boolean authorized_ = false;
    private boolean rosterLoaded = false;

    private UserInfo singleUserInfo;
    private String autoSubscribeDomain;
    private XmppForm xmppForm;

    private IBBFileTransfer ibb;

    private ServiceDiscovery serviceDiscovery = null;
    private AdHoc adhoc;
    private AffiliationListConf affListConf = null;
    private MirandaNotes notes = null;

    private SASL_ScramSha1 scramSHA1;

    private String myAvatarHash = null;

    private byte nativeStatus2StatusIndex(String rawStatus) {
        rawStatus = StringConvertor.notNull(rawStatus);
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (rawStatus.equals(statusCodes[i])) {
                return i;
            }
        }
        return StatusInfo.STATUS_ONLINE;
    }

    private String getNativeStatus(byte statusIndex) {
        return statusCodes[statusIndex];
    }

    static boolean isTrue(String val) {
        return S_TRUE.equals(val) || "1".equals(val);
    }

    public String getCaps() {
        return "<c xmlns='http://jabber.org/protocol/caps'"
                + " node='http://sawim.ru/caps' ver='"
                + Util.xmlEscape(verHash)
                + "' hash='md5'/>";
    }

    public XmppConnection() {
    }

    public void setXmpp(Xmpp xmpp) {
        protocol = xmpp;
        resource = xmpp.getResource();
        fullJid_ = xmpp.getUserId() + '/' + resource;
        domain_ = Jid.getDomain(fullJid_);
    }

    private void setProgress(int percent) {
        getXmpp().setConnectingProgress(percent);
    }

    private void setStreamCompression() throws SawimException {
        setProgress(20);
        try {
            socket.startCompression();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        write(getOpenStreamXml(domain_));
        readXmlNode(true);
        parseAuth(readXmlNode(true));
    }

    private void setStreamTls() throws SawimException {
        setProgress(15);
        socket.startTls(SawimApplication.sc, domain_);
        write(getOpenStreamXml(domain_));

        DebugLog.println("tls turn on");
        readXmlNode(true); // "stream:stream"
        parseAuth(readXmlNode(true));
    }

    public Xmpp getXmpp() {
        return protocol;
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

    public void putPacketIntoQueue(Object packet) {
        synchronized (packets) {
            packets.addElement(packet);
        }
    }

    private boolean hasOutPackets() {
        synchronized (packets) {
            return !packets.isEmpty();
        }
    }

    private void sendPacket() throws SawimException {
        String packet = null;
        synchronized (packets) {
            packet = (String) packets.elementAt(0);
            packets.removeElementAt(0);
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

    private void write(String xml) throws SawimException {
        if (DEBUGLOG)
            DebugLog.println("[OUT]:\n" + xml);
        write(StringConvertor.stringToByteArrayUtf8(xml));
    }

    void writePacket(String packet) throws SawimException {
        write(packet);
    }

    private XmlNode readXmlNode(boolean notEmpty) throws SawimException {
        XmlNode node = socket.readNode(notEmpty);
        if (DEBUGLOG && node != null)
            DebugLog.println("[IN]:\n" + node.toString());
        return node;
    }

    private void sendRequest(String request) throws SawimException {
        write(request);
    }

    private void setAuthStatus(boolean authorized) throws SawimException {
        if (!authorized_) {
            authorized_ = authorized;
            if (!authorized) {
                getXmpp().setPassword(null);
                throw new SawimException(111, 0);
            }
        }
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

    private String[] getHostAndPort(String server) {
        // TODO: legacy SSL
        String defaultServer = getXmpp().getDefaultServer(domain_);

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
            url = Util.explode(defaultServer, ':');
            if (3 == url.length) {
                socketUrl[0] = url[1];
                socketUrl[1] = url[2];
            }
        }
        return socketUrl;
    }

    protected final void connect() throws SawimException {
        connect = true;
        setProgress(0);
        initFeatures();
        protocol.net.SrvResolver resolver = new protocol.net.SrvResolver();
        String server = Config.getConfigValue(domain_, "/jabber-servers.txt");
        String defaultServer = getXmpp().getDefaultServer(domain_);
        if (StringConvertor.isEmpty(server) && (null == defaultServer)) {
            server = resolver.getXmpp(domain_);
        }
        if (StringConvertor.isEmpty(server)) {
            server = domain_;
        }
        String[] hostAndPort = getHostAndPort(server);
        connectTo(hostAndPort[0], Integer.parseInt(hostAndPort[1]));

        write(getOpenStreamXml(domain_));
        setProgress(10);

        readXmlNode(true);
        resolver.close();
        parseAuth(readXmlNode(true));
        while (!authorized_) {
            loginParse(readXmlNode(true));
        }

        setProgress(30);
        socket.start();
        write(GET_ROSTER_XML);
        setProgress(50);
        usePong();
        setProgress(60);
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

    private void parseAuth(XmlNode x) throws SawimException {
        if ((null == x) || !x.is("stream:features")) {
            nonSaslLogin();
        } else {
            loginParse(x);
        }
    }

    private void nonSaslLogin() throws SawimException {
        String user = Jid.getNick(protocol.getUserId());
        sendRequest(
                "<iq type='set' to='" + domain_ + "' id='login'>"
                        + "<query xmlns='jabber:iq:auth'>"
                        + "<username>" + Util.xmlEscape(user) + "</username>"
                        + "<password>" + Util.xmlEscape(protocol.getPassword()) + "</password>"
                        + "<resource>" + Util.xmlEscape(resource) + "</resource>"
                        + "</query>"
                        + "</iq>");
        XmlNode answer = readXmlNode(true);
        setAuthStatus(S_RESULT.equals(answer.getAttribute(S_TYPE)));
    }

    private void loginParse(XmlNode x) throws SawimException {
        if (x.is("stream:features")) {
            parseStreamFeatures(x);
            return;
        } else if (x.is("compressed")) {
            setStreamCompression();
            return;
        } else if (x.is("proceed")) {
            setStreamTls();
            return;
        } else if (x.is("challenge")) {
            parseChallenge(x);
            return;
        } else if (x.is("failure")) {
            DebugLog.systemPrintln("[INFO-JABBER] Failed");
            setAuthStatus(false);
            return;

        } else if (x.is("success")) {
            if (null != scramSHA1) {
                if (!scramSHA1.success(new String(Util.base64decode(x.value)))) {
                    DebugLog.systemPrintln("Server answer not valid");
                    setAuthStatus(false);
                    return;
                }
                scramSHA1 = null;
            }
            DebugLog.systemPrintln("[INFO-JABBER] Auth success");
            DebugLog.systemPrintln("auth " + authorized_);

            sendRequest(getOpenStreamXml(domain_));
            return;

        } else if (x.is("iq")) {
            XmlNode iqQuery = x.childAt(0);
            String id = x.getId();
            if ("sess".equals(id)) {
                setAuthStatus(true);
                return;
            }
            if (null == iqQuery) {
                return;
            }
            String queryName = iqQuery.name;
            if (IQ_TYPE_ERROR == getIqType(x)) {
                if ("jabber:iq:auth".equals(iqQuery.getXmlns())) {
                    setAuthStatus(false);
                }
            }
            if ("bind".equals(queryName)) {
                DebugLog.systemPrintln("[INFO-JABBER] Send open session request");
                fullJid_ = iqQuery.getFirstNodeValue(XmlNode.S_JID);
                sendRequest("<iq type='set' id='sess'>"
                        + "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>"
                        + "</iq>");
                return;
            }
        }
        parse(x);
    }

    private void parse(XmlNode x) throws SawimException {
        if (x.is("iq")) {
            parseIq(x);

        } else if (x.is("presence")) {
            parsePresence(x);

        } else if (x.is("message")) {
            parseMessage(x);

        } else if (x.is("stream:error")) {
            setAuthStatus(false);

            XmlNode err = (null == x.childAt(0)) ? x : x.childAt(0);
            DebugLog.systemPrintln("[INFO-JABBER] Stream error!: " + err.name + "," + err.value);
        }
    }

    private String generateId(String key) {
        return key + Util.uniqueValue();
    }

    private String generateId() {
        return "sawim" + Util.uniqueValue();
    }

    private boolean isNoAutorized(String subscription) {
        return S_NONE.equals(subscription) || S_FROM.equals(subscription);
    }

    private byte getIqType(XmlNode iq) {
        String iqType = iq.getAttribute(S_TYPE);
        if (S_RESULT.equals(iqType)) {
            return IQ_TYPE_RESULT;
        }
        if (S_GET.equals(iqType)) {
            return IQ_TYPE_GET;
        }
        if (S_SET.equals(iqType)) {
            return IQ_TYPE_SET;
        }
        return IQ_TYPE_ERROR;
    }

    private void parseIqError(XmlNode iqNode, String from) throws SawimException {
        XmlNode errorNode = iqNode.getFirstNode(S_ERROR);
        iqNode.removeNode(S_ERROR);

        if (null == errorNode) {
            DebugLog.println("Error without description is stupid");
        } else {
            /*DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + getError(errorNode));*/
        }


        XmlNode query = iqNode.childAt(0);
        if (null == query) {

        } else if (S_VCARD.equals(query.name)) {
            loadVCard(null, from);

        } else if (S_QUERY.equals(query.name)) {
            String xmlns = query.getXmlns();
            if ("jabber:iq:register".equals(xmlns) && (null != xmppForm)) {
                xmppForm.error(getError(errorNode));
                xmppForm = null;

            } else if ("jabber:iq:roster".equals(query.name)) {

            } else if ("http://jabber.org/protocol/disco#items".equals(xmlns)) {
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
        if (IQ_TYPE_RESULT != iqType) {
            return;
        }
        if (id.startsWith(S_VCARD)) {
            loadVCard(null, from);
        }
        if ((null != xmppForm) && xmppForm.getId().equals(id)) {
            xmppForm.success();
            xmppForm = null;
        }
    }

    private void parseIq(XmlNode iq) throws SawimException {
        String from = StringConvertor.notNull(iq.getAttribute(S_FROM));
        byte iqType = getIqType(iq);
        String id = iq.getId();
        if (StringConvertor.isEmpty(id)) {
            id = generateId();
        }
        if (null != ibb) {
            boolean processed = processIbb(iq, iqType, id);
            if (processed) {
                return;
            }
        }
        if (IQ_TYPE_ERROR == iqType) {
            parseIqError(iq, from);
            return;
        }
        XmlNode iqQuery = iq.childAt(0);
        if (null == iqQuery) {
            processEmptyId(id, iqType, from);
            return;
        }
        String queryName = iqQuery.name;
        Xmpp xmpp = getXmpp();
        if (S_QUERY.equals(queryName)) {
            String xmlns = iqQuery.getXmlns();
            if ("jabber:iq:roster".equals(xmlns)) {
                if (!isMy(from)) {
                    return;
                }
                if ((IQ_TYPE_RESULT == iqType) && !rosterLoaded) {
                    rosterLoaded = true;
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = iqQuery.popChildNode();
                        String jid = itemNode.getAttribute(XmlNode.S_JID);
                        Contact contact = xmpp.getItemByUID(jid);
                        if (contact == null) {
                            contact = xmpp.createContact(jid, jid);
                        }
                        contact.setName(itemNode.getAttribute(XmlNode.S_NAME));

                        String groupName = itemNode.getFirstNodeValue(S_GROUP);
                        if (StringConvertor.isEmpty(groupName) || Jid.isConference(jid)) {
                            groupName = contact.getDefaultGroupName();
                        }
                        Group g = xmpp.getOrCreateGroup(groupName);
                        contact.setGroup(g);

                        String subscription = itemNode.getAttribute("subscription");
                        contact.setBooleanValue(Contact.CONTACT_NO_AUTH, isNoAutorized(subscription));
                        if (!xmpp.getContactItems().contains(contact)) {
                            xmpp.getContactItems().add(contact);
                        }
                        RosterHelper.getInstance().updateGroup(xmpp, g);
                    }
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
                    getVCard(xmpp.getUserId());
                    String xcode = Xmpp.xStatus.getCode(xmpp.getProfile().xstatusIndex);
                    if ((null != xcode) && !xcode.startsWith(XmppXStatus.XSTATUS_START)) {
                        setXStatus();
                    }
                    setProgress(90);
                    getBookmarks();
                    putPacketIntoQueue("<iq type='get' id='getnotes'><query xmlns='jabber:iq:private'><storage xmlns='storage:rosternotes'/></query></iq>");
                    setProgress(100);

                } else if (IQ_TYPE_SET == iqType) {
                    while (0 < iqQuery.childrenCount()) {
                        XmlNode itemNode = iqQuery.popChildNode();
                        String subscription = itemNode.getAttribute("subscription");
                        String jid = itemNode.getAttribute(XmlNode.S_JID);
                        if (Jid.isConference(jid)) {

                        } else if ((S_REMOVE).equals(subscription)) {
                            xmpp.removeLocalContact(xmpp.getItemByUID(jid));
                        } else {
                            String name = itemNode.getAttribute(XmlNode.S_NAME);
                            Contact contact = xmpp.createTempContact(jid);
                            String group = itemNode.getFirstNodeValue(S_GROUP);
                            if (StringConvertor.isEmpty(group) || Jid.isConference(contact.getUserId())) {
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
                if (IQ_TYPE_GET == iqType) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<iq type='result' to='")
                            .append(Util.xmlEscape(from))
                            .append("' id='").append(Util.xmlEscape(id)).append("'>");
                    sb.append("<query xmlns='http://jabber.org/protocol/disco#info'>");
                    sb.append(featureList);
                    sb.append("</query></iq>");
                    write(sb.toString());
                    return;
                }
                if (IQ_TYPE_RESULT != iqType) {
                    return;
                }
                String name = iqQuery.getFirstNodeAttribute("identity", XmlNode.S_NAME);
                xmpp.setConferenceInfo(from, name);
                return;

            } else if ("http://jabber.org/protocol/disco#items".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    sendIqError(S_QUERY, xmlns, from, id);
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
                return;
            } else if ("http://jabber.org/protocol/muc#admin".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    sendIqError(S_QUERY, xmlns, from, id);
                    return;
                }
                AffiliationListConf uslist = affListConf;
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
            } else if ("jabber:iq:register".equals(xmlns)) {
                if ((null != xmppForm) && xmppForm.getId().equals(id)) {
                    if (xmppForm.isWaiting()) {
                        xmppForm.loadFromXml(iqQuery, iqQuery);
                        xmppForm = null;

                    } else {
                        processEmptyId(id, iqType, from);
                    }
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
                        loadMirandaNotes(storage);
                    }
                }
                if (IQ_TYPE_RESULT == iqType && ("getnotes").equals(id)) {
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
                if (IQ_TYPE_RESULT == iqType) {
                    String name = iqQuery.getFirstNodeValue(XmlNode.S_NAME);
                    String ver = iqQuery.getFirstNodeValue("version");
                    String os = iqQuery.getFirstNodeValue("os");
                    name = Util.notUrls(name);
                    ver = Util.notUrls(ver);
                    os = Util.notUrls(os);
                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);

                    DebugLog.println("ver " + jid + " " + name + " " + ver + " in " + os);

                    StatusView sv = RosterHelper.getInstance().getStatusView();
                    Contact c = sv.getContact();

                    if ((null != c) && c.getUserId().equals(jid)) {
                        sv.setClientVersion(name + " " + ver + " " + os);
                        getXmpp().updateStatusView(sv, c);
                    }
                    return;
                }
                String platform = Util.xmlEscape(SawimApplication.PHONE);
                if (IQ_TYPE_GET == iqType) {
                    putPacketIntoQueue("<iq type='result' to='"
                            + Util.xmlEscape(from) + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:version'><name>"
                            + Util.xmlEscape(SawimApplication.NAME)
                            + "</name><version>"
                            + Util.xmlEscape(SawimApplication.VERSION)
                            + "</version><os>"
                            + platform
                            + "</os></query></iq>");

                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
                    //MagicEye.addAction(xmpp, jid, "get_version");
                }
                return;

            } else if ("jabber:iq:last".equals(xmlns)) {
                if (IQ_TYPE_GET == iqType) {
                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
                    //MagicEye.addAction(xmpp, jid, "last_activity_request");

                    long time = SawimApplication.getCurrentGmtTime() - xmpp.getLastStatusChangeTime();
                    putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                            + "' id='" + Util.xmlEscape(id) + "'>"
                            + "<query xmlns='jabber:iq:last' seconds='"
                            + time
                            + "'/></iq>");
                }
                if (IQ_TYPE_RESULT == iqType) {
                    String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
                    String time = iqQuery.getAttribute("seconds");
                    time = Util.secDiffToDate(Integer.parseInt(time));
                    getXmpp().addMessage(new SystemNotice(getXmpp(), (byte) -1, Jid.getBareJid(from), time));
                }
                return;

            } else if ("http://jabber.org/protocol/muc#owner".equals(xmlns)) {
                if (IQ_TYPE_RESULT == iqType) {
                    if (null != xmppForm) {
                        xmppForm.loadFromXml(iqQuery, iq);
                        xmppForm = null;
                    }
                }
            }

        } else if (S_TIME.equals(queryName)) {
            if (IQ_TYPE_GET != iqType) {
                return;
            }
            String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
            //MagicEye.addAction(xmpp, jid, "get_time");

            putPacketIntoQueue("<iq type='result' to='" + Util.xmlEscape(from)
                    + "' id='" + Util.xmlEscape(id) + "'>"
                    + "<time xmlns='urn:xmpp:time'><tzo>"
                    + (0 <= SawimApplication.gmtOffset ? "+" : "-") + Util.makeTwo(Math.abs(SawimApplication.gmtOffset)) + ":00"
                    + "</tzo><utc>"
                    + Util.getUtcDateString(SawimApplication.getCurrentGmtTime())
                    + "</utc></time></iq>");
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

        } else if (S_VCARD.equals(queryName)) {
            if (IQ_TYPE_RESULT == iqType) {
                loadVCard(iqQuery, from);
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
            String jid = Jid.isConference(from) ? from : Jid.getBareJid(from);
            PlainMessage message = new PlainMessage(jid, getProtocol(),
                    SawimApplication.getCurrentGmtTime(), PlainMessage.CMD_WAKEUP, false);
            getXmpp().addMessage(message);
        }
        if (IQ_TYPE_GET == iqType) {
            sendIqError(iqQuery.name, iqQuery.getXmlns(), from, id);
        }
    }

    private void loadMirandaNotes(XmlNode storage) {
        MirandaNotes _notes = notes;
        notes = null;
        _notes.clear();
        while (0 < storage.childrenCount()) {
            XmlNode item = storage.popChildNode();
            String tags = item.getAttribute("tags");
            String title = item.getFirstNodeValue("title");
            String text = item.getFirstNodeValue("text");
            if (title == null) title = "";
            if (tags == null) tags = "";
            if (text == null) text = "";
            _notes.addNote(title, tags, text);
        }
    }

    void saveMirandaNotes(String storage) {
        StringBuilder xml = new StringBuilder();
        xml.append("<iq type='set'><query xmlns='jabber:iq:private'>");
        xml.append("<storage xmlns='http://miranda-im.org/storage#notes'>");
        xml.append(storage);
        xml.append("</storage></query></iq>");
        putPacketIntoQueue(xml.toString());
    }

    public void requestMirandaNotes() {
        notes = getXmpp().getMirandaNotes();
        String xml = "<iq type='get'>"
                + "<query xmlns='jabber:iq:private'>"
                + "<storage xmlns='http://miranda-im.org/storage#notes' />"
                + "</query></iq>";
        putPacketIntoQueue(xml);
    }

    public void saveVCard(UserInfo userInfo) {
        if (null == userInfo.vCard) {
            userInfo.vCard = XmlNode.getEmptyVCard();
        }
        userInfo.vCard.removeBadVCardTags("TEL");
        userInfo.vCard.removeBadVCardTags("EMAIL");
        userInfo.vCard.setValue("NICKNAME", userInfo.nick);
        userInfo.vCard.setValue("BDAY", userInfo.birthDay);
        userInfo.vCard.setValue("URL", userInfo.homePage);
        userInfo.vCard.setValue("FN", userInfo.getName());
        userInfo.vCard.setValue("N", null, "GIVEN", userInfo.firstName);
        userInfo.vCard.setValue("N", null, "FAMILY", userInfo.lastName);
        userInfo.vCard.setValue("N", null, "MIDDLE", "");
        userInfo.vCard.setValue("EMAIL", new String[]{"INTERNET"}, "USERID", userInfo.email);
        userInfo.vCard.setValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", userInfo.cellPhone);

        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "STREET", userInfo.homeAddress);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "LOCALITY", userInfo.homeCity);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "REGION", userInfo.homeState);

        userInfo.vCard.setValue("TEL", new String[]{}, "NUMBER", "");
        userInfo.vCard.setValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER", userInfo.workPhone);
        userInfo.vCard.setValue("ORG", null, "ORGNAME", userInfo.workCompany);
        userInfo.vCard.setValue("ORG", null, "ORGUNIT", userInfo.workDepartment);
        userInfo.vCard.setValue("TITLE", userInfo.workPosition);
        userInfo.vCard.setValue("DESC", userInfo.about);
        userInfo.vCard.cleanXmlTree();

        StringBuffer packet = new StringBuffer();
        packet.append("<iq type='set' to='").append(Util.xmlEscape(userInfo.uin))
                .append("' id='").append(generateId()).append("'>");
        userInfo.vCard.toString(packet);
        packet.append("</iq>");
        putPacketIntoQueue(packet.toString());
        updateAvatar(userInfo.vCard, userInfo.uin);
    }

    private void loadVCard(XmlNode vCard, String from) {
        updateAvatar(vCard, from);
        UserInfo userInfo = singleUserInfo;
        if (null != userInfo && from.equals(userInfo.realUin)) {
            userInfo.auth = false;
            userInfo.uin = from;
            if (Jid.isConference(from)) {
                Contact c = getXmpp().getItemByUID(Jid.getBareJid(from));
                if (c instanceof XmppServiceContact) {
                    XmppContact.SubContact sc = ((XmppServiceContact) c).getExistSubContact(Jid.getResource(from, null));
                    if ((null != sc) && (null != sc.realJid)) {
                        userInfo.uin = sc.realJid;
                    }
                }
            }
            if (null == vCard) {
                userInfo.updateProfileView();
                singleUserInfo = null;
                return;
            }
            String name[] = new String[3];
            name[0] = vCard.getFirstNodeValue("N", "GIVEN");
            name[1] = vCard.getFirstNodeValue("N", "MIDDLE");
            name[2] = vCard.getFirstNodeValue("N", "FAMILY");
            if (StringConvertor.isEmpty(Util.implode(name, ""))) {
                userInfo.firstName = vCard.getFirstNodeValue("FN");
                userInfo.lastName = null;
            } else {
                userInfo.lastName = name[2];
                name[2] = null;
                userInfo.firstName = Util.implode(name, " ");
            }
            userInfo.nick = vCard.getFirstNodeValue("NICKNAME");
            userInfo.birthDay = vCard.getFirstNodeValue("BDAY");
            userInfo.email = vCard.getFirstNodeValue("EMAIL", new String[]{"INTERNET"}, "USERID", true);
            userInfo.about = vCard.getFirstNodeValue("DESC");
            userInfo.homePage = vCard.getFirstNodeValue("URL");

            userInfo.homeAddress = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "STREET", true);
            userInfo.homeCity = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "LOCALITY", true);
            userInfo.homeState = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "REGION", true);
            userInfo.cellPhone = vCard.getFirstNodeValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", true);

            userInfo.workCompany = vCard.getFirstNodeValue("ORG", null, "ORGNAME");
            userInfo.workDepartment = vCard.getFirstNodeValue("ORG", null, "ORGUNIT");
            userInfo.workPosition = vCard.getFirstNodeValue("TITLE");
            userInfo.workPhone = vCard.getFirstNodeValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER");

            if (!Jid.isGate(from)) {
                userInfo.setOptimalName();
            }
            if (userInfo.isEditable()) {
                userInfo.vCard = vCard;
            }
            userInfo.updateProfileView();

            XmlNode bs64photo = vCard.getFirstNode("PHOTO");
            bs64photo = (null == bs64photo) ? null : bs64photo.getFirstNode("BINVAL");
            if (null != bs64photo) {
                new Thread(new AvatarLoader(userInfo, bs64photo), "XMPPAvatarLoad").start();
            }
            singleUserInfo = null;
        }
    }

    private void updateAvatar(XmlNode vCard, String from) {
        if (null != vCard) {
            Contact c = getXmpp().getItemByUID(Jid.getBareJid(from));
            XmlNode bs64photo = vCard.getFirstNode("PHOTO");
            bs64photo = (null == bs64photo) ? null : bs64photo.getFirstNode("BINVAL");
            if (bs64photo != null) {
                byte[] avatarBytes = Util.base64decode(bs64photo.value);
                String avatarHash = StringConvertor.byteArrayToHexString(SHA1.calculate(avatarBytes));
                if (from.equals(getXmpp().getUserId())) {
                    if (myAvatarHash == null || !myAvatarHash.equals(avatarHash)) {
                        myAvatarHash = avatarHash;
                        getXmpp().s_updateOnlineStatus();
                    }
                }
                if (Jid.isConference(from)) {
                    if (c != null && c instanceof XmppServiceContact) {
                        XmppContact.SubContact sc = ((XmppServiceContact) c).getExistSubContact(Jid.getResource(from, null));
                        if (null != sc) {
                            sc.avatarHash = avatarHash;
                            RosterStorage.updateAvatarHash(c.getUserId() + "/" + sc.resource, avatarHash);
                        } else {
                            c.avatarHash = avatarHash;
                            RosterStorage.updateAvatarHash(c.getUserId(), avatarHash);
                        }
                    }
                } else {
                    if (c != null) {
                        c.avatarHash = avatarHash;
                        RosterStorage.updateAvatarHash(c.getUserId(), avatarHash);
                    }
                }
                ImageCache.getInstance().save(FileSystem.openDir(FileSystem.AVATARS),
                        from, avatarHash, avatarBytes);
            } else {
                if (from.equals(getXmpp().getUserId())) {
                    if (myAvatarHash != null) {
                        myAvatarHash = null;
                        getXmpp().s_updateOnlineStatus();
                    }
                }
                if (Jid.isConference(from)) {
                    if (c != null && c instanceof XmppServiceContact) {
                        XmppContact.SubContact sc = ((XmppServiceContact) c).getExistSubContact(Jid.getResource(from, null));
                        if (null != sc) {
                            sc.avatarHash = "";
                        } else {
                            c.avatarHash = "";
                        }
                    }
                } else {
                    if (c != null) {
                        c.avatarHash = "";
                    }
                }
            }
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
        Vector groups = xmpp.getGroupItems();
        Vector contacts = xmpp.getContactItems();
        while (0 < storage.childrenCount()) {
            XmlNode item = storage.popChildNode();

            String jid = item.getAttribute(XmlNode.S_JID);
            if ((null == jid) || !Jid.isConference(jid)) {
                continue;
            }
            String name = item.getAttribute(XmlNode.S_NAME);
            String nick = item.getFirstNodeValue(S_NICK);
            boolean autojoin = isTrue(item.getAttribute("autojoin"));
            String password = item.getAttribute("password");

            XmppServiceContact conference = (XmppServiceContact) xmpp.createTempContact(jid, name);
            conference.setMyName(nick);
            conference.setTempFlag(false);
            conference.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            conference.setAutoJoin(autojoin);
            conference.setPassword(password);
            conference.setGroup(group);
            if (-1 == contacts.indexOf(conference)) {
                contacts.addElement(conference);
            }
            if (conference.isAutoJoin()) {
                xmpp.join(conference);
            }
            requestVCard(conference.getUserId(), null, conference.avatarHash);
        }
        xmpp.setContactListAddition(group);
        xmpp.rejoin();
    }

    private void requestVCard(String id, String newAvatarHash, String avatarHash) {
        if (avatarHash != null && !avatarHash.isEmpty()) {
            if (!ImageCache.getInstance().hasFile(FileSystem.openDir(FileSystem.AVATARS), avatarHash)) {
                getVCard(id);
                return;
            }
        }
        if (newAvatarHash == null) {
            if (avatarHash == null) {
                if (RosterStorage.getAvatarHash(id) == null) {
                    getVCard(id);
                }
            }
        } else {
            if (!newAvatarHash.equals(avatarHash)) {
                getVCard(id);
            }
        }
    }

    private void parsePresence(XmlNode x) {
        final String fromFull = x.getAttribute(S_FROM);
        final String from = Jid.getBareJid(fromFull);
        final String fromRes = Jid.getResource(fromFull, "");
        String type = x.getAttribute(S_TYPE);

        if (S_ERROR.equals(type)) {
            XmlNode errorNode = x.getFirstNode(S_ERROR);

            /*DebugLog.systemPrintln(
                    "[INFO-JABBER] <IQ> error received: " +
                    "Code=" + errorNode.getAttribute(S_CODE) + " " +
                    "Value=" + errorNode.getFirstNodeValue(S_TEXT));
            */

            boolean showError = Jid.isGate(from);
            if (Jid.isConference(from)) {
                XmppServiceContact conf = (XmppServiceContact) getXmpp().getItemByUID(from);
                if (null != conf) {
                    int code = Util.strToIntDef(errorNode.getAttribute(S_CODE), -1);
                    conf.nickError(getXmpp(), fromRes, code, getError(errorNode));
                    return;
                }
            }
            if (showError) {
                getXmpp().addMessage(new SystemNotice(getXmpp(),
                        SystemNotice.SYS_NOTICE_ERROR, from, getError(errorNode)));
            }

            Contact c = getXmpp().getItemByUID(from);
            if (null == c) {
                return;
            }
            c.setOfflineStatus();
            c.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            return;
        }

        if (("subscribe").equals(type)) {
            if (isAutoGateContact(from)) {
                sendSubscribed(from);
                requestSubscribe(from);
            } else {
                getXmpp().addMessage(new SystemNotice(getXmpp(), SystemNotice.SYS_NOTICE_AUTHREQ, from, null));
            }
            Contact c = getXmpp().getItemByUID(from);
            autoRenameContact(c, x);
            autoMoveContact(c, x);
            return;
        }
        if (("subscribed").equals(type)) {
            if (!isAutoGateContact(from)) {
                getXmpp().setAuthResult(from, true);
            }
            autoRenameContact(getXmpp().getItemByUID(from), x);
            return;
        }
        if (("unsubscribed").equals(type)) {
            getXmpp().setAuthResult(from, false);
            return;
        }
        if (null == type) {
            type = x.getFirstNodeValue("show");
        }
        if (null == type) {
            type = "";
        }

        XmppContact contact = (XmppContact) getXmpp().getItemByUID(from);
        if (null == contact) {
            String fullJid = Jid.realJidToSawimJid(fromFull);
            contact = (XmppContact) getXmpp().getItemByUID(fullJid);
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
        String statusString = x.getFirstNodeValue(S_STATUS);
        if (Jid.isConference(from)) {
            XmlNode xMuc = x.getXNode("http://jabber.org/protocol/muc#user");
            int code = 0;
            if (null != xMuc) {
                code = Util.strToIntDef(xMuc.getFirstNodeAttribute(S_STATUS, S_CODE), 0);
            }
            XmlNode item = (null == xMuc) ? null : xMuc.getFirstNode(S_ITEM);
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
                } else if (S_NONE.equals(role)) {
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
            getXmpp().setConfContactStatus(conf, fromRes,
                    nativeStatus2StatusIndex(type), statusString, priority, priorityA, rangVoice);
            if (null != item) {
                XmppContact.SubContact sc = conf.getExistSubContact(fromRes);
                if (sc != null) {
                    String id = conf.getUserId() + "/" + fromRes;
                    requestVCard(id, newAvatarHash, sc.avatarHash);
                }
                String newNick = item.getAttribute(XmlNode.S_NICK);
                String realJid = item.getAttribute(XmlNode.S_JID);
                if (null != newNick) {
                    getXmpp().setConfContactStatus(conf, newNick,
                            nativeStatus2StatusIndex(""), "", priority, priorityA, rangVoice);
                    conf.nickChainged(getXmpp(), fromRes, newNick);
                } else {
                    StringBuilder s = new StringBuilder();
                    if (statusString != null) {
                        s.append('(').append(statusString).append(") ");
                    }
                    s.append(StringConvertor.notNull(reasone));
                    if (null != realJid) {
                        s.append(Jid.getBareJid(realJid)).append(" ");
                    }
                    conf.nickOnline(getXmpp(), fromRes, s.toString());
                }
                if (null != realJid) {
                    conf.setRealJid(fromRes, Jid.getBareJid(realJid));
                }
                contact.setClient(fromRes, x.getFirstNodeAttribute("c", S_NODE));
                if (303 == code) {
                    conf.addPresence(getXmpp(), fromRes, ": " + JLocale.getString(R.string.change_nick) + " " + StringConvertor.notNull(newNick));
                }
            } else {
                conf.nickOffline(getXmpp(), fromRes, code, StringConvertor.notNull(reasone), StringConvertor.notNull(statusString));
            }
            if (conf.getMyName().equals(fromRes)) {
                getXmpp().ui_changeContactStatus(conf);
            }
            updateConfPrivate(conf, fromRes);
            if (RosterHelper.getInstance().getUpdateChatListener() != null)
                RosterHelper.getInstance().getUpdateChatListener().updateMucList();
        } else {
            requestVCard(contact.getUserId(), newAvatarHash, contact.avatarHash);
            if (!("unavailable").equals(type)) {
                if ((XStatusInfo.XSTATUS_NONE == contact.getXStatusIndex())
                        || !Xmpp.xStatus.isPep(contact.getXStatusIndex())) {
                    XmlNode xNode = x.getXNode(S_FEATURE_XSTATUS);
                    String id = getXStatus(xNode);

                    String xtext = null;
                    if (null != id) {
                        xtext = xNode.getFirstNodeValue(S_TITLE);
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
                    setXStatusToIcqTransport((XmppServiceContact) contact);
                }
            }
            getXmpp().setContactStatus(contact, fromRes, nativeStatus2StatusIndex(type), statusString, priority);
            contact.updateMainStatus(getXmpp());
            if (contact.isOnline()) {
                contact.setClient(fromRes, x.getFirstNodeAttribute("c", S_NODE));
            }
            if (contact.getUserId().equals(contact.getName())) {
                getXmpp().renameContact(contact, getNickFromNode(x));
            }
            getXmpp().ui_changeContactStatus(contact);
        }
    }

    private String getNickFromNode(XmlNode x) {
        String name = x.getFirstNodeValueRecursive("nickname");
        return (null == name) ? x.getFirstNodeValue(S_NICK) : name;
    }

    private void autoRenameContact(Contact contact, XmlNode x) {
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

    private void autoMoveContact(Contact contact, XmlNode presence) {
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
        Group g = getXmpp().getOrCreateGroup(x.getFirstNodeValue(S_GROUP));
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

    private void parseEvent(XmlNode eventNode, String fullJid) {
        if (null == eventNode) {
            return;
        }
        XmppContact contact = (XmppContact) getXmpp().getItemByUID(Jid.getBareJid(fullJid));
        if (null == contact) {
            return;
        }
        XmlNode statusNode = eventNode.getFirstNode(S_ITEMS);
        String eventType = "";
        if (null != statusNode) {
            eventType = statusNode.getAttribute(S_NODE);
            int start = eventType.lastIndexOf('/');
            if (-1 != start) {
                eventType = eventType.substring(start + 1);
            }
            statusNode = statusNode.getFirstNode(S_ITEM);
        }
        if (null != statusNode) {
            statusNode = statusNode.childAt(0);
        }
        if (-1 == "|mood|activity|tune".indexOf(eventType)) {
            return;
        }
        if ((null == statusNode) || (0 == statusNode.childrenCount())) {
            if ((XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex())
                    && Xmpp.xStatus.isType(contact.getXStatusIndex(), eventType)) {
                contact.setXStatus("", "");
            }
            return;
        }
        String text = statusNode.getFirstNodeValue(S_TEXT);
        statusNode.removeNode(S_TEXT);
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

    private String getXStatus(XmlNode x) {
        return (null == x) ? null : (XmppXStatus.XSTATUS_START + x.getId());
    }

    private void parseMessageEvent(XmlNode messageEvent, String from) {
        if (null == messageEvent) {
            return;
        }
        if (messageEvent.contains("offline")) {
            setMessageSended(messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_SERVER);
            return;
        }
        if (messageEvent.contains("delivered")) {
            setMessageSended(messageEvent.getFirstNodeValue(XmlNode.S_ID),
                    PlainMessage.NOTIFY_FROM_CLIENT);
            return;
        }
        getXmpp().beginTyping(from, messageEvent.contains(S_COMPOSING));
    }

    private void parseChatState(XmlNode message, String from) {
        if (message.contains(S_ACTIVE)
                || message.contains("gone")
                || message.contains(S_PAUSED)
                || message.contains("inactive")) {
            getXmpp().beginTyping(from, false);
        } else if (message.contains(S_COMPOSING)) {
            getXmpp().beginTyping(from, true);
        }
    }

    private String getDate(XmlNode message) {
        XmlNode offline = message.getXNode("jabber:x:delay");
        if (null == offline) {
            offline = message.getFirstNode("delay");
        }
        return (null == offline) ? null : offline.getAttribute("stamp");
    }

    private void prepareFirstPrivateMessage(String jid) {
        final XmppServiceContact conf =
                (XmppServiceContact) getXmpp().getItemByUID(Jid.getBareJid(jid));
        if (null == conf) {
            return;
        }

        XmppContact.SubContact sub = conf.getExistSubContact(Jid.getResource(jid, ""));
        if (null == sub) {
            return;
        }
        if (XmppServiceContact.ROLE_MODERATOR == sub.priority) {
            getXmpp().addTempContact(getXmpp().createTempContact(jid));
        }
    }

    private void parseMessage(XmlNode msg) {
        msg.removeNode("html");

        String type = msg.getAttribute(S_TYPE);
        boolean isGroupchat = ("groupchat").equals(type);
        boolean isError = S_ERROR.equals(type);

        String fullJid = msg.getAttribute(S_FROM);
        boolean isConference = Jid.isConference(fullJid);
        if (!isGroupchat) {
            fullJid = Jid.realJidToSawimJid(fullJid);
        }
        String from = Jid.getBareJid(fullJid);
        if (from.equals(getXmpp().getUserId())) {
            XmlNode addresses = msg.getFirstNode("addresses");
            if (null != addresses) {
                String ofrom = null;
                while (0 < addresses.childrenCount()) {
                    XmlNode address = addresses.popChildNode();
                    if ("ofrom".equals(address.getAttribute(S_TYPE))) {
                        ofrom = address.getAttribute(XmlNode.S_JID);
                        break;
                    }
                }
                if (null != ofrom) {
                    fullJid = ofrom;
                    if (!isGroupchat) {
                        fullJid = Jid.realJidToSawimJid(fullJid);
                    }
                    isConference = Jid.isConference(fullJid);
                    from = Jid.getBareJid(fullJid);
                }
            }
        }

        if (isConference && !isGroupchat) {
            from = fullJid;
        }
        String fromRes = Jid.getResource(fullJid, null);
        String subject = msg.getFirstNodeValue(S_SUBJECT);
        String text = msg.getFirstNodeValue(S_BODY);
        if ((null != subject) && (null == text)) {
            text = "";
        }
        if ("jubo@nologin.ru".equals(from) && msg.contains("juick")) {
            parseBlogMessage("juick@juick.com", msg, text, "JuBo");
            return;
        }
        if (protocol.isBlogBot(from)) {
            parseBlogMessage(from, msg, text, null);
            return;
        }

        if (!isConference) {
            if (msg.contains("attention")) {
                type = S_CHAT;
                text = PlainMessage.CMD_WAKEUP;
                subject = null;
            }
        }

        if (!isConference || !isGroupchat) {
            parseChatState(msg, from);
        }

        if (null == text) {
            XmlNode received = msg.getFirstNode("received");
            if (null != received) {
                String id = received.getId();
                if (null == id) {
                    id = msg.getId();
                }
                setMessageSended(id, PlainMessage.NOTIFY_FROM_CLIENT);
                return;
            }
            if (!Jid.isConference(from) && !isError) {
                parseMessageEvent(msg.getXNode("jabber:x:event"), from);
                parseEvent(msg.getFirstNode("event"), fullJid);
            }
            return;
        }

        msg.removeNode(S_SUBJECT);
        msg.removeNode(S_BODY);
        if ((null != subject) && (-1 == text.indexOf(subject))) {
            text = subject + "\n\n" + text;
        }
        text = StringConvertor.trim(text);

        Message message = null;
        final String date = getDate(msg);
        final boolean isOnlineMessage = (null == date);
        long time = isOnlineMessage ? SawimApplication.getCurrentGmtTime() : Util.createGmtDate(date);
        final XmppContact c = (XmppContact) getXmpp().getItemByUID(from);
        if (msg.contains(S_ERROR)) {
            final String errorText = getError(msg.getFirstNode(S_ERROR));
            if (null != errorText) {
                text = errorText + "\n-------\n" + text;
            }

        } else {
            if ((null != c) && msg.contains("captcha")) {
                final XmppForm form = new XmppForm(XmppForm.TYPE_CAPTCHA,
                        getXmpp(), from);
                form.showCaptcha(msg);
                return;
            }

            final XmlNode oobNode = msg.getXNode("jabber:x:oob");
            if (null != oobNode) {
                String url = oobNode.getFirstNodeValue(S_URL);
                if (null != url) {
                    text += "\n\n" + url;
                    String desc = oobNode.getFirstNodeValue(S_DESC);
                    if (null != desc) {
                        text += "\n" + desc;
                    }
                    msg.removeNode(S_URL);
                    msg.removeNode(S_DESC);
                }
            }

            if (!isGroupchat && msg.contains("request") && (null != msg.getId())) {
                putPacketIntoQueue("<message to='" + Util.xmlEscape(fullJid)
                        + "' id='" + Util.xmlEscape(msg.getId())
                        + "'><received xmlns='urn:xmpp:receipts' id='"
                        + Util.xmlEscape(msg.getId()) + "'/></message>");
            }

            if (c instanceof XmppServiceContact) {
                XmppServiceContact conference = (XmppServiceContact) c;
                isConference = conference.isConference();
                XmlNode xMuc = msg.getXNode("http://jabber.org/protocol/muc#user");
                if (null != xMuc) {
                    int code = Util.strToIntDef(xMuc.getFirstNodeAttribute(S_STATUS, S_CODE), 0);
                    if (code == 100) {
                        return;
                    }
                }
                if (null != subject && isConference && isGroupchat) {
                    String prevSubject = StringConvertor.notNull(conference.getSubject());
                    conference.setSubject(subject);
                    getXmpp().ui_changeContactStatus(conference);
                    fromRes = null;
                    boolean isOldSubject = prevSubject.equals(subject);
                    if (isOldSubject) {
                        return;
                    } else if (!StringConvertor.isEmpty(prevSubject) && !isOldSubject) {
                        message = new SystemNotice(getXmpp(), SystemNotice.SYS_NOTICE_CUSTOM, from, JLocale.getString(R.string.new_subject), text);
                    }
                }
            }
        }
        if (StringConvertor.isEmpty(text)) {
            return;
        }
        if (subject == null) {
            message = new PlainMessage(from, getXmpp(), time, text, !isOnlineMessage);
        } else if (!isGroupchat) {
            message = new SystemNotice(getXmpp(), SystemNotice.SYS_NOTICE_MESSAGE, from, text);
        }
        if (null == c) {
            if (isConference && !isGroupchat) {
                prepareFirstPrivateMessage(from);
            }
        } else {
            if (isConference) {
                final XmppServiceContact conf = (XmppServiceContact) c;
                if (isGroupchat && (null != fromRes)) {
                    if (isOnlineMessage && fromRes.equals(conf.getMyName())) {
                        if (isMessageExist(msg.getId())) {
                            setMessageSended(msg.getId(),
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
        if (subject == null && isConference && !isOnlineMessage) {
            if (HistoryStorage.hasLastMessage(getXmpp().getChat(c), message)) {
                return;
            }
        }
        if (message != null)
            getXmpp().addMessage(message, S_HEADLINE.equals(type));
    }

    private void parseBlogMessage(String to, XmlNode msg, String text, String botNick) {
        text = StringConvertor.notNull(text);
        String userNick = getNickFromNode(msg);
        if (null == userNick) {
            userNick = msg.getFirstNodeAttribute("juick", "uname");
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
        long time = (null == date) ? SawimApplication.getCurrentGmtTime() : Util.createGmtDate(date);
        PlainMessage message = new PlainMessage(to, getXmpp(), time, text, false);
        if (null != nick) {
            message.setName(('@' == nick.charAt(0)) ? nick.substring(1) : nick);
        }
        getXmpp().addMessage(message, true);
    }

    String getError(XmlNode errorNode) {
        if (null == errorNode) {
            return S_ERROR;
        }
        String errorText = errorNode.getFirstNodeValue(S_TEXT);
        if (null == errorText) {
            errorText = errorNode.value;
        }
        if (null == errorText) {
            errorText = "error " + errorNode.getAttribute(S_CODE);
            if (null != errorNode.childAt(0)) {
                errorText += ": " + errorNode.childAt(0).name;
            }
        }
        return errorText;
    }

    private boolean isMechanism(XmlNode list, String myMechanism) {
        for (int i = 0; i < list.childrenCount(); ++i) {
            XmlNode mechanism = list.childAt(i);
            if (mechanism.is("mechanism") && myMechanism.equals(mechanism.value)) {
                return true;
            }
        }
        return false;
    }

    private void parseStreamFeatures(XmlNode x) throws SawimException {
        XmlNode x2;
        if (0 == x.childrenCount()) {
            nonSaslLogin();
            return;
        }

        x2 = x.getFirstNode("starttls");
        if (null != x2) {
            DebugLog.println("starttls");
            sendRequest("<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>");
            return;
        }
        x2 = x.getFirstNode("compression");
        if ((null != x2) && "zlib".equals(x2.getFirstNodeValue("method"))) {
            sendRequest("<compress xmlns='http://jabber.org/protocol/compress'><method>zlib</method></compress>");
            return;
        }

        x2 = x.getFirstNode("mechanisms");
        if ((null != x2) && x2.contains("mechanism")) {

            String auth = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' ";

            String googleToken = null;

            if (isMechanism(x2, "X-GOOGLE-TOKEN")) {

                DebugLog.systemPrintln("[INFO-JABBER] Using X-GOOGLE-TOKEN");

                isGTalk_ = true;
                googleToken = getGoogleToken(getXmpp().getUserId(), getXmpp().getPassword());
                if (null == googleToken) {
                    throw new SawimException(111, 1);
                }
            }

            if (isMechanism(x2, "DIGEST-MD5")) {

                DebugLog.systemPrintln("[INFO-JABBER] Using DIGEST-MD5");

                auth += "mechanism='DIGEST-MD5'/>";

            } else if (isMechanism(x2, "SCRAM-SHA-1")) {
                auth += "mechanism='SCRAM-SHA-1'>";
                scramSHA1 = new SASL_ScramSha1();
                auth += Util.xmlEscape(scramSHA1.init(protocol.getUserId(), protocol.getPassword()));
                auth += "</auth>";

            } else if (null != googleToken) {
                auth += "mechanism='X-GOOGLE-TOKEN'>" + googleToken + "</auth>";
                /* PLAIN authentication */
            } else {
                boolean canUsePlain = true;
                canUsePlain = socket.isSecured();
                if (canUsePlain && isMechanism(x2, "PLAIN")) {
                    DebugLog.systemPrintln("[INFO-JABBER] Using PLAIN");
                    auth += "mechanism='PLAIN'>";
                    Util data = new Util();
                    data.writeUtf8String(getXmpp().getUserId());
                    data.writeByte(0);
                    data.writeUtf8String(Jid.getNick(getXmpp().getUserId()));
                    data.writeByte(0);
                    data.writeUtf8String(getXmpp().getPassword());
                    auth += Util.base64encode(data.toByteArray());
                    auth += "</auth>";

                } else if (canUsePlain && isGTalk_) {
                    nonSaslLogin();
                    return;
                } else {
                    setAuthStatus(false);
                    return;
                }
            }
            sendRequest(auth);
            return;
        }
        if (x.contains("bind")) {
            DebugLog.systemPrintln("[INFO-JABBER] Send bind request");
            sendRequest("<iq type='set' id='bind'>"
                    + "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>"
                    + "<resource>" + Util.xmlEscape(resource) + "</resource>"
                    + "</bind>"
                    + "</iq>");
            return;
        }
        x2 = x.getFirstNode("auth", "http://jabber.org/features/iq-auth");
        if (null != x2) {
            nonSaslLogin();
            return;
        }
    }

    private void parseChallenge(XmlNode x) throws SawimException {

        DebugLog.systemPrintln("[INFO-JABBER] Received challenge");

        String resp = "<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'";
        String challenge = Util.decodeBase64(x.value);

        if (null != scramSHA1) {
            resp += ">" + scramSHA1.response(challenge) + "</response>";

        } else {
            int nonceIndex = challenge.indexOf("nonce=");
            if (nonceIndex >= 0) {
                nonceIndex += 7;
                String nonce = challenge.substring(nonceIndex, challenge.indexOf('\"', nonceIndex));
                String cnonce = "123456789abcd";

                resp += ">";
                resp += responseMd5Digest(
                        Jid.getNick(getXmpp().getUserId()),
                        getXmpp().getPassword(),
                        domain_,
                        "xmpp/" + domain_,
                        nonce,
                        cnonce);
                resp += "</response>";
            } else {
                resp += "/>";
            }
        }

        sendRequest(resp);
    }

    private String responseMd5Digest(String user, String pass,
                                     String realm, String digestUri, String nonce, String cnonce) {
        MD5 hUserRealmPass = new MD5();
        hUserRealmPass.init();
        hUserRealmPass.updateASCII(user);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(realm);
        hUserRealmPass.update((byte) ':');
        hUserRealmPass.updateASCII(pass);

        MD5 hA1 = new MD5();
        hA1.init();
        hA1.update(hUserRealmPass.getDigestBits());
        hA1.update((byte) ':');
        hA1.updateASCII(nonce);
        hA1.update((byte) ':');
        hA1.updateASCII(cnonce);

        MD5 hA2 = new MD5();
        hA2.init();
        hA2.updateASCII("AUTHENTICATE:");
        hA2.updateASCII(digestUri);

        MD5 hResp = new MD5();
        hResp.init();
        hResp.updateASCII(hA1.getDigestHex());
        hResp.update((byte) ':');
        hResp.updateASCII(nonce);
        hResp.updateASCII(":00000001:");
        hResp.updateASCII(cnonce);
        hResp.updateASCII(":auth:");
        hResp.updateASCII(hA2.getDigestHex());

        String quote = "\"";
        return Util.base64encode(StringConvertor.stringToByteArrayUtf8(
                new StringBuffer().append("username=\"").append(user).append("\",realm=\"").append(realm)
                        .append("\",nonce=\"").append(nonce).append("\",cnonce=\"").append(cnonce)
                        .append("\",nc=00000001,digest-uri=\"").append(digestUri)
                        .append("\",qop=auth,response=").append(quote).append(hResp.getDigestHex())
                        .append(quote).append(",charset=utf-8").toString()
        ));
    }


    /**
     * Generates X-GOOGLE-TOKEN response by communication with
     * http://www.google.com
     * (From mGTalk project)
     *
     * @param jid
     * @param passwd
     * @return
     */
    private String getGoogleToken(String jid, String passwd) {
        try {
            String escapedJid = Util.urlEscape(jid);
            String first = "Email=" + escapedJid
                    + "&Passwd=" + Util.urlEscape(passwd)
                    + "&PersistentCookie=false&source=googletalk";
            HttpURLConnection c = (HttpURLConnection) new URL("https://www.google.com:443/accounts/ClientAuth?" + first).openConnection();

            DebugLog.systemPrintln("[INFO-JABBER] Connecting to www.google.com");

            if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = c.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String str = reader.readLine();
                if (str.startsWith("SID=")) {
                    String SID = str.substring(4, str.length());
                    str = reader.readLine();
                    String LSID = str.substring(5, str.length());
                    first = "SID=" + SID + "&LSID=" + LSID + "&service=mail&Session=true";
                    c.disconnect();
                    is.close();
                    c = (HttpURLConnection) new URL("https://www.google.com:443/accounts/IssueAuthToken?" + first).openConnection();

                    DebugLog.systemPrintln("[INFO-JABBER] Next www.google.com connection");
                    is = c.getInputStream();
                    if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader2 = new BufferedReader(
                                new InputStreamReader(is, "UTF-8"));

                        str = reader2.readLine();
                        c.disconnect();
                        is.close();
                        Util data = new Util();
                        data.writeByte(0);
                        data.writeUtf8String(Jid.getNick(jid));
                        data.writeByte(0);
                        data.writeUtf8String(str);
                        String token = Util.base64encode(data.toByteArray());
                        return token;
                    }
                }
            }
        } catch (Exception ex) {
            DebugLog.systemPrintln("EX: " + ex.toString());
        }
        return null;
    }

    private void updateConfPrivate(XmppServiceContact conf, String resource) {
        String privateJid = Jid.realJidToSawimJid(conf.getUserId() + '/' + resource);
        Contact privateContact = getXmpp().getItemByUID(privateJid);
        if (null != privateContact) {
            ((XmppServiceContact) privateContact).setPrivateContactStatus(conf);
            getXmpp().ui_changeContactStatus(privateContact);
        }
    }

    public void updateContacts(Vector contacts) {
        StringBuilder xml = new StringBuilder();

        int itemCount = 0;
        xml.append("<iq type='set' id='").append(generateId())
                .append("'><query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); ++i) {
            XmppContact contact = (XmppContact) contacts.elementAt(i);
            if (Jid.isConference(contact.getUserId())) {
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
        StringBuffer xml = new StringBuffer();
        Xmpp j = (Xmpp) protocol;
        Vector subscribes = new Vector();
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
                contact = (XmppContact) j.createTempContact(jid);
                contact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            }
            String group = item.getFirstNodeValue(S_GROUP);
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
                    subscribes.addElement(contact);
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
            xml = new StringBuffer();
            for (int i = 0; i < subscribes.size(); ++i) {
                xml.append("<presence type='subscribe' to='")
                        .append(Util.xmlEscape(((Contact) subscribes.elementAt(i)).getUserId()))
                        .append("'/>");
            }
            if (0 < xml.length()) {
                putPacketIntoQueue(xml.toString());
            }
        }
    }

    public String getConferenceStorage() {
        StringBuilder xml = new StringBuilder();
        Vector contacts = getXmpp().getContactItems();
        xml.append("<storage xmlns='storage:bookmarks'>");
        for (int i = 0; i < contacts.size(); ++i) {
            XmppContact contact = (XmppContact) contacts.elementAt(i);
            if (!contact.isConference() || contact.isTemp()) {
                continue;
            }
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);

            XmppServiceContact conf = (XmppServiceContact) contact;
            xml.append("<conference autojoin='");
            xml.append(conf.isAutoJoin() ? S_TRUE : S_FALSE);
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

    public void saveConferences() {
        StringBuilder xml = new StringBuilder();

        String storage = getConferenceStorage();
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

        putPacketIntoQueue(xml.toString());
    }

    public void removeGateContacts(String gate) {
        if (StringConvertor.isEmpty(gate)) {
            return;
        }
        gate = "@" + gate;
        Vector contacts = getXmpp().getContactItems();
        StringBuilder xml = new StringBuilder();

        xml.append("<iq type='set' id='").append(generateId())
                .append("'><query xmlns='jabber:iq:roster'>");
        for (int i = 0; i < contacts.size(); ++i) {
            XmppContact contact = (XmppContact) contacts.elementAt(i);
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
        if (contact.isConference() && Jid.isConference(contact.getUserId()) && !isGTalk_) {
            contact.setTempFlag(false);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, false);
            String groupName = contact.getDefaultGroupName();
            Group group = getXmpp().getOrCreateGroup(groupName);
            contact.setGroup(group);
            saveConferences();
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

    public void removeContact(String jid) {
        if (Jid.isConference(jid) && !isGTalk_) {
            saveConferences();
        }
        putPacketIntoQueue("<iq type='set' id='" + generateId()
                + "'><query xmlns='jabber:iq:roster'>"
                + "<item subscription='remove' jid='" + Util.xmlEscape(jid) + "'/>"
                + "</query></iq>");
    }

    public void getBookmarks() {
        putPacketIntoQueue("<iq type='get' id='0'><query xmlns='jabber:iq:private'><storage xmlns='storage:bookmarks'/></query></iq>");
        if (xep0048) {
            putPacketIntoQueue("<iq type='get' id='1'><pubsub xmlns='http://jabber.org/protocol/pubsub'><items node='storage:bookmarks'/></pubsub></iq>");
        }
    }

    private String getOpenStreamXml(String server) {
        return "<?xml version='1.0'?>"
                + "<stream:stream xmlns='jabber:client' "
                + "xmlns:stream='http:/" + "/etherx.jabber.org/streams' "
                + "version='1.0' "
                + "to='" + server + "'"
                + " xml:lang='" + JLocale.getSystemLanguage() + "'>";
    }

    private void getVCard(String jid) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(generateId(S_VCARD)) + "'>"
                + "<vCard xmlns='vcard-temp' version='2.0' prodid='-/"
                + "/HandGen/" + "/NONSGML vGen v1.0/" + "/EN'/>"
                + "</iq>");
    }

    private void sendMessage(String to, String msg, String type, boolean notify, String id) {
        to = Jid.SawimJidToRealJid(to);
        boolean buzz = msg.startsWith(PlainMessage.CMD_WAKEUP) && S_CHAT.equals(type);
        if (buzz) {
            type = S_HEADLINE;
            notify = false;
            if (!getXmpp().isContactOverGate(to)) {
                msg = msg.substring(PlainMessage.CMD_WAKEUP.length()).trim();
                if (StringConvertor.isEmpty(msg)) {
                    msg = "/me " + JLocale.getString(R.string.wake_you_up);
                }
            }
        }

        String chatState = "";
        if (S_CHAT.equals(type)) {
            chatState = getChatStateTag(S_ACTIVE);
        }
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to) + "'"
                + " type='" + type + "' id='" + Util.xmlEscape(id) + "'>"
                + (isGTalk_ ? "<nos:x value='disabled' xmlns:nos='google:nosave'/>" : "")
                + (buzz ? "<attention xmlns='urn:xmpp:attention:0'/>" : "")
                + "<body>" + Util.xmlEscape(msg) + "</body>"
                + (notify ? "<request xmlns='urn:xmpp:receipts'/><x xmlns='jabber:x:event'><offline/><delivered/></x>" : "")
                + chatState
                + "</message>");
    }

    void sendMessage(String to, String msg) {
        String type = S_CHAT;
        if (Jid.isConference(to) && (-1 == to.indexOf('/'))) {
            type = S_GROUPCHAT;
        }
        sendMessage(to, msg, type, false, generateId());
    }

    void sendMessage(PlainMessage message) {
        String to = message.getRcvrUin();
        XmppContact toContact = (XmppContact) protocol.getItemByUID(to);
        if (null != toContact) {
            to = toContact.getReciverJid();
        }
        String type = S_CHAT;
        if (Jid.isConference(to) && (-1 == to.indexOf('/'))) {
            type = S_GROUPCHAT;
        }
        message.setMessageId(Util.uniqueValue());
        boolean notify = true;

        sendMessage(to, message.getText(), type, S_CHAT.equals(type),
                String.valueOf(message.getMessageId()));

        if (notify) {
            addMessage(message);
        }
    }

    private String getChatStateTag(String state) {
        return "<" + state + " xmlns='http://jabber.org/protocol/chatstates'/>";
    }

    void sendTypingNotify(String to, boolean composing) {
        String tag = getChatStateTag(composing ? S_COMPOSING : S_ACTIVE/*S_PAUSED*/);
        putPacketIntoQueue("<message to='" + Util.xmlEscape(to)
                + "' id='0'>" + tag + "</message>");
    }

    void sendPresence(XmppServiceContact conf) {
        String to = conf.getUserId();
        String xml = "";
        if (conf.isConference()) {
            to += "/" + conf.getMyName();
            String xNode = "";
            String password = conf.getPassword();
            if (!StringConvertor.isEmpty(password)) {
                xNode += "<password>" + Util.xmlEscape(password) + "</password>";
            }
            HistoryStorage historyStorage = getXmpp().getChat(conf).getHistory();
            long time = historyStorage.getLastMessageTime();
            if (0 != time)
                xNode += "<history maxstanzas='"
                        + MESSAGE_COUNT_AFTER_CONNECT
                        + "' seconds='" + (SawimApplication.getCurrentGmtTime() - time) + "'/>";

            if (!StringConvertor.isEmpty(xNode)) {
                xml += "<x xmlns='http://jabber.org/protocol/muc'>" + xNode + "</x>";
            }
        }
        if (getXmpp().getProfile() != null) {
            String status = getNativeStatus(getXmpp().getProfile().statusIndex);
            if (!StringConvertor.isEmpty(status)) {
                xml += "<show>" + status + "</show>";
            }
        }
        String xstatusTitle = null;
        if (getXmpp().getProfile() != null) {
            xstatusTitle = getXmpp().getProfile().xstatusTitle;
            String descr = getXmpp().getProfile().xstatusDescription;
            if (!StringConvertor.isEmpty(descr)) {
                xstatusTitle += " " + descr;
            }
        }
        xml += (StringConvertor.isEmpty(xstatusTitle) ? "" : "<status>" + Util.xmlEscape(xstatusTitle) + "</status>");

        xml = "<presence to='" + Util.xmlEscape(to) + "'>" + xml + getCaps();
        if (myAvatarHash != null) {
            xml += "<x xmlns='vcard-temp:x:update'><photo>" + myAvatarHash + "</photo></x>";
        }
        xml += "</presence>";
        putPacketIntoQueue(xml);
    }

    void sendPresenceUnavailable(String to) {
        putPacketIntoQueue("<presence type='unavailable' to='" + Util.xmlEscape(to)
                + "'><status>I'll be back</status></presence>");
    }

    void setStatus(byte statusIndex, String msg, int priority) {
        setStatus(getNativeStatus(statusIndex), msg, priority);
    }

    void setStatus(String status, String msg, int priority) {
        String xXml = getQipXStatus();
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
                + getCaps();
        if (myAvatarHash != null) {
            xml += "<x xmlns='vcard-temp:x:update'><photo>" + myAvatarHash + "</photo></x>";
        }
        xml += xXml + "</presence>";
        putPacketIntoQueue(xml);
        //if (!AutoAbsence.getInstance().isChangeStatus())
        setConferencesXStatus(status, msg, priority);
    }

    void setConferencesXStatus(String status, String msg, int priority) {
        String xml;
        Vector contacts = getXmpp().getContactItems();
        for (int i = 0; i < contacts.size(); ++i) {
            XmppContact contact = (XmppContact) contacts.elementAt(i);
            if ((contact).isConference() && contact.isOnline()) {
                if (0 <= priority) {
                    xml = "<presence to='" + Util.xmlEscape(contact.getUserId()) + "'>";
                    xml += (StringConvertor.isEmpty(status) ? "" : "<show>" + status + "</show>");
                    xml += (StringConvertor.isEmpty(msg) ? "" : "<status>" + Util.xmlEscape(msg) + "</status>");
                    xml += getCaps();
                    if (myAvatarHash != null) {
                        xml += "<x xmlns='vcard-temp:x:update'><photo>" + myAvatarHash + "</photo></x>";
                    }
                    xml += "</presence>";
                    putPacketIntoQueue(xml);

                }
            }
        }
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

    private void requestIq(String jid, String xmlns, String id) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jid)
                + "' id='" + Util.xmlEscape(id) + "'><query xmlns='" + xmlns + "'/></iq>");
    }

    private void requestIq(String jid, String xmlns) {
        requestIq(jid, xmlns, generateId());
    }

    public void requestClientVersion(String jid) {
        requestIq(jid, "jabber:iq:version");
    }

    public void requestConferenceInfo(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#info");
    }

    public void requestConferenceUsers(String jid) {
        requestIq(jid, "http://jabber.org/protocol/disco#items");
        serviceDiscovery = getXmpp().getServiceDiscovery();
    }

    public void requestDiscoItems(String server) {
        requestIq(server, "http://jabber.org/protocol/disco#items");
        serviceDiscovery = getXmpp().getServiceDiscovery();
    }

    public void requestAffiliationListConf(String jidConference, String affiliation) {
        putPacketIntoQueue("<iq type='get' to='" + Util.xmlEscape(jidConference)
                + "' id='" + Util.xmlEscape(affiliation)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation) + "'/></query></iq>");
        affListConf = getXmpp().getAffiliationListConf();
    }

    public void setAffiliationListConf(String jidConference, String jidItem, String setAffiliation, String setReason) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jidConference)
                + "' id='admin_modify'><query xmlns='http://jabber.org/protocol/muc#admin'><item jid='"
                + Util.xmlEscape(jidItem) + "' affiliation='" + Util.xmlEscape(setAffiliation)
                + "'><reason>" + Util.xmlEscape(setReason) + "</reason></item></query></iq>");
        affListConf = getXmpp().getAffiliationListConf();
    }

    public void sendInvite(String jidMessTo, String jidInviteTo, String setReason) {
        putPacketIntoQueue("<message to='" + Util.xmlEscape(jidMessTo)
                + "'><x xmlns='http://jabber.org/protocol/muc#user'><invite to='"
                + Util.xmlEscape(jidInviteTo) + "'><reason>" + Util.xmlEscape(setReason) + "</reason></invite></x></message>");
    }

    void requestRawXml(String xml) {
        putPacketIntoQueue(xml);
    }

    public void setMucRole(String jid, String nick, String role) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'/></query></iq>");
    }

    public void setMucAffiliation(String jid, String userJid, String affiliation) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'/></query></iq>");
    }

    public void setMucRoleR(String jid, String nick, String role, String setReason) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='itemmuc'><query xmlns='http://jabber.org/protocol/muc#admin'><item nick='"
                + Util.xmlEscape(nick)
                + "' role='" + Util.xmlEscape(role)
                + "'><reason>" + Util.xmlEscape(setReason) + "</reason></item>"
                + "</query></iq>");
    }

    public void setMucAffiliationR(String jid, String userJid, String affiliation, String setReason) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='itemmuc'><query xmlns='http://jabber.org/protocol/muc#admin'><item affiliation='"
                + Util.xmlEscape(affiliation)
                + "' jid='" + Util.xmlEscape(userJid)
                + "'><reason>" + Util.xmlEscape(setReason) + "</reason></item>"
                + "</query></iq>");
    }

    UserInfo getUserInfo(Contact contact) {
        singleUserInfo = new UserInfo(getXmpp(), contact.getUserId());
        getVCard(contact.getUserId());
        return singleUserInfo;
    }

    void register2(XmppForm form, String rawXml, String jid) {
        xmppForm = form;
        autoSubscribeDomain = jid;
        requestRawXml(rawXml);
    }

    private boolean isAutoGateContact(String jid) {
        return !StringConvertor.isEmpty(autoSubscribeDomain)
                && (jid.equals(autoSubscribeDomain) || jid.endsWith('@' + autoSubscribeDomain));
    }

    XmppForm register(String jid) {
        xmppForm = new XmppForm(XmppForm.TYPE_REGISTER, getXmpp(), jid);
        requestIq(jid, "jabber:iq:register", xmppForm.getId());
        return xmppForm;
    }

    void unregister(String jid) {
        putPacketIntoQueue("<iq type='set' to='" + Util.xmlEscape(jid)
                + "' id='unreg1'><query xmlns='jabber:iq:register'><remove/></query></iq>");
    }

    XmppForm requestOwnerForm(String jid) {
        xmppForm = new XmppForm(XmppForm.TYPE_OWNER, getXmpp(), jid);
        requestIq(jid, "http://jabber.org/protocol/muc#owner", xmppForm.getId());
        return xmppForm;
    }

    void showContactSeen(String jid) {
        putPacketIntoQueue("<iq to='" + Util.xmlEscape(jid) + "' type='get' id='last_seen'><query xmlns='jabber:iq:last'/></iq>");
    }

    private void sendXStatus(String xstatus, String text) {
        String[] path = Util.explode(Util.xmlEscape(xstatus), ':');
        StringBuilder sb = new StringBuilder();
        String typeUrl = "http://jabber.org/protocol/" + path[0];

        sb.append("<iq type='set' id='").append(generateId());
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
        putPacketIntoQueue(sb.toString());
    }

    private String getQipXStatus() {
        byte x = getXmpp().getProfile().xstatusIndex;
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
                + Util.xmlEscape(getXmpp().getProfile().xstatusTitle)
                + "</title></x>";
    }

    private static final String S_FEATURE_XSTATUS = "http://qip.ru/x-status";

    void setXStatus() {
        String xstatusCode = Xmpp.xStatus.getCode(getXmpp().getProfile().xstatusIndex);
        if (null == xstatusCode) {
            return;
        }

        setXStatusToIcqTransports();

        if (xstatusCode.startsWith(XmppXStatus.XSTATUS_START)) {
            setStatus(getNativeStatus(getXmpp().getProfile().statusIndex),
                    getXmpp().getProfile().statusMessage, Xmpp.PRIORITY);
            return;
        }
        final String mood = "mood";
        final String activity = "activity";
        if (!xstatusCode.startsWith(mood)) {
            sendXStatus(mood, null);
        }
        if (!xstatusCode.startsWith(activity)) {
            sendXStatus(activity, null);
        }
        if (xstatusCode.startsWith(mood) || xstatusCode.startsWith(activity)) {
            sendXStatus(xstatusCode, getXmpp().getProfile().xstatusTitle);
        }
    }

    private void setXStatusToIcqTransport(XmppServiceContact gate) {
        String xstatus = Xmpp.xStatus.getIcqXStatus(getXmpp().getProfile().xstatusIndex);
        if (null == xstatus) {
            return;
        }
        String desc = "None".equals(xstatus) ? null : getXmpp().getProfile().xstatusTitle;
        desc = StringConvertor.notNull(desc);
        if (gate.isOnline() && Jid.isPyIcqGate(gate.getUserId())) {
            String out = "<iq type='set' id='" + generateId() + "' to='"
                    + Util.xmlEscape(gate.getUserId())
                    + "'><command xmlns='http://jabber.org/protocol/commands' node='setxstatus' action='complete'><x xmlns='jabber:x:data' type='submit'><field var='xstatus_desc'><value>"
                    + Util.xmlEscape(desc)
                    + "</value></field><field var='xstatus_name'><value>"
                    + Util.xmlEscape(xstatus)
                    + "</value></field></x></command></iq>";
            putPacketIntoQueue(out);
        }
    }

    private void setXStatusToIcqTransports() {
        String x = Xmpp.xStatus.getIcqXStatus(getXmpp().getProfile().xstatusIndex);
        if (null == x) {
            return;
        }
        Vector contacts = getXmpp().getContactItems();
        for (int i = contacts.size() - 1; i >= 0; --i) {
            XmppContact c = (XmppContact) contacts.elementAt(i);
            if (c.isOnline() && Jid.isPyIcqGate(c.getUserId())) {
                setXStatusToIcqTransport((XmppServiceContact) c);
            }
        }
    }

    private String getVerHash(Vector features) {
        StringBuilder sb = new StringBuilder();
        sb.append("client/phone/" + "/" + SawimApplication.NAME + "<");
        for (int i = 0; i < features.size(); ++i) {
            sb.append(features.elementAt(i)).append('<');
        }
        return Util.base64encode(MD5.calculate(StringConvertor.stringToByteArrayUtf8(sb.toString())));
    }

    private String getFeatures(Vector features) {
        StringBuilder sb = new StringBuilder();
        sb.append("<identity category='client' type='phone' name='" + SawimApplication.NAME + "'/>");
        for (int i = 0; i < features.size(); ++i) {
            sb.append("<feature var='").append(features.elementAt(i)).append("'/>");
        }
        return sb.toString();
    }

    private void initFeatures() {
        Vector features = new Vector();
        features.addElement("bugs");

        features.addElement("http://jabber.org/protocol/activity");
        features.addElement("http://jabber.org/protocol/activity+notify");

        features.addElement("http://jabber.org/protocol/chatstates");

        features.addElement("http://jabber.org/protocol/disco#info");

        features.addElement("http://jabber.org/protocol/mood");
        features.addElement("http://jabber.org/protocol/mood+notify");

        features.addElement("http://jabber.org/protocol/rosterx");

        features.addElement(S_FEATURE_XSTATUS);

        features.addElement("jabber:iq:last");
        features.addElement("jabber:iq:version");
        features.addElement("urn:xmpp:attention:0");
        features.addElement("urn:xmpp:time");

        verHash = getVerHash(features);
        featureList = getFeatures(features);
    }

    private boolean isMessageExist(String id) {
        return isMessageExist(Util.strToIntDef(id, -1));
    }

    private void setMessageSended(String id, int state) {
        markMessageSended(Util.strToIntDef(id, -1), state);
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
        if (IQ_TYPE_RESULT != type) {

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
}
