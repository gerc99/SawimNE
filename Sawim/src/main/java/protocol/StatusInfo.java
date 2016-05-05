package protocol;

import ru.sawim.R;
import ru.sawim.comm.JLocale;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;


public final class StatusInfo {
    public static final byte STATUS_OFFLINE = 0;
    public static final byte STATUS_ONLINE = 1;
    public static final byte STATUS_AWAY = 2;
    public static final byte STATUS_CHAT = 3;

    public static final byte STATUS_XA = 9;
    public static final byte STATUS_DND = 11;

    public static final byte STATUS_UNDETERMINATED = 10;
    public static final byte STATUS_INVISIBLE = 12;

    public static final byte STATUS_NA = 9;
    public static final byte STATUS_OCCUPIED = 10;
    public static final byte STATUS_INVIS_ALL = 13;

    public static final byte STATUS_EVIL = 6;
    public static final byte STATUS_DEPRESSION = 7;

    public static final byte STATUS_HOME = 4;
    public static final byte STATUS_WORK = 5;
    public static final byte STATUS_LUNCH = 8;

    public static final byte STATUS_NOT_IN_LIST = 14;

    public final ImageList statusIcons;
    public final int[] statusIconIndex;
    public final byte[] applicableStatuses;
    private static final int[] statusWidth = {29, 1, 7, 0, 2, 3, 4, 5, 6, 8, 9, 10, 11, 12, 14};
    private static final int[] statusNames = {
            R.string.status_offline,
            R.string.status_online,
            R.string.status_away,
            R.string.status_chat,
            R.string.status_home,
            R.string.status_work,
            R.string.status_evil,
            R.string.status_depression,
            R.string.status_lunch,
            R.string.status_na,
            R.string.status_occupied,
            R.string.status_dnd,
            R.string.status_invisible,
            R.string.status_invis_all,
            R.string.status_not_in_list
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