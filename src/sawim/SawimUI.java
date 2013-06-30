package sawim;

import sawim.comm.StringConvertor;
import ru.sawim.activities.SawimActivity;

public final class SawimUI {

    private static String clipBoardText;
    private static String clipBoardHeader;
    private static boolean clipBoardIncoming;

    private static void insertQuotingChars(StringBuffer out, String text, char qChars) {
        int size = text.length();
        boolean wasNewLine = true;
        for (int i = 0; i < size; ++i) {
            char chr = text.charAt(i);
            if (wasNewLine) out.append(qChars).append(' ');
            out.append(chr);
            wasNewLine = (chr == '\n');
        }
    }

    public static boolean isClipBoardEmpty() {
        return null == clipBoardText;
    }

    public static String getClipBoardText() {
        return getClipBoardText(false);
    }

    public static String getClipBoardText(boolean quote) {
        String androidClipboard = SawimActivity.getInstance().getFromClipboard();
        if (!StringConvertor.isEmpty(androidClipboard) && !androidClipboard.equals(clipBoardText)) {
            clipBoardText = androidClipboard;
            clipBoardHeader = "mobile";
            clipBoardIncoming = true;
        }
        
        if (isClipBoardEmpty()) {
            return "";
        }
        if (!quote || (null == clipBoardHeader)) {
            return clipBoardText + " ";
        }
        return serialize(clipBoardIncoming, clipBoardHeader, clipBoardText) + "\n\n";
    }

    public static String serialize(boolean incoming, String header, String text) {
        StringBuffer sb = new StringBuffer();
        sb.append('[').append(header).append(']').append('\n');
        insertQuotingChars(sb, text, incoming ? '\u00bb' : '\u00ab');
        return sb.toString();
    }

    public static void setClipBoardText(String header, String text) {
        clipBoardText     = text;
        clipBoardHeader   = header;
        clipBoardIncoming = true;
        SawimActivity.getInstance().putToClipboard(clipBoardHeader, clipBoardText);
        
    }
    public static void setClipBoardText(String text) {
        clipBoardText     = text;
        clipBoardHeader   = null;
        clipBoardIncoming = true;
        SawimActivity.getInstance().putToClipboard(clipBoardHeader, clipBoardText);
    }

    public static void setClipBoardText(boolean incoming, String from, String date, String text) {
        clipBoardText     = text;
        clipBoardHeader   = null;
        clipBoardIncoming = incoming;
        SawimActivity.getInstance().putToClipboard(clipBoardHeader, clipBoardText);
    }

    public static void clearClipBoardText() {
        clipBoardText = null;
    }
}