package sawim.history;

import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import ru.sawim.config.HomeDirectory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 15.12.12 18:35
 *
 * @author vladimir
 */
public class AndroidHistoryStorage {
    private HistoryStorage historyStorage;
    private Vector<String> messages = null;

    AndroidHistoryStorage(HistoryStorage storage) {
        historyStorage = storage;
    }

    public void addText(String text, boolean incoming,
                                     String from, long gmtTime) {
        sawim.modules.fs.JSR75FileSystem fs = getFile();
        try {
            boolean exist = fs.exists();
            OutputStream out = fs.openForAppendOutputStream();
            try {
                if (!exist) {
                    String header = "# history with " + historyStorage.getUniqueUserId() + "\n\n";
                    out.write(StringConvertor.stringToByteArrayUtf8(header));
                }
                String f = "[" + from + " " + Util.getLocalDateString(gmtTime, false) + "]\n" + text + "\n";
                out.write(StringConvertor.stringToByteArrayUtf8(f));
            } finally {
                out.close();
                fs.close();
            }
        } catch (Exception ignored) {
        }
        fs.close();
    }


    public int getHistorySize() {
        return 5;
    }
    public CachedRecord getRecord(int recNo) {
        return getLastRecord(getHistorySize() - recNo - 1);
    }

    private CachedRecord getLastRecord(int recNo) {
        sawim.modules.fs.JSR75FileSystem fs = getFile();
        CachedRecord message = null;
        try {
            String last = toString(readLast(fs, 10 * 1024));
            if (null == messages) messages = explodeMessages(last, getHistorySize());
            message = parseMessage(messages.get(recNo));
            if (recNo == messages.size() - 1) messages = null;
        } catch (Exception ignored) {
        }
        fs.close();
        return message;
    }

    private CachedRecord parseMessage(String msg) {
        CachedRecord cachedRecord = new CachedRecord();
        String header = msg.substring(1, msg.indexOf("]\n"));
        int loginTimeDelim = header.lastIndexOf(' ', header.lastIndexOf(' ') - 1);
        cachedRecord.from = header.substring(0, loginTimeDelim);
        cachedRecord.date = header.substring(loginTimeDelim + 1);
        cachedRecord.text = msg.substring(msg.indexOf("]\n") + 2);
        cachedRecord.type = 0;
        if (ContactList.getInstance().getProtocol(historyStorage.getContact())
                .getNick().equals(cachedRecord.from)) {
            cachedRecord.type = 1;
        }
        return cachedRecord;
    }

    private sawim.modules.fs.JSR75FileSystem getFile() {
        return HomeDirectory.getFile(sawim.modules.fs.FileSystem.HISTORY + "/" + historyStorage.getUniqueUserId() + ".txt");
    }

    public String getTextFile() {
        try {
            return getFile().exists() ? getFile().getAbsolutePath() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readLast(sawim.modules.fs.JSR75FileSystem fs, int read) throws Exception {
        InputStream in = fs.openInputStream();
        try {
            int size = in.available();
            read = Math.min(size, read);
            int offset = size - read;
            if (0 < offset) in.skip(offset);
            byte[] buffer = new byte[read];
            int used = in.read(buffer);
            return (used < buffer.length) ? trim(buffer, used) : buffer;
        } finally {
            in.close();
        }
    }
    private byte[] trim(byte[] array, int size) {
        byte[] out = new byte[size];
        System.arraycopy(array, 0, out, 0, size);
        return out;
    }

    private String toString(byte[] in) {
        for (int i = 0; i < in.length; ++i) {
            if (in[i] == '[') break;
            in[i] = ' ';
        }
        return StringConvertor.utf8beByteArrayToString(in, 0, in.length).trim();
    }

    private Vector<String> explodeMessages(String str, int limit) {
        Vector<String> messages = new Vector<String>();
        int messageEnd = str.length();
        int cursor = messageEnd;
        while (0 < cursor) {
            int headerStart = str.lastIndexOf('[', cursor);
            if (isHeader(str, headerStart)) {
                messages.add(str.substring(headerStart, messageEnd));
                messageEnd = headerStart - 1;
                if (messages.size() == limit) break;
            }
            cursor = headerStart - 1;
        }
        return messages;
    }

    private boolean isHeader(String str, int headerStart) {
        int headerEnd = str.indexOf("]", headerStart);
        if ((headerStart < 1) || (headerEnd < 2)) return false;
        Matcher matcher = headerPattern.matcher(str.substring(headerStart - 1, headerEnd + 2));
        if (matcher.matches()) {
            char first = str.charAt(headerEnd + 2);
            return ('«' != first) && ( '»' != first);
        }
        return false;
    }
    private static final Pattern headerPattern = Pattern.compile("\n\\[[^\n]+ \\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}\\]\n");
}
