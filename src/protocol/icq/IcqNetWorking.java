package protocol.icq;

import protocol.Protocol;
import protocol.icq.action.IcqAction;
import protocol.icq.packet.*;
import protocol.net.ClientConnection;
import protocol.net.TcpSocket;
import ru.sawim.SawimException;
import ru.sawim.comm.Util;


public final class IcqNetWorking extends ClientConnection {

    private byte[] flapHeader = new byte[6];
    private int nextIcqSequence;
    private TcpSocket socket;
    private Icq icq;
    private IcqNetState queue;
    private boolean icqConnected = false;

    private int flapSEQ = Util.nextRandInt() % 0x8000;
    private byte[] pingPacket = null;

    private int counter = 0;

    private static final int CHANNEL_CONNECT = 0x01;
    private static final int CHANNEL_SNAC = 0x02;
    private static final int CHANNEL_ERROR = 0x03;
    private static final int CHANNEL_DISCONNECT = 0x04;
    private static final int CHANNEL_PING = 0x05;

    public IcqNetWorking() {
    }

    public int getNextCounter() {
        return ++counter;
    }

    public final void processIcqException(SawimException e) {
        icq.processException(e);
    }

    public void initNet(Icq icq) {
        this.icq = icq;
        queue = new IcqNetState();
        queue.login(this);
    }

    public boolean isIcqConnected() {
        return icqConnected;
    }

    public void setIcqConnected() {
        icqConnected = true;
    }

    public void requestAction(IcqAction act) {
        queue.requestAction(act);
    }

    public Icq getIcq() {
        return icq;
    }

    protected Protocol getProtocol() {
        return icq;
    }

    public final void connectTo(String server, int port) throws SawimException {
        if (null != socket) {
            socket.close();
        }
        if (!isConnected()) {
            return;
        }
        socket = new TcpSocket();
        socket.connectTo(server, port);
    }

    private int getFlapSequence() {
        flapSEQ = (++flapSEQ) & 0x7FFF;
        return flapSEQ;
    }

    public void sendPacket(Packet packet) throws SawimException {
        if (packet instanceof ToIcqSrvPacket) {
            ((ToIcqSrvPacket) packet).setIcqSequence(nextIcqSequence++);
        }
        write(packet.toByteArray());
    }

    private void write(byte[] out) throws SawimException {
        Util.putWordBE(out, 2, getFlapSequence());
        socket.write(out);
        socket.flush();
    }

    private void readPacket(TcpSocket socket) throws SawimException {
        socket.readFully(flapHeader);


        if (0x2A != flapHeader[0]) {
            throw new SawimException(124, 0);
        }

        byte[] flapData = new byte[Util.getWordBE(flapHeader, 4)];
        socket.readFully(flapData);

        Packet packet = parse(Util.getByte(flapHeader, 1), flapData);
        flapData = null;
        if (null != packet) {

            int flapSequence = Util.getWordBE(flapHeader, 2);
            queue.processPacket(packet);
        }
    }

    protected void ping() throws SawimException {
        if (null != pingPacket) {
            write(pingPacket);
        }
    }

    public void initPing() {
        pingPacket = new byte[6];
        Util.putByte(pingPacket, 0, 0x2a);
        Util.putByte(pingPacket, 1, CHANNEL_PING);
        Util.putWordBE(pingPacket, 2, 0);
        Util.putWordBE(pingPacket, 4, 0);
    }

    protected void connect() throws SawimException {
        connect = true;
        nextIcqSequence = 0;
        queue.processActions();
    }

    protected boolean processPacket() throws SawimException {
        boolean action = queue.processActions();
        if ((null != socket) && (0 < socket.available())) {
            readPacket(socket);
            return true;
        }
        return action;
    }

    protected void closeSocket() {
        if (null != socket) {
            socket.close();
        }
    }

    private Packet parse(int channel, byte[] flapData) throws SawimException {
        try {
            switch (channel) {
                case CHANNEL_SNAC:
                    int family = Util.getWordBE(flapData, 0);
                    int command = Util.getWordBE(flapData, 2);
                    if (SnacPacket.OLD_ICQ_FAMILY == family) {
                        return (SnacPacket.SRV_FROMICQSRV_COMMAND == command)
                                ? FromIcqSrvPacket.parse(flapData) : null;
                    }
                    return SnacPacket.parse(family, command, flapData);

                case CHANNEL_CONNECT:
                    return ConnectPacket.parse(flapData);
                case CHANNEL_DISCONNECT:
                    return DisconnectPacket.parse(flapData);
            }
        } catch (SawimException e) {
            throw e;
        } catch (Exception e) {
            ru.sawim.modules.DebugLog.dump("broken packet " + channel, flapData);
        }
        return null;
    }

    public void disconnect() {
        icq = null;
        IcqNetState l = queue;
        queue = null;
        if (null != l) {
            l.disconnect();
        }
        connect = false;
    }
}