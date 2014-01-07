package protocol.xmpp;

import protocol.Profile;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import sawim.Options;
import sawim.SawimException;
import sawim.comm.StringConvertor;
import sawim.comm.Util;


public class XmppRegistration implements Runnable, FormListener {
    private XForm form;
    private XmppConnection connection;
    private byte type;
    private String id;

    private String domain = "";
    private String xml = null;
    private String username;
    private String password;

    public final static byte TYPE_NEW_ACCOUNT_DOMAIN = 0;
    public final static byte TYPE_NEW_ACCOUNT_CREATE = 1;
    public final static byte TYPE_NONE = 2;
    private final static int FORM_SERVER = 0;

    public Forms init() {
        form = new XForm();
        type = TYPE_NEW_ACCOUNT_DOMAIN;
        id = "reg0";
        form = new XForm();
        form.init("registration", this);
        form.getForm().addTextField(FORM_SERVER, "domain", General.DEFAULT_SERVER);
        form.getForm().addString(String.format(SawimApplication.getContext().getString(R.string.hint_registration_domen), General.DEFAULT_SERVER));
        return form.getForm();
    }

    private String getServer(String domain) {
        protocol.net.SrvResolver r = new protocol.net.SrvResolver();
        String server = r.getXmpp(domain);
        r.close();
        return StringConvertor.isEmpty(server) ? (domain + ":5222") : server;
    }

    private OnAddAccount listener;

    public void setListener(OnAddAccount listener) {
        this.listener = listener;
    }

    public interface OnAddAccount {
        void addAccount(int num, Profile acc);
    }

    public void run() {
        String error = null;
        try {
            connection = new XmppConnection();
            XmlNode xform = connection.newAccountConnect(domain, "socket://" + getServer(domain));
            id = "reg1";
            form.loadFromXml(xform.childAt(0), xform);
            while (null == xml) {
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
            if (0 == xml.length()) {
                throw new SawimException(0, 0);
            }
            XmlNode n = connection.newAccountRegister(xml);
            if (("r" + "esult").equals(n.getAttribute("t" + "ype"))) {
                Profile account = new Profile();
                account.protocolType = Profile.PROTOCOL_JABBER;
                account.userId = username + "@" + domain;
                account.password = password;
                account.nick = "";
                account.isActive = true;
                listener.addAccount(Options.getAccountCount(), account);

            } else {
                error = connection.getError(n.getFirstNode("e" + "rror"));
            }

        } catch (SawimException ex) {
        } catch (Exception ex) {
        }
        try {
            connection.disconnect();
        } catch (Exception ex) {
        }

        if (null == error) {
            form.back();
            //opts = null;
            listener = null;

        } else {
            type = TYPE_NONE;
            form.setErrorMessage(error);
        }
    }

    private void register(String form) {
        xml = form;
    }

    private void cancel() {
        xml = "";
    }

    private void requestForm(String domain) {
        this.domain = domain;
        new Thread(this).start();
    }

    public void formAction(Forms form, boolean apply) {
        if (apply) {
            if ((0 < form.getSize()) || (TYPE_NEW_ACCOUNT_DOMAIN == type)) {
                doAction();
            }
        } else {
            cancel();
            form.back();
        }
    }

    private String getRegisterXml() {
        return "<iq type='set' to='" + Util.xmlEscape(domain) + "' id='"
                + Util.xmlEscape(id)
                + "'><query xmlns='jabber:iq:register'>"
                + form.getXmlForm()
                + "</query></iq>";
    }

    private void doAction() {
        switch (type) {
            case TYPE_NEW_ACCOUNT_DOMAIN:
                String jid = form.getForm().getTextFieldValue(FORM_SERVER);
                if (!StringConvertor.isEmpty(jid)) {
                    form.setWainting();
                    requestForm(jid);
                    type = TYPE_NEW_ACCOUNT_CREATE;
                }
                break;

            case TYPE_NEW_ACCOUNT_CREATE:
                username = StringConvertor.notNull(form.getField(XForm.S_USERNAME));
                password = StringConvertor.notNull(form.getField(XForm.S_PASSWORD));
                register(getRegisterXml());
                break;
        }
    }
}


