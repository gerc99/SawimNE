


package protocol.icq;

import DrawControls.icons.ImageList;
import protocol.XStatusInfo;
import ru.sawim.R;
import sawim.comm.GUID;
import sawim.comm.StringConvertor;
import sawim.comm.Util;

import java.util.HashMap;


public class IcqXStatus {
    private byte[][] guids;
    private short[] ids;
    private final XStatusInfo info;

    private static HashMap<String, Integer> statusesMap = new HashMap<String, Integer>();
    private void load() {
        statusesMap.put("23 01D8D7EEAC3B492AA58DD3D877E66B92", R.string.xstatus_angry);
        statusesMap.put("1 5A581EA1E580430CA06F612298B7E4C7", R.string.xstatus_duck);
        statusesMap.put("2 83C9B78E77E74378B2C5FB6CFCC35BEC", R.string.xstatus_tired);
        statusesMap.put("3 E601E41C33734BD1BC06811D6C323D81", R.string.xstatus_party);
        statusesMap.put("4 8C50DBAE81ED4786ACCA16CC3213C7B7", R.string.xstatus_beer);
        statusesMap.put("5 3FB0BD36AF3B4A609EEFCF190F6A5A7F", R.string.xstatus_thinking);
        statusesMap.put("6 F8E8D7B282C4414290F810C6CE0A89A6", R.string.xstatus_eating);
        statusesMap.put("7 80537DE2A4674A76B3546DFD075F5EC6", R.string.xstatus_tv);
        statusesMap.put("8 F18AB52EDC57491D99DC6444502457AF", R.string.xstatus_friends);
        statusesMap.put("9 1B78AE31FA0B4D3893D1997EEEAFB218", R.string.xstatus_coffee);
        statusesMap.put("10 61BEE0DD8BDD475D8DEE5F4BAACF19A7", R.string.xstatus_music);
        statusesMap.put("11 488E14898ACA4A0882AA77CE7A165208", R.string.xstatus_business);
        statusesMap.put("12 107A9A1812324DA4B6CD0879DB780F09", R.string.xstatus_camera);
        statusesMap.put("13 6F4930984F7C4AFFA27634A03BCEAEA7", R.string.xstatus_funny);
        statusesMap.put("14 1292E5501B644F66B206B29AF378E48D", R.string.xstatus_phone);
        statusesMap.put("15 D4A611D08F014EC09223C5B6BEC6CCF0", R.string.xstatus_games);
        statusesMap.put("16 609D52F8A29A49A6B2A02524C5E9D260", R.string.xstatus_college);
        statusesMap.put("63627337A03F49FF80E5F709CDE0A4EE", R.string.xstatus_shopping);
        statusesMap.put("17 1F7A4071BF3B4E60BC324C5787B04CF1", R.string.xstatus_sick);
        statusesMap.put("18 785E8C4840D34C65886F04CF3F3F43DF", R.string.xstatus_sleeping);
        statusesMap.put("19 A6ED557E6BF744D4A5D4D2E7D95CE81F", R.string.xstatus_surfing);
        statusesMap.put("20 12D07E3EF885489E8E97A72A6551E58D", R.string.xstatus_internet);
        statusesMap.put("21 BA74DB3E9E24434B87B62F6B8DFEE50F", R.string.xstatus_engineering);
        statusesMap.put("22 634F6BD8ADD24AA1AAB9115BC26D05A1", R.string.xstatus_typing);
        statusesMap.put("2CE0E4E57C6443709C3A7A1CE878A7DC", R.string.xstatus_unk);
        statusesMap.put("101117C9A3B040F981AC49E159FBD5D4", R.string.xstatus_ppc);
        statusesMap.put("160C60BBDD4443F39140050F00E6C009", R.string.xstatus_mobile);
        statusesMap.put("6443C6AF22604517B58CD7DF8E290352", R.string.xstatus_man);
        statusesMap.put("16F5B76FA9D240358CC5C084703C98FA", R.string.xstatus_wc);
        statusesMap.put("631436FF3F8A40D0A5CB7B66E051B364", R.string.xstatus_question);
        statusesMap.put("B70867F538254327A1FFCF4CC1939797", R.string.xstatus_way);
        statusesMap.put("DDCF0EA971954048A9C6413206D6F280", R.string.xstatus_heart);
        statusesMap.put("3FB0BD36AF3B4A609EEFCF190F6A5A7E", R.string.xstatus_cigarette);
        statusesMap.put("E601E41C33734BD1BC06811D6C323D82", R.string.xstatus_sex);
        statusesMap.put("D4E2B0BA334E4FA598D0117DBF4D3CC8", R.string.xstatus_rambler_search);
        statusesMap.put("CD5643A2C94C4724B52CDC0124A1D0CD", R.string.xstatus_rambler_love);
        statusesMap.put("0072D9084AD143DD91996F026966026F", R.string.xstatus_rambler_journal);
    }

    /*private int[] names = {
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
    };*/

    public IcqXStatus() {
        ImageList icons = ImageList.createImageList("/icq-xstatus.png");
        load();
        int size = statusesMap.size();
        int[] statusesNames = new int[size];
        guids = new byte[size][];
        ids = new short[size];
        for (int i = 0; i < size; ++i) {
            String code = (String)statusesMap.keySet().toArray()[i];
            String[] codes = Util.explode(code, StringConvertor.DELEMITER);
            ids[i] = -1;
            statusesNames[i] = statusesMap.get(code);
            for (int j = 0; j < codes.length; ++j) {
                if (codes[j].length() != 32) {
                    ids[i] = (short) Util.strToIntDef(codes[j], -1);
                } else {
                    guids[i] = getGuid(codes[j]);
                }
            }
        }
        info = new XStatusInfo(icons, statusesNames);
    }

    public XStatusInfo getInfo() {
        return info;
    }

    private byte[] getGuid(String line) {
        byte[] data = new byte[16];
        for (int i = 0; i < data.length; ++i) {
            data[i] = (byte) ((Character.digit(line.charAt(i * 2), 16) << 4)
                    | (Character.digit(line.charAt(i * 2 + 1), 16)));

        }
        return data;
    }

    private int getGuid(byte[] myguids) {
        byte[] guid;
        for (int i = 0; i < guids.length; ++i) {
            guid = guids[i];
            if (null == guid) {
                continue;
            }
            for (int j = 0; j < myguids.length; j += 16) {
                for (int k = 0; k < 16; ++k) {
                    if (guid[k] != myguids[j + k]) {
                        break;
                    }
                    if (15 == k) {
                        return i;
                    }
                }
            }
        }
        return XStatusInfo.XSTATUS_NONE;
    }

    public int createXStatus(byte[] myguids, String mood) {
        if ((null != mood) && mood.startsWith("0icqmood")) {
            int id = Util.strToIntDef(mood.substring(8), -1);
            for (int i = 0; i < ids.length; ++i) {
                if (ids[i] == id) {
                    return (byte) i;
                }
            }
        }
        if (null != myguids) {
            return (byte) getGuid(myguids);
        }
        return XStatusInfo.XSTATUS_NONE;
    }


    public int getIcqMood(int xstatusIndex) {
        int index = xstatusIndex;
        if ((0 <= index) && (index < ids.length)) {
            return ids[index];
        }
        return -1;
    }

    public GUID getIcqGuid(int xstatusIndex) {
        int index = xstatusIndex;
        if ((0 <= index) && (index < ids.length)) {
            return new GUID(guids[index]);
        }
        return null;
    }
}



