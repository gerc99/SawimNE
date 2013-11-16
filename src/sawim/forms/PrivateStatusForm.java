


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

    private static final String[] statusNames = {
            "ps_all",
            "ps_visible_list",
            "ps_exclude_invisible",
            "ps_contact_list",
            "ps_none"
    };

    private static final int[] statusIds = {
            PSTATUS_ALL,
            PSTATUS_VISIBLE_ONLY,
            PSTATUS_NOT_INVISIBLE,
            PSTATUS_CL_ONLY,
            PSTATUS_NONE
    };

    public static String[] statusNames() {
        return statusNames;
    }

    public static int[] statusIds(Protocol protocol) {
        if (protocol instanceof Icq) {
            return statusIds;
        }
        if (protocol instanceof Mrim) {
            int[] statusIds = {1, 2};
            return statusIds;
        }
        return null;
    }
}


