

package protocol.icq.action;

import protocol.icq.Icq;
import protocol.icq.IcqNetWorking;
import protocol.icq.packet.Packet;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;

public abstract class IcqAction {
    private IcqNetWorking connection;
    private long lastActivity;

    protected final void active() {
        lastActivity = SawimApplication.getCurrentGmtTime();
    }

    protected final boolean isNotActive(long timeout) {
        return lastActivity + timeout < SawimApplication.getCurrentGmtTime();
    }


    public abstract boolean isCompleted();


    public abstract boolean isError();

    public final void setConnection(IcqNetWorking connection) {
        this.connection = connection;
    }

    protected final Icq getIcq() {
        return connection.getIcq();
    }

    protected final IcqNetWorking getConnection() {
        return connection;
    }

    protected final void sendPacket(Packet packet) throws SawimException {
        connection.sendPacket(packet);
        active();
    }

    public abstract void init() throws SawimException;


    public abstract boolean forward(Packet packet) throws SawimException;
}



