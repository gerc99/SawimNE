


package protocol.icq.action;

import protocol.icq.packet.Packet;
import ru.sawim.SawimException;


public class OtherAction extends IcqAction {


    private Packet packet;

    public OtherAction(Packet sp) {
        packet = sp;
    }

    public void init() throws SawimException {
        sendPacket(packet);
    }

    public boolean forward(Packet packet) throws SawimException {
        return false;
    }

    public boolean isCompleted() {
        return true;
    }

    public boolean isError() {
        return false;
    }
}


