package protocol.xmpp;

import android.util.Log;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.crypto.MD5;

/**
 * Created by gerc on 02.01.2016.
 */
public class Auth {

    public boolean authorized_ = false;

    /*static String rebindSessionId = "";
    static boolean rebindSupported = false;
    static boolean rebindEnabled = false;*/

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
        /*} else if (x.is("resumed")) {
            XmppSession.getInstance().save(this);
            setAuthStatus(true);
            DebugLog.systemPrintln("[INFO-JABBER] Resumed session ID=" + smSessionId);
        } else if (x.is("failed")) {
            // expired session
            DebugLog.systemPrintln("[INFO-JABBER] Failed to resume session ID=" + smSessionId);
            setSessionManagementEnabled(false);
            smSessionId = "";
            smPacketsIn = 0;
            smPacketsOut = 0;
            XmppSession.getInstance().save(this);
            resourceBinding();*/
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
        /*    saveRebindSessionId(connection, */connection.readXmlNode(true)/*)*/; // "stream:stream"
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
        /*x2 = x.getFirstNode("rebind", "p1:rebind");
        if (x2 != null) {
            rebindSupported = true;
            if (tryRebind(connection)) {
                return;
            }
        }
        x2 = x.getFirstNode("push", "p1:push");
        if (x2 != null) {
            XmppSession.getInstance().enableRebind(connection);
        }*/
        if (0 == x.childrenCount()) {
            nonSaslLogin(connection);
            return;
        }

        /*x2 = x.getFirstNode("sm", "urn:xmpp:sm:3");
        if (null != x2) {
            smSupported = true;
        }*/

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
        /*if (smSupported) {
            XmppSession.getInstance().load(this);
            if (!smSessionId.equals("")) {
                sendRequest("<resume xmlns='urn:xmpp:sm:3' previd='" + smSessionId + "' h='" + smPacketsIn + "' />");
                return;
            }
        }*/
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

    /*private void saveRebindSessionId(XmppConnection connection, XmlNode x) throws SawimException {
        if (x.is("stream:stream")) {
            Log.e("saveRebindSessionId", "" + rebindSupported);
            XmppSession.getInstance().load(connection);
            if (rebindSupported) {
                rebindSessionId = x.getId();
                XmppSession.getInstance().save(connection);
                DebugLog.systemPrintln("[INFO-JABBER] rebind supported ID=" + rebindSessionId);
            }
            return;
        }
    }*/

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

    /*private boolean tryRebind(XmppConnection connection) throws SawimException {
        connection.setProgress(50);
        XmppSession.getInstance().load(connection);
        if (rebindSessionId != null) {
            connection.write("<rebind xmlns='p1:rebind'><jid>" +
                    connection.fullJid_ + "</jid>" +
                    "<sid>" + rebindSessionId + "</sid></rebind>");
            XmlNode rebind = connection.readXmlNode(true);
            if (rebind != null && rebind.is("rebind")) {
                DebugLog.systemPrintln("[INFO-JABBER] rebound session ID=" + rebindSessionId);
                rebindEnabled = true;
                connection.getXmpp().s_updateOnlineStatus();

                XmppSession.getInstance().save(connection);
                XmppSession.getInstance().load(connection);
                connection.getXmpp().load();
                setAuthStatus(connection, true);
                return true;
            }
        }
        XmppSession.getInstance().clearRebindSessionId(connection);
        //getXmpp().getStorage().setOfflineStatuses(getXmpp());
        DebugLog.systemPrintln("[INFO-JABBER] failed to rebind");
        return false;
    }*/

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
