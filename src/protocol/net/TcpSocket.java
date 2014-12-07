package protocol.net;

import ru.sawim.SawimException;
import ru.sawim.modules.DebugLog;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

public final class TcpSocket {
    private Socket socket;
    private InputStream is;
    private OutputStream os;

    public TcpSocket() {
    }

    public void connectTo(String host, int port) throws SawimException {
        try {
            socket = new Socket(host, port);
            socket.setTcpNoDelay(true);
            //socket.setSocketOption(SocketConnection.KEEPALIVE, 2*60);
            //socket.setSocketOption(SocketConnection.LINGER, 0);
            //socket.setSocketOption(SocketConnection.RCVBUF, 10*1024);
            //socket.setSocketOption(SocketConnection.SNDBUF, 10*1024);
            os = socket.getOutputStream();
            is = socket.getInputStream();
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

    public void connectForReadingTo(String host, int port) throws SawimException {
        try {
            socket = new Socket(host, port);
            is = socket.getInputStream();
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

    public InputStream getIs() {
        return is;
    }

    public OutputStream getOs() {
        return os;
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
            if (os != null) {
                os.write(data, offset, length);
            }
        } catch (IOException e) {
            ru.sawim.modules.DebugLog.panic("write", e);
            throw new SawimException(120, 2);
        }
    }

    public void flush() throws SawimException {
        try {
            if (os != null) {
                os.flush();
            }
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

    public void startTls(SSLContext sc, String host) {
        try {
            DebugLog.println("startTls start " + socket + os + is);
	        SSLSocketFactory sslFactory = (SSLSocketFactory)sc.getSocketFactory();
            socket = sslFactory.createSocket(socket, host, socket.getPort(), true);
            ((SSLSocket) socket).setUseClientMode(true);
            dump();

            is = socket.getInputStream();
            os = socket.getOutputStream();
            DebugLog.println("startTls done " + socket + os + is);
        } catch (Exception e) {
            e.printStackTrace();
            DebugLog.panic("startTls error", e);
        }
    }

    private void dump() throws SSLPeerUnverifiedException {
        SSLSession session = ((SSLSocket) socket).getSession();
        java.security.cert.Certificate[] cchain = session.getPeerCertificates();
        System.out.println("The Certificates used by peer");
        for (int i = 0; i < cchain.length; i++) {
            System.out.println(((X509Certificate) cchain[i]).getSubjectDN());
        }
        System.out.println("Peer host is " + session.getPeerHost());
        System.out.println("Cipher is " + session.getCipherSuite());
        System.out.println("Protocol is " + session.getProtocol());
        System.out.println("ID is " + session.getId().length);
        System.out.println("Session created in " + session.getCreationTime());
        System.out.println("Session accessed in " + session.getLastAccessedTime());
        System.out.println("Session valid in " + session.isValid());
    }

    public void close() {
        if (is != null) {
            close(is);
            is = null;
        }
        if (os != null) {
            close(os);
            os = null;
        }
        if (socket != null) {
            close(socket);
            socket = null;
        }
    }

    public static void close(Socket s) {
        try {
            s.close();
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