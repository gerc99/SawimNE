


package protocol.mrim;

import protocol.StatusInfo;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

import java.io.ByteArrayOutputStream;


class MrimBuffer {
    private ByteArrayOutputStream out;
    private byte[] data;
    private int position = 0;

    public MrimBuffer() {
        out = new ByteArrayOutputStream();
    }

    public MrimBuffer(byte[] buf) {
        data = buf;
    }

    void putDWord(long value) {
        putDWord((int) value);
    }

    void putDWord(int value) {
        try {
            out.write(value & 0x000000FF);
            out.write(((value & 0x0000FF00) >> 8) & 0xFF);
            out.write(((value & 0x00FF0000) >> 16) & 0xFF);
            out.write(((value & 0xFF000000) >> 24) & 0xFF);
        } catch (Exception ex) {
        }
    }

    void putString(String str) {
        byte[] strBytes = StringConvertor.stringToByteArray1251(str);
        putDWord(strBytes.length);
        putBytes(strBytes);
    }

    void putUcs2String(String str) {
        putDWord(str.length() * 2);
        try {
            for (int i = 0; i < str.length(); ++i) {
                char ch = str.charAt(i);
                out.write(ch & 0xFF);
                out.write((ch >> 8) & 0xFF);
            }
        } catch (Exception ex) {
        }
    }

    void putStringAsQWord(String value) {
        putDWord(hex2long(value.substring(8, 16)));
        putDWord(hex2long(value.substring(0, 8)));
    }

    void putBytes(byte[] data) {
        try {
            out.write(data);
        } catch (Exception ex) {
        }
    }

    void putSearchParam(int key, String value) {
        if (!StringConvertor.isEmpty(value)) {
            putDWord(key);
            putString(value);
        }
    }

    void putUcs2SearchParam(int key, String value) {
        if (!StringConvertor.isEmpty(value)) {
            putDWord(key);
            putUcs2String(value);
        }
    }

    public final void putStatusInfo(Mrim mrim) {
        String title = "";
        String desc = "";
        StatusInfo info = mrim.getStatusInfo();
        byte statusIndex = mrim.getProfile().statusIndex;
        int status = MrimConnection.getNativeStatus(statusIndex);
        status |= mrim.getPrivateStatusMask();
        String xstatus = MrimConnection.getNativeXStatus(statusIndex);

        String x = Mrim.xStatus.getNativeXStatus(mrim.getProfile().xstatusIndex);
        if (!StringConvertor.isEmpty(x)) {
            title = mrim.getProfile().xstatusTitle;
            desc = mrim.getProfile().xstatusDescription;
            status = 4;
            xstatus = x;
        }
        if (StatusInfo.STATUS_INVISIBLE == statusIndex) {
            status |= MrimConnection.STATUS_FLAG_INVISIBLE;
        }

        putDWord(status);
        putString(xstatus);
        putUcs2String(title);
        putUcs2String(desc);
        putDWord(0x0002 + 0x0004 + 0x0010 + 0x0020);
    }

    public final void putContact(int flags, int groupId, String uin, String name, String phone) {
        putDWord(flags);
        putDWord(groupId);
        putString(uin);
        putUcs2String(name);
        putString(phone);
    }

    byte[] toByteArray() {
        return (null == out) ? data : out.toByteArray();
    }

    private long hex2long(String val) {
        try {
            while ((1 < val.length()) && ('0' == val.charAt(0))) {
                val = val.substring(1);
            }
            return Long.parseLong(val, 16);
        } catch (Exception e) {
            return 0;
        }
    }

    private String long2hex(long val) {
        String result = Long.toString(val, 16);
        while (result.length() < 8) {
            result = "0" + result;
        }
        return result;
    }

    public String getQWordAsString() {
        long x1 = getDWord() & 0xFFFFFFFFL;
        long x2 = getDWord() & 0xFFFFFFFFL;
        return (long2hex(x2) + long2hex(x1)).toUpperCase();
    }

    public long getDWord() {
        long res = Util.getDWordLE(data, position);
        position += 4;
        return res;
    }

    public String getBirthdayString() {
        int msgLen = (int) getDWord();
        while (0 < msgLen) {
            if (0 != data[position]) break;
            position++;
            msgLen--;
        }
        String str = StringConvertor.byteArray1251ToString(data, position, msgLen);
        position += msgLen;
        return str;
    }

    public String getString() {
        int msgLen = (int) getDWord();
        if (0 == msgLen) {
            return "";
        }
        if (((msgLen & 1) == 0)
                && ((data[position + 1] & 7) == data[position + 1])) {
            return getUcs2StringZ(msgLen);
        }
        String str = StringConvertor.byteArray1251ToString(data, position, msgLen);
        position += msgLen;
        return StringConvertor.removeCr(str);
    }

    public String getUtf8String() {
        int msgLen = (int) getDWord();
        String str = StringConvertor.utf8beByteArrayToString(data, position, msgLen);
        position += msgLen;
        return StringConvertor.removeCr(str);
    }

    public String getUcs2String() {
        int length = (int) getDWord();
        return getUcs2StringZ(length);
    }

    public String getUcs2StringZ(int dataLength) {
        StringBuilder sb = new StringBuilder(dataLength / 2);
        int pos = position;
        for (int i = 0; i < dataLength; i += 2) {
            int ch = 0xFF & data[pos++];
            ch |= ((0xFF & data[pos++]) << 8);
            sb.append((char) ch);
        }
        position += dataLength;
        return sb.toString();
    }

    public byte[] getUidl() {
        byte[] uidl = new byte[8];
        System.arraycopy(data, position, uidl, 0, 8);
        position += 8;
        return uidl;
    }

    public boolean isEOF() {
        return position == data.length;
    }

    public void skipFormattedRecord(String format, int start) {
        for (int i = start; i < format.length(); ++i) {
            if (format.charAt(i) == 's') {
                int len = (int) getDWord();
                position += len;
            } else {
                getDWord();
            }
        }
    }
}


