package ru.sawim;

import ru.sawim.comm.Config;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;

import java.util.Vector;


public class Scheme {

    public static final byte THEME_BACKGROUND = 0;
    public static final byte THEME_TEXT = 1;
    public static final byte THEME_ITEM_SELECTED = 2;
    public static final byte THEME_PARAM_VALUE = 3;

    public static final byte THEME_CHAT_INMSG = 4;
    public static final byte THEME_CHAT_OUTMSG = 5;

    public static final byte THEME_CONTACT_ONLINE = 6;
    public static final byte THEME_CONTACT_WITH_CHAT = 7;
    public static final byte THEME_CONTACT_OFFLINE = 8;
    public static final byte THEME_CONTACT_TEMP = 9;

    public static final byte THEME_NUMBER = 10;

    public static final byte THEME_GROUP = 11;
    public static final byte THEME_CHAT_HIGHLIGHT_MSG = 12;
    public static final byte THEME_CONTACT_STATUS = 13;
    public static final byte THEME_PROTOCOL_BACKGROUND = 14;
    public static final byte THEME_LINKS = 15;
    public static final byte THEME_LINKS_HIGHLIGHT = 16;
    public static final byte THEME_DIVIDER = 17;
    public static final byte THEME_UNREAD_MESSAGE_DIVIDER = 18;

    public static final byte FONT_STYLE_PLAIN = 0;
    public static final byte FONT_STYLE_BOLD = 1;

    private static boolean[] isBlack;
    private static boolean[] isSystemBackground;

    private Scheme() {
    }

    private static final int[] baseTheme = {
            0xE4E4E4,
            0x000000,
            0xc8c8c8,
            0xCB0000,
            0xCB0000,
            0x33B5E5,
            0x000000,
            0x669900,
            0x000000,
            0x777777,
            0xA00000,
            0x000000,
            0xff8800,
            0x777777,
            0x000000,
            0x0099CB,
            0xB0F0FF,
            0xafafaf,
            0x669900};

    private static int[] currentTheme = new int[baseTheme.length];
    private static int[][] themeColors;
    private static String[] themeNames;
    private static int oldTheme;

    public static void load() {
        setColorScheme(baseTheme);

        Vector themes = new Vector();
        try {
            String content = Config.loadResource("/themes.txt");
            Config.parseIniConfig(content, themes);
        } catch (Exception ignored) {
        }
        isBlack = new boolean[themes.size() + 1];
        isSystemBackground = new boolean[themes.size() + 1];
        themeNames = new String[themes.size() + 1];
        themeColors = new int[themes.size() + 1][];

        themeNames[0] = "Light Holo";
        themeColors[0] = baseTheme;
        for (int i = 0; i < themes.size(); ++i) {
            Config config = (Config) themes.elementAt(i);
            isBlack[i + 1] = Boolean.valueOf(config.getValues()[0]);
            isSystemBackground[i + 1] = Boolean.valueOf(config.getValues()[1]);
            themeNames[i + 1] = config.getName();
            themeColors[i + 1] = configToTheme(config);
        }
        Scheme.setColorScheme(getThemeId());
    }

    public static boolean isBlack() {
        return isBlack[getThemeId()];
    }

    public static boolean isSystemBackground() {
        return isSystemBackground[getThemeId()];
    }

    private static int[] configToTheme(Config config) {
        String[] keys = config.getKeys();
        String[] values = config.getValues();
        int[] theme = new int[baseTheme.length];
        System.arraycopy(baseTheme, 0, theme, 0, theme.length);
        try {
            for (int keyIndex = 2; keyIndex < keys.length; ++keyIndex) {
                int index = Util.strToIntDef(keys[keyIndex], -1);
                if ((0 <= index) && (index < theme.length)) {
                    theme[index] = Integer.parseInt(values[keyIndex].substring(2), 16);
                    if (1 == index) {
                        theme[12] = theme[11] = theme[1];
                    } else if (8 == index) {
                        theme[13] = theme[8];
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return theme;
    }

    public static int[] getScheme() {
        return currentTheme;
    }

    public static String[] getSchemeNames() {
        return themeNames;
    }

    public static void setColorScheme(int schemeNum) {
        if (themeNames.length <= schemeNum) {
            schemeNum = 0;
        }
        setColorScheme(themeColors[schemeNum]);
    }

    private static void setColorScheme(int[] scheme) {
        System.arraycopy(scheme, 0, currentTheme, 0, currentTheme.length);
    }

    public static int getThemeId() {
        return getThemeId(Options.getString(JLocale.getString(R.string.pref_color_scheme)));
    }

    public static int getThemeId(String theme) {
        for (int i = 0; i < themeNames.length; ++i) {
            if (theme.equals(themeNames[i])) {
                return i;
            }
        }
        return 0;
    }

    public static boolean isChangeTheme(int newTheme) {
        if (oldTheme != newTheme) {
            oldTheme = newTheme;
            return true;
        }
        return false;
    }

    public static int getColor(byte color) {
        return 0xff000000 | getScheme()[color];
    }

    public static int getInversColor(int c) {
        return 0xFF550000 ^ getScheme()[c];
    }
}