package sawim.comm;

import android.util.Patterns;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import sawim.Options;
import sawim.chat.Chat;
import sawim.roster.TreeNode;
import sawim.util.JLocale;

import java.io.ByteArrayOutputStream;
import java.util.*;


public class Util {
    private ByteArrayOutputStream stream = new ByteArrayOutputStream();

    public Util() {
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }

    public int size() {
        return stream.size();
    }

    public void reset() {
        try {
            stream.reset();
        } catch (Exception ignored) {
        }
    }

    public void writeZeroes(int count) {
        for (int i = 0; i < count; ++i) {
            writeByte(0);
        }
    }

    public void writeWordBE(int value) {
        try {
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
            stream.write(value & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeWordLE(int value) {
        try {
            stream.write(value & 0xFF);
            stream.write(((value & 0xFF00) >> 8) & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeByteArray(byte[] array) {
        try {
            stream.write(array);
        } catch (Exception ignored) {
        }
    }

    public void writeByteArray(byte[] array, int offset, int length) {
        try {
            stream.write(array, offset, length);
        } catch (Exception ignored) {
        }
    }

    public void writeDWordBE(long longValue) {
        try {
            int value = (int) longValue;
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0x0000FF00) >> 8) & 0xFF);
            stream.write(value & 0x000000FF);
        } catch (Exception ignored) {
        }
    }

    public void writeDWordLE(long longValue) {
        try {
            int value = (int) longValue;
            stream.write(value & 0x000000FF);
            stream.write(((value & 0x0000FF00) >> 8) & 0xFF);
            stream.write(((value & 0x00FF0000) >> 16) & 0xFF);
            stream.write(((value & 0xFF000000) >> 24) & 0xFF);
        } catch (Exception ignored) {
        }
    }

    public void writeByte(int value) {
        try {
            stream.write(value);
        } catch (Exception ignored) {
        }
    }

    public void writeShortLenAndUtf8String(String value) {
        byte[] raw = StringConvertor.stringToByteArrayUtf8(value);
        writeByte(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }

    public void writeLenAndUtf8String(String value) {
        byte[] raw = StringConvertor.stringToByteArrayUtf8(value);
        writeWordBE(raw.length);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }

    public void writeUtf8String(String value) {
        byte[] raw = StringConvertor.stringToByteArrayUtf8(value);
        try {
            stream.write(raw, 0, raw.length);
        } catch (Exception ignored) {
        }
    }

    public void writeProfileAsciizTLV(int type, String value) {
        value = StringConvertor.notNull(value);

        byte[] raw = StringConvertor.stringToByteArray1251(value);
        writeWordLE(type);
        writeWordLE(raw.length + 3);
        writeWordLE(raw.length + 1);
        writeByteArray(raw);
        writeByte(0);
    }

    public void writeTlvECombo(int type, String value, int code) {
        value = StringConvertor.notNull(value);
        writeWordLE(type);
        byte[] raw = StringConvertor.stringToByteArray(value);
        writeWordLE(raw.length + 4);
        writeWordLE(raw.length + 1);
        try {
            stream.write(raw, 0, raw.length);
            stream.write(0);
            stream.write(code);
        } catch (Exception ignored) {
        }
    }

    public void writeTLV(int type, byte[] data) {
        writeWordBE(type);
        int length = (null == data) ? 0 : data.length;
        writeWordBE(length);
        if (length > 0) {
            try {
                stream.write(data, 0, data.length);
            } catch (Exception ignored) {
            }
        }
    }

    public void writeTLVWord(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(2);
        writeWordBE(wordValue);
    }

    public void writeTLVDWord(int type, long wordValue) {
        writeWordBE(type);
        writeWordBE(4);
        writeDWordBE(wordValue);
    }

    public void writeTLVByte(int type, int wordValue) {
        writeWordBE(type);
        writeWordBE(1);
        writeByte(wordValue);
    }


    private static final byte[] PASSENC_KEY = {(byte) 0xF3, (byte) 0x26, (byte) 0x81, (byte) 0xC4,
            (byte) 0x39, (byte) 0x86, (byte) 0xDB, (byte) 0x92,
            (byte) 0x71, (byte) 0xA3, (byte) 0xB9, (byte) 0xE6,
            (byte) 0x53, (byte) 0x7A, (byte) 0x95, (byte) 0x7C};


    public static int getByte(byte[] buf, int off) {
        return ((int) buf[off]) & 0x000000FF;
    }

    public static void putByte(byte[] buf, int off, int val) {
        buf[off] = (byte) (val & 0x000000FF);
    }

    public static int getWordLE(byte[] buf, int off) {
        int val = (((int) buf[off])) & 0x000000FF;
        return val | (((int) buf[++off]) << 8) & 0x0000FF00;
    }

    public static int getWordBE(byte[] buf, int off) {
        int val = (((int) buf[off]) << 8) & 0x0000FF00;
        return val | (((int) buf[++off])) & 0x000000FF;
    }

    public static void putWordLE(byte[] buf, int off, int val) {
        buf[off] = (byte) ((val) & 0x000000FF);
        buf[++off] = (byte) ((val >> 8) & 0x000000FF);
    }

    public static void putWordBE(byte[] buf, int off, int val) {
        buf[off] = (byte) ((val >> 8) & 0x000000FF);
        buf[++off] = (byte) ((val) & 0x000000FF);
    }

    public static long getDWordLE(byte[] buf, int off) {
        long val;

        val = (((long) buf[off])) & 0x000000FF;
        val |= (((long) buf[++off]) << 8) & 0x0000FF00;
        val |= (((long) buf[++off]) << 16) & 0x00FF0000;
        val |= (((long) buf[++off]) << 24) & 0xFF000000;
        return val;
    }

    public static long getDWordBE(byte[] buf, int off) {
        long val;
        val = (((long) buf[off]) << 24) & 0xFF000000;
        val |= (((long) buf[++off]) << 16) & 0x00FF0000;
        val |= (((long) buf[++off]) << 8) & 0x0000FF00;
        val |= (((long) buf[++off])) & 0x000000FF;
        return val;
    }

    public static void putDWordLE(byte[] buf, int off, long val) {

        buf[off] = (byte) ((val) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 8) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 16) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 24) & 0x00000000000000FF);
    }

    public static void putDWordBE(byte[] buf, int off, long val) {
        buf[off] = (byte) ((val >> 24) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 16) & 0x00000000000000FF);
        buf[++off] = (byte) ((val >> 8) & 0x00000000000000FF);
        buf[++off] = (byte) ((val) & 0x00000000000000FF);
    }


