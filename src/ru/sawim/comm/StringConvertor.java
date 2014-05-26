package ru.sawim.comm;

import ru.sawim.modules.DebugLog;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;


public final class StringConvertor {

    public static final char DELEMITER = ' ';

    static public String bytesToSizeString(int v, boolean force) {
        int size = v;
        if (v < 1024 || force) {
            return size + " B";
        }
        size = (v + 512) / 1024;
        v /= 1024;
        if (v < 1024 * 10) {
            return size + " KiB";
        }
        size = (v + 512) / 1024;
        v /= 1024;
        return size + " MiB";
    }

    public static String utf8beByteArrayToString(byte[] buf, int off, int len) {
        try {
            if ((0 < len) && (0x00 == buf[off + len - 1])) {
                len--;
            }
            if (0 == len) {
                return "";
            }
            byte[] buf2 = new byte[len + 2];
            Util.putWordBE(buf2, 0, len);
            System.arraycopy(buf, off, buf2, 2, len);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf2);
            DataInputStream dis = new DataInputStream(bais);
            return removeCr(dis.readUTF());
        } catch (Exception e) {
        }
        return "";
    }

    public static String removeCr(String val) {
        if (val.indexOf('\r') < 0) {
            return val;
        }
        if (-1 == val.indexOf('\n')) {
            return val.replace('\r', '\n');
        }

        StringBuilder result = new StringBuilder();
        int size = val.length();
        for (int i = 0; i < size; ++i) {
            char chr = val.charAt(i);
            if ((chr == 0) || (chr == '\r')) continue;
            result.append(chr);
        }
        return result.toString();
    }

    public static String restoreCrLf(String val) {
        StringBuilder result = new StringBuilder();
        int size = val.length();
        for (int i = 0; i < size; ++i) {
            char chr = val.charAt(i);
            if (chr == '\r') continue;
            if (chr == '\n') {
                result.append("\r\n");
            } else {
                result.append(chr);
            }
        }
        return result.toString();
    }

    public static String trim(String msg) {
        if (StringConvertor.isEmpty(msg)) {
            return "";
        }
        return msg.trim();
    }

    public static byte[] stringToByteArrayUtf8(String val) {
        try {
            return val.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            DebugLog.panic("Unsupported write Encoding", e);
        }
        return null;
    }

    public static boolean isEmpty(String value) {
        return (null == value) || (0 == value.length());
    }

    public static String notNull(String value) {
        return (null == value) ? "" : value;
    }
}

