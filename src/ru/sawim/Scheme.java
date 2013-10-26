package ru.sawim;

import sawim.Options;
import sawim.comm.Config;
import sawim.comm.Util;
import java.util.Vector;


public class Scheme {

    public static final byte THEME_BACKGROUND           = 0;
    public static final byte THEME_TEXT                 = 1;
    public static final byte THEME_CAP_BACKGROUND       = 2;
    public static final byte THEME_CAP_TEXT             = 3;
    public static final byte THEME_PARAM_VALUE          = 4;

    public static final byte THEME_CHAT_INMSG           = 5;
    public static final byte THEME_CHAT_OUTMSG          = 6;

    public static final byte THEME_CONTACT_ONLINE       = 7;
    public static final byte THEME_CONTACT_WITH_CHAT    = 8;
    public static final byte THEME_CONTACT_OFFLINE      = 9;
    public static final byte THEME_CONTACT_TEMP         = 10;

    public static final byte THEME_MAGIC_EYE_NUMBER     = 11;
    public static final byte THEME_MAGIC_EYE_USER       = 12;
    public static final byte THEME_MAGIC_EYE_TEXT       = 13;

    public static final byte THEME_GROUP                = 14;
    public static final byte THEME_CHAT_HIGHLIGHT_MSG   = 15;
    public static final byte THEME_CONTACT_STATUS       = 16;

    public static final byte THEME_CHAT_BG_IN           = 17;
    public static final byte THEME_CHAT_BG_OUT          = 18;
    public static final byte THEME_CHAT_BG_IN_ODD       = 19;
    public static final byte THEME_CHAT_BG_OUT_ODD      = 20;
    public static final byte THEME_CHAT_BG_MARKED       = 21;
    public static final byte THEME_CHAT_BG_SYSTEM       = 22;


    public static final byte FONT_STYLE_PLAIN = 0;
    public static final byte FONT_STYLE_BOLD = 1;

    private Scheme() {
    }

    private static final int[] baseTheme = {
            0xE4E4E4,
            0x000000,
            0xC0C0C0,
            0x000000,
            0x804040,
            0x1E2858,
            0x804040,
            0x000000,
            0x804040,
            0x000000,
            0x666666,
            0xA00000,
            0x1E2858,
            0x000000,
            0x000000,
            0xA00000,
            0x999999,
            0xFFE7BA,
            0xBFEFFF,
            0xEED8AE,
            0xB2DFEE,
            0xFFA54F,
            0xF8F8FF};

    private static int[] currentTheme = new int[baseTheme.length];
    private static int[][] themeColors;
    private static String[] themeNames;

    public static void load() {
        setColorScheme(baseTheme);

        Vector themes = new Vector();
        try {
            String content = Config.loadResource("/themes.txt");
            Config.parseIniConfig(content, themes);
        } catch (Exception ignored) {
        }
        themeNames  = new String[themes.size() + 1];
        themeColors = new int[themes.size() + 1][];

        themeNames[0]  = "Gray (default)";
        themeColors[0] = baseTheme;
        for (int i = 0; i < themes.size(); ++i) {
            Config config = (Config)themes.elementAt(i);
            themeNames[i + 1]  = config.getName();
            themeColors[i + 1] = configToTheme(config);
        }
    }

    private static int[] configToTheme(Config config) {
        String[] keys = config.getKeys();
        String[] values = config.getValues();
        int[] theme = new int[baseTheme.length];
        System.arraycopy(baseTheme, 0, theme, 0, theme.length);
        try {
            for (int keyIndex = 0; keyIndex < keys.length; ++keyIndex) {
                int index = Util.strToIntDef(keys[keyIndex], -1);
                if ((0 <= index) && (index < theme.length)) {
                    theme[index] = Integer.parseInt(values[keyIndex].substring(2), 16);
                    if (0 == index) {
                        theme[17] = theme[18] = theme[19] = theme[20] = theme[22] = theme[0];
                    } else if (1 == index) {
                        theme[15] = theme[14] = theme[1];
                    } else if (9 == index) {
                        theme[16] = theme[9];
                    } else if (53 == index) {
                        theme[21] = getInversColor(theme[0]);
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
        Options.setInt(Options.OPTION_COLOR_SCHEME, schemeNum);
        setColorScheme(themeColors[schemeNum]);
    }
    private static void setColorScheme(int[] scheme) {
        System.arraycopy(scheme, 0, currentTheme, 0 , currentTheme.length);
    }

    public static int getColor(byte color) {
        return 0xff000000 | getScheme()[color];
    }

    public static int getColorWithAlpha(int color) {
        return 0x33000000 | getScheme()[color];
    }

    public static int getInversColor(int c) {
        return 0xFF550000 ^ getScheme()[c];
    }
}