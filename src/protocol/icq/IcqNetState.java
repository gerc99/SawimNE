


package protocol.icq;

import protocol.icq.action.IcqAction;
import protocol.icq.packet.Packet;
import protocol.icq.packet.SnacPacket;
import ru.sawim.SawimException;
import ru.sawim.modules.DebugLog;

import java.util.Vector;


class IcqNetState {

    private final Vector actActions = new Vector();
    private final Vector reqAction = new Vector();
    private IcqNetDefActions defActionListener;
    private IcqNetWorking connection;

    public IcqNetState() {
    }

    public void login(IcqNetWorking con) {
        connection = con;
        defActionListener = new IcqNetDefActions(connection);
    }

    public void disconnect() {
        connection = null;
    }


    public void requestAction(IcqAction act) {

        act.setConnection(connection);
        synchronized (reqAction) {
            cleanActions();
            reqAction.addElement(act);
        }
    }

    void processPacket(Packet packet) throws SawimException {

        if (null == packet) {
            DebugLog.println("packet is null");
            return;
        }


        for (int i = 0; i < actActions.size(); ++i) {
            IcqAction act = (IcqAction) actActions.elementAt(i);
            if (act.isError() || act.isCompleted()) {
                continue;
            }
            try {
                if (act.forward(packet)) {
                    if (act.isCompleted() || act.isError()) {
                        actActions.removeElement(act);
                    }
                    return;
                }
            } catch (SawimException e) {
                throw e;
            } catch (Exception e) {

                DebugLog.panic("Icq action error", e);
                if (packet instanceof SnacPacket) {
                    SnacPacket snacPacket = (SnacPacket) packet;
                    DebugLog.println(actActions.elementAt(i).getClass().toString());
                    DebugLog.println("family = 0x" + Integer.toHexString(snacPacket.getFamily())
                            + " command = 0x" + Integer.toHexString(snacPacket.getCommand()));
                }

            }
        }
        try {
            defActionListener.forward(packet);
        } catch (SawimException e) {
            throw e;
        } catch (Exception e) {

            DebugLog.panic("Icq listener error", e);
            if (packet instanceof SnacPacket) {
                SnacPacket snacPacket = (SnacPacket) packet;
                DebugLog.println("family = 0x" + Integer.toHexString(snacPacket.getFamily())
                        + " command = 0x" + Integer.toHexString(snacPacket.getCommand()));
            }

        }

    }

    private IcqAction getNewAction() {
        IcqAction newAction = null;
        synchronized (reqAction) {
            if (0 < reqAction.size()) {
                newAction = (IcqAction) reqAction.elementAt(0);
                reqAction.removeElementAt(0);
            }
        }
        return newAction;
    }

    public boolean processActions() {
        IcqAction newAction = getNewAction();
        if (null == newAction) {
            return false;
        }


        try {
            newAction.init();
        } catch (SawimException e) {

            connection.getIcq().processException(e);
        } catch (Exception e) {

            DebugLog.panic("newAction.init()", e);

        }
        if (!newAction.isCompleted() && !newAction.isError()) {
            actActions.addElement(newAction);
        }
        return true;
    }

    private boolean cleanActions() {

        for (int i = actActions.size() - 1; i >= 0; --i) {
            IcqAction act = (IcqAction) actActions.elementAt(i);
            if (act.isCompleted() || act.isError()) {
                actActions.removeElementAt(i);
            }
        }
        return false;
    }
}


