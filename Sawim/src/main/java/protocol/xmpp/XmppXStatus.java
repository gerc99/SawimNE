


package protocol.xmpp;

import protocol.XStatusInfo;
import ru.sawim.R;
import ru.sawim.comm.StringConvertor;
import ru.sawim.icons.ImageList;


public class XmppXStatus {
    private XStatusInfo info;

    public static final int TYPE_X = 0x1000;
    public static final int TYPE_MOOD = 0x2000;
    public static final int TYPE_ACTIVITY = 0x4000;
    public static final int TYPE_TUNE = 0x8000;

    public static final String XSTATUS_TEXT_NONE = "qip:none";
    public static final String XSTATUS_START = "qip:";

    private static final String[] xstatusCaps = {
            "qip:1 mood:angry pyicq:xstatus_angry",
            "qip:2 activity:grooming:taking_a_bath pyicq:xstatus_taking_a_bath",
            "qip:3 mood:stressed pyicq:xstatus_tired",
            "qip:4 activity:relaxing:partying pyicq:xstatus_party",
            "qip:5 activity:drinking:having_a_beer pyicq:xstatus_drinking_beer",
            "qip:6 mood:serious pyicq:xstatus_thinking",
            "qip:7 activity:eating pyicq:xstatus_eating",
            "qip:8 activity:relaxing:watching_tv pyicq:xstatus_watching_tv",
            "qip:9 activity:relaxing:socializing pyicq:xstatus_meeting",
            "qip:10 activity:drinking:having_coffee pyicq:xstatus_coffee",
            "qip:11 tune:title pyicq:xstatus_listening_to_music",
            "qip:12 activity:having_appointment pyicq:xstatus_business",
            "qip:13 activity:traveling:commuting pyicq:xstatus_shooting",
            "qip:14 mood:contented mood:happy pyicq:xstatus_having_fun",
            "qip:15 activity:talking:on_the_phone pyicq:xstatus_on_the_phone",
            "qip:16 activity:relaxing:gaming pyicq:xstatus_gaming",
            "qip:17 activity:working:studying pyicq:xstatus_studying",
            "qip:18 activity:relaxing:shopping pyicq:xstatus_shopping",
            "qip:19 mood:sick pyicq:xstatus_feeling_sick",
            "qip:20 activity:inactive:sleeping pyicq:xstatus_sleeping",
            "qip:21 activity:exercising:swimming pyicq:xstatus_surfing",
            "qip:22 activity:relaxing:reading pyicq:xstatus_browsing",
            "qip:23 activity:working pyicq:xstatus_working",
            "qip:24 activity:working:writing pyicq:xstatus_typing",
            "qip:25 activity:relaxing:going_out pyicq:xstatus_cn1",
            "qip:26 pyicq:xstatus_cn2",
            "qip:27 activity:talking:in_real_life pyicq:xstatus_cn3",
            "qip:28 activity:inactive:hanging_out pyicq:xstatus_cn4",
            "qip:29 mood:excited pyicq:xstatus_cn5",
            "qip:30 mood:amazed pyicq:xstatus_de1",
            "qip:31 activity:relaxing:watching_a_movie pyicq:xstatus_de2",
            "qip:32 mood:in_love pyicq:xstatus_de3",
            "qip:cigarette",
            "qip:sex",
            "qip:33 mood:curious pyicq:xstatus_ru1",
            "qip:34 mood:flirtatious working:in_a_meeting pyicq:xstatus_ru2",
            "qip:35 mood:impressed pyicq:xstatus_ru3"
    };

