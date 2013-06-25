package protocol.net;

import sawim.SawimException;

import javax.microedition.io.Connection;
import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public final class TcpSocket {
    private StreamConnection sc;
    private InputStream is;
    private OutputStream os;

    public TcpSocket() {
    }

    public void connectTo(String url) throws SawimException {
        try {
            sc = (StreamConnection)Connector.open(url, Connector.READ_WRITE);
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
            sc = (StreamConnection)Connector.open(url, Connector.READ);
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
    public void waitData() throws SawimException {
        while (0 == available()) {
            try {
                Thread.sleep(100);
            } catch (Exception ignored) {
            }
        }
    }
    public int read(byte[] data, int offset, int length) throws SawimException {
        try {
            length = Math.min(length, is.available());
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
                    waitData();
                }
                bReadSum += bRead;
            } while (bReadSum < data.length);

            return bReadSum;
        } catch (IOException e) {
            throw new SawimException(120, 1);
        }
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