package protocol.jabber;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;
import sawim.SawimException;
import sawim.modules.DebugLog;
import protocol.net.TcpSocket;


final class Socket {
    private TcpSocket socket = new TcpSocket();
    private boolean connected;
    private byte[] inputBuffer = new byte[1024];
    private int inputBufferLength = 0;
    public int inputBufferIndex = 0;
    
    private ZInputStream zin;
    private ZOutputStream zout;
    private boolean compressed;

    public Socket() {
    }
    
    public void activateStreamCompression() {
        zin = new ZInputStream(socket);
        zout = new ZOutputStream(socket, JZlib.Z_DEFAULT_COMPRESSION);
        zout.setFlushMode(JZlib.Z_SYNC_FLUSH);
        compressed = true;
        DebugLog.println("zlib is working");
    }

    public boolean isConnected() {
        return connected;
    }
    
    public void connectTo(String url) throws SawimException {
        System.out.println("url: " + url);
        socket.connectTo(url);
        connected = true;
    }

    private int read(byte[] data) throws SawimException {
        if (compressed) {
            int bRead = zin.read(data);
            if (-1 == bRead) {
                throw new SawimException(120, 13);
            }
            return bRead;
        }
        
        int length = Math.min(data.length, socket.available());
        if (0 == length) {
            return 0;
        }
        int bRead = socket.read(data, 0, length);
        if (-1 == bRead) {
            throw new SawimException(120, 12);
        }
        return bRead;
    }

    public void write(byte[] data) throws SawimException {
        
        if (compressed) {
            zout.write(data);
            zout.flush();
            return;
        }
        
        socket.write(data, 0, data.length);
        socket.flush();
    }
    public void close() {
        connected = false;
        try {
            zin.close();
            zout.close();
        } catch (Exception ex) {
        }
        socket.close();
        inputBufferLength = 0;
        inputBufferIndex = 0;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ex) {
        }
    }
    
    private byte readByte() throws SawimException {
        if (inputBufferIndex >= inputBufferLength) {
            inputBufferIndex = 0;
            inputBufferLength = read(inputBuffer);
            while (0 == inputBufferLength) {
                sleep(100);
                inputBufferLength = read(inputBuffer);
            }
        }
        return inputBuffer[inputBufferIndex++];
    }
    public int available() throws SawimException {
        if (inputBufferIndex < inputBufferLength) {
            return (inputBufferLength - inputBufferIndex);
        }
        return socket.available();
    }

    char readChar() throws SawimException {
        try {
            byte bt = readByte();
            if (0 <= bt) {
                return (char)bt;
            }
            if ((bt & 0xE0) == 0xC0) {
                byte bt2 = readByte();
                return (char)(((bt & 0x3F) << 6) | (bt2 & 0x3F));

            } else if ((bt & 0xF0) == 0xE0) {
                byte bt2 = readByte();
                byte bt3 = readByte();
                return (char)(((bt & 0x1F) << 12) | ((bt2 & 0x3F) << 6) | (bt3 & 0x3F));

            } else {
                int seqLen = 0;
                if ((bt & 0xF8) == 0xF0) seqLen = 3;
                else if ((bt & 0xFC) == 0xF8) seqLen = 4;
                else if ((bt & 0xFE) == 0xFC) seqLen = 5;
                for (; 0 < seqLen; --seqLen) {
                    bt = readByte();
                }
                return '?';
            }
        } catch (SawimException e) {
            
            DebugLog.panic("readChar je ", e);
            
            throw e;
        } catch (Exception e) {
            
            DebugLog.panic("readChar e ", e);
            
            throw new SawimException(120, 7);
        }
    }
}