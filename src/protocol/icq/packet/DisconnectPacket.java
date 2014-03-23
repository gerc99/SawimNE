

package protocol.icq.packet;

import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

public class DisconnectPacket extends Packet {


    public static final int TYPE_SRV_COOKIE = 1;
    public static final int TYPE_SRV_GOODBYE = 2;
    public static final int TYPE_CLI_GOODBYE = 3;

    protected String uin;

    protected String server;

    protected byte[] cookie;

    protected int error;

    protected String description;


    public DisconnectPacket(String uin, String server, byte[] cookie) {
        this.uin = uin;
        this.server = server;
        this.cookie = new byte[cookie.length];
        System.arraycopy(cookie, 0, this.cookie, 0, cookie.length);
        this.error = -1;
        this.description = null;
    }


    public DisconnectPacket(int error, String description) {
        this.uin = null;
        this.server = null;
        this.cookie = null;
        this.error = error;
        this.description = description;
    }


    public DisconnectPacket() {
        this.uin = null;
        this.server = null;
        this.cookie = null;
        this.error = -1;
        this.description = null;
    }

    public String getUin() {
        return uin;
    }


    public int getType() {
        if (this.uin != null) {
            return DisconnectPacket.TYPE_SRV_COOKIE;
        } else if (this.error >= 0) {
            return DisconnectPacket.TYPE_SRV_GOODBYE;
        } else {
            return DisconnectPacket.TYPE_CLI_GOODBYE;
        }
    }

    public SawimException makeException() {

        int toThrow = 100;
        switch (getError()) {

            case 0x0001:
                toThrow = 110;
                break;

            case 0x0004:
            case 0x0005:
                toThrow = 111;
                break;

            case 0x0007:
            case 0x0008:
                toThrow = 112;
                break;

            case 0x0015:
            case 0x0016:
                toThrow = 113;
                break;

            case 0x0018:
            case 0x001d:
                toThrow = 114;
                break;
        }
        return new SawimException(toThrow, getError());
    }


    public String getServer() {
        if (this.getType() == DisconnectPacket.TYPE_SRV_COOKIE) {
            return this.server;
        } else {
            return null;
        }
    }


    public byte[] getCookie() {
        if (DisconnectPacket.TYPE_SRV_COOKIE == this.getType()) {
            byte[] result = new byte[this.cookie.length];
            System.arraycopy(this.cookie, 0, result, 0, this.cookie.length);
            return result;
        } else {
            return null;
        }
    }


    public int getError() {
        return (TYPE_SRV_GOODBYE == getType()) ? error : -1;
    }


    public byte[] toByteArray() {

        Util buf = new Util();

        buf.writeZeroes(6);


        if (this.getType() == DisconnectPacket.TYPE_SRV_COOKIE) {


            buf.writeTLV(0x0001, StringConvertor.stringToByteArray(this.uin));


            buf.writeTLV(0x0005, StringConvertor.stringToByteArray(this.server));


            buf.writeTLV(0x0006, this.cookie);

        } else if (this.getType() == DisconnectPacket.TYPE_SRV_GOODBYE) {


            buf.writeTLV(0x0001, StringConvertor.stringToByteArray(this.uin));


            buf.writeTLV(0x0004, StringConvertor.stringToByteArray(description));


            buf.writeTLVWord(0x0008, error);
        }


        byte[] _buf = buf.toByteArray();
        assembleFlapHeader(_buf, 0x04);
        return _buf;
    }


    public static Packet parse(byte[] flapData) throws SawimException {


        String uin = null;
        String server = null;
        byte[] cookie = null;
        int error = -1;
        String description = null;


        ArrayReader marker = new ArrayReader(flapData, 0);
        while (marker.isNotEnd()) {
            int tlvType = marker.getTlvType();
            byte[] tlvValue = marker.getTlv();
            if (null == tlvValue) {
                throw new SawimException(135, 0);
            }


            switch (tlvType) {
                case 0x0001:
                    uin = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0005:
                    server = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                case 0x0006:
                    cookie = tlvValue;
                    break;
                case 0x0008:
                case 0x0009:
                    error = Util.getWordBE(tlvValue, 0);
                    break;
                case 0x0004:
                case 0x000B:
                    description = StringConvertor.byteArrayToAsciiString(tlvValue);
                    break;
                default:

            }

        }


        if ((uin == null) && (server == null) && (cookie == null) && (error == -1) && (description == null)) {
            return new DisconnectPacket();


        } else if ((uin != null) && (server != null) && (cookie != null) && (error == -1) && (description == null)) {
            return new DisconnectPacket(uin, server, cookie);


        } else if ((server == null) && (cookie == null) && (error != -1) && (description != null)) {
            return new DisconnectPacket(error, description);

        } else {

            throw new SawimException(135, 2);
        }

    }
}


