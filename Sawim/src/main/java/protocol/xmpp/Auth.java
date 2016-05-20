package protocol.xmpp;

import android.preference.PreferenceManager;

import protocol.StatusInfo;
import protocol.net.ClientConnection;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.gcm.Preferences;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.crypto.MD5;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created by gerc on 02.01.2016.
 */
public class Auth {

    public boolean authorized_ = false;
    private boolean rebindSupported = false;
    private SASL_ScramSha1 scramSHA1;

    public void parseAuth(XmppConnection connection, XmlNode x) throws SawimException {
        if ((null == x) || !x.is("stream:features")) {
            nonSaslLogin(connection);
        } else {
            loginParse(connection, x);
        }
    }

    public void loginParse(XmppConnection connection, XmlNode x) throws SawimException {
        if (x.is("stream:features")) {
            parseStreamFeatures(connection, x);
            return;
        } else if (x.is("compressed")) {
            setStreamCompression(connection);
            return;
        } else if (x.is("proceed")) {
            setStreamTls(connection);
            return;
        } else if (x.is("challenge")) {
            parseChallenge(connection, x);
            return;
        } else if (x.is("failure")) {
            DebugLog.systemPrintln("[INFO-JABBER] Failed");
            setAuthStatus(connection, false);
            return;

        } else if (x.is("success")) {
            if (null != scramSHA1) {
                if (!scramSHA1.success(new String(Util.base64decode(x.value)))) {
                    DebugLog.systemPrintln("Server answer not valid");
                    setAuthStatus(connection, false);
                    return;
                }
                scramSHA1 = null;
            }
            DebugLog.systemPrintln("[INFO-JABBER] Auth success");
            DebugLog.systemPrintln("auth " + authorized_);

            connection.sendRequest(connection.getOpenStreamXml(connection.domain_));
            XmlNode streamNode = connection.readXmlNode(true);
            if (rebindSupported) {
                saveSessionData(connection, streamNode);
            }

            return;
        } else if (x.is("iq")) {
            XmlNode iqQuery = x.childAt(0);
            String id = x.getId();
            if ("sess".equals(id)) {
                setAuthStatus(connection, true);
                return;
            }
            if (null == iqQuery) {
                return;
            }
            String queryName = iqQuery.name;
            if (XmlConstants.IQ_TYPE_ERROR == connection.getIqType(x)) {
                if ("jabber:iq:auth".equals(iqQuery.getXmlns())) {
                    setAuthStatus(connection, false);
                }
            }
            if ("bind".equals(queryName)) {
                DebugLog.systemPrintln("[INFO-JABBER] Send open session request");
                connection.fullJid_ = iqQuery.getFirstNodeValue(XmlNode.S_JID);
                connection.sendRequest("<iq type='set' id='sess'>"
                        + "<session xmlns='urn:ietf:params:xml:ns:xmpp-session'/>"
                        + "</iq>");
                return;
            }
        }
        connection.parse(x);
    }

