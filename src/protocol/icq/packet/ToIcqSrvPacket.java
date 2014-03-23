

package protocol.icq.packet;

import ru.sawim.comm.Util;

public final class ToIcqSrvPacket extends SnacPacket {


    public static final int CLI_REQOFFLINEMSGS_SUBCMD = 0x003C;

    public static final int CLI_ACKOFFLINEMSGS_SUBCMD = 0x003E;

    public static final int CLI_META_SUBCMD = 0x07D0;
    public static final int CLI_META_REQINFO_TYPE = 0x04D0;
    public static final int CLI_META_REQMOREINFO_TYPE = 0x04B2;
    public static final int CLI_META_REQUEST_FULL_INFO = 0x0FA0;
    public static final int CLI_SET_FULLINFO = 0x0C3A;


    private int icqSequence;

    private String uin;

    private int subcommand;


    public ToIcqSrvPacket(long reference, int snacFlags, int icqSequence, String uin, int subcommand, byte[] extData, byte[] data) {
        super(SnacPacket.OLD_ICQ_FAMILY, SnacPacket.CLI_TOICQSRV_COMMAND, snacFlags, reference, extData, data);
        this.icqSequence = icqSequence;
        this.uin = uin;
        this.subcommand = subcommand;
    }


    public ToIcqSrvPacket(long reference, int icqSequence, String uin, int subcommand, byte[] extData, byte[] data) {
        this(reference, 0, icqSequence, uin, subcommand, extData, data);
    }


    public ToIcqSrvPacket(long reference, String uin, int subcommand, byte[] extData, byte[] data) {
        this(reference, 0, -1, uin, subcommand, extData, data);
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
        Util buf = new Util();
        buf.writeZeroes(16);

        if (extData.length > 0) {
            buf.writeWordBE(extData.length);
            buf.writeByteArray(extData);
        }
        buf.writeWordBE(0x0001);
        buf.writeWordBE(10 + data.length);
        buf.writeWordLE(8 + data.length);
        buf.writeDWordLE(Long.parseLong(uin));
        buf.writeWordLE(subcommand);
        buf.writeWordLE(icqSequence);
        buf.writeByteArray(data);

        byte _buf[] = buf.toByteArray();
        assembleFlapHeader(_buf, 0x02);
        assembleSnacHeader(_buf);
        return _buf;
    }
}


