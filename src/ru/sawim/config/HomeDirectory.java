package ru.sawim.config;

import protocol.net.TcpSocket;
import sawim.SawimException;
import sawim.comm.StringConvertor;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 25.12.12 19:05
 *
 * @author vladimir
 */
public class HomeDirectory {
    public static sawim.modules.fs.JSR75FileSystem getFile(String file) {
        sawim.modules.fs.JSR75FileSystem fs = sawim.modules.fs.FileSystem.getInstance();
        String home = sawim.modules.fs.FileSystem.getSawimHome();
        try {
            fs.openFile(home + file);
        } catch (SawimException ignored) {
        }
        return fs;
    }

    public static String getContent(String file) {
        sawim.modules.fs.JSR75FileSystem fs = getFile(file);
        String result = null;
        if (fs.exists()) {
            InputStream stream = null;
            try {
                stream = fs.openInputStream();
                byte[] str = new byte[stream.available()];
                stream.read(str);
                result = StringConvertor.utf8beByteArrayToString(str, 0, str.length);
            } catch (Exception ignored) {
            }
            TcpSocket.close(stream);
        }
        fs.close();
        return result;
    }

    public static void putContent(String file, String content) {
        sawim.modules.fs.JSR75FileSystem fs = getFile(file);
        OutputStream stream = null;
        try {
            stream = fs.openOutputStream();
            stream.write(StringConvertor.stringToByteArrayUtf8(content));
        } catch (Exception ignored) {
        }
        try {
            stream.close();
        } catch (Exception ignored) {
        }
        fs.close();
    }

    public static boolean exist(String file) {
        sawim.modules.fs.JSR75FileSystem fs = getFile(file);
        boolean exist = fs.exists();
        fs.close();
        return exist;
    }

    public static void init() {
        try {
            String home = sawim.modules.fs.FileSystem.getSawimHome();
            sawim.modules.fs.JSR75FileSystem fs = sawim.modules.fs.FileSystem.getInstance();
            fs.mkdir(home.substring(0, home.length() - 1));
            fs.mkdir(home + sawim.modules.fs.FileSystem.HISTORY);
            fs.mkdir(home + sawim.modules.fs.FileSystem.RES);
        } catch (Exception ignored) {
        }
    }
}
