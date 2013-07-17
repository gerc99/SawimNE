package sawim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Build;
import ru.sawim.SawimApplication;
import ru.sawim.activities.SawimActivity;
import sawim.comm.StringConvertor;

import java.util.concurrent.atomic.AtomicReference;

public final class Clipboard {

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

    public static String serialize(boolean incoming, String header, String text) {
        StringBuffer sb = new StringBuffer();
        sb.append('[').append(header).append(']').append('\n');
        insertQuotingChars(sb, text, incoming ? '\u00bb' : '\u00ab');
        return sb.toString();
    }

    public static void setClipBoardText(String text) {
        putToClipboard(text);
    }

    public static void putToClipboard(final String text) {
        SawimApplication.getInstance().runOnUiThread(new Runnable() {
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