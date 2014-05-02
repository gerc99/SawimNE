package protocol.mrim;

import protocol.XStatusInfo;
import ru.sawim.R;
import ru.sawim.comm.StringConvertor;
import ru.sawim.icons.ImageList;


public class MrimXStatusInfo {
    private XStatusInfo info;

    private static final String[] statusCodes = {
            "status_5",
            "status_18",
            "status_19",
            "status_7",
            "status_10",
            "status_47",
            "status_22",
            "status_26",
            "status_24",
            "status_27",
            "status_23",
            "status_4",
            "status_9",
            "status_6",
            "status_21",
            "status_20",
            "status_17",
            "status_8",
            "status_15",
            "status_16",
            "status_28",
            "status_51",
            "status_52",
            "status_46",
            "status_12",
            "status_13",
            "status_11",
            "status_14",
            "status_48",
            "status_53",
            "status_29",
            "status_30",
            "status_32",
            "status_33",
            "status_40",
            "status_41",
            "status_34",
            "status_35",
            "status_36",
            "status_37",
            "status_38",
            "status_39",
            "status_42",
            "status_43",
            "status_49",
            "status_44",
            "status_45",
            "status_50",
    };

    private static final int[] statusesNames = {
            R.string.at_home,
            R.string.at_work,
            R.string.meeting,
            R.string.where_i_am,
            R.string.walking,
            R.string.rocket,
            R.string.working,
            R.string.learning,
            R.string.phone,
            R.string.at_scool,
            R.string.sleeping,
            R.string.sick,
            R.string.cooking,
            R.string.eating,
            R.string.drinking_coffee,
            R.string.beer,
            R.string.smoking,
            R.string.wc,
            R.string.duck,
            R.string.playing,
            R.string.wrong_number,
            R.string.squirrel,
            R.string.star,
            R.string.skull,
            R.string.i_am_shrimp,
            R.string.lost,
            R.string.alien,
            R.string.in_love,
            R.string.cthulhu,
            R.string.listening_music,
            R.string.happy,
            R.string.teasing,
            R.string.wide_smile,
            R.string.foureyes,
            R.string.heart,
            R.string.drowsy,
            R.string.sad,
            R.string.crying,
            R.string.ooooo,
            R.string.angry,
            R.string.hell,
            R.string.ass,
            R.string.great,
            R.string.peace,
            R.string.cool,
            R.string.nuts_to_you,
            R.string.fuck_you,
            R.string.sucks
    };

    public MrimXStatusInfo() {
        ImageList xstatusIcons = ImageList.createImageList("/mrim-xstatus.png");
        info = new XStatusInfo(xstatusIcons, statusesNames);
    }

    public XStatusInfo getInfo() {
        return info;
    }

    public String getNativeXStatus(byte statusIndex) {
        if (0 <= statusIndex && statusIndex < statusCodes.length) {
            return statusCodes[statusIndex];
        }
        return null;
    }

    public int createStatus(String nativeStatus) {
        if (StringConvertor.isEmpty(nativeStatus)) {
            return XStatusInfo.XSTATUS_NONE;
        }
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (statusCodes[i].equals(nativeStatus)) {
                return i;
            }
        }
        return XStatusInfo.XSTATUS_NONE;
    }
}



