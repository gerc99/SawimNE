package ru.sawim.forms;

import protocol.Protocol;
import protocol.mrim.Mrim;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.roster.RosterHelper;

import java.util.Vector;

public class SmsForm implements FormListener {

    public SmsForm(Protocol protocol, String phones) {
        this.phones = phones;

        Protocol[] protos;
        if (null == protocol) {
            protos = RosterHelper.getInstance().getProtocols();
        } else {
            protos = new Protocol[]{protocol};
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

    public void show(BaseActivity activity) {
        if (0 == agents.length()) {
            return;
        }
        form = new Forms(R.string.send_sms, this, true);
        if (null == phones) {
            form.addTextField(PHONE, R.string.phone, "");

        } else {
            form.addSelector(PHONE, "phone", phones.replace(',', '|'), 0);
        }

        if (0 < agents.indexOf('|')) {
            form.addSelector(AGENT, R.string.send_via, agents, 0);
        } else {
            form.addString(R.string.send_via, agents);
        }
        form.addTextField(TEXT, R.string.message, " ");
        form.show(activity);
    }

    private void sendSms(Protocol p, String phone, String text) {
        if (p instanceof Mrim) {
            ((Mrim) p).sendSms(phone, text);
        }
    }

    private String getPhone() {
        if (null != phones) {
            return form.getSelectorString(PHONE);
        }
        return form.getTextFieldValue(PHONE);
    }

    public void formAction(BaseActivity activity, Forms form, boolean apply) {
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