package protocol.xmpp;

import java.util.HashMap;

/**
 * Created by gerc on 12.03.2015.
 */
public class ServerFeatures {

    private HashMap<String, String> serverDiscoItems = new HashMap<>();
    private String mucServer;
    private boolean hasMessageArchiveManagement;
    private boolean hasCarbon;
    private boolean carbonsEnabled;

    public void parseServerFeatures(XmlNode iqQuery, String id) {
        while (0 < iqQuery.childrenCount()) {
            XmlNode featureNode = iqQuery.popChildNode();
            String feature = featureNode.getAttribute("var");
            if (feature != null) {
                switch (feature) {
                    case "http://jabber.org/protocol/muc":
                        mucServer = serverDiscoItems.get(id);
                        break;
                    case "urn:xmpp:mam:0":
                        hasMessageArchiveManagement = true;
                        break;
                    case "urn:xmpp:carbons:2":
                        hasCarbon = true;
                        break;
                }
            }
        }
    }

    public boolean hasMessageArchiveManagement() {
        return hasMessageArchiveManagement;
    }

    public HashMap<String, String> getServerDiscoItems() {
        return serverDiscoItems;
    }

    public String mucServer() {
        return mucServer;
    }

    public boolean hasCarbon() {
        return hasCarbon;
    }

    public boolean isCarbonsEnabled() {
        return carbonsEnabled;
    }

    public void setCarbonsEnabled(boolean carbonsEnabled) {
        this.carbonsEnabled = carbonsEnabled;
    }
}
