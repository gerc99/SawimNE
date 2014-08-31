package protocol.xmpp;

import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.Util;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.roster.RosterHelper;

final class XmppForm implements FormListener {
    private XForm form;
    private Xmpp xmpp;
    private String jid;
    private String id;

    public final static byte TYPE_REGISTER = 0;
    public final static byte TYPE_CAPTCHA = 1;
    public final static byte TYPE_OWNER = 2;
    public final static byte TYPE_NONE = 3;
    private byte type = TYPE_CAPTCHA;

    public XmppForm(byte formType, Xmpp protocol, String resourceJid) {
        xmpp = protocol;
        jid = resourceJid;
        type = formType;
        id = newId();
        form = new XForm();
    }

    private int typeToTitle(byte type) {
        switch (type) {
            case TYPE_CAPTCHA:
                return R.string.captcha;
            case TYPE_REGISTER:
                return R.string.registration;
            case TYPE_OWNER:
                return R.string.options;
        }
        return -1;
    }

    private String newId() {
        return "forms" + Util.uniqueValue();
    }

    private String getJid() {
        return jid;
    }

    public boolean isWaiting() {
        return form.isWaiting();
    }

    public void loadFromXml(XmlNode xml, XmlNode baseXml) {
        id = newId();
        form.loadFromXml(xml, baseXml);
    }

    public void show(BaseActivity activity) {
        form.init(typeToTitle(type), this);
        form.setWaiting();
        form.getForm().show(activity);
    }

    public void showCaptcha(XmlNode baseNode) {
        form.init(jid, this);
        final String S_CAPTCHA = "captcha";
        XmlNode captcha = baseNode.getFirstNode(S_CAPTCHA);
        id = baseNode.getAttribute("id");
        form.loadXFromXml(captcha, baseNode);
        form.getForm().showCaptcha();
    }

    public String getId() {
        return id;
    }

    public void formAction(BaseActivity activity, Forms f, boolean apply) {
        if (apply) {
            if (0 < form.getSize()) {
                doAction();
            }
        }
        form.back();
    }

    void error(String description) {
        type = TYPE_NONE;
        form.setErrorMessage(description);
    }

    void success() {
        RosterHelper.getInstance().activate(xmpp.getItemByUID(jid));
    }

    private String getCaptchaXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><captcha xmlns='urn:xmpp:captcha'>"
                + form.getXmlForm()
                + "</captcha></iq>";
    }

    private String getRegisterXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='jabber:iq:register'>"
                + form.getXmlForm()
                + "</query></iq>";
    }

    private String getOwnerXml() {
        return "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='http://jabber.org/protocol/muc#owner'>"
                + form.getXmlForm()
                + "</query></iq>";
    }

    private void doAction() {
        switch (type) {
            case TYPE_REGISTER:
                xmpp.getConnection().register2(this, getRegisterXml(), getJid());
                break;

            case TYPE_CAPTCHA:
                xmpp.getConnection().requestRawXml(getCaptchaXml());
                break;

            case TYPE_OWNER:
                xmpp.getConnection().requestRawXml(getOwnerXml());
                break;

            case TYPE_NONE:
                form.back();
                break;
        }
    }
}