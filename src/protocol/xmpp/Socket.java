/*
 * Socket.java
 *
 * Created on 4 Февраль 2009 г., 15:11
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package protocol.xmpp;

import protocol.net.TcpSocket;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.zlib.ZLibInputStream;
import ru.sawim.modules.zlib.ZLibOutputStream;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Vladimir Krukov
 */
final class Socket implements Runnable {
    private TcpSocket socket = new TcpSocket();
    private volatile boolean connected;
    private byte[] inputBuffer = new byte[1024];
    private int inputBufferLength = 0;
    public int inputBufferIndex = 0;
    private ZLibInputStream zIn;
    private ZLibOutputStream zOut;
    private boolean compressed;
    private boolean secured;
    private final LinkedBlockingQueue<Object> read = new LinkedBlockingQueue<>();

    public Socket() {
    }

    public void startCompression() throws IOException, NoSuchAlgorithmException {
        zIn = new ZLibInputStream(socket.getIs());
        zOut = new ZLibOutputStream(socket.getOs());
        compressed = true;
        DebugLog.println("zlib is working");
    }

    public void startTls(SSLContext sslctx, String host) {
        socket.startTls(sslctx, host);
        secured = true;
    }

    public boolean isConnected() {
        return connected;
    }

    public void connectTo(String host, int port) throws SawimException {
        System.out.println("host: " + host);
        socket.connectTo(host, port);
        connected = true;
    }

    private int read(byte[] data) throws SawimException {
        if (compressed) {
            int bRead = 0;
            try {
                bRead = zIn.read(data);
            } catch (IOException ignored) {
            }
            if (-1 == bRead) {
                throw new SawimException(120, 13);
            }
            return bRead;
        }
        int bRead = socket.read(data, 0, data.length);
        if (-1 == bRead) {
            throw new SawimException(120, 12);
        }
        return bRead;
    }

    public void write(byte[] data) throws SawimException, IOException {
        if (compressed) {
            zOut.write(data);
            zOut.flush();
            return;
        }
        socket.write(data, 0, data.length);
        socket.flush();
    }

    public void close() {
        connected = false;
        try {
            zIn.close();
            zOut.close();
        } catch (Exception ignored) {
        }
        socket.close();
        inputBufferLength = 0;
        inputBufferIndex = 0;
    }

    private void fillBuffer() throws SawimException {
        inputBufferIndex = 0;
        inputBufferLength = read(inputBuffer);
        while (0 == inputBufferLength) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            inputBufferLength = read(inputBuffer);
        }
    }

    private byte readByte() throws SawimException {
        if (inputBufferIndex >= inputBufferLength) {
            fillBuffer();
        }
        return inputBuffer[inputBufferIndex++];
    }

    char readChar() throws SawimException {
        try {
            return (char) (readByte() & 0xff);
        } catch (SawimException e) {
            DebugLog.panic("readChar se ", e);
            throw e;
        } catch (Exception e) {
            DebugLog.panic("readChar e ", e);
            throw new SawimException(120, 7);
        }
    }

    boolean isSecured() {
        return secured;
    }

    private Object readObject() throws SawimException {
        Object readObject = null;
        while (connected && null == readObject) {
            readObject = XmlNode.parse(this);
            //if (null == readObject) sleep(100);
        }
        return readObject;
    }

    @Override
    public void run() {
        Object readObject;
        while (connected) {
            try {
                readObject = readObject();
            } catch (SawimException e) {
                connected = false;
                readObject = e;
            }
            if (null != readObject) {
                read.add(readObject);
            }
        }
    }

    public XmlNode readNode(boolean wait) throws SawimException {
        Object readObject = null;
        if (wait) {
            readObject = readObject();
        } else {
            if (0 < read.size()) {
                try {
                    readObject = read.take();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        if (readObject instanceof SawimException) throw (SawimException) readObject;
        return (XmlNode) readObject;
    }

    public void start() {
        SawimApplication.getExecutor().execute(this);
    }
}