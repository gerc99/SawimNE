package ru.sawim.io;

import protocol.net.TcpSocket;
import ru.sawim.SawimException;
import ru.sawim.comm.StringConvertor;

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
    public static FileSystem getFile(String file) {
        FileSystem fs = FileSystem.getInstance();
        String home = fs.getCardDir().getAbsolutePath() + FileSystem.getSawimHome();
        try {
            fs.openFile(home + file);
        } catch (SawimException ignored) {
        }
        return fs;
    }

    public static String getContent(String file) {
        FileSystem fs = getFile(file);
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
        FileSystem fs = getFile(file);
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
        FileSystem fs = getFile(file);
        boolean exist = fs.exists();
        fs.close();
        return exist;
    }

    public static void init() {
        try {
            String home = FileSystem.getSawimHome();
            FileSystem fs = FileSystem.getInstance();
            fs.mkdir(home.substring(0, home.length() - 1));
            fs.mkdir(home + FileSystem.HISTORY);
            fs.mkdir(home + FileSystem.RES);
        } catch (Exception ignored) {
        }
    }
}
