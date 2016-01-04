package protocol.xmpp;

/**
 * Created by gerc on 02.01.2016.
 */
public class XmlConstants {

    public static final String[] statusCodes = {
            "unavailable",
            "",
            "away",
            "chat",
            "",
            "",
            "",
            "",
            "",
            "xa",
            "",
            "dnd",
            "",
            ""
    };
    public static final String S_TEXT = "text";
    public static final String S_FROM = "from";
    public static final String S_IQ = "iq";
    public static final String S_TO = "to";
    public static final String S_TYPE = "type";
    public static final String S_ERROR = "error";
    public static final String S_NONE = "none";
    public static final String S_NODE = "node";
    public static final String S_NICK = "nick";
    public static final String S_SET = "set";
    public static final String S_REMOVE = "remove";
    public static final String S_RESULT = "result";
    public static final String S_GROUP = "group";
    public static final String S_ITEM = "item";
    public static final String S_ITEMS = "items";
    public static final String S_TRUE = "true";
    public static final String S_FALSE = "false";
    public static final String S_GET = "get";
    public static final String S_TIME = "time";
    public static final String S_TITLE = "title";
    public static final String S_CODE = "code";
    public static final String S_QUERY = "query";
    public static final String S_STATUS = "status";
    public static final String S_SUBJECT = "subject";
    public static final String S_BODY = "body";
    public static final String S_URL = "url";
    public static final String S_DESC = "desc";
    public static final String S_COMPOSING = "composing";
    public static final String S_ACTIVE = "active";
    public static final String S_PAUSED = "paused";
    public static final String S_CHAT = "chat";
    public static final String S_GROUPCHAT = "groupchat";
    public static final String S_HEADLINE = "headline";
    public static final String GET_ROSTER_XML = "<iq type='get' id='roster'>"
            + "<query xmlns='jabber:iq:roster'/>"
            + "</iq>";
    public static final byte IQ_TYPE_RESULT = 0;
    public static final byte IQ_TYPE_GET = 1;
    public static final byte IQ_TYPE_SET = 2;
    public static final byte IQ_TYPE_ERROR = 3;
}
