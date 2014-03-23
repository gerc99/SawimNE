


package protocol.icq.packet;


import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.Util;


public class SnacPacket extends Packet {


    public static final int SERVICE_FAMILY = 0x0001;

    public static final int CLI_READY_COMMAND = 0x0002;
    public static final int SRV_FAMILIES_COMMAND = 0x0003;
    public static final int CLI_RATESREQUEST_COMMAND = 0x0006;
    public static final int SRV_RATES_COMMAND = 0x0007;
    public static final int CLI_ACKRATES_COMMAND = 0x0008;
    public static final int CLI_REQINFO_COMMAND = 0x000E;
    public static final int SRV_REPLYINFO_COMMAND = 0x000F;
    public static final int SRV_MOTD_COMMAND = 0x0013;
    public static final int CLI_FAMILIES_COMMAND = 0x0017;
    public static final int SRV_FAMILIES2_COMMAND = 0x0018;
    public static final int CLI_SETSTATUS_COMMAND = 0x001E;


    public static final int LOCATION_FAMILY = 0x0002;

    public static final int CLI_REQLOCATION_COMMAND = 0x0002;
    public static final int SRV_REPLYLOCATION_COMMAND = 0x0003;
    public static final int CLI_SETUSERINFO_COMMAND = 0x0004;


    public static final int CONTACT_FAMILY = 0x0003;

    public static final int CLI_REQBUDDY_COMMAND = 0x0002;
    public static final int SRV_REPLYBUDDY_COMMAND = 0x0003;
    public static final int CLI_BUDDYLIST_ADD_COMMAND = 0x0004;
    public static final int CLI_BUDDYLIST_REMOVE_COMMAND = 0x0005;
    public static final int SRV_USERONLINE_COMMAND = 0x000B;
    public static final int SRV_USEROFFLINE_COMMAND = 0x000C;


    public static final int CLI_ICBM_FAMILY = 0x0004;

    public static final int CLI_SETICBM_COMMAND = 0x0002;
    public static final int CLI_REQICBM_COMMAND = 0x0004;
    public static final int SRV_REPLYICBM_COMMAND = 0x0005;
    public static final int CLI_SENDMSG_COMMAND = 0x0006;
    public static final int SRV_RECVMSG_COMMAND = 0x0007;
    public static final int SRV_MISSED_MESSAGE_COMMAND = 0x000A;
    public static final int CLI_ACKMSG_COMMAND = 0x000B;
    public static final int SRV_MSG_ACK_COMMAND = 0x000C;
    public static final int CLI_MTN_COMMAND = 0x0014;
    public static final int SRV_MTN_COMMAND = 0x0014;


    public static final int BOS_FAMILY = 0x0009;

    public static final int CLI_REQBOS_COMMAND = 0x0002;
    public static final int SRV_REPLYBOS_COMMAND = 0x0003;


    public static final int SSI_FAMILY = 0x0013;

    public static final int CLI_REQLISTS_COMMAND = 0x0002;
    public static final int SRV_REPLYLISTS_COMMAND = 0x0003;
    public static final int CLI_REQROSTER_COMMAND = 0x0004;
    public static final int CLI_CHECKROSTER_COMMAND = 0x0005;
    public static final int SRV_REPLYROSTER_COMMAND = 0x0006;
    public static final int CLI_ROSTERACK_COMMAND = 0x0007;
    public static final int CLI_ROSTERADD_COMMAND = 0x0008;
    public static final int CLI_ROSTERUPDATE_COMMAND = 0x0009;
    public static final int CLI_ROSTERDELETE_COMMAND = 0x000A;
    public static final int SRV_UPDATEACK_COMMAND = 0x000E;
    public static final int SRV_REPLYROSTEROK_COMMAND = 0x000F;
    public static final int CLI_ADDSTART_COMMAND = 0x0011;
    public static final int CLI_ADDEND_COMMAND = 0x0012;
    public static final int CLI_GRANT_FUTURE_AUTH_COMMAND = 0x0014;
    public static final int SRV_GRANT_FUTURE_AUTH_COMMAND = 0x0015;
    public static final int CLI_REMOVEME_COMMAND = 0x0016;
    public static final int CLI_REQAUTH_COMMAND = 0x0018;
    public static final int SRV_AUTHREQ_COMMAND = 0x0019;
    public static final int CLI_AUTHORIZE_COMMAND = 0x001A;
    public static final int SRV_AUTHREPLY_COMMAND = 0x001B;
    public static final int SRV_ADDEDYOU_COMMAND = 0x001C;


