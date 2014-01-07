


package protocol.xmpp;

import DrawControls.icons.ImageList;
import protocol.XStatusInfo;
import sawim.comm.Config;
import sawim.comm.StringConvertor;


public class XmppXStatus {
    private final String[] xstatusCaps;
    private final XStatusInfo info;

    public static final int TYPE_X = 0x1000;
    public static final int TYPE_MOOD = 0x2000;
    public static final int TYPE_ACTIVITY = 0x4000;
    public static final int TYPE_TUNE = 0x8000;

    public static final String XSTATUS_TEXT_NONE = "qip:none";
    public static final String XSTATUS_START = "qip:";

    public XmppXStatus() {
        Config cfg = new Config().loadLocale("/jabber-xstatus.txt");
        xstatusCaps = cfg.getKeys();
        String[] xstatusNames = cfg.getValues();
        ImageList xstatusIcons = ImageList.createImageList("/jabber-xstatus.png");
        info = new XStatusInfo(xstatusIcons, xstatusNames);
    }

    public XStatusInfo getInfo() {
        return info;
    }

    private int getType(String type) {
        if (type.startsWith("q" + "ip")) {
            return TYPE_X;
        }
        if (type.startsWith("m" + "ood")) {
            return TYPE_MOOD;
        }
        if (type.startsWith("a" + "ctivity")) {
            return TYPE_ACTIVITY;
        }
        if (type.startsWith("t" + "une")) {
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



