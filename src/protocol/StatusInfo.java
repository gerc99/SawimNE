package protocol;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import sawim.util.JLocale;


public final class StatusInfo {
    public static final byte STATUS_OFFLINE = 0;
    public static final byte STATUS_ONLINE  = 1;
    public static final byte STATUS_AWAY    = 2;
    public static final byte STATUS_CHAT    = 3;
    
    public static final byte STATUS_XA = 9;
    public static final byte STATUS_DND = 11;
    
    public static final byte STATUS_UNDETERMINATED = 10;
    public static final byte STATUS_INVISIBLE      = 12;
    
    public static final byte STATUS_NA         = 9;
    public static final byte STATUS_OCCUPIED   = 10;
    public static final byte STATUS_INVIS_ALL  = 13;

    public static final byte STATUS_EVIL       = 6;
    public static final byte STATUS_DEPRESSION = 7;

    public static final byte STATUS_HOME       = 4;
    public static final byte STATUS_WORK       = 5;
    public static final byte STATUS_LUNCH      = 8;
    
    public static final byte STATUS_NOT_IN_LIST  = 14;

    public final ImageList statusIcons;
    public final int[] statusIconIndex;
    public final byte[] applicableStatuses;
    private static final int[] statusWidth = {29, 1, 7, 0, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 14}; 
    private static final String[] statusNames = {
        "status_offline",
        "status_online",
        "status_away",
        "status_chat",
        "status_home",
        "status_work",
        "status_evil",
        "status_depression",
        "status_lunch",
        "status_na",
        "status_occupied",
        "status_dnd",
        "status_invisible",
        "status_invis_all",
        "status_not_in_list"
    };
    
    
    public StatusInfo(ImageList statuses, int[] index, byte[] applicableStatuses) {
        statusIcons = statuses;
        statusIconIndex = index;
        if (null == statuses.iconAt(index[StatusInfo.STATUS_NOT_IN_LIST])) {
            index[StatusInfo.STATUS_NOT_IN_LIST] = index[StatusInfo.STATUS_OFFLINE];
        }
        this.applicableStatuses = applicableStatuses;
    }
    public String getName(byte statusIndex) {
        return JLocale.getString(statusNames[statusIndex]);
    }

    public Icon getIcon(byte statusIndex) {
        return statusIcons.iconAt(statusIconIndex[statusIndex]);
    }
    public static int getWidth(byte status) {
        return statusWidth[status];
    }
    public final boolean isAway(byte statusIndex) {
        switch (statusIndex) {
            case StatusInfo.STATUS_OFFLINE:
            case StatusInfo.STATUS_AWAY:
            case StatusInfo.STATUS_DND:
            case StatusInfo.STATUS_XA:
            case StatusInfo.STATUS_UNDETERMINATED:
            case StatusInfo.STATUS_INVISIBLE:
            case StatusInfo.STATUS_INVIS_ALL:
            case StatusInfo.STATUS_NOT_IN_LIST:
                return true;
        }
        return false;
    }
	public final boolean isOffline(byte statusIndex) {
        if (statusIndex == StatusInfo.STATUS_OFFLINE) {
            return true;
        }
        return false;
    }
}