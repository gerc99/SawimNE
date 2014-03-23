

package protocol.icq.packet;

import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;

public final class FromIcqSrvPacket extends SnacPacket {


    public static final int SRV_OFFLINEMSG_SUBCMD = 0x0041;

    public static final int SRV_DONEOFFLINEMSGS_SUBCMD = 0x0042;

    public static final int SRV_META_SUBCMD = 0x07DA;
    public static final int SRV_META_GENERAL_TYPE = 0x00C8;
    public static final int META_SET_FULLINFO_ACK = 0x0C3F;
    public static final int SRV_META_FULL_INFO = 0x0fb4;


    private int icqSequence;

    private String uin;

    private int subcommand;


    public FromIcqSrvPacket(long reference, int snacFlags, int icqSequence, String uin, int subcommand, byte[] extData, byte[] data) {
        super(SnacPacket.OLD_ICQ_FAMILY, SnacPacket.SRV_FROMICQSRV_COMMAND, snacFlags, reference, extData, data);
        this.icqSequence = icqSequence;
        this.uin = uin;
        this.subcommand = subcommand;
    }


    public final int getIcqSequence() {
        return this.icqSequence;
    }


    public final void setIcqSequence(int icqSequence) {
        this.icqSequence = icqSequence;
    }


    public final int getSubcommand() {
        return this.subcommand;
    }


    public byte[] toByteArray() {

        ru.sawim.modules.DebugLog.panic("Unsupported operation (FromIcqSrvPacket.toByteArray()");

        return null;
    }


    public static Packet parse(byte[] flapData) throws SawimException {
        ArrayReader reader = new ArrayReader(flapData, 4);

        int snacFlags = reader.getWordBE();


        long snacReference = reader.getDWordBE();


        byte[] extData;
        String uin;
        int subcommand;
        int icqSequence;
        if (snacFlags == 0x8000) {

            int extDataLength = reader.getWordBE();


            extData = reader.getArray(extDataLength);

        } else {
            extData = emptyArray;
        }

        reader.skip(4);
        int dataLength = reader.getWordLE() - (4 + 2 + 2);

        uin = String.valueOf(reader.getDWordLE());
        subcommand = reader.getWordLE();
        icqSequence = reader.getWordLE();


        byte[] data = reader.getArray(dataLength);


        return new FromIcqSrvPacket(snacReference, snacFlags, icqSequence, uin, subcommand, extData, data);
    }
}


