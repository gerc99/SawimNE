package ru.sawim.config;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 28.12.12 15:22
 *
 * @author vladimir
 */
class IniBuilder {
    private final StringBuilder sb = new StringBuilder();

    public void comment() {
        sb.append(";; ");
    }

    public void line(String key, Object value) {
        sb.append(key).append(" = ").append(toString(value)).append("\n");
    }

    private String toString(Object o) {
        if (o instanceof String) {
            return "'" + IniBuilder.escape(o.toString()) + "'";
        }
        if (o instanceof Long) {
            return o.toString();
        }
        return o.toString();
    }

    public String toString() {
        return sb.toString();
    }

    static String extract(String s) {
        s = s.trim();
        if (1 < s.length()) {
            char mark = s.charAt(0);
            if ((mark == s.charAt(s.length() - 1)) && (-1 != "\"\'".indexOf(mark))) {
                s = unescape(s.substring(1, s.length() - 1));
            }
        }
        return s;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n");
    }

    private static String unescape(String s) {
        return s.replace("\\n", "\n").replace("\\\\", "\\");
    }
}
