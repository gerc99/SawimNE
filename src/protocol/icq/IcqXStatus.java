


package protocol.icq;

import protocol.XStatusInfo;
import ru.sawim.R;
import ru.sawim.comm.GUID;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.ImageList;


public class IcqXStatus {
    private byte[][] guids;
    private short[] ids;
    private final XStatusInfo info;

    private static final String[] statusCodes = {
            "23 01D8D7EEAC3B492AA58DD3D877E66B92",
            "1 5A581EA1E580430CA06F612298B7E4C7",
            "2 83C9B78E77E74378B2C5FB6CFCC35BEC",
            "3 E601E41C33734BD1BC06811D6C323D81",
            "4 8C50DBAE81ED4786ACCA16CC3213C7B7",
            "5 3FB0BD36AF3B4A609EEFCF190F6A5A7F",
            "6 F8E8D7B282C4414290F810C6CE0A89A6",
            "7 80537DE2A4674A76B3546DFD075F5EC6",
            "8 F18AB52EDC57491D99DC6444502457AF",
            "9 1B78AE31FA0B4D3893D1997EEEAFB218",
            "10 61BEE0DD8BDD475D8DEE5F4BAACF19A7",
            "11 488E14898ACA4A0882AA77CE7A165208",
            "12 107A9A1812324DA4B6CD0879DB780F09",
            "13 6F4930984F7C4AFFA27634A03BCEAEA7",
            "14 1292E5501B644F66B206B29AF378E48D",
            "15 D4A611D08F014EC09223C5B6BEC6CCF0",
            "16 609D52F8A29A49A6B2A02524C5E9D260",
            "63627337A03F49FF80E5F709CDE0A4EE",
            "17 1F7A4071BF3B4E60BC324C5787B04CF1",
            "18 785E8C4840D34C65886F04CF3F3F43DF",
            "19 A6ED557E6BF744D4A5D4D2E7D95CE81F",
            "20 12D07E3EF885489E8E97A72A6551E58D",
            "21 BA74DB3E9E24434B87B62F6B8DFEE50F",
            "22 634F6BD8ADD24AA1AAB9115BC26D05A1",
            "2CE0E4E57C6443709C3A7A1CE878A7DC",
            "101117C9A3B040F981AC49E159FBD5D4",
            "160C60BBDD4443F39140050F00E6C009",
            "6443C6AF22604517B58CD7DF8E290352",
            "16F5B76FA9D240358CC5C084703C98FA",
            "631436FF3F8A40D0A5CB7B66E051B364",
            "B70867F538254327A1FFCF4CC1939797",
            "DDCF0EA971954048A9C6413206D6F280",
            "3FB0BD36AF3B4A609EEFCF190F6A5A7E",
            "E601E41C33734BD1BC06811D6C323D82",
            "D4E2B0BA334E4FA598D0117DBF4D3CC8",
            "CD5643A2C94C4724B52CDC0124A1D0CD",
            "0072D9084AD143DD91996F026966026F"};

    private static final int[] statusNames = {
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

    public IcqXStatus() {
        ImageList icons = ImageList.createImageList("/icq-xstatus.png");
        int size = statusCodes.length;
        guids = new byte[size][];
        ids = new short[size];
        for (int i = 0; i < size; ++i) {
            String[] codes = Util.explode(statusCodes[i], StringConvertor.DELEMITER);
            ids[i] = -1;
            for (int j = 0; j < codes.length; ++j) {
                if (codes[j].length() != 32) {
                    ids[i] = (short) Util.strToIntDef(codes[j], -1);
                } else {
                    guids[i] = getGuid(codes[j]);
                }
            }
        }
        info = new XStatusInfo(icons, statusNames);
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



