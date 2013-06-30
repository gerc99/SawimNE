

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

    public static final byte THEME_CONTACT_ONLINE       = 8;
    public static final byte THEME_CONTACT_WITH_CHAT    = 9;
    public static final byte THEME_CONTACT_OFFLINE      = 10;
    public static final byte THEME_CONTACT_TEMP         = 11;

    public static final byte THEME_SCROLL_BACK          = 12;
    public static final byte THEME_SELECTION_RECT       = 13;
    public static final byte THEME_SPLASH_BACKGROUND    = 15;
    public static final byte THEME_SPLASH_LOGO_TEXT     = 16;
    public static final byte THEME_SPLASH_MESSAGES      = 17;
    public static final byte THEME_SPLASH_DATE          = 18;
    public static final byte THEME_SPLASH_PROGRESS_BACK = 19;
    public static final byte THEME_SPLASH_PROGRESS_TEXT = 20;
    public static final byte THEME_SPLASH_LOCK_BACK     = 21;
    public static final byte THEME_SPLASH_LOCK_TEXT     = 22;

    public static final byte THEME_MAGIC_EYE_NUMBER     = 23;
    public static final byte THEME_MAGIC_EYE_ACTION     = 24;
    public static final byte THEME_MAGIC_EYE_NL_USER    = 25;
    public static final byte THEME_MAGIC_EYE_USER       = 26;
    public static final byte THEME_MAGIC_EYE_TEXT       = 27;


    public static final byte THEME_MENU_BACK            = 29;
    public static final byte THEME_MENU_BORDER          = 30;
    public static final byte THEME_MENU_TEXT            = 31;
    public static final byte THEME_MENU_SEL_BACK        = 32;

    public static final byte THEME_MENU_SEL_TEXT        = 34;

    public static final byte THEME_POPUP_BORDER         = 36;
    public static final byte THEME_POPUP_BACK           = 37;
    public static final byte THEME_POPUP_TEXT           = 38;

    public static final byte THEME_GROUP                = 39;
    public static final byte THEME_CHAT_HIGHLIGHT_MSG   = 40;
    public static final byte THEME_SELECTION_BACK       = 41;
    public static final byte THEME_CONTACT_STATUS       = 42;
    public static final byte THEME_PROTOCOL             = 43;
    public static final byte THEME_PROTOCOL_BACK        = 44;

    public static final byte THEME_FORM_EDIT            = 45;
    public static final byte THEME_FORM_TEXT            = 46;
    public static final byte THEME_FORM_BORDER          = 47;
    public static final byte THEME_FORM_BACK            = 48;

    public static final byte THEME_CHAT_BG_IN           = 49;
    public static final byte THEME_CHAT_BG_OUT          = 50;
    public static final byte THEME_CHAT_BG_IN_ODD       = 51;
    public static final byte THEME_CHAT_BG_OUT_ODD      = 52;
    public static final byte THEME_CHAT_BG_MARKED       = 53;
    public static final byte THEME_CHAT_BG_SYSTEM       = 54;
    public static final byte FONT_STYLE_PLAIN = 0;
    public static final byte FONT_STYLE_BOLD = 1;

    private Scheme() {
    }

    private static final int[] baseTheme = {
        0xFFFFFF, 0x000000, 0xF0F0F0, 0x000000, 0x0000FF,
        0xFF0000, 0x0000FF, 0x808080, 0x000000, 0x0000FF,
        0x404040, 0x808080, 0x808080, 0x0000FF, 0xE0E0E0,
        0x006FB1, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF,
        0x000000, 0xFFFFFF, 0x000000, 0x0000FF, 0x000000,
        0xFF0000, 0x0000FF, 0x000000, 0x606060, 0xD0D0D0,
        0x202020, 0x202020, 0xC0F0C0, 0xD0D0D0, 0x000000,
        0x606060, 0x202020, 0xD0D0D0, 0x202020, 0x000000,
        0x800000, 0xC0C0FF, 0x808080, 0x000000, 0xF0F0F0,
        0x0000FF, 0x000000, 0x0000FF, 0xFFFFFF,
        0xFFE7BA, 0xBFEFFF, 0xEED8AE, 0xB2DFEE, 0xFFA54F, 0xF8F8FF};

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

        themeNames[0]  = "Black on white (default)";
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
                        theme[48] = theme[0];
                        theme[49] = theme[50] = theme[51] = theme[52] = theme[53] = theme[54] = theme[0];
                    } else if (1 == index) {
                        theme[46] = theme[41] = theme[40] = theme[39] = theme[1];
                    } else if (2 == index) {
                        theme[44] = theme[2];
                    } else if (4 == index) {
                        theme[45] = theme[4];
                    } else if (13 == index) {
                        theme[47] = theme[13];
                    } else if (10 == index) {
                        theme[42] = theme[10];
                    } else if (39 == index) {
                        theme[43] = theme[39];
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
}