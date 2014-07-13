package protocol;

import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.comm.Util;


public final class Profile {

    public static final int PROTOCOL_ICQ = 0;
    public static final int PROTOCOL_MRIM = 1;
    public static final int PROTOCOL_JABBER = 2;
    public static final int PROTOCOL_FACEBOOK = 10;
    public static final int PROTOCOL_LJ = 11;
    public static final int PROTOCOL_YANDEX = 12;
    public static final int PROTOCOL_GTALK = 14;
    public static final int PROTOCOL_QIP = 15;
    public static final int PROTOCOL_ODNOKLASSNIKI = 16;
    public static final int PROTOCOL_VK_API = 20;
    public static final String[] protocolNames = Util.explode((""
            + "|ICQ"
            + "|XMPP"
            + "|vk.com (api)"
            + "|Mail.ru Agent"
            + "|Facebook"
            + "|" + SawimApplication.getContext().getString(R.string.classmates)
            + "|LiveJournal"
            + "|GTalk"
            + "|Yandex"
            + "|QIP"
    ).substring(1), '|');
    public static final byte[] protocolTypes = new byte[]{
            PROTOCOL_ICQ,
            PROTOCOL_JABBER,
            PROTOCOL_VK_API,
            PROTOCOL_MRIM,
            PROTOCOL_FACEBOOK,
            PROTOCOL_ODNOKLASSNIKI,
            PROTOCOL_LJ,
            PROTOCOL_GTALK,
            PROTOCOL_YANDEX,
            PROTOCOL_QIP,
    };
    public static final String[] protocolIds = new String[]{
            "UIN/E-mail",
            SawimApplication.getContext().getString(R.string.acc_login),
            "E-mail/phone",
            "E-mail",
            SawimApplication.getContext().getString(R.string.acc_login),
            "ID",
            SawimApplication.getContext().getString(R.string.acc_login),
            SawimApplication.getContext().getString(R.string.acc_login),
            SawimApplication.getContext().getString(R.string.acc_login),
            SawimApplication.getContext().getString(R.string.acc_login),
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