package protocol.xmpp;

import protocol.Protocol;
import ru.sawim.comm.Util;

/**
 * Created by mrdoctorwho on 1/13/15.
 */

public class XmppChangePassword {

    public String getXml(String domain, String username, String password) {
        return "<iq xmlns='jabber:client' to='" + Util.xmlEscape(domain) + "' type='set'>"
                + "<query xmlns='jabber:iq:register'>"
                + "<username>" + Util.xmlEscape(username) + "</username>"
                + "<password>" + Util.xmlEscape(password) + "</password>"
                + "</query></iq>";
    }

    public void sendXml(String xml, Xmpp protocol) {
        protocol.getConnection().requestRawXml(xml);
    }

}


