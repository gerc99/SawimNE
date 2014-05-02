package protocol.xmpp;

import protocol.ClientInfo;
import ru.sawim.comm.Config;
import ru.sawim.icons.ImageList;


public final class XmppClient {
    private static final ImageList clientIcons = ImageList.createImageList("/jabber-clients.png");
    private static final String[] clientCaps;
    private static final String[] clientNames;

    static {
        Config cfg = new Config().load("/jabber-clients.txt");
        clientCaps = cfg.getKeys();
        clientNames = cfg.getValues();
    }

    public static ClientInfo get() {
        return new ClientInfo(clientIcons, clientNames);
    }

    public static final byte CLIENT_NONE = -1;

    public static short createClient(String caps) {
        if (null == caps) {
            return CLIENT_NONE;
        }
        caps = caps.toLowerCase();
        for (short capsIndex = 0; capsIndex < clientCaps.length; ++capsIndex) {
            if (-1 != caps.indexOf(clientCaps[capsIndex])) {
                return capsIndex;
            }
        }
        return CLIENT_NONE;
    }

}
