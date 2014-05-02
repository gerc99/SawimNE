package protocol.mrim;

import protocol.ClientInfo;
import ru.sawim.comm.Config;
import ru.sawim.comm.StringConvertor;
import ru.sawim.icons.ImageList;
import ru.sawim.modules.DebugLog;


public final class MrimClient {
    private static final ImageList clientIcons = ImageList.createImageList("/mrim-clients.png");
    private static final String[] clientIds;
    private static final String[] clientNames;

    static {
        Config cfg = new Config().load("/mrim-clients.txt");
        clientIds = cfg.getKeys();
        clientNames = cfg.getValues();
    }

    static ClientInfo get() {
        return new ClientInfo(clientIcons, clientNames);
    }


    private MrimClient() {
    }

    private static String getValue(String str, String key) {
        String fullKey = key + "=\"";
        int keyIndex = str.indexOf(fullKey);
        int valueIndex = keyIndex + fullKey.length();
        int endIndex = str.indexOf('"', valueIndex);
        if ((-1 == keyIndex) || (-1 == endIndex)) {
            return "";
        }
        return str.substring(valueIndex, endIndex);
    }

    static public void createClient(MrimContact contact, String caps) {
        if (StringConvertor.isEmpty(caps)) {
            contact.setClient(ClientInfo.CLI_NONE, null);
            return;
        }

        String clientClient = getValue(caps, "rosterient");
        String clientName = getValue(caps, "name");
        short clientIndex = ClientInfo.CLI_NONE;
        for (short idIndex = 0; idIndex < clientIds.length; ++idIndex) {
            if (-1 != clientName.indexOf(clientIds[idIndex])) {
                clientIndex = idIndex;
                break;
            }
            if (-1 != clientClient.indexOf(clientIds[idIndex])) {
                clientIndex = idIndex;
                break;
            }
        }
        if (ClientInfo.CLI_NONE == clientIndex) {

            DebugLog.println("Unknown client: " + caps);

            contact.setClient(ClientInfo.CLI_NONE, null);
            return;
        }
        contact.setClient(clientIndex, getValue(caps, "version"));
    }
}