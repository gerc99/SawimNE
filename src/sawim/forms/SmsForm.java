package sawim.forms;

import java.util.Vector;
import protocol.mrim.*;
import protocol.Protocol;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import sawim.cl.ContactList;
import sawim.util.JLocale;

public class SmsForm implements FormListener {

    /** Creates a new instance of SmsForm */
    public SmsForm(Protocol protocol, String phones) {
        this.phones = phones;

        Protocol[] protos;
        if (null == protocol) {
            protos = ContactList.getInstance().getProtocols();
        } else {
            protos = new Protocol[] {protocol};
        }

        protocols = new Vector();
        agents = "";

        for (int i = 0; i < protos.length; ++i) {
            if ((protos[i] instanceof Mrim) && protos[i].isConnected()) {
                agents += "|" + protos[i].getUserId();
                protocols.addElement(protos[i]);
            }
        }
        if (agents.startsWith("|")) {
            agents = agents.substring(1);
        }
    }
    private String phones;
    private Forms form;
    private String agents;
    private Vector protocols;

    private static final int PHONE = 0;
    private static final int TEXT = 1;
    private static final int AGENT = 2;

    public void show() {
        if (0 == agents.length()) {
            return;
        }
        form = new Forms("send_sms", this);
        if (null == phones) {
            form.addTextField(PHONE, "phone", "");

        } else {
            form.addSelector(PHONE, "phone", phones.replace(',', '|'), 0);
        }

        if (0 < agents.indexOf('|')) {
            form.addSelector(AGENT, "send_via", agents, 0);
        } else {
            form.addString("send_via", JLocale.getString(agents));
        }
        form.addTextField(TEXT, "message", "");
        form.show();
    }

    private void sendSms(Protocol p, String phone, String text) {
        if (p instanceof Mrim) {
            ((Mrim)p).sendSms(phone, text);
            return;
        }
    }
    private String getPhone() {
        if (null != phones) {
            return form.getSelectorString(PHONE);
        }
        return form.getTextFieldValue(PHONE);
    }
    public void formAction(Forms form, boolean apply) {
        if (apply) {
            final String text = form.getTextFieldValue(TEXT);
            final String phone = getPhone();
            if ((0 < text.length()) && (0 < phone.length())) {
                int agent = (0 < agents.indexOf('|')) ? form.getSelectorValue(AGENT) : 0;
                sendSms((Protocol) protocols.elementAt(agent), phone, text);
            }
        }
        form.back();
    }
}