package protocol.mrim;

import ru.sawim.SawimApplication;
import ru.sawim.comm.Config;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.crypto.MD5;
import ru.sawim.modules.search.Search;


final class MrimPacket {
    public static final int MRIM_CS_HELLO = 0x1001;
    public static final int MRIM_CS_HELLO_ACK = 0x1002;
    public static final int MRIM_CS_LOGIN_ACK = 0x1004;
    public static final int MRIM_CS_LOGIN_REJ = 0x1005;
    public static final int MRIM_CS_PING = 0x1006;
    public static final int MRIM_CS_LOGIN2 = 0x1038;
    public static final int MRIM_CS_MESSAGE = 0x1008;
    public static final int MRIM_CS_MESSAGE_ACK = 0x1009;
    public static final int MRIM_CS_MESSAGE_RECV = 0x1011;
    public static final int MRIM_CS_MESSAGE_STATUS = 0x1012;
    public static final int MRIM_CS_LOGOUT = 0x1013;
    public static final int MRIM_CS_USER_STATUS = 0x100F;
    public static final int MRIM_CS_USER_INFO = 0x1015;
    public static final int MRIM_CS_ADD_CONTACT = 0x1019;
    public static final int MRIM_CS_ADD_CONTACT_ACK = 0x101A;
    public static final int MRIM_CS_MODIFY_CONTACT = 0x101B;
    public static final int MRIM_CS_MODIFY_CONTACT_ACK = 0x101C;
    public static final int MRIM_CS_WP_REQUEST = 0x1029;
    public static final int MRIM_CS_ANKETA_INFO = 0x1028;
    public static final int MRIM_CS_OFFLINE_MESSAGE_ACK = 0x101D;
    public static final int MRIM_CS_DELETE_OFFLINE_MESSAGE = 0x101E;
    public static final int MRIM_CS_AUTHORIZE = 0x1020;
    public static final int MRIM_CS_AUTHORIZE_ACK = 0x1021;
    public static final int MRIM_CS_CHANGE_STATUS = 0x1022;
    public static final int MRIM_CS_SMS = 0x1039;
    public static final int MRIM_CS_CONTACT_LIST2 = 0x1037;
    public static final int MRIM_CS_NEW_EMAIL = 0x1048;
    public static final int MRIM_MICROBLOG_RECORD = 0x1063;
    public static final int MRIM_MICROBLOG_ADD_RECORD = 0x1064;

    public static final int MESSAGE_FLAG_OFFLINE = 0x00000001;
    public static final int MESSAGE_FLAG_NORECV = 0x00000004;
    public static final int MESSAGE_FLAG_AUTHORIZE = 0x00000008;
    public static final int MESSAGE_FLAG_SYSTEM = 0x00000040;
    public static final int MESSAGE_FLAG_RTF = 0x00000080;
    public static final int MESSAGE_FLAG_CONTACT = 0x00000200;
    public static final int MESSAGE_FLAG_NOTIFY = 0x00000400;
    public static final int MESSAGE_FLAG_MULTICAST = 0x00001000;
    public static final int MESSAGE_FLAG_ALARM = 0x00004000;
    public static final int MESSAGE_DELIVERED = 0x0000;
    public static final int MESSAGE_FLAG_OLD = 0x00200000;

    private MrimBuffer data;
    private int cmd;
    private int seq;
    private static final long ip = 0x7F000001;
    private static final long port = 666;


    public MrimPacket(int cmd) {
        this.cmd = cmd;
        this.data = new MrimBuffer();
    }

    public MrimPacket(int cmd, MrimBuffer data) {
        this.cmd = cmd;
        this.data = data;
    }

    void setSeq(int s) {
        seq = s;
    }

    public int getSeq() {
        return seq;
    }

    MrimPacket(int cmd, int seq, byte[] data) {
        this.cmd = cmd;
        this.seq = seq;
        this.data = new MrimBuffer((null == data) ? new byte[0] : data);
    }

    public MrimBuffer getData() {
        return data;
    }

    public int getCommand() {
        return cmd;
    }


    private void putDWord(byte[] data, int pos, long dword) {
        Util.putDWordLE(data, pos, dword);
    }

    public byte[] toByteArray() {
        byte[] body = data.toByteArray();
        byte[] packet = new byte[4 * 7 + 16 + body.length];
        if (body.length > 0) {
            System.arraycopy(body, 0, packet, 4 * 7 + 16, body.length);
        }
        putDWord(packet, 0, 0xDEADBEEF);
        putDWord(packet, 4, 0x00010016);
        putDWord(packet, 8, seq);
        putDWord(packet, 12, cmd);
        putDWord(packet, 16, body.length);
        putDWord(packet, 20, ip);
        putDWord(packet, 24, port);
        return packet;
    }

    static MrimPacket getSetStatusPacket(Mrim mrim) {
        MrimBuffer out = new MrimBuffer();
        out.putStatusInfo(mrim);
        return new MrimPacket(MrimPacket.MRIM_CS_CHANGE_STATUS, out);
    }

    public static MrimPacket getLoginPacket(Mrim mrim) {
        MrimBuffer out = new MrimBuffer();
        out.putString(mrim.getUserId());

        byte[] pass = MD5.calculate(mrim.getPassword());
        out.putDWord(pass.length);
        out.putBytes(pass);
        out.putStatusInfo(mrim);

        String version = "client=\"sawim\" title=\"sawin\" "
                + "version=\"" + SawimApplication.VERSION + "\"";
        out.putString(version);
        out.putString("ru");
        out.putDWord(0);
        out.putDWord(0);
        out.putString("sawim");
        return new MrimPacket(MrimPacket.MRIM_CS_LOGIN2, out);
    }

