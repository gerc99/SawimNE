package ru.sawim;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import ru.sawim.activities.SawimActivity;

import java.util.concurrent.atomic.AtomicReference;

public final class Clipboard {

    public static void insertQuotingChars(StringBuilder out, CharSequence text, boolean isSubstring, char qChars) {
        int size = text.length();
        //int quoteMaxLen = 100;
        //boolean isSubstr = size > quoteMaxLen && isSubstring;
        //if (isSubstr) size = quoteMaxLen;
        boolean wasNewLine = true;
        for (int i = 0; i < size; ++i) {
            char chr = text.charAt(i);
            if (wasNewLine) out.append(qChars).append(' ');
            out.append(chr);
            wasNewLine = (chr == '\n');
        }
        //if (isSubstr) out.append("...");
    }

    public static String serialize(boolean isSubstring, boolean incoming, String header, CharSequence text) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(header).append(" ]").append('\n');
        insertQuotingChars(sb, text, isSubstring, incoming ? '\u00bb' : '\u00ab');
        return sb.toString();
    }

    public static String serialize(boolean isSubstring, boolean incoming, String header1, String header2, CharSequence text) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ").append(header1).append(" ").append(header2).append(" ]").append('\n');
        insertQuotingChars(sb, text, isSubstring, incoming ? '\u00bb' : '\u00ab');
        return sb.toString();
    }

    public static void setClipBoardText(Context context, String text) {
        try {
            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager)
                        context.getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                clipboard.setText(text);
            } else {
                ClipboardManager clipboard = (ClipboardManager)
                        context.getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("", text));
            }
        } catch (Throwable e) {
            ru.sawim.modules.DebugLog.panic("set clipboard", e);
        }
    }

    public static String getClipBoardText(Context context) {
        final AtomicReference<String> text = new AtomicReference<String>();
        text.set(null);
        try {
            if (Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
                android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                text.set(clipboard.hasText() ? clipboard.getText().toString() : null);
            } else {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(SawimActivity.CLIPBOARD_SERVICE);
                text.set(clipboard.hasPrimaryClip() ? clipboard.getText().toString() : null);
            }
        } catch (Throwable e) {
            ru.sawim.modules.DebugLog.panic("get clipboard", e);
        }
        return text.get();
    }
}