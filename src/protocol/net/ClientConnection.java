package protocol.net;

import ru.sawim.General;
import sawim.SawimException;
import sawim.chat.message.PlainMessage;
import sawim.comm.StringConvertor;
import sawim.modules.DebugLog;
import protocol.Protocol;

import java.util.Vector;


public abstract class ClientConnection implements Runnable {
    private long keepAliveInterv;
    private boolean usePong;
    protected boolean connect;
    private Vector messages = new Vector();

    private long nextPingTime;
    private long pongTime;

    private static final int PING_INTERVAL = 90 ;
    private static final int PONG_TIMEOUT = 4 * 60 ;

    protected final void setPingInterval(long interval) {
        keepAliveInterv = Math.min(keepAliveInterv, interval);
        nextPingTime = General.getCurrentGmtTime() + keepAliveInterv;
    }
    protected final long getPingInterval() {
        return keepAliveInterv;
    }
    protected final void usePong() {
        usePong = true;
        updateTimeout();
    }

    private void initPingValues() {
        usePong = false;
        keepAliveInterv = PING_INTERVAL;
        nextPingTime = General.getCurrentGmtTime() + keepAliveInterv;
    }
    public final void start() {
        new Thread(this).start();
    }
    public final void run() {
        initPingValues();
        SawimException exception = null;
        try {
            connect();
            while (isConnected()) {
                boolean doing = processPacket();
                if (!doing) {
                    sleep(250);
                    doPingIfNeeeded();
                }
            }
        } catch (SawimException e) {
            exception = e;
        } catch (OutOfMemoryError err) {
            exception = new SawimException(100, 2);
        } catch (Exception ex) {
            if (null != getProtocol()) {
                DebugLog.panic("die " + getId(), ex);
            }
            exception = new SawimException(100, 1);
        }
        if (null != exception) {
            try {
                Protocol p = getProtocol();
                if (null != p) {
                    p.processException(exception);
                }
            } catch (Exception ex) {
                DebugLog.panic("die2 " + getId(), ex);
            }
        }
        disconnect();
        try {
            closeSocket();
        } catch (Exception e) {
            DebugLog.panic("die3 " + getId(), e);
        }
        connect = false;
    }
    
    private String getId() {
        Protocol p = getProtocol();
        if (null != p) {
            return p.getUserId();
        }
        return "" + this;
    }
    
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {
        }
    }

    private void doPingIfNeeeded() throws SawimException {
        long now = General.getCurrentGmtTime();
        if (usePong && (pongTime + PONG_TIMEOUT < now)) {
            throw new SawimException(120, 9);
        }
        if (nextPingTime <= now) {
            if (usePong) {
                pingForPong();

            } else {
                ping();
            }
            nextPingTime = now + keepAliveInterv;
        }
    }
    protected final void updateTimeout() {
        pongTime = General.getCurrentGmtTime();
    }
    public final boolean isConnected() {
        return connect;
    }

    public final void addMessage(PlainMessage msg) {
        messages.addElement(msg);
        markMessageSended(-1, -1);
    }
    public final boolean isMessageExist(long msgId) {
        if (-1 < msgId) {
            PlainMessage msg = null;
            for (int i = 0; i < messages.size(); ++i) {
                PlainMessage m = (PlainMessage)messages.elementAt(i);
                if (m.getMessageId() == msgId) {
                    return true;
                }
            }
        }
        return false;
    }
    public final void markMessageSended(long msgId, int status) {
        PlainMessage msg = null;
        for (int i = 0; i < messages.size(); ++i) {
            PlainMessage m = (PlainMessage)messages.elementAt(i);
            if (m.getMessageId() == msgId) {
                msg = m;
                break;
            }
        }
        if (null != msg) {
            msg.setSendingState(status);
            if (PlainMessage.NOTIFY_FROM_CLIENT == status) {
                messages.removeElement(msg);
            }
        }
        long date = General.getCurrentGmtTime() - 5 * 60;
        for (int i = messages.size() - 1; i >= 0; --i) {
            PlainMessage m = (PlainMessage)messages.elementAt(i);
            if (date > m.getNewDate()) {
                messages.removeElement(m);
            }
        }
    }

    protected void pingForPong() throws SawimException {
    }
    public abstract void disconnect();
    protected abstract Protocol getProtocol();
    protected abstract void closeSocket();
    protected abstract void connect() throws SawimException;
    protected abstract void ping() throws SawimException;
    protected abstract boolean processPacket() throws SawimException;
}