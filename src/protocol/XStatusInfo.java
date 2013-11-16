

package protocol;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import sawim.util.JLocale;


public class XStatusInfo {
    public static final int XSTATUS_NONE = -1;
    private final ImageList icons;
    private final String[] names;

    public XStatusInfo(ImageList icons, String[] names) {
        this.icons = icons;
        this.names = names;
    }

    public Icon getIcon(int index) {
        index = (index < 0) ? index : (index & 0xFF);
        return icons.iconAt(index);
    }

    public String getName(int index) {
        index = (index < 0) ? index : (index & 0xFF);
        if ((0 <= index) && (index < names.length)) {
            return names[index];
        }
        return JLocale.getString("xstatus_none");
    }

    public int getXStatusCount() {
        return names.length;
    }
}

