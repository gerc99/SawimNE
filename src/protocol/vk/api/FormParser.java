package protocol.vk.api;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 09.03.13 7:33
 *
 * @author vladimir
 */
public class FormParser {
    private String removeComments(String html) {
        StringBuilder sb = new StringBuilder();
        int startComment = 0;
        int endComment = 0;
        startComment = html.indexOf("<!--", endComment);
        while (0 < startComment) {
            endComment = html.indexOf("-->", startComment + 4);
            sb.append(html.substring(0, startComment));
            startComment = html.indexOf("<!--", endComment);
        }
        sb.append(html.substring(endComment));
        return sb.toString();
    }

    private String getForm(String html) {
        int start = html.indexOf("<form");
        return html.substring(start, html.indexOf("</form>", start));
    }

    private int startTag(String form, String tag, int start) {
        return form.indexOf("<" + tag, start);
    }

    private String getAttribute(String form, String tag, String attr, int start) {
        start = startTag(form, tag, start);
        return getAttribute(form, attr, start);
    }

    private String getAttribute(String form, String attr, int start) {
        int from = form.indexOf(attr, start);
        if (-1 == from) return "";
        if (form.indexOf(">", start) < from) return "";
        from += attr.length() + "=".length();
        char q = form.charAt(from);
        return form.substring(from + 1, form.indexOf(q, from + 1));
    }

    public HashMap<String, String> process(String html) {
        HashMap<String, String> keys = new HashMap<String, String>();
        String form = removeComments(getForm(html));
        keys.put("@method", getAttribute(form, "form", "method", 0));
        keys.put("@action", getAttribute(form, "form", "action", 0));
        int start = startTag(form, "input", 0);
        while (0 < start) {
            String key = getAttribute(form, "input", "name", start);
            if (!"".equals(key) && !key.startsWith("@")) {
                keys.put(key, getAttribute(form, "input", "value", start));
            }
            start = startTag(form, "input", start + 1);
        }
        return keys;
    }
}