    private int[] xstatusNames = {
            R.string.xstatus_angry,
            R.string.xstatus_duck,
            R.string.xstatus_tired,
            R.string.xstatus_party,
            R.string.xstatus_beer,
            R.string.xstatus_thinking,
            R.string.xstatus_eating,
            R.string.xstatus_tv,
            R.string.xstatus_friends,
            R.string.xstatus_coffee,
            R.string.xstatus_music,
            R.string.xstatus_business,
            R.string.xstatus_camera,
            R.string.xstatus_funny,
            R.string.xstatus_phone,
            R.string.xstatus_games,
            R.string.xstatus_college,
            R.string.xstatus_shopping,
            R.string.xstatus_sick,
            R.string.xstatus_sleeping,
            R.string.xstatus_surfing,
            R.string.xstatus_internet,
            R.string.xstatus_engineering,
            R.string.xstatus_typing,
            R.string.xstatus_unk,
            R.string.xstatus_ppc,
            R.string.xstatus_mobile,
            R.string.xstatus_man,
            R.string.xstatus_wc,
            R.string.xstatus_question,
            R.string.xstatus_way,
            R.string.xstatus_heart,
            R.string.xstatus_cigarette,
            R.string.xstatus_sex,
            R.string.xstatus_rambler_search,
            R.string.xstatus_rambler_love,
            R.string.xstatus_rambler_journal
    };

    public XmppXStatus() {
        ImageList xstatusIcons = ImageList.createImageList("/jabber-xstatus.png");
        info = new XStatusInfo(xstatusIcons, xstatusNames);
    }

    public XStatusInfo getInfo() {
        return info;
    }

    private int getType(String type) {
        if (type.startsWith("qip")) {
            return TYPE_X;
        }
        if (type.startsWith("mood")) {
            return TYPE_MOOD;
        }
        if (type.startsWith("activity")) {
            return TYPE_ACTIVITY;
        }
        if (type.startsWith("tune")) {
            return TYPE_TUNE;
        }
        return 0;
    }

    public int createXStatus(String id) {
        if (StringConvertor.isEmpty(id)) {
            return XStatusInfo.XSTATUS_NONE;
        }
        if (XSTATUS_TEXT_NONE.equals(id)) {
            return XStatusInfo.XSTATUS_NONE;
        }
        for (int capsIndex = 0; capsIndex < xstatusCaps.length; ++capsIndex) {
            int index = xstatusCaps[capsIndex].indexOf(id);
            if (-1 != index) {
                String xstr = xstatusCaps[capsIndex];
                final int endPos = index + id.length();
                if ((endPos < xstr.length()) && (StringConvertor.DELEMITER != xstr.charAt(endPos))) {
                    continue;
                }
                return capsIndex | getType(id);
            }
        }
        return XStatusInfo.XSTATUS_NONE;
    }

    private String substr(String str, int pos, String defval) {
        if (pos < 0) {
            return defval;
        }
        int strEnd = str.indexOf(StringConvertor.DELEMITER, pos);
        if (-1 == strEnd) {
            str = str.substring(pos);
        } else {
            str = str.substring(pos, strEnd);
        }
        return "-".equals(str) ? defval : str;
    }

    public String getCode(byte xstatusIndex) {
        if (0 == xstatusCaps.length) {
            return null;
        }
        boolean isXStatus = xstatusCaps[0].startsWith(XSTATUS_START);
        if (XStatusInfo.XSTATUS_NONE == xstatusIndex) {
            return isXStatus ? XSTATUS_TEXT_NONE : "";
        }
        if (xstatusCaps.length <= xstatusIndex) {
            return "";
        }
        return substr(xstatusCaps[xstatusIndex], 0,
                isXStatus ? XSTATUS_TEXT_NONE : "");
    }

    public String getIcqXStatus(byte xstatusIndex) {
        if (0 == xstatusCaps.length) {
            return null;
        }
        final String ICQ_XSTATUS_PREFIX = "pyicq:";
        final String ICQ_XSTATUS_NONE = "None";
        if (-1 == xstatusCaps[0].indexOf(ICQ_XSTATUS_PREFIX)) {
            return null;
        }
        if (XStatusInfo.XSTATUS_NONE == xstatusIndex) {
            return ICQ_XSTATUS_NONE;
        }
        int index = xstatusCaps[xstatusIndex].indexOf(ICQ_XSTATUS_PREFIX);
        if (-1 != index) {
            index += ICQ_XSTATUS_PREFIX.length();
        }
        return substr(xstatusCaps[xstatusIndex], index, ICQ_XSTATUS_NONE);
    }

    final boolean isType(int index, String path) {
        return (0 == (index & 0xF000)) || ((index & 0xF000) == getType(path));
    }

    public final boolean isPep(int index) {
        return TYPE_X != (index & 0xF000);
    }
}