    public static byte[] decipherPassword(byte[] buf) {
        byte[] ret = new byte[buf.length];
        for (int i = 0; i < buf.length; ++i) {
            ret[i] = (byte) (buf[i] ^ PASSENC_KEY[i % PASSENC_KEY.length]);
        }
        return ret;
    }

    public static String makeTwo(int number) {
        if (number < 10) {
            return "0" + String.valueOf(number);
        }
        return String.valueOf(number);
    }

    private static Random rand = new Random(System.currentTimeMillis());

    public static int nextRandInt() {
        return Math.abs(Math.max(Integer.MIN_VALUE + 1, rand.nextInt()));
    }

    private final static int TIME_SECOND = 0;
    private final static int TIME_MINUTE = 1;
    private final static int TIME_HOUR = 2;
    private final static int TIME_DAY = 3;
    private final static int TIME_MON = 4;
    private final static int TIME_YEAR = 5;

    final private static byte[] dayCounts = {
            31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    private final static int[] calFields = {
            Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH,
            Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND};

    private final static int[] ofsFieldsA = {0, 4, 6, 9, 12, 15};
    private final static int[] ofsFieldsB = {0, 5, 8, 11, 14, 17};
    private final static String[] months = {"Jan", "Feb", "Mar", "Apr",
            "May", "Jun", "Jul", "Aug",
            "Sep", "Oct", "Nov", "Dec"};

    public static long createGmtDate(String sdate) {
        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try {
            sdate = sdate.trim();
            int[] ofs = sdate.endsWith("Z") ? ofsFieldsB : ofsFieldsA;
            long result;
            if (Character.isDigit(sdate.charAt(0))) {
                int fieldLength = 4;
                for (int i = 0; i < calFields.length; ++i) {
                    int begIndex = ofs[i];
                    int field = strToIntDef(sdate.substring(begIndex, begIndex + fieldLength), 0);
                    if (1 == i) {
                        field += Calendar.JANUARY - 1;
                    }
                    fieldLength = 2;
                    c.set(calFields[i], field);
                }
                result = Math.max(0, c.getTime().getTime() / 1000);

            } else {
                String[] rfcDate = Util.explode(sdate, ' ');
                c.set(Calendar.YEAR, strToIntDef(rfcDate[3], 0));

                for (int i = 0; i < months.length; ++i) {
                    if (months[i].equals(rfcDate[2])) {
                        c.set(Calendar.MONTH, i);
                        break;
                    }
                }
                c.set(Calendar.DAY_OF_MONTH, strToIntDef(rfcDate[1], 0));
                c.set(Calendar.HOUR_OF_DAY, strToIntDef(rfcDate[4].substring(0, 2), 0));
                c.set(Calendar.MINUTE, strToIntDef(rfcDate[4].substring(3, 5), 0));
                c.set(Calendar.SECOND, strToIntDef(rfcDate[4].substring(6), 0));

                long delta = strToIntDef(rfcDate[5].substring(1, 3), 0) * 60 * 60
                        + strToIntDef(rfcDate[5].substring(3, 5), 0) * 60;
                if ('+' == rfcDate[5].charAt(0)) {
                    delta = -delta;
                }
                result = Math.max(0, c.getTime().getTime() / 1000 + delta);
            }
            return result;
        } catch (Exception ignored) {
        }
        return 0;
    }

    public static long createLocalDate(String date) {
        try {
            date = date.replace('.', ' ').replace(':', ' ');
            String[] values = Util.explode(date, ' ');
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.set(Calendar.YEAR, Util.strToIntDef(values[2], 0));
            c.set(Calendar.MONTH, Util.strToIntDef(values[1], 0) - 1);
            c.set(Calendar.DAY_OF_MONTH, Util.strToIntDef(values[0], 0));
            c.set(Calendar.HOUR_OF_DAY, Util.strToIntDef(values[3], 0));
            c.set(Calendar.MINUTE, Util.strToIntDef(values[4], 0));
            c.set(Calendar.SECOND, 0);
            return localTimeToGmtTime(c.getTime().getTime() / 1000);
        } catch (Exception ignored) {
            return 0;
        }
    }

    public static long createCurrentLocalTime() {
        return gmtTimeToLocalTime(SawimApplication.getCurrentGmtTime());
    }

    public static String getLocalDayOfWeek_(long gmtTime) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.setTime(new Date(Util.gmtTimeToLocalTime(gmtTime) * 1000));
        String[] days = {"", "sunday", "monday", "tuesday", "wednesday",
                "thursday", "friday", "saturday"};
        return days[cal.get(Calendar.DAY_OF_WEEK)];
    }

