

package protocol.icq;

import protocol.StatusInfo;
import ru.sawim.comm.GUID;


public class IcqStatusInfo {

    public static final int STATUS_OFFLINE = 0xFFFF0000;
    public static final int STATUS_ONLINE = 0x00000000;


    public static final int STATUS_AWAY = 0x00000001;
    public static final int STATUS_DND = 0x00000002;
    public static final int STATUS_NA = 0x00000004;
    public static final int STATUS_OCCUPIED = 0x00000010;
    public static final int STATUS_INVISIBLE = 0x00000100;
    public static final int STATUS_INVIS_ALL = 0x00000200;

    public static final int STATUS_CHAT = 0x00000020;
    public static final int STATUS_EVIL = 0x00003000;
    public static final int STATUS_DEPRESSION = 0x00004000;
    public static final int STATUS_HOME = 0x00005000;
    public static final int STATUS_WORK = 0x00006000;
    public static final int STATUS_LUNCH = 0x00002001;


    private static final byte[] qipIndexes = {0, 0, 0, 0x75, 0x76, 0x77, 0x79, 0x70, 0x78, 0, 0, 0, 0, 0};

    public static byte getExtStatus(byte statusIndex) {
        return qipIndexes[statusIndex];
    }

    public static byte getStatusIndex(int status, byte[] guids) {
        if (STATUS_OFFLINE == status) return StatusInfo.STATUS_OFFLINE;
        for (int i = 0; i < guids.length; i += 16) {
            if (GUID.CAP_QIP_STATUS.equals(guids, i, 15)) {
                byte qipIndex = guids[i + 15];
                for (byte j = 0; j < qipIndexes.length; ++j) {
                    if ((0 != qipIndexes[j]) && (qipIndex == qipIndexes[j])) {
                        return j;
                    }
                }
            }
        }
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (statusCodes[i] == status) {
                return i;
            }
        }
        status &= ~(STATUS_INVISIBLE | STATUS_INVIS_ALL);
        status &= 0xFFFF;
        for (byte i = 0; i < statusCodes.length; ++i) {
            int st = statusCodes[i];
            if (0 == st) {
                continue;
            }
            if ((st & status) == st) {
                return i;
            }
        }
        return StatusInfo.STATUS_ONLINE;
    }


    private static final int[] statusCodes = {
            STATUS_OFFLINE,
            STATUS_ONLINE,
            STATUS_AWAY,
            STATUS_CHAT,
            STATUS_HOME,
            STATUS_WORK,
            STATUS_EVIL,
            STATUS_DEPRESSION,
            STATUS_LUNCH,
            STATUS_NA,
            STATUS_OCCUPIED,
            STATUS_DND,
            STATUS_INVISIBLE,
            STATUS_INVIS_ALL
    };
    private static final int[] outStatusCodes = {
            STATUS_OFFLINE,
            STATUS_ONLINE,
            STATUS_AWAY,
            STATUS_ONLINE,
            STATUS_ONLINE,
            STATUS_ONLINE,
            STATUS_ONLINE,
            STATUS_ONLINE,
            STATUS_AWAY,
            STATUS_NA,
            STATUS_OCCUPIED,
            STATUS_DND,
            STATUS_INVISIBLE,
            STATUS_INVISIBLE
    };

    public static int getNativeStatus(int statusIndex) {
        return outStatusCodes[statusIndex];
    }
}

