package sawim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import ru.sawim.activities.SawimActivity;
import sawim.comm.StringConvertor;

import java.util.concurrent.atomic.AtomicReference;

public final class Clipboard {

    private static final Object lock = new Object();
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

    public static String getClipBoardText(boolean quote) {
        String androidClipboard = getFromClipboard();
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

    public static void setClipBoardText(String text) {
        clipBoardText = text;
        clipBoardHeader = null;
        clipBoardIncoming = true;
        putToClipboard(clipBoardText);
    }

    public static void setClipBoardText(boolean incoming, String from, String date, String text) {
        clipBoardText = text;
        clipBoardHeader = null;
        clipBoardIncoming = incoming;
        putToClipboard(clipBoardText);
    }

    public static void clearClipBoardText() {
        clipBoardText = null;
    }

    public static String getFromClipboard() {
        final AtomicReference<String> text = new AtomicReference<String>();
        text.set(null);
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) SawimActivity.getInstance().getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                        text.set(clipboard.hasText() ? clipboard.getText().toString() : null);
                    } else {
                        ClipboardManager clipboard = (ClipboardManager)
                                SawimActivity.getInstance().getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                        text.set(clipboard.hasPrimaryClip() ? clipboard.getPrimaryClip().toString() : null);
                    }
                    synchronized (lock) {
                        lock.notify();
                    }
                } catch (Throwable e) {
                    sawim.modules.DebugLog.panic("get clipboard", e);
                }
            }
        });
        return text.get();
    }

    public static void putToClipboard(final String text) {
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            public void run() {
                try {
                    if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                                SawimActivity.getInstance().getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                        clipboard.setText(text);
                    } else {
                        ClipboardManager clipboard = (ClipboardManager)
                                SawimActivity.getInstance().getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                        clipboard.setPrimaryClip(ClipData.newPlainText("", text));
                    }
                } catch (Throwable e) {
                    sawim.modules.DebugLog.panic("set clipboard", e);
                }
            }
        });
    }
}