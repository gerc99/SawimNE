package protocol.net;

import sawim.comm.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;


public final class SrvResolver {
    private String server;
    private TcpSocket socket = new TcpSocket();
    
    
    public SrvResolver() {
        server = "8.8.8.8";
    }

    private byte[] packet(String[] domain) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x12); out.write(0x10); 
        out.write(1); out.write(0); 
        out.write(0); out.write(1); 
        out.write(0); out.write(0); 
        out.write(0); out.write(0); 
        out.write(0); out.write(0); 
        for (int i = 0; i < domain.length; ++i) {
            byte[] l = domain[i].getBytes();
            out.write(l.length);
            out.write(l);
        }
        out.write(0);
        out.write(0);out.write(33); 
        out.write(0);out.write(1); 
        return out.toByteArray();
    }
    private String read(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        short id = in.readShort(); 
        short flags = in.readShort(); 
        int questions = in.readShort();
        int answers = in.readShort();
        in.readShort();
        in.readShort();
        for (int i = 0; i < questions; ++i) {
            while (true) {
                int length = in.readUnsignedByte();
                if (0 == length) break;
                for (int j = 0; j < length; ++j) {
                    in.readUnsignedByte();
                }
            }
            in.readShort();
            in.readShort();
        }
        for (int i = 0; i < answers; ++i) {
            in.readUnsignedShort(); 
            in.readUnsignedShort(); 
            in.readUnsignedShort(); 
            in.readInt(); 
            int rdlength = in.readUnsignedShort(); 
            
            in.readUnsignedShort();
            in.readUnsignedShort();
            int port = in.readUnsignedShort(); 
            StringBuffer result = new StringBuffer();
            while (true) {
                int length = in.readUnsignedByte();
                if (0 == length) break;
                for (int j = 0; j < length; ++j) {
                    result.append((char)in.readUnsignedByte());
                }
                result.append('.');
            }
            if (443 == port) {
                port = 5222;
            }
            return result.toString().substring(0, result.length() - 1) + ":" + port;
        }
        return null;
    }

    public String getXmpp(String domain) {
        return get("_xmpp-client._tcp." + domain);
    }
    public String get(String domain) {
        return get(Util.explode(domain, '.'));
    }
    private String get(String[] domain) {
        try {
            return sendTcp(packet(domain));
        } catch (IOException ex) {
            return null;
        }
    }

    private String sendTcp(byte[] message) {
        try {
            socket.connectTo("socket://" + server + ":53");
            byte[] packet = new byte[2 + message.length];
            System.arraycopy(message, 0, packet, 2, message.length);
            Util.putWordBE(packet, 0, message.length);
            socket.write(packet);
            socket.flush();
            
            byte[] header = new byte[2];
            socket.readFully(header);
            byte[] data = new byte[Util.getWordBE(header, 0)];
            socket.readFully(data);
            return read(data);
        } catch (Exception e) {
        }
        return null;
    }
    public void close() {
        socket.close();
    }
}

