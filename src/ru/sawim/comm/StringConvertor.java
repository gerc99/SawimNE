package ru.sawim.comm;

import ru.sawim.modules.DebugLog;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.UnsupportedEncodingException;


public final class StringConvertor {

    public static final char DELEMITER = ' ';

    private static boolean systemWin1251 = true;

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

    public static String byteArrayToHexString(byte[] buf) {
        StringBuilder hexString = new StringBuilder(buf.length * 2);
        for (int i = 0; i < buf.length; ++i) {
            String hex = Integer.toHexString(buf[i] & 0x00FF);
            if (hex.length() < 2) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static boolean isDataUCS2(byte[] array, int start, int lenght) {
        if ((lenght & 1) != 0) {
            return false;
        }
        int end = start + lenght;
        byte b;
        boolean result = true;
        for (int i = start; i < end; i += 2) {
            b = array[i];
            if (0 < b && b < 0x09) {
                return true;
            }
            if (0x00 == b && array[i + 1] != 0) {
                return true;
            }
            if (0x20 == b && array[i + 1] >= 0x20) {
                result = false;
            }
            if (0x20 < b || b < 0x00) {
                result = false;
            }
        }
        return result;
    }


    public static boolean isDataUTF8(byte[] array, int start, int length) {
        if (0 == length) {
            return false;
        }

        for (int i = start, len = length; len > 0; ) {
            byte bt = array[i++];
            len--;

            int seqLen = 0;
            if ((bt & 0xE0) == 0xC0) seqLen = 1;
            else if ((bt & 0xF0) == 0xE0) seqLen = 2;
            else if ((bt & 0xF8) == 0xF0) seqLen = 3;
            else if ((bt & 0xFC) == 0xF8) seqLen = 4;
            else if ((bt & 0xFE) == 0xFC) seqLen = 5;

            if (0 == seqLen) {
                if ((bt & 0x80) == 0x80) {
                    return false;
                }
                continue;
            }

            for (int j = 0; j < seqLen; ++j) {
                if (len == 0) return false;
                bt = array[i++];
                if ((bt & 0xC0) != 0x80) return false;
                len--;
            }
        }
        return true;
    }

    public static byte[] stringToByteArray1251(String s) {
        if (null == s) {
            return new byte[0];
        }
        if (systemWin1251) {
            try {
                return s.getBytes("Windows-1251");
            } catch (Exception e) {
                systemWin1251 = false;
            }
        }
        byte buf[] = new byte[s.length()];
        int size = s.length();
        for (int i = 0; i < size; ++i) {
            char ch = s.charAt(i);
            switch (ch) {
                case 1025:
                    buf[i] = -88;
                    break;
                case 1105:
                    buf[i] = -72;
                    break;
                case 1168:
                    buf[i] = -91;
                    break;
                case 1028:
                    buf[i] = -86;
                    break;
                case 1031:
                    buf[i] = -81;
                    break;
                case 1030:
                    buf[i] = -78;
                    break;
                case 1110:
                    buf[i] = -77;
                    break;
                case 1169:
                    buf[i] = -76;
                    break;
                case 1108:
                    buf[i] = -70;
                    break;
                case 1111:
                    buf[i] = -65;
                    break;
                default:
                    if (ch >= '\u0410' && ch <= '\u044F') {
                        buf[i] = (byte) ((ch - 1040) + 192);
                    } else {
                        buf[i] = (byte) ((int) ch & 0xFF);
                    }
                    break;
            }
        }
        return buf;
    }

    public static String byteArray1251ToString(byte buf[], int pos, int len) {
        if (systemWin1251) {
            try {
                return new String(buf, pos, len, "Windows-1251");
            } catch (Exception e) {
                systemWin1251 = false;
            }
        }
        int end = pos + len;
        StringBuilder stringbuffer = new StringBuilder(len);
        for (int i = pos; i < end; ++i) {
            int ch = buf[i] & 0xff;
            switch (ch) {
                case 168:
                    stringbuffer.append('\u0401');
                    break;
                case 184:
                    stringbuffer.append('\u0451');
                    break;
                case 165:
                    stringbuffer.append('\u0490');
                    break;
                case 170:
                    stringbuffer.append('\u0404');
                    break;
                case 175:
                    stringbuffer.append('\u0407');
                    break;
                case 178:
                    stringbuffer.append('\u0406');
                    break;
                case 179:
                    stringbuffer.append('\u0456');
                    break;
                case 180:
                    stringbuffer.append('\u0491');
                    break;
                case 186:
                    stringbuffer.append('\u0454');
                    break;
                case 191:
                    stringbuffer.append('\u0457');
                    break;
                default:
                    try {
                        if (ch >= 192 && ch <= 255) {
                            stringbuffer.append((char) ((1040 + ch) - 192));
                        } else {
                            stringbuffer.append((char) ch);
                        }
                    } catch (Exception e) {
                    }
                    break;
            }
        }
        return removeCr(stringbuffer.toString());
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

    public static byte[] stringToUcs2beByteArray(String val) {
        byte[] ucs2be = new byte[val.length() * 2];
        int size = val.length();
        for (int i = 0; i < size; ++i) {
            Util.putWordBE(ucs2be, i * 2, (int) val.charAt(i));
        }
        return ucs2be;
    }

    public static String ucs2beByteArrayToString(byte[] buf, int off, int len) {
        if ((off + len > buf.length) || (len % 2 != 0)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int end = off + len;
        for (int i = off; i < end; i += 2) {
            sb.append((char) Util.getWordBE(buf, i));
        }
        return removeCr(sb.toString());
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

    public static byte[] stringToByteArray(String val) {
        return isEmpty(val) ? new byte[0] : val.getBytes();
    }

    public static String byteArrayToString(byte[] buf, int off, int len) {
        if (buf.length < off + len) {
            return "";
        }

        while ((0 < len) && (buf[off + len - 1] == 0x00)) {
            len--;
        }
        if (0 == len) {
            return "";
        }

        if (isDataUCS2(buf, off, len)) {
            return ucs2beByteArrayToString(buf, off, len);
        }

        if (isDataUTF8(buf, off, len)) {
            return utf8beByteArrayToString(buf, off, len);
        }

        return byteArrayToWinString(buf, off, len);
    }

    public static String byteArrayToWinString(byte[] buf, int off, int len) {
        if (buf.length < off + len) {
            return "";
        }

        if ((0 < len) && (0x00 == buf[off + len - 1])) {
            len--;
        }
        if (0 == len) {
            return "";
        }
        return byteArray1251ToString(buf, off, len);
    }

    public static String byteArrayToAsciiString(byte[] buf) {
        return new String(buf, 0, buf.length);
    }

    public static String byteArrayToAsciiString(byte[] buf, int offset, int length) {
        return new String(buf, offset, length);
    }

    public static boolean isEmpty(String value) {
        return (null == value) || (0 == value.length());
    }

    public static String notNull(String value) {
        return (null == value) ? "" : value;
    }
}

