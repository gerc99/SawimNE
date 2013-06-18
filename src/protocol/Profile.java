

package protocol;

import sawim.comm.Util;


public final class Profile {

    public static final int PROTOCOL_ICQ = 0;
    public static final int PROTOCOL_MRIM = 1;
    public static final int PROTOCOL_JABBER = 2;
    public static final int PROTOCOL_MSN = 4;
    public static final int PROTOCOL_OBIMP = 9;
    public static final int PROTOCOL_FACEBOOK = 10;
    public static final int PROTOCOL_LJ = 11;
    public static final int PROTOCOL_YANDEX = 12;
    public static final int PROTOCOL_VK = 13;
    public static final int PROTOCOL_GTALK = 14;
    public static final int PROTOCOL_QIP = 15;
    public static final int PROTOCOL_ODNOKLASSNIKI = 16;
    public static final int PROTOCOL_VK_API = 20;
    public static final String[] protocolNames = Util.explode((""
            + "|ICQ"
            + "|Mail.ru Agent"
            + "|Jabber"
            + "|Facebook"
            + "|\u041e\u0434\u043d\u043e\u043a\u043b\u0430\u0441\u0441\u043d\u0438\u043a\u0438"
            + "|VKontakte"
            + "|LiveJournal"
            + "|GTalk"
            + "|Ya.Online"
            + "|QIP"
    ).substring(1), '|');
    public static final byte[] protocolTypes = new byte[] {
            PROTOCOL_ICQ,
            PROTOCOL_MRIM,
            PROTOCOL_JABBER,
            PROTOCOL_FACEBOOK,
            PROTOCOL_ODNOKLASSNIKI,
            PROTOCOL_VK,
            PROTOCOL_LJ,
            PROTOCOL_GTALK,
            PROTOCOL_YANDEX,
            PROTOCOL_QIP,
    };
    public static final String[] protocolIds = new String[] {
            "UIN/E-mail",
            "e-mail",
            "jid",
            "Login",
            "ID",
            "ID",
            "Login",
            "Login",
            "Login",
            "Login",
    };

    public byte protocolType;
    public String userId = "";
    public String password = "";
    public String nick = "";

    public byte statusIndex = StatusInfo.STATUS_OFFLINE;
    public String statusMessage;

    public byte xstatusIndex = -1;
    public String xstatusTitle;
    public String xstatusDescription;
    public boolean isActive;

    public Profile() {
        protocolType = Profile.protocolTypes[0];
    }

    public boolean isConnected() {
        return StatusInfo.STATUS_OFFLINE != statusIndex;
    }
}