    public static MrimBuffer getMessageBuffer(String to, String msg, int flags) {
        MrimBuffer out = new MrimBuffer();
        out.putDWord(flags);
        out.putString(to);
        out.putUcs2String(msg);
        out.putString("");
        return out;
    }

    public static MrimPacket getMessagePacket(String to, String msg, int flags) {
        MrimBuffer out = getMessageBuffer(to, msg, flags);
        return new MrimPacket(MrimPacket.MRIM_CS_MESSAGE, out);
    }

    public static MrimPacket getMessageRecvPacket(String from, long msgId) {
        MrimBuffer out = new MrimBuffer();
        out.putString(from);
        out.putDWord(msgId);
        return new MrimPacket(MrimPacket.MRIM_CS_MESSAGE_RECV, out);
    }

    static MrimPacket getDeleteOfflineMessagePacket(byte[] msg) {
        MrimBuffer out = new MrimBuffer();
        out.putBytes(msg);
        return new MrimPacket(MrimPacket.MRIM_CS_DELETE_OFFLINE_MESSAGE, out);
    }

    static MrimPacket getAutorizePacket(String uin) {
        MrimBuffer out = new MrimBuffer();
        out.putString(uin);
        return new MrimPacket(MrimPacket.MRIM_CS_AUTHORIZE, out);
    }

    static MrimPacket getAddContactPacket(int flags, int groupId, String uin, String name, String phone, String request) {
        MrimBuffer out = new MrimBuffer();
        out.putContact(flags, groupId, uin, name, phone);
        out.putString(request);
        out.putString("");
        return new MrimPacket(MrimPacket.MRIM_CS_ADD_CONTACT, out);
    }

    static MrimPacket getModifyContactPacket(int id, int flags, int groupId, String uin, String name, String phone) {
        MrimBuffer out = new MrimBuffer();
        out.putDWord(id);
        out.putContact(flags, groupId, uin, name, phone);
        return new MrimPacket(MrimPacket.MRIM_CS_MODIFY_CONTACT, out);
    }

    private static final int MRIM_CS_WP_REQUEST_PARAM_USER = 0;
    private static final int MRIM_CS_WP_REQUEST_PARAM_DOMAIN = 1;
    private static final int MRIM_CS_WP_REQUEST_PARAM_NICKNAME = 2;
    private static final int MRIM_CS_WP_REQUEST_PARAM_FIRSTNAME = 3;
    private static final int MRIM_CS_WP_REQUEST_PARAM_LASTNAME = 4;
    private static final int MRIM_CS_WP_REQUEST_PARAM_SEX = 5;
    private static final int MRIM_CS_WP_REQUEST_PARAM_BIRTHDAY = 6;
    private static final int MRIM_CS_WP_REQUEST_PARAM_DATE1 = 7;
    private static final int MRIM_CS_WP_REQUEST_PARAM_DATE2 = 8;
    private static final int MRIM_CS_WP_REQUEST_PARAM_ONLINE = 9;
    private static final int MRIM_CS_WP_REQUEST_PARAM_STATUS = 10;
    private static final int MRIM_CS_WP_REQUEST_PARAM_CITY_ID = 11;
    private static final int MRIM_CS_WP_REQUEST_PARAM_ZODIAC = 12;
    private static final int MRIM_CS_WP_REQUEST_PARAM_BIRTHDAY_MONTH = 13;
    private static final int MRIM_CS_WP_REQUEST_PARAM_BIRTHDAY_DAY = 14;
    private static final int MRIM_CS_WP_REQUEST_PARAM_COUNTRY_ID = 15;

    public static final int MRIM_ANKETA_INFO_STATUS_OK = 1;

    static String getSityId(String sity) {
        return Config.getConfigValue(sity, "/cities.txt");
    }

    static MrimPacket getUserSearchRequestPacket(String[] userInfo) {
        MrimBuffer out = new MrimBuffer();
        if (!StringConvertor.isEmpty(userInfo[Search.UIN])) {
            String[] s = Util.explode(userInfo[Search.UIN], '@');
            if ((1 == s.length) && (0 != Util.strToIntDef(s[0], 0))) {
                s = new String[]{s[0], "uin.icq"};
            }
            if (2 == s.length) {
                out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_USER, s[0]);
                out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_DOMAIN, s[1]);
            }
        }
        out.putUcs2SearchParam(MRIM_CS_WP_REQUEST_PARAM_FIRSTNAME, userInfo[Search.FIRST_NAME]);
        out.putUcs2SearchParam(MRIM_CS_WP_REQUEST_PARAM_LASTNAME, userInfo[Search.LAST_NAME]);
        out.putUcs2SearchParam(MRIM_CS_WP_REQUEST_PARAM_NICKNAME, userInfo[Search.NICK]);
        if (Util.strToIntDef(userInfo[Search.GENDER], 0) > 0) {
            String sex = (1 == Util.strToIntDef(userInfo[Search.GENDER], 1)) ? "2" : "1";
            out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_SEX, sex);
        }
        String[] age = Util.explode(userInfo[Search.AGE], '-');
        if (age.length == 2) {
            out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_DATE1, age[0]);
            out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_DATE2, age[1]);
        }
        out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_CITY_ID, getSityId(userInfo[Search.CITY]));


        if ("1".equals(userInfo[Search.ONLY_ONLINE])) {
            out.putSearchParam(MRIM_CS_WP_REQUEST_PARAM_ONLINE, userInfo[Search.ONLY_ONLINE]);
        }
        return new MrimPacket(MrimPacket.MRIM_CS_WP_REQUEST, out);
    }
}


