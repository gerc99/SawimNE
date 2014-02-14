
package sawim.modules.fs;

import protocol.net.TcpSocket;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public abstract class FileSystem {

    public static final String HISTORY = "history";
    public static final String RES = "res";

    public static JSR75FileSystem getInstance() {
        return new JSR75FileSystem();
    }

    public static String getSawimHome() {
        return "/e:/sawimne/";
    }

    public static InputStream openSawimFile(String file) {
        JSR75FileSystem fs = FileSystem.getInstance();
        byte[] buffer = null;
        try {
            fs.openFile(getSawimHome() + RES + "/" + file);
            InputStream in = fs.openInputStream();
            buffer = new byte[in.available()];
            in.read(buffer);
            TcpSocket.close(in);
        } catch (Exception e) {
            buffer = null;
        }

        fs.close();
        if (null != buffer) {
            return new ByteArrayInputStream(buffer);
        }
        return null;
    }

}