    public static String getLocalDateString(long gmtDate, boolean onlyTime) {
        if (0 == gmtDate) return "***error***";
        int[] localDate = createDate(gmtTimeToLocalTime(gmtDate));
        StringBuilder sb = new StringBuilder(16);
        if (!onlyTime) {
            sb.append(Util.makeTwo(localDate[TIME_DAY]))
                    .append('.')
                    .append(Util.makeTwo(localDate[TIME_MON]))
                    .append('.')
                    .append(localDate[TIME_YEAR])
                    .append(' ');
        }
        sb.append(Util.makeTwo(localDate[TIME_HOUR]))
                .append(':')
                .append(Util.makeTwo(localDate[TIME_MINUTE]));
        return sb.toString();
    }

    public static String getDate(String format, long anyDate) {
        if (0 == anyDate) return "error";
        int[] localDate = createDate(anyDate);
        format = Util.replace(format, "%H", Util.makeTwo(localDate[TIME_HOUR]));
        format = Util.replace(format, "%M", Util.makeTwo(localDate[TIME_MINUTE]));
        format = Util.replace(format, "%S", Util.makeTwo(localDate[TIME_SECOND]));
        format = Util.replace(format, "%Y", "" + localDate[TIME_YEAR]);
        format = Util.replace(format, "%y", Util.makeTwo(localDate[TIME_YEAR] % 100));
        format = Util.replace(format, "%m", Util.makeTwo(localDate[TIME_MON]));
        format = Util.replace(format, "%d", Util.makeTwo(localDate[TIME_DAY]));
        return format;
    }