    public void parseStreamFeatures(XmppConnection connection, XmlNode x) throws SawimException {
        XmlNode x2;
        if (0 == x.childrenCount()) {
            nonSaslLogin(connection);
            return;
        }

        x2 = x.getFirstNode("starttls");
        if (null != x2) {
            DebugLog.println("starttls");
            connection.sendRequest("<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>");
            return;
        }
        x2 = x.getFirstNode("compression");
        if ((null != x2) && "zlib".equals(x2.getFirstNodeValue("method"))) {
            connection.sendRequest("<compress xmlns='http://jabber.org/protocol/compress'><method>zlib</method></compress>");
            return;
        }

        x2 = x.getFirstNode("push", "p1:push");
        if (x2 != null) {
            rebindSupported = true;
            setupSessionKeep(connection);
        }

        x2 = x.getFirstNode("rebind", "p1:rebind");
        if (x2 != null) {
            XmppSession xmppSession = connection.getXmppSession();
            if (rebindSupported && !xmppSession.isEmpty()) {
                if (rebindSession(connection)) {
                    return;
                }
            }
        }

        x2 = x.getFirstNode("mechanisms");
        if ((null != x2) && x2.contains("mechanism")) {
            String auth = "<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' ";
            Xmpp protocol = connection.getXmpp();
            if (isMechanism(x2, "DIGEST-MD5")) {
                DebugLog.systemPrintln("[INFO-JABBER] Using DIGEST-MD5");
                auth += "mechanism='DIGEST-MD5'/>";
            } else if (isMechanism(x2, "SCRAM-SHA-1")) {
                auth += "mechanism='SCRAM-SHA-1'>";
                scramSHA1 = new SASL_ScramSha1();
                auth += Util.xmlEscape(scramSHA1.init(protocol.getUserId(), protocol.getPassword()));
                auth += "</auth>";
            } else {
                boolean canUsePlain = true;
                canUsePlain = connection.socket.isSecured();
                if (canUsePlain && isMechanism(x2, "PLAIN")) {
                    DebugLog.systemPrintln("[INFO-JABBER] Using PLAIN");
                    auth += "mechanism='PLAIN'>";
                    Util data = new Util();
                    data.writeUtf8String(protocol.getUserId());
                    data.writeByte(0);
                    data.writeUtf8String(Jid.getNick(protocol.getUserId()));
                    data.writeByte(0);
                    data.writeUtf8String(protocol.getPassword());
                    auth += Util.base64encode(data.toByteArray());
                    auth += "</auth>";
                } else if (canUsePlain) {
                    nonSaslLogin(connection);
                    return;
                } else {
                    setAuthStatus(connection, false);
                    return;
                }
            }
            connection.sendRequest(auth);
            return;
        }

        if (x.contains("bind")) {
            resourceBinding(connection);
            return;
        }
        x2 = x.getFirstNode("auth", "http://jabber.org/features/iq-auth");
        if (null != x2) {
            nonSaslLogin(connection);
            return;
        }
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

    private void resourceBinding(XmppConnection connection) throws SawimException {
        DebugLog.systemPrintln("[INFO-JABBER] Send bind request");
        connection.sendRequest("<iq type='set' id='bind'>"
                + "<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>"
                + "<resource>" + Util.xmlEscape(connection.resource) + "</resource>"
                + "</bind>"
                + "</iq>");
    }

    public void nonSaslLogin(XmppConnection connection) throws SawimException {
        Xmpp protocol = connection.getXmpp();
        String user = Jid.getNick(protocol.getUserId());
        connection.sendRequest(
                "<iq type='set' to='" + connection.domain_ + "' id='login'>"
                        + "<query xmlns='jabber:iq:auth'>"
                        + "<username>" + Util.xmlEscape(user) + "</username>"
                        + "<password>" + Util.xmlEscape(protocol.getPassword()) + "</password>"
                        + "<resource>" + Util.xmlEscape(connection.resource) + "</resource>"
                        + "</query>"
                        + "</iq>");
        XmlNode answer = connection.readXmlNode(true);
        setAuthStatus(connection, XmlConstants.S_RESULT.equals(answer.getAttribute(XmlConstants.S_TYPE)));
    }

    private boolean rebindSession(XmppConnection connection) throws SawimException {
        XmppSession xmppSession = connection.getXmppSession();
        DebugLog.systemPrintln("[SESSION] Try to rebind " + xmppSession.getSessionId());

        XmlNode rebindNode = XmlNode.addXmlns("rebind", "p1:rebind");
        rebindNode.setValue("jid", xmppSession.getUserId());
        rebindNode.setValue("sid", xmppSession.getSessionId());
        connection.writePacket(rebindNode.toString());

        XmlNode rebind = connection.readXmlNode(true);
        if (rebind != null && rebind.is("rebind")) {
            DebugLog.systemPrintln("[SESSION] Rebound " + xmppSession.getSessionId());

            xmppSession.setAsRestored();

            setAuthStatus(connection, true);

            return true;
        } else {
            DebugLog.systemPrintln("[SESSION] Rebind failed");
            xmppSession.resetSessionData();
            connection.getXmpp().setStatusesOffline();

            return false;
        }
    }

    private void saveSessionData(XmppConnection connection, XmlNode streamNode) throws SawimException {
        DebugLog.systemPrintln(
                String.format("[SESSION] Keep session with id=%s and user=%s",
                        streamNode.getId(), connection.fullJid_));
        XmppSession xmppSession = connection.getXmppSession();
        xmppSession.setSessionData(connection.fullJid_, streamNode.getId());
    }

    public void setupSessionKeep(XmppConnection connection) throws SawimException {
        XmlNode rebindNode = new XmlNode(XmlConstants.S_IQ);
        rebindNode.setId(XmppConnection.generateId());
        rebindNode.setType(XmlConstants.S_SET);
        XmlNode pushNode = rebindNode.addNode(XmlNode.addXmlns("push", "p1:push"));
        pushNode.addSubTag("keepalive").putAttribute("max", String.valueOf(ClientConnection.PING_INTERVAL));
        pushNode.addSubTag("session").putAttribute("duration", "1440");
        pushNode.addSubTag(XmlConstants.S_BODY)
                .putAttribute("send", "all")
                .putAttribute(XmlConstants.S_GROUPCHAT, XmlConstants.S_TRUE)
                .putAttribute(XmlConstants.S_FROM, "name");
        pushNode.addSubTag(XmlConstants.S_STATUS).putAttribute(
                XmlConstants.S_TYPE,
                connection.getNativeStatus(StatusInfo.STATUS_AWAY));
        pushNode.setValue("offline", XmlConstants.S_TRUE);

        String token = PreferenceManager.getDefaultSharedPreferences(SawimApplication.getContext()).getString(Preferences.TOKEN, "");
        if (!token.isEmpty()) {
            XmlNode notification = new XmlNode("notification");
            notification.setValue(XmlConstants.S_TYPE, "gcm");
            notification.setValue(XmlNode.S_ID, token);
            pushNode.addNode(notification);
            pushNode.setValue("appid", "ru.sawim");
        }
        connection.putPacketIntoQueue(rebindNode.toString());
    }

    public void setAuthStatus(XmppConnection connection, boolean authorized) throws SawimException {
        if (!authorized_) {
            authorized_ = authorized;
            if (!authorized) {
                connection.getXmpp().setPassword(null);
                throw new SawimException(111, 0);
            }
        }
    }

    private void openStreamXml(XmppConnection connection) throws SawimException {
        connection.write(connection.getOpenStreamXml(connection.domain_));
        connection.readXmlNode(true); // "stream:stream"
        parseAuth(connection, connection.readXmlNode(true));
    }

    private void setStreamCompression(XmppConnection connection) throws SawimException {
        connection.setProgress(20);
        try {
            connection.socket.startCompression();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        openStreamXml(connection);
    }

    private void setStreamTls(XmppConnection connection) throws SawimException {
        connection.setProgress(15);
        connection.socket.startTls(SawimApplication.sc, connection.domain_);
        DebugLog.println("tls turn on");
        openStreamXml(connection);
    }

    public void parseChallenge(XmppConnection connection, XmlNode x) throws SawimException {
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
                        Jid.getNick(connection.getXmpp().getUserId()),
                        connection.getXmpp().getPassword(),
                        connection.domain_,
                        "xmpp/" + connection.domain_,
                        nonce,
                        cnonce);
                resp += "</response>";
            } else {
                resp += "/>";
            }
        }

        connection.sendRequest(resp);
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
                new StringBuilder().append("username=\"").append(user).append("\",realm=\"").append(realm)
                        .append("\",nonce=\"").append(nonce).append("\",cnonce=\"").append(cnonce)
                        .append("\",nc=00000001,digest-uri=\"").append(digestUri)
                        .append("\",qop=auth,response=").append(quote).append(hResp.getDigestHex())
                        .append(quote).append(",charset=utf-8").toString()
        ));
    }
}
