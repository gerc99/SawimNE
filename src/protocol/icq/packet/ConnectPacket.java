

package protocol.icq.packet;

import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

public class ConnectPacket extends Packet {


    public static final int SRV_CLI_HELLO = 1;
    public static final int CLI_COOKIE = 2;
    public static final int CLI_IDENT = 3;

    protected byte[] cookie;

    protected String uin;

    protected String password;


    public ConnectPacket() {
        this.cookie = null;
        this.uin = null;
        this.password = null;
    }


    public ConnectPacket(byte[] cookie) {
        this.cookie = new byte[cookie.length];
        System.arraycopy(cookie, 0, this.cookie, 0, cookie.length);
        this.uin = null;
        this.password = null;
    }


    public ConnectPacket(String uin, String password) {
        this.cookie = null;
        this.uin = uin;
        this.password = password;
    }


    public int getType() {
        if ((this.cookie == null) && (this.uin == null)) {
            return (ConnectPacket.SRV_CLI_HELLO);
        } else if (this.uin != null) {
            return (ConnectPacket.CLI_IDENT);
        } else {
            return (ConnectPacket.CLI_COOKIE);
        }
    }


    public byte[] getCookie() {
        if (ConnectPacket.CLI_COOKIE == this.getType()) {
            byte[] result = new byte[this.cookie.length];
            System.arraycopy(this.cookie, 0, result, 0, this.cookie.length);
            return (result);
        } else {
            return (null);
        }
    }

    public static void putVersion(Util stream, boolean first) {
        if (first) {
            stream.writeTLV(0x4C, null);
        }
        stream.writeTLVWord(0xA2, 0x05);
        stream.writeTLVWord(0xA3, 0x05);
        stream.writeTLVWord(0xA4, 0x00);
        stream.writeTLVWord(0xA5, 0x17F2);
        stream.writeTLV(0x03, "ICQ Client".getBytes());
        stream.writeTLVWord(0x17, 20);

        stream.writeTLVWord(0x18, 52);
        stream.writeTLVWord(0x19, 0);
        stream.writeTLVWord(0x1A, 3003);
        stream.writeTLVWord(0x16, 266);
        stream.writeTLV(0x14, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x3D});
        stream.writeTLV(0x0F, "en".getBytes());
        stream.writeTLV(0x0E, "us".getBytes());
        if (!first) {
            stream.writeTLVWord(0x9e, 0x0002);
        }
        if (!first) {
            stream.writeTLVWord(0x9F, 0x0000);
            stream.writeTLVWord(0xA0, 0x0000);
            stream.writeTLVWord(0xA1, 0x08AF);
            stream.writeTLVByte(0x94, 0x00);
        }

        stream.writeTLVByte(0x4A, 0x01);
        if (!first) {
            stream.writeTLVByte(0xAC, 0x00);
        }
    }

    public static void putLiteVersion(Util stream, boolean first) {
        if (first) {
            stream.writeTLV(0x4C, null);
        }
        stream.writeTLVWord(0xA2, 0x05);
        stream.writeTLVWord(0xA3, 0x05);
        stream.writeTLVWord(0xA4, 0x00);
        stream.writeTLVWord(0xA5, 0x17F2);
        stream.writeTLV(0x03, "ICQ Client".getBytes());
        stream.writeTLVWord(0x17, 0x14);

        stream.writeTLVWord(0x18, 0x34);
        stream.writeTLVWord(0x19, 0x00);
        stream.writeTLVWord(0x1A, 0x0BBB);
        stream.writeTLVWord(0x16, 0x010A);
        stream.writeTLV(0x14, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x04, (byte) 0x3D});
        stream.writeTLV(0x0F, "ru".getBytes());
        stream.writeTLV(0x0E, "ru".getBytes());
        if (!first) {
            stream.writeTLVWord(0x9e, 0x0002);
        }
        if (!first) {
            stream.writeTLVWord(0x9F, 0x0000);
            stream.writeTLVWord(0xA0, 0x0000);
            stream.writeTLVWord(0xA1, 0x08AF);
            stream.writeTLVByte(0x94, 0x00);
        }

        stream.writeTLVByte(0x4A, 0x01);
        if (!first) {
            stream.writeTLVByte(0xAC, 0x00);
        }
    }


    public byte[] toByteArray() {
        Util buf = new Util();

        buf.writeZeroes(6);


        buf.writeDWordBE(0x00000001);


        if (this.getType() == ConnectPacket.CLI_COOKIE) {


            buf.writeTLV(0x0006, this.cookie);


        } else if (this.getType() == ConnectPacket.CLI_IDENT) {
            if (-1 != uin.indexOf('@')) {
                buf.writeTLV(0x0056, null);
            }


            buf.writeTLV(0x0001, StringConvertor.stringToByteArray(this.uin));


            buf.writeTLV(0x0002, Util.decipherPassword(
                    StringConvertor.stringToByteArray(this.password)));

        } else {
            buf.writeTLVDWord(0x8003, 0x00100000);
        }

        if (this.getType() == ConnectPacket.CLI_COOKIE) {
            ConnectPacket.putVersion(buf, false);
            buf.writeTLVDWord(0x8003, 0x00100000);

        } else if (this.getType() == ConnectPacket.CLI_IDENT) {
            if (0 < uin.indexOf('@')) {
                ConnectPacket.putVersion(buf, true);
                buf.writeTLVDWord(0x8003, 0x00100000);
            }
        }

        byte[] _buf = buf.toByteArray();
        assembleFlapHeader(_buf, 0x01);
        return _buf;
    }


    public static Packet parse(byte[] flapData) throws SawimException {

        if ((flapData.length < 4) || (Util.getDWordBE(flapData, 0) != 0x00000001)) {
            throw new SawimException(132, 0);
        }


        byte[] cookie = null;
        String uin = null;
        String password = null;
        String version = null;
        byte[] unknown = null;


        ArrayReader marker = new ArrayReader(flapData, 4);
        while (marker.isNotEnd()) {
            int tlvType = marker.getTlvType();
            byte[] tlvValue = marker.getTlv();
            if (tlvValue == null) {
                throw new SawimException(132, 1);
            }


            switch (tlvType) {
                case 0x0006:
                    cookie = tlvValue;
                    break;
                case 0x0001:
                    uin = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0002:
                    password = StringConvertor.byteArrayToAsciiString(Util.decipherPassword(tlvValue));
                    break;
                case 0x0003:
                    version = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0016:
                    unknown = tlvValue;
                    break;
            }

        }


        if ((cookie == null) && (uin == null) && (password == null) && (version == null) && (unknown == null)) {
            return new ConnectPacket();


        } else if ((cookie != null) && (uin == null) && (password == null) && (version == null) && (unknown == null)) {
            return new ConnectPacket(cookie);


        } else if ((cookie == null) && (uin != null) && (password != null) && (version != null) && (unknown != null)) {
            return new ConnectPacket(uin, password);


        } else {
            throw new SawimException(132, 3);
        }

    }
}