    public static String getUtcDateString(long gmtTime) {
        return getDate("%Y-%m-%dT%H:%M:%SZ", gmtTime);
    }

    public static String xep0082UtcTime(long date) {
        int[] loclaDate = createDate(date);

        StringBuilder sb = new StringBuilder();

        sb.append(loclaDate[TIME_YEAR]);

        sb.append(Util.makeTwo(loclaDate[TIME_MON]));

        sb.append(Util.makeTwo(loclaDate[TIME_DAY]))
                .append('T');

        sb.append(Util.makeTwo(loclaDate[TIME_HOUR]))
                .append(':')
                .append(Util.makeTwo(loclaDate[TIME_MINUTE]))
                .append(':')
                .append(Util.makeTwo(loclaDate[TIME_SECOND]));


        return sb.toString();
    }

    public static long createGmtTime(int year, int mon, int day,
                                     int hour, int min, int sec) {
        try {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, mon - 1);
            c.set(Calendar.DAY_OF_MONTH, day);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
            c.set(Calendar.SECOND, sec);
            return c.getTime().getTime() / 1000;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int[] createDate(long value) {
        int total_days, last_days, i;
        int sec, min, hour, day, mon, year;

        sec = (int) (value % 60);

        min = (int) ((value / 60) % 60);
        value -= 60 * min;

        hour = (int) ((value / 3600) % 24);
        value -= 3600 * hour;

        total_days = (int) (value / (3600 * 24));

        year = 1970;
        for (; ; ) {
            last_days = total_days - ((year % 4 == 0) && (year != 2000) ? 366 : 365);
            if (last_days <= 0) break;
            total_days = last_days;
            year++;
        }

        int febrDays = ((year % 4 == 0) && (year != 2000)) ? 29 : 28;

        mon = 1;
        for (i = 0; i < 12; ++i) {
            last_days = total_days - ((i == 1) ? febrDays : dayCounts[i]);
            if (last_days <= 0) break;
            mon++;
            total_days = last_days;
        }

        day = total_days;

        return new int[]{sec, min, hour, day, mon, year};
    }

    public static long gmtTimeToLocalTime(long gmtTime) {
        return gmtTime + Options.getInt(Options.OPTION_GMT_OFFSET) * 3600L;
    }

    public static long localTimeToGmtTime(long localTime) {
        return localTime - Options.getInt(Options.OPTION_GMT_OFFSET) * 3600L;
    }

    public static String longitudeToString(long seconds) {
        int days = (int) (seconds / 86400);
        seconds %= 86400;
        int hours = (int) (seconds / 3600);
        seconds %= 3600;
        int minutes = (int) (seconds / 60);

        StringBuilder buf = new StringBuilder();
        if (days != 0) {
            buf.append(days).append(' ').append(JLocale.getString(R.string.days)).append(' ');
        }
        if (hours != 0) {
            buf.append(hours).append(' ').append(JLocale.getString(R.string.hours)).append(' ');
        }
        if (minutes != 0) {
            buf.append(minutes).append(' ').append(JLocale.getString(R.string.minutes));
        }

        return buf.toString();
    }

    public static String secDiffToDate(int seconds) {
        String result = "";
        int d = 0, h = 0, m = 0, s = 0;
        if (seconds > 86400) {
            d = (seconds / 86400);
            seconds = seconds - (d * 86400);
        }
        if (seconds > 3600) {
            h = (seconds / 3600);
            seconds = seconds - (h * 3600);
        }
        if (seconds > 60) {
            m = (seconds / 60);
            seconds = seconds - (m * 60);
        }
        s = seconds;

        if (d > 0) {
            result += d + " " + goodWordForm(d, 3);
        }
        if (h > 0) {
            if (d > 0) result += ", ";
            result += h + " " + goodWordForm(h, 2);
        }
        if (m > 0) {
            if ((d > 0) || (h > 0)) result += ", ";
            result += m + " " + goodWordForm(m, 1);
        }
        if (s > 0) {
            if ((d > 0) || (h > 0) || (m > 0)) result += ", ";
            result += s + " " + goodWordForm(s, 0);
        }
        if (result.equals("") && s == 0)
            result = s + " " + goodWordForm(s, 0);
        return result;
    }

    public static String goodWordForm(int d, int field) {
        String[] suf = {"sec", "minutes", "hours", "days"};
        return suf[field];
    }

    public static int uniqueValue() {
        int time = (int) (SawimApplication.getCurrentGmtTime() & 0x7FFF);
        return (time << 16) | (rand.nextInt() & 0xFFFF);
    }

    private static final int URL_CHAR_PROTOCOL = 0;
    private static final int URL_CHAR_PREV = 1;
    private static final int URL_CHAR_OTHER = 2;
    private static final int URL_CHAR_DIGIT = 3;
    private static final int URL_CHAR_NONE = 4;

    private static boolean isURLChar(char chr, int mode) {
        if (mode == URL_CHAR_PROTOCOL) {
            return ((chr >= 'A') && (chr <= 'Z')) ||
                    ((chr >= 'a') && (chr <= 'z'));
        }

        if (mode == URL_CHAR_PREV) {
            return ((chr >= 'A') && (chr <= 'Z'))
                    || ((chr >= 'a') && (chr <= 'z'))
                    || ((chr >= '0') && (chr <= '9'))
                    || ('@' == chr) || ('-' == chr)
                    || ('_' == chr) || ('%' == chr);
        }
        if (URL_CHAR_DIGIT == mode) return Character.isDigit(chr);
        if (URL_CHAR_NONE == mode) return (' ' == chr) || ('\n' == chr);

        if ((chr <= ' ') || (chr == '\n')) return false;
        return true;
    }

    public static String getUrlWithoutProtocol(String url) {
        int index = url.indexOf(':');
        if (-1 != index) {
            url = url.substring(index + 1);
            if (url.startsWith("\57\57")) {
                url = url.substring(2);
            }
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
        }
        return url;
    }

    public static String notUrls(String str) {
        str = StringConvertor.notNull(str);
        return (-1 != str.indexOf("http://")) ? "" : str;
    }

    public static boolean isUrl(String text) {
        return Patterns.WEB_URL.matcher(text).matches();
    }

    public static boolean isImageFile(String filename) {
        filename = filename.toLowerCase();
        return filename.endsWith(".bmp")
                || filename.endsWith(".gif")
                || filename.endsWith(".jpg")
                || filename.endsWith(".jpeg")
                || filename.endsWith(".png");
    }

    public static int strToIntDef(String str, int defValue) {
        if (null == str) {
            return defValue;
        }
        try {
            while ((1 < str.length()) && ('0' == str.charAt(0))) {
                str = str.substring(1);
            }
            return Integer.parseInt(str);
        } catch (Exception ignored) {
        }
        return defValue;
    }


    public static String replace(String text, String from, String to) {
        int fromSize = from.length();
        int start = 0;
        int pos = 0;
        StringBuilder sb = new StringBuilder();
        for (; ; ) {
            pos = text.indexOf(from, pos);
            if (-1 == pos) break;
            sb.append(text.substring(start, pos)).append(to);
            pos += fromSize;
            start = pos;
        }
        if (start < text.length()) {
            sb.append(text.substring(start));
        }
        return sb.toString();
    }

    public static String replace(String text, String[] from, String[] to, String keys) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < text.length()) {
            char ch = text.charAt(pos);
            int index = keys.indexOf(ch);
            while (-1 != index) {
                if (text.startsWith(from[index], pos)) {
                    pos += from[index].length();
                    result.append(to[index]);
                    break;
                }
                index = keys.indexOf(text.charAt(pos), index + 1);
            }
            if (-1 == index) {
                result.append(ch);
                pos++;
            }
        }
        return result.toString();
    }

    static public String[] explode(String text, char separator) {
        if (StringConvertor.isEmpty(text)) {
            return new String[0];
        }
        Vector tmp = new Vector();
        int start = 0;
        int end = text.indexOf(separator, start);
        while (end >= start) {
            tmp.addElement(text.substring(start, end));
            start = end + 1;
            end = text.indexOf(separator, start);
        }
        tmp.addElement(text.substring(start));
        String[] result = new String[tmp.size()];
        tmp.copyInto(result);
        return result;
    }

    static public String implode(String[] text, String separator) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < text.length; ++i) {
            if (null != text[i]) {
                if (0 != result.length()) {
                    result.append(separator);
                }
                result.append(text[i]);
            }
        }
        return result.toString();
    }

    private static final String base64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    private static int base64GetNextChar(String str, int index) {
        if (-1 == index) return -2;
        char ch = str.charAt(index);
        if ('=' == ch) {
            return -1;
        }
        return base64.indexOf(ch);
    }

    private static int base64GetNextIndex(String str, int index) {
        for (; index < str.length(); ++index) {
            char ch = str.charAt(index);
            if ('=' == ch) {
                return index;
            }
            int code = base64.indexOf(ch);
            if (-1 != code) {
                return index;
            }
        }
        return -1;
    }

    public static byte[] base64decode(String str) {
        if (null == str) str = "";
        Util out = new Util();
        for (int strIndex = 0; strIndex < str.length(); ++strIndex) {
            strIndex = base64GetNextIndex(str, strIndex);
            if (-1 == strIndex) break;
            int ch1 = base64GetNextChar(str, strIndex);
            if (-1 == ch1) break;

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch2 = base64GetNextChar(str, strIndex);
            if (-1 == ch2) break;
            out.writeByte((byte) (0xFF & ((ch1 << 2) | (ch2 >>> 4))));

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch3 = base64GetNextChar(str, strIndex);
            if (-1 == ch3) break;
            out.writeByte((byte) (0xFF & ((ch2 << 4) | (ch3 >>> 2))));

            strIndex = base64GetNextIndex(str, strIndex + 1);
            if (-1 == strIndex) break;
            int ch4 = base64GetNextChar(str, strIndex);
            if (-1 == ch4) break;
            out.writeByte((byte) (0xFF & ((ch3 << 6) | (ch4))));
        }
        return out.toByteArray();
    }

    public static String base64encode(final byte[] data) {
        char[] out = new char[((data.length + 2) / 3) * 4];
        for (int i = 0, index = 0; i < data.length; i += 3, index += 4) {
            boolean quad = false;
            boolean trip = false;

            int val = (0xFF & data[i]);
            val <<= 8;
            if ((i + 1) < data.length) {
                val |= (0xFF & data[i + 1]);
                trip = true;
            }
            val <<= 8;
            if ((i + 2) < data.length) {
                val |= (0xFF & data[i + 2]);
                quad = true;
            }
            out[index + 3] = base64.charAt(quad ? (val & 0x3F) : 64);
            val >>= 6;
            out[index + 2] = base64.charAt(trip ? (val & 0x3F) : 64);
            val >>= 6;
            out[index + 1] = base64.charAt(val & 0x3F);
            val >>= 6;
            out[index + 0] = base64.charAt(val & 0x3F);
        }
        return new String(out);
    }

    private static final String[] escapedChars = {"&quot;", "&apos;", "&gt;", "&lt;", "&amp;"};
    private static final String[] unescapedChars = {"\"", "'", ">", "<", "&"};

    public static String xmlEscape(String text) {
        text = StringConvertor.notNull(text);
        return Util.replace(text, unescapedChars, escapedChars, "\"'><&");
    }

    public static String xmlUnescape(String text) {
        if (-1 == text.indexOf('&')) {
            return text;
        }
        return Util.replace(text, escapedChars, unescapedChars, "&&&&&");
    }

    public static String[] vectorToArray(Vector v) {
        String[] result = new String[v.size()];
        v.copyInto(result);
        return result;
    }

    public static String[] vectorToArray_(Vector v) {
        String[] stringArray = new String[v.size()];
        for (int i = 0; i < v.size(); i++) {
            stringArray[i] = v.get(i).toString();
        }
        return stringArray;
    }

    public static int stringsToInt(String[] mass, String s) {
        for (int i = 0; i < mass.length; ++i) {
            if (mass[i].equals(s))
                return i;
        }
        return 0;
    }

    public static int compareNodes(TreeNode node1, TreeNode node2) {
        int result = node1.getNodeWeight() - node2.getNodeWeight();
        if (0 == result) {
            result = StringConvertor.stringCompare(node1.getText(), node2.getText());
        }
        return result;
    }

    public static void sort(Vector<TreeNode> subnodes) {
        for (int i = 1; i < subnodes.size(); ++i) {
            TreeNode currNode = subnodes.elementAt(i);
            int j = i - 1;
            for (; j >= 0; --j) {
                TreeNode itemJ = subnodes.elementAt(j);
                if (compareNodes(itemJ, currNode) <= 0) {
                    break;
                }
                subnodes.setElementAt(itemJ, j + 1);
            }
            if (j + 1 != i) {
                subnodes.setElementAt(currNode, j + 1);
            }
        }
    }

    public static void sortChats(List<Chat> subnodes) {
        for (int i = 1; i < subnodes.size(); ++i) {
            Chat currNode = subnodes.get(i);
            int j = i - 1;
            for (; j >= 0; --j) {
                Chat itemJ = subnodes.get(j);
                if (compareNodes(itemJ.getContact(), currNode.getContact()) <= 0) {
                    break;
                }
                subnodes.set(j + 1, itemJ);
            }
            if (j + 1 != i) {
                subnodes.set(j + 1, currNode);
            }
        }
    }

    private static void putCh(StringBuffer sb, int ch) {
        String s = Integer.toHexString(ch);
        sb.append("%");
        if (1 == s.length()) sb.append('0');
        sb.append(s);
    }

    public static String urlEscape(String param) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < param.length(); ++i) {
            char ch = param.charAt(i);
            char lowerCh = Character.toLowerCase(ch);
            if (Character.isDigit(ch) || (-1 != "qwertyuiopasdfghjklzxcvbnm@.-".indexOf(lowerCh))) {
                sb.append(ch);

            } else if (' ' == ch) {
                sb.append('+');

            } else if ((0x7F & ch) == ch) {
                putCh(sb, ch);

            } else if ((0xFFF & ch) == ch) {
                putCh(sb, 0xD0 | (ch >> 6));
                putCh(sb, 0x80 | (0x3F & ch));

            } else {
                putCh(sb, 0xE8 | (ch >> 12));
                putCh(sb, 0x80 | (0x3F & (ch >> 6)));
                putCh(sb, 0x80 | (0x3F & ch));
            }
        }
        return sb.toString();
    }

    public static int getIndex(List v, Object o) {
        int size = v.size();
        for (int i = 0; i < size; ++i) {
            if (v.get(i) == o) {
                return i;
            }
        }
        return -1;
    }

    public static void removeAll(Vector to, Vector all) {
        synchronized (to) {
            int current = 0;
            for (int index = 0; index < to.size(); ++index) {
                if (0 <= Util.getIndex(all, to.elementAt(index))) continue;
                if (current < index) {
                    to.setElementAt(to.elementAt(index), current);
                    current++;
                }
            }
            if (current < to.size()) to.setSize(current);
        }
    }
}

