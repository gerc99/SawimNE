package protocol.icq.plugin;

import protocol.icq.IcqContact;
import protocol.icq.IcqNetDefActions;
import protocol.icq.IcqStatusInfo;
import protocol.icq.packet.SnacPacket;
import ru.sawim.SawimApplication;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.GUID;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;


public class XtrazMessagePlugin {
    private long time;
    private long cookie1;
    private long cookie2;
    private IcqContact rcvr;
    private String msg;
    private static final String NAME = "Script Plug-in: Remote Notification Arrive";
    public static final GUID XTRAZ_GUID = new GUID(new byte[]{(byte) 0x3b, (byte) 0x60, (byte) 0xb3, (byte) 0xef, (byte) 0xd8, (byte) 0x2a, (byte) 0x6c, (byte) 0x45, (byte) 0xa4, (byte) 0xe0, (byte) 0x9c, (byte) 0x5a, (byte) 0x5e, (byte) 0x67, (byte) 0xe8, (byte) 0x65});
    public static final int MGTYPE_SCRIPT_NOTIFY = 0x0008;

    public void setCookie(ArrayReader reader) {
        cookie1 = reader.getDWordBE();
        cookie2 = reader.getDWordBE();
    }

    public XtrazMessagePlugin(IcqContact rcvr, String msg) {
        this.rcvr = rcvr;
        this.msg = msg;
    }

    private boolean isRequest() {
        return msg.startsWith("<N>");
    }

    private byte[] getData() {
        byte[] str = StringConvertor.stringToByteArrayUtf8(msg);
        Util buffer = new Util();
        buffer.writeDWordLE(str.length);
        buffer.writeByteArray(str);
        return buffer.toByteArray();
    }

    public SnacPacket getPacket() {
        time = SawimApplication.getCurrentGmtTime() * 1000;
        if (!isRequest()) {
            return new SnacPacket(SnacPacket.CLI_ICBM_FAMILY,
                    SnacPacket.CLI_ACKMSG_COMMAND,
                    initAckMsg());
        } else {
            return new SnacPacket(SnacPacket.CLI_ICBM_FAMILY,
                    SnacPacket.CLI_SENDMSG_COMMAND,
                    initReqMsg());
        }
    }

    private byte[] initAckMsg() {

        byte[] uinRaw = StringConvertor.stringToByteArray(rcvr.getUserId());

        Util buffer = new Util();

        buffer.writeDWordBE(cookie1);
        buffer.writeDWordBE(cookie2);
        buffer.writeWordBE(0x0002);
        buffer.writeByte(uinRaw.length);
        buffer.writeByteArray(uinRaw);
        buffer.writeWordBE(0x0003);
        buffer.writeByteArray(makeTlv1127());

        return buffer.toByteArray();
    }

    private byte[] makeTlv5() {
        Util tlv5 = new Util();
        tlv5.writeWordBE(0x0000);
        tlv5.writeDWordBE(time);
        tlv5.writeDWordBE(0x00000000);
        tlv5.writeByteArray(GUID.CAP_AIM_SERVERRELAY.toByteArray());

        tlv5.writeTLVWord(0x000a, 0x0001);

        tlv5.writeTLV(0x000f, null);

        tlv5.writeTLV(0x2711, makeTlv1127());
        return tlv5.toByteArray();
    }

    private byte[] initReqMsg() {
        byte[] uinRaw = StringConvertor.stringToByteArray(rcvr.getUserId());

        Util buffer = new Util();

        buffer.writeDWordBE(time);
        buffer.writeDWordBE(0x00000000);
        buffer.writeWordBE(0x0002);
        buffer.writeByte(uinRaw.length);
        buffer.writeByteArray(uinRaw);

        buffer.writeTLV(0x0005, makeTlv5());
        buffer.writeTLV(0x0003, null);
        return buffer.toByteArray();
    }

    private byte[] makeTlv1127() {
        byte[] textRaw = new byte[0];
        byte[] pluginData = pluginData();
        Util tlv1127 = new Util();

        tlv1127.writeWordLE(0x001B);

        tlv1127.writeWordLE(0x0008);

        tlv1127.writeDWordBE(0x00000000);
        tlv1127.writeDWordBE(0x00000000);
        tlv1127.writeDWordBE(0x00000000);
        tlv1127.writeDWordBE(0x00000000);

        tlv1127.writeWordLE(0x0000);
        tlv1127.writeByte(0x03);

        tlv1127.writeDWordBE(0x00000000);

        int SEQ1 = 0xffff - 1;
        tlv1127.writeWordLE(SEQ1);
        tlv1127.writeWordLE(0x000E);
        tlv1127.writeWordLE(SEQ1);

        tlv1127.writeDWordLE(0x00000000);
        tlv1127.writeDWordLE(0x00000000);
        tlv1127.writeDWordLE(0x00000000);

        tlv1127.writeWordLE(IcqNetDefActions.MESSAGE_TYPE_EXTENDED);

        tlv1127.writeWordLE(IcqStatusInfo.STATUS_ONLINE);

        tlv1127.writeWordLE(0x0001);

        tlv1127.writeWordLE(textRaw.length + 1);
        tlv1127.writeByteArray(textRaw);
        tlv1127.writeByte(0x00);
        tlv1127.writeByteArray(pluginData);

        return tlv1127.toByteArray();
    }

    private byte[] pluginData() {
        byte[] subType = StringConvertor.stringToByteArray(NAME);
        byte[] data = getData();
        GUID guid = XTRAZ_GUID;
        int command = MGTYPE_SCRIPT_NOTIFY;
        int flag2 = 0x00000000;

        int headerLen = 16 + 2 + 4 + subType.length + 4 + 4 + 4 + 2 + 1;
        Util buffer = new Util();

        buffer.writeWordLE(headerLen);

        buffer.writeByteArray(guid.toByteArray());
        buffer.writeWordLE(command);

        buffer.writeDWordLE(subType.length);
        buffer.writeByteArray(subType);

        buffer.writeDWordBE(0x00000100);
        buffer.writeDWordBE(flag2);
        buffer.writeDWordBE(0x00000000);
        buffer.writeWordBE(0x0000);
        buffer.writeByte(0x00);

        buffer.writeDWordLE(data.length);
        buffer.writeByteArray(data);

        return buffer.toByteArray();
    }
}