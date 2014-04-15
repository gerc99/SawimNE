


package protocol.icq.action;

import protocol.icq.packet.FromIcqSrvPacket;
import protocol.icq.packet.Packet;
import protocol.icq.packet.SnacPacket;
import protocol.icq.packet.ToIcqSrvPacket;
import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;


public class SearchAction extends IcqAction {


    private static final int STATE_ERROR = -1;
    private static final int STATE_SEARCH_SENT = 1;
    private static final int STATE_NEXT_RECEIVED = 2;
    private static final int STATE_LAST_RECEIVED = 3;
    private static final int STATE_FINISHED = 4;


    public static final int TLV_TYPE_UIN = 0x0136;
    public static final int TLV_TYPE_NICK = 0x0154;
    public static final int TLV_TYPE_FIRSTNAME = 0x0140;
    public static final int TLV_TYPE_LASTNAME = 0x014A;
    public static final int TLV_TYPE_EMAIL = 0x015E;
    public static final int TLV_TYPE_CITY = 0x0190;
    public static final int TLV_TYPE_GENDER = 0x017C;
    public static final int TLV_TYPE_ONLYONLINE = 0x0230;
    public static final int TLV_TYPE_AGE = 0x0168;


    public static final int TIMEOUT = 60;


    private int state;


    private Search cont;
    private int searchId;

    public SearchAction(Search cont) {
        super();
        this.cont = cont;
        searchId = cont.getSearchId();
    }

    private boolean isCanceled() {
        return searchId != cont.getSearchId();
    }

    private void addStr(Util buffer, int type, int param) {
        String str = cont.getSearchParam(param);
        if (null != str) {
            buffer.writeProfileAsciizTLV(type, str);
        }
    }

    private void addByte(Util buffer, int type, int value) {
        buffer.writeWordLE(type);
        buffer.writeWordLE(1);
        buffer.writeByte(value);
    }

    public void init() throws SawimException {
        Util buffer = new Util();

        buffer.writeWordLE(0x055f);


        int uin = Util.strToIntDef(cont.getSearchParam(Search.UIN), 0);
        if (0 != uin) {
            buffer.writeWordLE(TLV_TYPE_UIN);
            buffer.writeWordLE(0x0004);
            buffer.writeDWordLE(uin);
        }

        addStr(buffer, TLV_TYPE_NICK, Search.NICK);
        addStr(buffer, TLV_TYPE_FIRSTNAME, Search.FIRST_NAME);
        addStr(buffer, TLV_TYPE_LASTNAME, Search.LAST_NAME);
        addStr(buffer, TLV_TYPE_EMAIL, Search.EMAIL);
        addStr(buffer, TLV_TYPE_CITY, Search.CITY);


        String[] age = Util.explode(cont.getSearchParam(Search.AGE), '-');
        if ((age.length == 2) && ((age[0].length() > 0) || (age[1].length() > 0))) {
            buffer.writeWordLE(TLV_TYPE_AGE);
            buffer.writeWordLE(4);
            buffer.writeWordLE(Util.strToIntDef(age[0], 0));
            buffer.writeWordLE(Util.strToIntDef(age[1], 99));
        }


        int gender = Util.strToIntDef(cont.getSearchParam(Search.GENDER), 0);
        if (0 != gender) {
            addByte(buffer, TLV_TYPE_GENDER, gender);
        }


        if ("1".equals(cont.getSearchParam(Search.ONLY_ONLINE))) {
            addByte(buffer, TLV_TYPE_ONLYONLINE, 1);
        }

        sendPacket(new ToIcqSrvPacket(SnacPacket.CLI_TOICQSRV_COMMAND,
                0x0002, getIcq().getUserId(), 0x07D0, new byte[0], buffer.toByteArray()));

        active();
        this.state = STATE_SEARCH_SENT;
    }


    public boolean forward(Packet packet) throws SawimException {
        if (isCanceled() || !(packet instanceof FromIcqSrvPacket)) {
            return false;
        }


        FromIcqSrvPacket fromIcqServerPacket = (FromIcqSrvPacket) packet;
        UserInfo info = new UserInfo(getIcq());

        ArrayReader reader = fromIcqServerPacket.getReader();

        int dataSubType = reader.getWordBE();
        if (0xa401 == dataSubType) {
            this.state = STATE_NEXT_RECEIVED;
        } else if (0xae01 == dataSubType) {
            this.state = STATE_LAST_RECEIVED;
        } else {
            return false;
        }

        if (0x0A != reader.getByte()) {
            this.state = STATE_ERROR;
            return true;
        }

        reader.getWordLE();

        info.uin = String.valueOf(reader.getDWordLE());


        String[] strings = new String[4];


        for (int i = 0; i < 4; ++i) {
            int len = reader.getWordLE();
            byte[] str = reader.getArray(len);
            strings[i] = StringConvertor.byteArrayToWinString(str, 0, len);
        }
        info.nick = strings[0];
        info.firstName = strings[1];
        info.lastName = strings[2];
        info.email = strings[3];


        info.auth = (0 == reader.getByte());


        int status = reader.getWordLE();
        info.status = Integer.toString(status);


        info.gender = (byte) reader.getByte();


        info.age = reader.getWordLE();
        cont.addResult(info);
        if (this.state == STATE_LAST_RECEIVED) {
            long foundleft = reader.getDWordLE();
            this.state = STATE_FINISHED;
            cont.finished();
        }

        active();

        return true;
    }


    public boolean isCompleted() {
        return (state == STATE_FINISHED) || isCanceled();
    }


    public boolean isError() {
        if (!isCompleted() && isNotActive(TIMEOUT)) {
            cont.finished();
            state = STATE_ERROR;
        }
        return (STATE_ERROR == state);
    }
}