    public static final int OLD_ICQ_FAMILY = 0x0015;

    public static final int SRV_TOICQERR_COMMAND = 0x0001;
    public static final int CLI_TOICQSRV_COMMAND = 0x0002;
    public static final int SRV_FROMICQSRV_COMMAND = 0x0003;


    protected static final byte[] emptyArray = new byte[0];


    protected int family;


    protected int command;


    protected int snacFlags;


    protected long reference;


    protected byte[] extData;


    protected byte[] data;


    public SnacPacket(int family, int command, int snacFlags, long reference, byte[] extData, byte[] data) {
        this.family = family;
        this.command = command;
        this.snacFlags = snacFlags;
        this.reference = (-1 == reference) ? 0 : (reference & 0x7FFF0000) + command;
        this.extData = extData;
        this.data = data;
    }


    public SnacPacket(int family, int command, long reference, byte[] extData, byte[] data) {
        this(family, command, 0, reference, extData, data);
    }

    public SnacPacket(int family, int command, long reference, byte[] data) {
        this(family, command, 0, reference, emptyArray, data);
    }

    public SnacPacket(int family, int command, byte[] data) {
        this(family, command, 0, 0, emptyArray, data);
    }

    public SnacPacket(int family, int command) {
        this(family, command, 0, 0, emptyArray, emptyArray);
    }

    public SnacPacket(int family, int command, long reference) {
        this(family, command, 0, reference, emptyArray, emptyArray);
    }


    public final int getFamily() {
        return family;
    }


    public final int getCommand() {
        return command;
    }


    public final int getFlags() {
        return snacFlags;
    }


    public final long getReference() {
        return reference;
    }


    public final ArrayReader getReader() {
        return new ArrayReader(data, 0);
    }

    protected final void assembleSnacHeader(byte[] buf) {
        Util.putWordBE(buf, 6, family);
        Util.putWordBE(buf, 8, command);
        Util.putWordBE(buf, 10, (extData.length > 0 ? 0x8000 : 0x0000));
        Util.putDWordBE(buf, 12, reference);
    }


    public byte[] toByteArray() {


        int dataLength = 10 + data.length + (extData.length > 0 ? 2 + extData.length : 0);
        byte buf[] = new byte[6 + dataLength];

        assembleFlapHeader(buf, 0x02);
        assembleSnacHeader(buf);

        int ip = 16;

        if (extData.length > 0) {
            Util.putWordBE(buf, ip, extData.length);
            System.arraycopy(extData, 0, buf, ip + 2, extData.length);

            ip += 2 + extData.length;
        }
        System.arraycopy(data, 0, buf, ip, data.length);
        return buf;
    }


    public static Packet parse(int family, int command, byte[] flapData) throws SawimException {

        int flapLength = flapData.length;


        int snacFlags = Util.getWordBE(flapData, 4);


        long snacReference = Util.getDWordBE(flapData, 6);


        byte[] extData;
        byte[] data;

        int extDataDelta = 0;
        if (snacFlags == 0x8000) {

            int extDataLength = Util.getWordBE(flapData, 10);


            extData = new byte[extDataLength];
            System.arraycopy(flapData, 10 + 2, extData, 0, extDataLength);
            extDataDelta = 2 + extDataLength;

        } else {
            extData = emptyArray;
        }

        data = new byte[flapLength - 10 - extDataDelta];
        System.arraycopy(flapData, 10 + extDataDelta, data, 0, data.length);


        return new SnacPacket(family, command, snacFlags, snacReference, extData, data);
    }
}


