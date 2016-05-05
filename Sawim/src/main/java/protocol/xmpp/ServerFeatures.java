package protocol.xmpp;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import ru.sawim.SawimApplication;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.crypto.MD5;

/**
 * Created by gerc on 12.03.2015.
 */
public class ServerFeatures {

    //private String mucServer;
    private boolean hasMessageArchiveManagement;
    private boolean hasCarbon;
    private boolean carbonsEnabled;

    private static String verHash = "";
    private String featureList = "";

    public void parseServerFeatures(XmlNode iqQuery, String id) {
        while (0 < iqQuery.childrenCount()) {
            XmlNode featureNode = iqQuery.popChildNode();
            String feature = featureNode.getAttribute("var");
            if (feature != null) {
                switch (feature) {
                    case "http://jabber.org/protocol/muc":
                        //mucServer = serverDiscoItems.get(id);
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

    //public String mucServer() {
    //    return mucServer;
    //}

    public boolean hasCarbon() {
        return hasCarbon;
    }

    public boolean isCarbonsEnabled() {
        return carbonsEnabled;
    }

    public void setCarbonsEnabled(boolean carbonsEnabled) {
        this.carbonsEnabled = carbonsEnabled;
    }

    public static String getFeatures(List<String> features) {
        StringBuilder sb = new StringBuilder();
        sb.append("<identity category='client' type='phone' name='" + SawimApplication.NAME + "'/>");
        for (int i = 0; i < features.size(); ++i) {
            sb.append("<feature var='").append(features.get(i)).append("'/>");
        }
        return sb.toString();
    }

    public void initFeatures() {
        List<String> features = new ArrayList<>();
        features.add("bugs");
        features.add("http://jabber.org/protocol/activity");
        features.add("http://jabber.org/protocol/activity+notify");
        features.add("http://jabber.org/protocol/chatstates");
        features.add("http://jabber.org/protocol/disco#info");
        features.add("http://jabber.org/protocol/mood");
        features.add("http://jabber.org/protocol/mood+notify");
        features.add("http://jabber.org/protocol/rosterx");
        features.add(XStatus.S_FEATURE_XSTATUS);
        features.add("jabber:iq:last");
        features.add("jabber:iq:version");
        features.add("urn:xmpp:attention:0");
        features.add("urn:xmpp:time");
        features.add("urn:xmpp:mam:0");
        features.add("urn:xmpp:carbons:2");
        verHash = getVerHash(features);
        featureList = getFeatures(features);
    }

    private static String getVerHash(List<String> features) {
        StringBuilder sb = new StringBuilder();
        sb.append("client/phone/" + "/" + SawimApplication.NAME + "<");
        for (String feature : features) {
            sb.append(feature).append('<');
        }
        return Util.base64encode(MD5.calculate(StringConvertor.stringToByteArrayUtf8(sb.toString())));
    }

    public static String getCaps() {
        return "<c xmlns='http://jabber.org/protocol/caps'"
                + " node='http://sawim.ru/caps' ver='"
                + Util.xmlEscape(verHash)
                + "' hash='md5'/>";
    }

    public String getFeatureList() {
        return featureList;
    }
}
