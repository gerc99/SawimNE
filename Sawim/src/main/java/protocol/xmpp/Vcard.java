package protocol.xmpp;

import protocol.Contact;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.crypto.SHA1;
import ru.sawim.modules.search.UserInfo;

/**
 * Created by gerc on 02.01.2016.
 */
public class Vcard {

    public static final String S_VCARD = "vCard";

    public static void saveVCard(XmppConnection connection, UserInfo userInfo) {
        if (null == userInfo.vCard) {
            userInfo.vCard = XmlNode.getEmptyVCard();
        }
        userInfo.vCard.removeBadVCardTags("TEL");
        userInfo.vCard.removeBadVCardTags("EMAIL");
        userInfo.vCard.setValue("NICKNAME", userInfo.nick);
        userInfo.vCard.setValue("BDAY", userInfo.birthDay);
        userInfo.vCard.setValue("URL", userInfo.homePage);
        userInfo.vCard.setValue("FN", userInfo.getName());
        userInfo.vCard.setValue("N", null, "GIVEN", userInfo.firstName);
        userInfo.vCard.setValue("N", null, "FAMILY", userInfo.lastName);
        userInfo.vCard.setValue("N", null, "MIDDLE", "");
        userInfo.vCard.setValue("EMAIL", new String[]{"INTERNET"}, "USERID", userInfo.email);
        userInfo.vCard.setValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", userInfo.cellPhone);

        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "STREET", userInfo.homeAddress);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "LOCALITY", userInfo.homeCity);
        userInfo.vCard.setValue("ADR", new String[]{"HOME"}, "REGION", userInfo.homeState);

        userInfo.vCard.setValue("TEL", new String[]{}, "NUMBER", "");
        userInfo.vCard.setValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER", userInfo.workPhone);
        userInfo.vCard.setValue("ORG", null, "ORGNAME", userInfo.workCompany);
        userInfo.vCard.setValue("ORG", null, "ORGUNIT", userInfo.workDepartment);
        userInfo.vCard.setValue("TITLE", userInfo.workPosition);
        userInfo.vCard.setValue("DESC", userInfo.about);
        userInfo.vCard.cleanXmlTree();

        StringBuilder packet = new StringBuilder();
        packet.append("<iq type='set' to='").append(Util.xmlEscape(userInfo.uin))
                .append("' id='").append(XmppConnection.generateId()).append("'>");
        userInfo.vCard.toString(packet);
        packet.append("</iq>");
        connection.putPacketIntoQueue(packet.toString());
    }

    public static void loadVCard(XmppConnection connection, XmlNode vCard, String from) {
        UserInfo userInfo = connection.singleUserInfo;
        if (null != userInfo && from.equals(userInfo.realUin)) {
            userInfo.auth = false;
            userInfo.uin = from;
            Contact c = connection.getXmpp().getItemByUID(Jid.getBareJid(from));
            if (c instanceof XmppServiceContact) {
                XmppContact.SubContact sc = ((XmppServiceContact) c).getExistSubContact(Jid.getResource(from, null));
                if ((null != sc) && (null != sc.realJid)) {
                    userInfo.uin = sc.realJid;
                }
            }
            if (null == vCard) {
                userInfo.updateProfileView();
                connection.singleUserInfo = null;
                return;
            }
            String name[] = new String[3];
            name[0] = vCard.getFirstNodeValue("N", "GIVEN");
            name[1] = vCard.getFirstNodeValue("N", "MIDDLE");
            name[2] = vCard.getFirstNodeValue("N", "FAMILY");
            if (StringConvertor.isEmpty(Util.implode(name, ""))) {
                userInfo.firstName = vCard.getFirstNodeValue("FN");
                userInfo.lastName = null;
            } else {
                userInfo.lastName = name[2];
                name[2] = null;
                userInfo.firstName = Util.implode(name, " ");
            }
            userInfo.nick = vCard.getFirstNodeValue("NICKNAME");
            userInfo.birthDay = vCard.getFirstNodeValue("BDAY");
            userInfo.email = vCard.getFirstNodeValue("EMAIL", new String[]{"INTERNET"}, "USERID", true);
            userInfo.about = vCard.getFirstNodeValue("DESC");
            userInfo.homePage = vCard.getFirstNodeValue("URL");

            userInfo.homeAddress = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "STREET", true);
            userInfo.homeCity = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "LOCALITY", true);
            userInfo.homeState = vCard.getFirstNodeValue("ADR", new String[]{"HOME"}, "REGION", true);
            userInfo.cellPhone = vCard.getFirstNodeValue("TEL", new String[]{"HOME", "VOICE"}, "NUMBER", true);

            userInfo.workCompany = vCard.getFirstNodeValue("ORG", null, "ORGNAME");
            userInfo.workDepartment = vCard.getFirstNodeValue("ORG", null, "ORGUNIT");
            userInfo.workPosition = vCard.getFirstNodeValue("TITLE");
            userInfo.workPhone = vCard.getFirstNodeValue("TEL", new String[]{"WORK", "VOICE"}, "NUMBER");

            if (!Jid.isGate(from)) {
                userInfo.setOptimalName();
            }
            if (userInfo.isEditable()) {
                userInfo.vCard = vCard;
            }
            userInfo.updateProfileView();

            XmlNode bs64photo = vCard.getFirstNode("PHOTO");
            bs64photo = (null == bs64photo) ? null : bs64photo.getFirstNode("BINVAL");
            if (null != bs64photo) {
                new Thread(new AvatarLoader(userInfo, bs64photo), "XMPPAvatarLoad").start();
            }
            connection.singleUserInfo = null;
        }
    }

    public static void requestVCard(XmppConnection connection, String id, String newAvatarHash, String avatarHash, OnAvatarLoadListener listener) {
        if (!Options.getBoolean(JLocale.getString(R.string.pref_users_avatars))) return;
        if (newAvatarHash == null) {
            if (avatarHash == null) {
                getVCard(connection, id, listener);
            }
        } else {
            if (!newAvatarHash.equals(avatarHash)) {
                getVCard(connection, id, listener);
            }
        }
    }

    public static void getVCard(final XmppConnection connection, String jid, final OnAvatarLoadListener listener) {
        XmlNode x = new XmlNode(XmlConstants.S_IQ);
        x.setId(XmppConnection.generateId(S_VCARD));
        x.setType(XmlConstants.S_GET);
        x.putAttribute(XmlConstants.S_TO, Util.xmlEscape(jid));

        XmlNode vCardNode = new XmlNode("vCard");
        vCardNode.putAttribute(XmlNode.S_XMLNS, "vcard-temp");
        vCardNode.putAttribute("version", "2.0");
        vCardNode.putAttribute("prodid", "-//HandGen//NONSGML vGen v1.0//EN");
        x.addNode(vCardNode);
        connection.request(x, new OnIqReceived() {
            @Override
            public void onIqReceived(XmlNode iq) {
                XmlNode vCard = iq.childAt(0);
                String from = StringConvertor.notNull(iq.getAttribute(XmlConstants.S_FROM));
                Contact c = connection.getXmpp().getItemByUID(Jid.getBareJid(from));
                Vcard.loadVCard(connection, vCard, from);
                if (vCard == null) return;
                XmlNode bs64photo = vCard.getFirstNode("PHOTO");
                bs64photo = (null == bs64photo) ? null : bs64photo.getFirstNode("BINVAL");
                if (bs64photo != null) {
                    byte[] avatarBytes = Util.base64decode(bs64photo.value);
                    String avatarHash = StringConvertor.byteArrayToHexString(SHA1.calculate(avatarBytes));
                    if (c != null && c instanceof XmppServiceContact) {
                        XmppContact.SubContact sc = ((XmppServiceContact) c).getExistSubContact(Jid.getResource(from, null));
                        if (null != sc) {
                            sc.avatarHash = avatarHash;
                            connection.getXmpp().getStorage().updateSubContactAvatarHash(c.getUserId(), sc.resource, avatarHash);
                        } else {
                            c.avatarHash = avatarHash;
                            connection.getXmpp().getStorage().updateAvatarHash(c.getUserId(), avatarHash);
                        }
                    } else {
                        if (c != null) {
                            c.avatarHash = avatarHash;
                            connection.getXmpp().getStorage().updateAvatarHash(c.getUserId(), avatarHash);
                        }
                    }
                    listener.onLoaded(avatarHash, avatarBytes);
                } else {
                    if (c != null && c instanceof XmppServiceContact) {
                        XmppContact.SubContact sc = ((XmppServiceContact) c).getExistSubContact(Jid.getResource(from, null));
                        if (null != sc) {
                            sc.avatarHash = "";
                            connection.getXmpp().getStorage().updateSubContactAvatarHash(c.getUserId(), sc.resource, c.avatarHash);
                        } else {
                            c.avatarHash = "";
                            connection.getXmpp().getStorage().updateAvatarHash(c.getUserId(), c.avatarHash);
                        }
                    } else {
                        if (c != null) {
                            c.avatarHash = "";
                            connection.getXmpp().getStorage().updateAvatarHash(c.getUserId(), c.avatarHash);
                        }
                    }
                }
            }
        });
    }

    public interface OnAvatarLoadListener {
        void onLoaded(String avatarHash, byte[] avatarBytes);
    }
}
