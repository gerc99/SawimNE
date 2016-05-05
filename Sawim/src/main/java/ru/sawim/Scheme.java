package ru.sawim;

import android.content.Context;

import java.util.HashMap;

import ru.sawim.comm.JLocale;
import ru.sawim.ui.widget.Util;

public class Scheme {

    public static final byte FONT_STYLE_PLAIN = 0;
    public static final byte FONT_STYLE_BOLD = 1;

    private Scheme() {
    }

    private static HashMap<Integer, Integer> themeColorsMap = new HashMap<>();

    private static final int[] themeColors = new int[] {
            R.attr.text,
            R.attr.list_divider,
            R.attr.item_selected,
            R.attr.param_value,
            R.attr.chat_in_msg_text,
            R.attr.chat_out_msg_text,
            R.attr.chat_highlight_msg,
            R.attr.contact_online,
            R.attr.contact_with_chat,
            R.attr.contact_offline,
            R.attr.contact_temp,
            R.attr.group,
            R.attr.contact_status,
            R.attr.protocol_background,
            R.attr.link,
            R.attr.link_highlight,
            R.attr.divider,
            R.attr.unread_message_divider,
            R.attr.bar_unread_message,
            R.attr.bar_personal_unread_message,
            R.attr.unread_message,
            R.attr.personal_unread_message
    };

    private static String[] themeNames = new String[] {"Light", "Black"};
    private static String currentTheme;
    private static String oldTheme = getSavedTheme();
    private static boolean isLoad;
    private static boolean isBlackTheme;

    public static void load(Context context) {
        if (!isLoad) {
            themeColorsMap.clear();
            for (int themeColor : themeColors) {
                themeColorsMap.put(themeColor, Util.getColorFromAttribute(context, themeColor));
            }
            isLoad = true;
        }
    }

    public static void init() {
        currentTheme = getSavedTheme();
        isBlackTheme = getSavedTheme().equals(themeNames[1]);
    }

    public static boolean isBlack() {
        return getSavedTheme().equals(themeNames[1]);
    }

    public static String[] getSchemeNames() {
        return themeNames;
    }

    public static String getSavedTheme() {
        return Options.getString(JLocale.getString(R.string.pref_color_scheme));
    }

    public static boolean isChangeTheme(String newTheme) {
        if (!currentTheme.equals(newTheme)) return true;
        if (oldTheme != null && !oldTheme.equals(newTheme)) {
            oldTheme = newTheme;
            //isBlackTheme = !isBlackTheme;
            isLoad = false;
            return true;
        }
        return false;
    }

    public static int getColor(int color) {
        if (themeColorsMap.isEmpty()) return 0xff000000;
        return themeColorsMap.get(color);
    }

    public static int getInversColor(int c) {
        return 0xFF550000 ^ themeColorsMap.get(c);
    }

    public static void setColorScheme(String colorScheme) {
        currentTheme = colorScheme;
    }
}
