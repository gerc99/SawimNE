package protocol.xmpp;

import ru.sawim.SawimException;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class XmlNode {

    public String name;
    public String value;

    private ConcurrentHashMap<String, String> attribs = new ConcurrentHashMap<>();
    private List<XmlNode> children = new ArrayList<>();

    private static final int MAX_BIN_VALUE_SIZE = 54 * 1024;
    private static final int MAX_VALUE_SIZE = 10 * 1024;
    public final static String S_ID = "id";
    public static final String S_JID = "jid";
    public static final String S_NICK = "nick";
    public static final String S_NAME = "name";
    public static final String S_ROLE = "role";
    public static final String S_AFFILIATION = "affiliation";
    private static final String S_BINVAL = "BINVAL";
    public static final String S_XMLNS = "xmlns";

    private XmlNode() {
    }

    public XmlNode(String name) {
        this.name = name;
    }

    private XmlNode unsafeChildAt(int index) {
        return children.get(index);
    }

    public XmlNode childAt(int index) {
        if (children.size() <= index) {
            return null;
        }
        return children.get(index);
    }

    public int childrenCount() {
        return children.size();
    }

    public String getAttribute(String key) {
        return attribs.get(key);
    }

    public XmlNode putAttribute(String key, String value) {
        if (S_JID.equals(key)) {
            key = S_JID;
        } else if (S_NAME.equals(key)) {
            key = S_NAME;
        }
        attribs.put(key, value);
        return this;
    }

    public String getXmlns() {
        String xmlns = getAttribute(S_XMLNS);
        if (null == xmlns) {
            Enumeration e = attribs.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (key.startsWith(S_XMLNS.concat(":"))) {
                    return getAttribute(key);
                }
            }
        }
        return xmlns;
    }

    public String getId() {
        return getAttribute(S_ID);
    }

    public void setId(String id) {
        putAttribute(S_ID, id);
    }

    public String getType() {
        return getAttribute(XmlConstants.S_TYPE);
    }

    public void setType(String type) {
        putAttribute(XmlConstants.S_TYPE, type);
    }

    public static XmlNode parse(Socket socket) throws SawimException {
        char ch = socket.readChar();
        if ('<' != ch) {
            return null;
        }
        ch = removeXmlHeader(socket);
        if ('/' == ch) {
            throw new SawimException(128, 0);
        }
        XmlNode xml = new XmlNode();
        boolean parsed = xml.parseNode(socket, ch);
        return parsed ? xml : null;
    }

    private void setName(String tagName) {
        if (-1 == tagName.indexOf(':') || tagName.contains("stream:")) {
            name = tagName;
            return;
        }
        name = tagName.substring(tagName.indexOf(':') + 1);
    }

    private int getMaxDataSize(String name) {
        if (S_BINVAL.equals(name)) {
            return MAX_BIN_VALUE_SIZE * 2;
        }
        return MAX_VALUE_SIZE;
    }

    private String readCdata(Socket socket) throws SawimException {
        StringBuilder out = new StringBuilder();
        char ch = socket.readChar();
        int maxSize = getMaxDataSize(name);
        int size = 0;
        for (int state = 0; state < 3; ) {
            ch = socket.readChar();
            if (size == maxSize) {
                out.append(ch);
                size++;
            }
            if (']' == ch) {
                state = Math.min(state + 1, 2);
            } else if ((2 == state) && ('>' == ch)) {
                state++;
            } else {
                state = 0;
            }

        }
        out.delete(0, 7);
        out.delete(Math.max(0, out.length() - 3), out.length());
        return out.toString();
    }

    private void readEscapedChar(StringBuilder out, Socket socket) throws SawimException {
        StringBuilder buffer = new StringBuilder(6);
        int limit = 6;
        char ch = socket.readChar();
        while (';' != ch) {
            if (0 < limit) {
                buffer.append(ch);
                limit--;
            }
            ch = socket.readChar();
        }
        if (0 == buffer.length()) {
            out.append('&');
            return;
        }
        String code = buffer.toString();
        if ("quot".equals(code)) {
            out.append('\"');
        } else if ("gt".equals(code)) {
            out.append('>');
        } else if ("lt".equals(code)) {
            out.append('<');
        } else if ("apos".equals(code)) {
            out.append('\'');
        } else if ("amp".equals(code)) {
            out.append('&');
        } else if ('#' == buffer.charAt(0)) {
            try {
                buffer.deleteCharAt(0);
                int radix = 10;
                if ('x' == buffer.charAt(0)) {
                    buffer.deleteCharAt(0);
                    radix = 16;
                }
                out.append((char) Integer.parseInt(buffer.toString(), radix));
            } catch (Exception e) {
                out.append('?');
            }
        } else {
            out.append('&');
            out.append(code);
            out.append(';');
        }
    }

    private String readString(Socket socket, char endCh, int limit) throws SawimException {
        char ch = socket.readChar();
        if (endCh == ch) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        while (endCh != ch) {
            if (sb.length() < limit) {
                if ('\t' == ch) {
                    sb.append("  ");
                } else if ('&' != ch) {
                    sb.append(ch);
                } else {
                    readEscapedChar(sb, socket);
                }
            }
            ch = socket.readChar();
        }
        String s = sb.toString();
        try {
            s = new String(s.getBytes("ISO8859-1"), "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return s;
    }

    private boolean parseNode(Socket socket, char ch0) throws SawimException {
        // tag name
        char ch = ch0;
        if ('!' == ch) {
            readCdata(socket);
            return false;
        }
        if ('/' == ch) {
            ch = socket.readChar();
            while ('>' != ch) {
                ch = socket.readChar();
            }
            return false;
        }
        StringBuilder tagName = new StringBuilder();
        while (' ' != ch && '>' != ch) {
            tagName.append(ch);
            ch = socket.readChar();
            if ('/' == ch) {
                setName(tagName.toString());
                ch = socket.readChar(); // '>'
                return true;
            }
        }
        setName(tagName.toString());
        tagName = null;

        // tag attributes
        while ('>' != ch) {
            while (' ' == ch) {
                ch = socket.readChar();
            }
            if ('/' == ch) {
                ch = socket.readChar(); // '>'
                return true;
            }
            if ('>' == ch) {
                break;
            }
            StringBuilder attrName = new StringBuilder();
            while ('=' != ch) {
                attrName.append(ch);
                ch = socket.readChar();
            }

            char startValueCh = socket.readChar(); // '"' or '\''
            String attribValue = readString(socket, startValueCh, 2 * 1024);
            if (0 < attrName.length()) {
                if (null == attribValue) {
                    attribValue = "";
                }
                putAttribute(attrName.toString(), attribValue);
            }
            ch = socket.readChar();
        }
        if ("stream:stream".equals(name)) {
            return true;
        }

        value = readString(socket, '<', getMaxDataSize(name));

        while (true) {
            ch = socket.readChar();
            if ('!' == ch) {
                value = readCdata(socket);

            } else {
                XmlNode xml = new XmlNode();
                if (!xml.parseNode(socket, ch)) {
                    break;
                }
                children.add(xml);
            }

            ch = socket.readChar();
            while ('<' != ch) {
                ch = socket.readChar();
            }
        }
        if (StringConvertor.isEmpty(value)) {
            value = null;
        }
        return true;
    }

    private static char removeXmlHeader(Socket socket) throws SawimException {
        char ch = socket.readChar();
        if ('?' != ch) {
            return ch;
        }
        while ('?' != ch) {
            ch = socket.readChar();
        }
        ch = socket.readChar();
        ch = socket.readChar();
        while ('<' != ch) {
            ch = socket.readChar();
        }
        return socket.readChar();
    }

    public final XmlNode popChildNode() {
        XmlNode node = childAt(0);
        children.remove(0);
        return node;
    }

    public final void removeNode(String name) {
        for (int i = 0; i < children.size(); ++i) {
            if (unsafeChildAt(i).is(name)) {
                children.remove(i);
                return;
            }
        }
    }

    public final boolean is(String name) {
        return this.name.equals(name);
    }

    public XmlNode getFirstNodeRecursive(String name) {
        for (int i = 0; i < children.size(); ++i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(name)) {
                return node;
            }
            XmlNode result = node.getFirstNodeRecursive(name);
            if (null != result) {
                return result;
            }
        }
        return null;
    }

    public String getFirstNodeValueRecursive(String name) {
        XmlNode node = getFirstNodeRecursive(name);
        return (null == node) ? null : node.value;
    }

    public XmlNode getFirstNode(String name) {
        for (int i = 0; i < children.size(); ++i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(name)) {
                return node;
            }
        }
        return null;
    }

    public XmlNode getFirstNode(String name, String xmlns) {
        for (int i = 0; i < children.size(); ++i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(name) && xmlns.equals(node.getXmlns())) {
                return node;
            }
        }
        return null;
    }

    public XmlNode getXNode(String xmlns) {
        return getFirstNode("x", xmlns);
    }

    public String getFirstNodeValue(String name) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.value;
    }

    public String getFirstNodeValue(String parentNodeName, String nodeName) {
        XmlNode parentNode = getFirstNode(parentNodeName);
        return (null == parentNode) ? null : parentNode.getFirstNodeValue(nodeName);
    }

    public String getFirstNodeValue(String tag, String[] cond, String subtag) {
        for (int i = 0; i < childrenCount(); ++i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(tag) && node.isContains(cond)) {
                return node.getFirstNodeValue(subtag);
            }
        }
        return null;
    }

    public String getFirstNodeValue(String tag, String[] subtags, String subtag, boolean isDefault) {
        String result = getFirstNodeValue(tag, subtags, subtag);
        if (null != result) {
            return result;
        }
        for (int i = 0; i < childrenCount(); ++i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(tag) && (0 < node.childrenCount())) {
                XmlNode firstNode = node.unsafeChildAt(0);
                if (null != firstNode.value) {
                    return node.getFirstNodeValue(subtag);
                }
            }
        }
        return null;
    }

    public String getFirstNodeAttribute(String name, String key) {
        XmlNode node = getFirstNode(name);
        return (null == node) ? null : node.getAttribute(key);
    }

    public boolean contains(String name) {
        return null != getFirstNode(name);
    }

    private String _toString(StringBuilder sb, String spaces) {
        sb.append(spaces).append("<").append(name);
        if (0 != attribs.size()) {
            Enumeration e = attribs.keys();
            while (e.hasMoreElements()) {
                Object k = e.nextElement();
                sb.append(" ").append(k).append("='").append(attribs.get(k)).append("'");
            }
        }
        if (0 != childrenCount()) {
            sb.append(">");
            sb.append("\n");
            for (int i = 0; i < childrenCount(); ++i) {
                unsafeChildAt(i)._toString(sb, spaces + " ");
                sb.append("\n");
            }
            sb.append(spaces).append("</").append(name).append(">");
        } else if (null != value) {
            sb.append(">");
            sb.append(value);
            sb.append("</").append(name).append(">");
        } else {
            sb.append("/>");
        }
        return sb.toString();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        _toString(sb, "");
        return sb.toString();
    }

    public String popValue() {
        String result = value;
        value = null;
        return result;
    }

    public byte[] popBinValue() {
        if (null == value) {
            return null;
        }
        return Util.base64decode(popValue());
    }

    public byte[] getBinValue() {
        if (null == value) {
            return null;
        }
        return Util.base64decode(value);
    }

    private boolean isContains(String[] subtags) {
        if (null == subtags) {
            return true;
        }
        for (String subtag : subtags) {
            if (!contains(subtag)) {
                return false;
            }
        }
        return true;
    }

    public void setValue(String subtag, String value) {
        XmlNode content = getFirstNode(subtag);
        if (null == content) {
            content = new XmlNode(subtag);
            children.add(content);
        }
        content.value = value;
    }

    public void setValue(String tag, String[] subtags, String subtag, String value) {
        for (int i = 0; i < childrenCount(); ++i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(tag) && node.isContains(subtags)) {
                node.setValue(subtag, value);
                return;
            }
        }
        if (StringConvertor.isEmpty(value)) {
            return;
        }

        XmlNode node = new XmlNode(tag);
        children.add(node);
        if (null != subtags) {
            for (String subtag1 : subtags) {
                node.children.add(new XmlNode(subtag1));
            }
        }
        node.setValue(subtag, value);
    }

    public void removeBadVCardTags(String tag) {
        for (int i = childrenCount() - 1; 0 <= i; --i) {
            XmlNode node = unsafeChildAt(i);
            if (node.is(tag) && (0 < node.childrenCount())) {
                if (null != node.unsafeChildAt(0).value) {
                    children.remove(i);
                }
            }
        }
    }

    private boolean isEmptySubNodes() {
        if (null != value) return false;
        for (int i = childrenCount() - 1; i >= 0; --i) {
            if (null != unsafeChildAt(i).value) {
                return false;
            }
        }
        return true;
    }

    public void cleanXmlTree() {
        for (int i = childrenCount() - 1; i >= 0; --i) {
            if (unsafeChildAt(i).isEmptySubNodes()) {
                children.remove(i);
            }
        }
    }

    public void toString(StringBuilder sb) {
        sb.append('<').append(name);
        if (0 != attribs.size()) {
            Enumeration e = attribs.keys();
            while (e.hasMoreElements()) {
                String k = (String) e.nextElement();
                String v = attribs.get(k);
                sb.append(' ').append(Util.xmlEscape(k)).append("='")
                        .append(Util.xmlEscape(v)).append("'");
            }
        }
        if ((0 == childrenCount()) && StringConvertor.isEmpty(value)) {
            sb.append("/>");
            return;
        }
        sb.append('>');

        if (0 != childrenCount()) {
            for (int i = 0; i < childrenCount(); ++i) {
                unsafeChildAt(i).toString(sb);
            }

        } else if (null != value) {
            sb.append(Util.xmlEscape(value));
        }

        sb.append("</").append(name).append(">");
    }

    public static XmlNode addXmlns(String name, String xmlns) {
        XmlNode child = new XmlNode(name);
        child.putAttribute(S_XMLNS, xmlns);
        return child;
    }

    public static XmlNode getEmptyVCard() {
        XmlNode vCard = XmlNode.addXmlns("vCard", "vcard-temp");
        vCard.putAttribute("version", "2.0");
        vCard.putAttribute("prodid", "-/" + "/HandGen/" + "/NONSGML vGen v1.0/" + "/EN");
        return vCard;
    }

    public XmlNode addSubTag(String subtag) {
        XmlNode content = new XmlNode(subtag);
        children.add(content);
        return content;
    }

    public XmlNode addNode(XmlNode subNode) {
        /*if (value == null) {
            value = subNode.toString();
        } else {
            value += subNode.toString();
        }*/
        children.add(subNode);
        return subNode;
    }
}