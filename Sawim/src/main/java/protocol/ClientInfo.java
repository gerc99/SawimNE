package protocol;


import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;


public final class ClientInfo {
    public static final short CLI_NONE = -1;
    private final ImageList icons;
    public final String[] names;
    private final short[] iconIndexes;

    public ClientInfo(ImageList icons, String[] names) {
        this(icons, null, names);
    }

    public ClientInfo(ImageList icons, short[] iconIndexes, String[] names) {
        this.icons = icons;
        this.iconIndexes = iconIndexes;
        this.names = names;
    }

    public Icon getIcon(int clientIndex) {
        if ((null != iconIndexes) && (CLI_NONE != clientIndex)) {
            if (iconIndexes.length > clientIndex)
                clientIndex = iconIndexes[clientIndex];
        }
        return icons.iconAt(clientIndex);
    }

    public String getName(int clientIndex) {
        if (CLI_NONE == clientIndex) {
            return null;
        }
        return (clientIndex < names.length) ? names[clientIndex] : null;
    }
}
