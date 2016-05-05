package protocol;

import protocol.net.TcpSocket;
import ru.sawim.modules.search.UserInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 23.06.13
 * Time: 15:17
 * To change this template use File | Settings | File Templates.
 */
public class GetAvatar implements Runnable {
    String url;
    UserInfo userInfo;

    public void getAvatar(UserInfo userInfo) {
        this.userInfo = userInfo;
        String uin = userInfo.uin;
        if (uin.endsWith("uin.icq")) {
            uin = uin.substring(0, uin.indexOf('@'));
        }
        if (-1 != uin.indexOf('@')) {
            String domain = uin.substring(uin.indexOf("@") + 1);
            String secondaryDomain = domain.substring(0, domain.indexOf('.'));
            String emailName = uin.substring(0, uin.indexOf("@"));
            url = "http://avt.foto.mail.ru/" + secondaryDomain + "/" + emailName + "/_avatar180";
        } else {
            url = "http://api.icq.net/expressions/get?f=native&type=buddyIcon&t=" + uin;
        }
        try {
            new Thread(this,"SetAvatar").start();
            Thread.sleep(10);
        } catch (Exception ignored) {
        }
    }

    private byte[] read(InputStream in, int length) throws IOException {
        if (0 == length) {
            return null;
        }
        if (0 < length) {
            byte[] bytes = new byte[length];
            int readCount = 0;
            while (readCount < bytes.length) {
                int c = in.read(bytes, readCount, bytes.length - readCount);
                if (-1 == c) break;
                readCount += c;
            }
            return bytes;
        }

        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int i = 0; i < 100 * 1024; ++i) {
            int ch = in.read();
            if (-1 == ch) break;
            bytes.write(ch);
        }
        byte[] content = bytes.toByteArray();
        bytes.close();
        return content;
    }

    private byte[] getAvatar() {
        HttpURLConnection httemp = null;
        InputStream istemp = null;
        try {
            httemp = (HttpURLConnection) new URL(url).openConnection();
            if (HttpURLConnection.HTTP_OK != httemp.getResponseCode()) {
                throw new IOException();
            }
            istemp = httemp.getInputStream();
            byte[] avatarBytes = read(istemp, httemp.getContentLength());
            return avatarBytes;
        } catch (Exception ignored) {
        }
        httemp.disconnect();
        TcpSocket.close(istemp);
        return null;
    }

    @Override
    public void run() {
        try {
            userInfo.setAvatar(getAvatar());
            if (null != userInfo.avatar) {
                userInfo.updateProfileView();
            }
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            userInfo.setAvatar(null);
        }
    }
}