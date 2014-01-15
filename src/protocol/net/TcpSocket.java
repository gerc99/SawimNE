package protocol.net;

import sawim.SawimException;
import sawim.modules.DebugLog;

import javax.microedition.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class TcpSocket {
    private StreamConnection sc;
    private InputStream is;
    private OutputStream os;

    public TcpSocket() {
    }

    public void connectTo(String host, int port) throws SawimException {
        connectTo("socket://" + host + ":" + port);
    }

    public void connectTo(String url) throws SawimException {
        try {
            sc = (StreamConnection) Connector.open(url, Connector.READ_WRITE);
            SocketConnection socket = (SocketConnection) sc;
            socket.setSocketOption(SocketConnection.DELAY, 0);
            //socket.setSocketOption(SocketConnection.KEEPALIVE, 2*60);
            //socket.setSocketOption(SocketConnection.LINGER, 0);
            //socket.setSocketOption(SocketConnection.RCVBUF, 10*1024);
            //socket.setSocketOption(SocketConnection.SNDBUF, 10*1024);
            os = sc.openOutputStream();
            is = sc.openInputStream();
        } catch (ConnectionNotFoundException e) {
            throw new SawimException(121, 0);
        } catch (IllegalArgumentException e) {
            throw new SawimException(122, 0);
        } catch (SecurityException e) {
            throw new SawimException(123, 9);
        } catch (IOException e) {
            throw new SawimException(120, 0);
        } catch (Exception e) {
            throw new SawimException(120, 10);
        }
    }

    public void connectForReadingTo(String url) throws SawimException {
        try {
            sc = (StreamConnection) Connector.open(url, Connector.READ);
            is = sc.openInputStream();
        } catch (ConnectionNotFoundException e) {
            throw new SawimException(121, 0);
        } catch (IllegalArgumentException e) {
            throw new SawimException(122, 0);
        } catch (SecurityException e) {
            throw new SawimException(123, 9);
        } catch (IOException e) {
            throw new SawimException(120, 0);
        } catch (Exception e) {
            throw new SawimException(120, 10);
        }
    }

    public final int read() throws SawimException {
        try {
            return is.read();
        } catch (Exception e) {
            throw new SawimException(120, 4);
        }
    }

    public int read(byte[] data, int offset, int length) throws SawimException {
        try {
            if (0 == length) {
                return 0;
            }
            int bRead = is.read(data, offset, length);
            if (-1 == bRead) {
                throw new IOException("EOF");
            }
            return bRead;

        } catch (IOException e) {
            throw new SawimException(120, 1);
        }
    }

    public final int readFully(byte[] data) throws SawimException {
        if ((null == data) || (0 == data.length)) {
            return 0;
        }
        try {
            int bReadSum = 0;
            do {
                int bRead = is.read(data, bReadSum, data.length - bReadSum);
                if (-1 == bRead) {
                    throw new IOException("EOF");
                } else if (0 == bRead) {
                    try {
                        Thread.sleep(100);
                    } catch (Exception ignored) {
                    }
                }
                bReadSum += bRead;
            } while (bReadSum < data.length);
            return bReadSum;
        } catch (IOException e) {
            throw new SawimException(120, 1);
        }
    }

    public static int readFully(InputStream in, byte[] data, final int offset, final int length) throws IOException {
        if ((null == data) || (0 == data.length)) {
            return 0;
        }
        int bReadSum = 0;
        do {
            int bRead = in.read(data, offset + bReadSum, length - bReadSum);
            if (-1 == bRead) {
                throw new IOException("EOF");
            } else if (0 == bRead) {
                try {
                    Thread.sleep(100);
                } catch (Exception ignored) {
                }
            }
            bReadSum += bRead;
        } while (bReadSum < length);
        return bReadSum;
    }

    public final void write(byte[] data) throws SawimException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length) throws SawimException {
        try {
            os.write(data, offset, length);
        } catch (IOException e) {
            sawim.modules.DebugLog.panic("write", e);
            throw new SawimException(120, 2);
        }
    }

    public void flush() throws SawimException {
        try {
            os.flush();
        } catch (IOException e) {
            throw new SawimException(120, 2);
        }
    }

    public int available() throws SawimException {
        try {
            return is.available();
        } catch (IOException ex) {

            throw new SawimException(120, 3);
        }
    }

    public void startTls(String host) {
        try {
            DebugLog.println("startTls start " + sc + os + is);
            ((org.microemu.cldc.socket.SocketConnection) sc).startTls(host);
            is = sc.openInputStream();
            os = sc.openOutputStream();
            DebugLog.println("startTls done " + sc + os + is);
        } catch (Exception e) {
            DebugLog.panic("startTls error", e);
        }
    }

    public void close() {
        close(is);
        close(os);
        close(sc);
    }

    public static void close(Connection c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    public static void close(InputStream c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }

    public static void close(OutputStream c) {
        try {
            c.close();
        } catch (Exception ignored) {
        }
    }
}