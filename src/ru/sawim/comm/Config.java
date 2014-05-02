package ru.sawim.comm;

import ru.sawim.SawimApplication;

import java.io.InputStream;
import java.util.Vector;


public final class Config {
    private String name;
    private String[] keys;
    private String[] values;

    public static String loadResource(String path) {
        String res = "";
        InputStream stream = null;
        try {
            stream = SawimApplication.getResourceAsStream(path);
            byte[] str = new byte[stream.available()];
            stream.read(str);
            res = StringConvertor.utf8beByteArrayToString(str, 0, str.length);
        } catch (Exception e) {
        }
        try {
            stream.close();
        } catch (Exception e) {
        }
        return res;
    }

    private String loadLocateResource(String path) {
        String lang = JLocale.getSystemLanguage().toUpperCase();
        int index = path.lastIndexOf('.');
        if (-1 == index) {
            index = path.length();
        }
        String localPath = path.substring(0, index)
                + "." + lang + path.substring(index);
        String config = Config.loadResource(localPath).trim();
        if (0 == config.length()) {
            return Config.loadResource(path).trim();
        }
        return config;
    }

    public Config load(String path) {
        parseConfig(Config.loadResource(path), 0);
        return this;
    }

    public Config loadLocale(String path) {
        parseConfig(loadLocateResource(path), 0);
        return this;
    }

    private char getChar(String content, int index) {
        return (index < content.length()) ? content.charAt(index) : '\n';
    }

    private int parseComment(String content, int index) {
        if (';' != getChar(content, index + 1)) {
            System.out.println("Warning! Comment is written in bad style at " + name + ":" + index);
        }

        for (; index <= content.length(); ++index) {
            if ('\n' == getChar(content, index)) return index;
        }
        return index;
    }

    private int parseName(String content, int index) {
        int beginPos = index;
        for (; index <= content.length(); ++index) {
            char ch = getChar(content, index);
            if ('[' == ch) {
                beginPos = index + 1;
                break;

            } else if (';' == ch) {
                index = parseComment(content, index) - 1;
                continue;
            }
        }
        for (; index <= content.length(); ++index) {
            char ch = getChar(content, index);
            if (']' == ch) {
                this.name = unescape(content.substring(beginPos, index));
                return index + 1;
            }
        }
        return index;
    }

    private int parseConfig(String content, int index) {
        final int PARSER_LINE = 1;
        final int PARSER_FROM = 2;
        final int PARSER_FROM_ESCAPE = 3;
        final int PARSER_TO = 4;
        final int PARSER_COMMENT = 5;

        Vector _keys = new Vector();
        Vector _values = new Vector();

        int state = PARSER_LINE;
        int beginPos = index;
        for (; index <= content.length(); ++index) {
            char ch = getChar(content, index);
            switch (state) {
                case PARSER_LINE:
                    if ('[' == ch) {
                        this.keys = vectorToArray(_keys);
                        this.values = vectorToArray(_values);
                        return index - 1;

                    } else if ((' ' == ch) || ('\n' == ch) || ('\r' == ch)) {

                    } else if (';' == ch) {
                        index = parseComment(content, index) - 1;
                        continue;

                    } else if ('\\' == ch) {
                        beginPos = index;
                        state = PARSER_FROM_ESCAPE;

                    } else {
                        beginPos = index;
                        state = PARSER_FROM;
                    }
                    break;

                case PARSER_FROM:
                    if ('=' == ch) {
                        _keys.addElement(unescape(content.substring(beginPos, index)));
                        beginPos = index + 1;
                        state = PARSER_TO;

                    } else if ('\\' == ch) {
                        state = PARSER_FROM_ESCAPE;
                    }
                    break;

                case PARSER_FROM_ESCAPE:
                    state = PARSER_FROM;
                    break;

                case PARSER_TO:
                    if (('\n' == ch) || ('\r' == ch)) {
                        _values.addElement(unescape(content.substring(beginPos, index)));
                        state = PARSER_LINE;
                    }
                    break;

                case PARSER_COMMENT:
                    if (('\n' == ch) || ('\r' == ch)) {
                        state = PARSER_LINE;
                    }
                    break;
            }
        }
        this.keys = vectorToArray(_keys);
        this.values = vectorToArray(_values);
        return index;
    }

    public static void parseIniConfig(String content, Vector configs) {
        try {
            Config currentConfig = new Config();
            int index = 0;
            while (index < content.length()) {
                index = currentConfig.parseName(content, index);
                index = currentConfig.parseConfig(content, index);
                if (!currentConfig.isEmpty()) {
                    configs.addElement(currentConfig);
                    currentConfig = new Config();
                }
            }
            if (!currentConfig.isEmpty()) {
                configs.addElement(currentConfig);
            }
        } catch (Exception e) {
        }
    }

    private String unescape(String str) {
        str = str.trim();
        if (-1 == str.indexOf('\\')) {
            return str;
        }
        StringBuilder buffer = new StringBuilder();
        boolean escape = false;
        for (int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if (escape && ('n' == ch)) {
                ch = '\n';
            }
            escape = !escape && ('\\' == ch);
            if (!escape) {
                buffer.append(ch);
            }
        }
        return buffer.toString();
    }

    public static String getConfigValue(String key, String path) {
        if (StringConvertor.isEmpty(key)) {
            return null;
        }
        final int PARSER_LINE = 0;
        final int PARSER_NAME = 1;
        final int PARSER_FROM = 2;
        final int PARSER_TO = 3;
        final int PARSER_COMMENT = 6;
        int state = PARSER_LINE;
        int beginPos = 0;
        String name = null;
        String currentKey = null;

        String content = loadResource(path);
        final int contentLenght = content.length();
        for (int i = 0; i <= contentLenght; ++i) {
            char ch = (i < contentLenght) ? content.charAt(i) : '\n';
            switch (state) {
                case PARSER_LINE:
                    if (';' == ch) {
                        state = PARSER_COMMENT;
                    } else if (('\n' == ch) || ('\r' == ch) || (' ' == ch)) {
                        beginPos = i + 1;
                    } else {
                        beginPos = i;
                        state = PARSER_FROM;
                    }
                    break;

                case PARSER_FROM:
                    if ('=' == ch) {
                        currentKey = content.substring(beginPos, i).trim();
                        beginPos = i + 1;
                        if (key.equalsIgnoreCase(currentKey)) {
                            state = PARSER_TO;
                        } else {
                            state = PARSER_COMMENT;
                        }
                    }
                    break;

                case PARSER_TO:
                    if (('\n' == ch) || ('\r' == ch)) {
                        return content.substring(beginPos, i).trim();
                    }
                    break;

                case PARSER_COMMENT:
                    if (('\n' == ch) || ('\r' == ch)) {
                        state = PARSER_LINE;
                    }
                    break;
            }
        }
        return null;
    }

    private String[] vectorToArray(Vector v) {
        String[] result = new String[v.size()];
        v.copyInto(result);
        return result;
    }

    public Config() {
    }

    public Config(String content) {
        parseConfig(StringConvertor.notNull(content), 0);
    }

    public final String getName() {
        return name;
    }

    public final String[] getKeys() {
        return keys;
    }

    public final String[] getValues() {
        return values;
    }

    private boolean isEmpty() {
        return (null == keys) || (0 == keys.length);
    }

    public final String getValue(String key) {
        for (int i = 0; i < keys.length; ++i) {
            if (key.equals(keys[i])) return values[i];
        }
        return null;
    }
}