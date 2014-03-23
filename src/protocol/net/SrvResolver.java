/*******************************************************************************
 Jimm Is Mobile Messenger
 Copyright (C) 2003  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the  Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 Author: Vladimir Kryukov
 *******************************************************************************/
package protocol.net;

import ru.sawim.comm.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * @author Vladimir Kryukov
 */
public final class SrvResolver {
    private static final String server = "8.8.8.8";
    private TcpSocket socket = new TcpSocket();

    /**
     * Creates a new instance of SrvResolver
     */
    public SrvResolver() {
    }

    private byte[] packet(String[] domain) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x12);
        out.write(0x10); // id
        out.write(1);
        out.write(0); // flags
        out.write(0);
        out.write(1); // queries count
        out.write(0);
        out.write(0); // resources count
        out.write(0);
        out.write(0); // answers count
        out.write(0);
        out.write(0); // additions count
        for (String domainPart : domain) {
            byte[] l = domainPart.getBytes();
            out.write(l.length);
            out.write(l);
        }
        out.write(0);
        out.write(0);
        out.write(33); // type: SRV
        out.write(0);
        out.write(1); // class: Internet
        return out.toByteArray();
    }

    private String read(byte[] data) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        short id = in.readShort(); // id
        short flags = in.readShort(); // flags
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
            in.readUnsignedShort(); // ...
            in.readUnsignedShort(); // type
            in.readUnsignedShort(); // class
            in.readInt(); // ttl
            int rdlength = in.readUnsignedShort(); // length

            in.readUnsignedShort();
            in.readUnsignedShort();
            int port = in.readUnsignedShort(); // port
            StringBuilder result = new StringBuilder();
            while (true) {
                int length = in.readUnsignedByte();
                if (0 == length) break;
                for (int j = 0; j < length; ++j) {
                    result.append((char) in.readUnsignedByte());
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
            socket.connectTo(server, 53);
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
        } catch (Exception ignored) {
        }
        return null;
    }

    public void close() {
        socket.close();
    }
}
