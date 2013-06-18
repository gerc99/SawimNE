


package sawim.forms;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import protocol.Protocol;
import protocol.icq.Icq;
import protocol.mrim.Mrim;


public final class PrivateStatusForm {
    private Protocol protocol;

    public static final ImageList privateStatusIcons = ImageList.createImageList("/privatestatuses.png");
    
    public static final int PSTATUS_ALL = 0;
    public static final int PSTATUS_VISIBLE_ONLY = 1;
    public static final int PSTATUS_NOT_INVISIBLE = 2;
    public static final int PSTATUS_CL_ONLY = 3;
    public static final int PSTATUS_NONE = 4;


    public PrivateStatusForm(Protocol protocol) {
        this.protocol = protocol;
    }
        
    public static Icon getIcon(Protocol protocol) {
        return privateStatusIcons.iconAt(protocol.getPrivateStatus());
    }
    private static final String[] statusIcqNames = {
            "ps_all",
            "ps_visible_list",
            "ps_exclude_invisible",
            "ps_contact_list",
            "ps_none"
    };
    private static final String[] statusMrimNames = {
            "ps_visible_list",
            "ps_exclude_invisible"
    };

    private static final int[] statusIcqIds = {
        PSTATUS_ALL,
        PSTATUS_VISIBLE_ONLY,
        PSTATUS_NOT_INVISIBLE,
        PSTATUS_CL_ONLY,
        PSTATUS_NONE
    };
    private static final int[] statusMrimIds = {
            PSTATUS_VISIBLE_ONLY,
            PSTATUS_NOT_INVISIBLE
    };

    public static String[] statusNames(Protocol protocol) {
        if (protocol instanceof Icq) {
            return statusIcqNames;
        }
        if (protocol instanceof Mrim) {
            return statusMrimNames;
        }
        return null;
    }
    public static int[] statusIds(Protocol protocol) {
        if (protocol instanceof Icq) {
            return statusIcqIds;
        }
        if (protocol instanceof Mrim) {
            return statusMrimIds;
        }
        return null;
    }
}


