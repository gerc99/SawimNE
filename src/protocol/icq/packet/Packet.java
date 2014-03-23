

package protocol.icq.packet;

import ru.sawim.comm.Util;

public abstract class Packet {

    protected final void assembleFlapHeader(byte[] buf, int channel) {
        Util.putByte(buf, 0, 0x2a);
        Util.putByte(buf, 1, channel);
        Util.putWordBE(buf, 2, 0);
        Util.putWordBE(buf, 4, buf.length - 6);
    }


    public abstract byte[] toByteArray();
}


