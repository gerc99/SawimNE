package protocol.mrim;

import DrawControls.icons.ImageList;
import protocol.XStatusInfo;
import ru.sawim.R;
import sawim.comm.Config;
import sawim.comm.StringConvertor;

import java.util.HashMap;


public class MrimXStatusInfo {
    private String[] statusCodes;
    private XStatusInfo info;
    private static HashMap<String, Integer> statusesMap = new HashMap<String, Integer>();
    private void load() {
            statusesMap.put("status_5", R.string.at_home);
            statusesMap.put("status_18", R.string.at_work);
            statusesMap.put("status_19", R.string.meeting);
            statusesMap.put("status_7", R.string.where_i_am);
            statusesMap.put("status_10", R.string.walking);
            statusesMap.put("status_47", R.string.rocket);
            statusesMap.put("status_22", R.string.working);
            statusesMap.put("status_26", R.string.learning);
            statusesMap.put("status_24", R.string.phone);
            statusesMap.put("status_27", R.string.at_scool);
            statusesMap.put("status_23", R.string.sleeping);
            statusesMap.put("status_4", R.string.sick);
            statusesMap.put("status_9", R.string.cooking);
            statusesMap.put("status_6", R.string.eating);
            statusesMap.put("status_21", R.string.drinking_coffee);
            statusesMap.put("status_20", R.string.beer);
            statusesMap.put("status_17", R.string.smoking);
            statusesMap.put("status_8", R.string.wc);
            statusesMap.put("status_15", R.string.duck);
            statusesMap.put("status_16", R.string.playing);
            statusesMap.put("status_28", R.string.wrong_number);
            statusesMap.put("status_51", R.string.squirrel);
            statusesMap.put("status_52", R.string.star);
            statusesMap.put("status_46", R.string.skull);
            statusesMap.put("status_12", R.string.i_am_shrimp);
            statusesMap.put("status_13", R.string.lost);
            statusesMap.put("status_11", R.string.alien);
            statusesMap.put("status_14", R.string.in_love);
            statusesMap.put("status_48", R.string.cthulhu);
            statusesMap.put("status_53", R.string.listening_music);
            statusesMap.put("status_29", R.string.happy);
            statusesMap.put("status_30", R.string.teasing);
            statusesMap.put("status_32", R.string.wide_smile);
            statusesMap.put("status_33", R.string.foureyes);
            statusesMap.put("status_40", R.string.heart);
            statusesMap.put("status_41", R.string.drowsy);
            statusesMap.put("status_34", R.string.sad);
            statusesMap.put("status_35", R.string.crying);
            statusesMap.put("status_36", R.string.ooooo);
            statusesMap.put("status_37", R.string.angry);
            statusesMap.put("status_38", R.string.hell);
            statusesMap.put("status_39", R.string.ass);
            statusesMap.put("status_42", R.string.great);
            statusesMap.put("status_43", R.string.peace);
            statusesMap.put("status_49", R.string.cool);
            statusesMap.put("status_44", R.string.nuts_to_you);
            statusesMap.put("status_45", R.string.fuck_you);
            statusesMap.put("status_50", R.string.sucks);
    }

    public MrimXStatusInfo() {
        ImageList xstatusIcons = ImageList.createImageList("/mrim-xstatus.png");
        load();
        int size = statusesMap.size();
        int[] statusesNames = new int[size];
        statusCodes = new String[size];
        for (int i = 0; i < size; ++i) {
            String code = (String)statusesMap.keySet().toArray()[i];
            statusesNames[i] = statusesMap.get(code);
            statusCodes[i] = code;
        }
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



