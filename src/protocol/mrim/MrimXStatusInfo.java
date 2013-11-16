


package protocol.mrim;

import DrawControls.icons.ImageList;
import protocol.XStatusInfo;
import sawim.comm.Config;
import sawim.comm.StringConvertor;


public class MrimXStatusInfo {
    private final String[] statusCodes;
    private final XStatusInfo info;

    public MrimXStatusInfo() {
        Config config = new Config().loadLocale("/mrim-xstatus.txt");
        statusCodes = config.getKeys();
        ImageList xstatusIcons = ImageList.createImageList("/mrim-xstatus.png");
        String[] statusStrings = config.getValues();
        info = new XStatusInfo(xstatusIcons, statusStrings);
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



