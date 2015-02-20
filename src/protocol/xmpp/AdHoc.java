package protocol.xmpp;

import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;

public final class AdHoc implements FormListener, ControlStateListener {
    private XmppContact contact;
    private Xmpp protocol;
    private String jid = "";
    private String[] nodes;
    private String[] names;
    private XForm commandForm;
    private static final String FORM_RESOURCE = "form_resource";
    private static final String FORM_COMMAND = "form_command";
    private Forms commandsListForm;

    public AdHoc(Xmpp protocol, XmppContact contact) {
        this.protocol = protocol;
        this.contact = contact;
        this.jid = contact.getUserId() + "/" + contact.currentResource;
        nodes = new String[0];
        names = new String[0];
    }

    public String getJid() {
        return jid;
    }

    private String resourceConf;

    public void setResource(String res) {
        resourceConf = res;
    }

    public void show(BaseActivity activity) {
        commandsListForm = new Forms(R.string.adhoc, this, true);
        updateForm(false);
        commandsListForm.setControlStateListener(this);
        commandsListForm.show(activity);
        requestCommandsForCurrentResource();
    }

    private String[] getResources() {
        String[] resources = new String[contact.subcontacts.size()];
        for (int i = resources.length - 1; 0 <= i; --i) {
            XmppContact.SubContact sub = contact.subcontacts.get(i);
            resources[i] = sub.resource;
        }
        return resources;
    }

    private void updateForm(boolean loaded) {
        String[] resources = getResources();
        int selectedResource = 0;
        if (commandsListForm.hasControl(FORM_RESOURCE)) {
            selectedResource = commandsListForm.getSelectorValue(FORM_RESOURCE);
        } else {
            for (int i = resources.length - 1; 0 <= i; --i) {
                if (resources[i].equals(contact.currentResource)) {
                    selectedResource = i;
                }
            }
        }
        commandsListForm.clearForm();
        if (1 < resources.length && !Jid.isConference(protocol.getConnection().getMucServer(), contact.getUserId())) {
            commandsListForm.addSelector(FORM_RESOURCE, R.string.resource, resources, selectedResource);
        }
        if (0 < names.length) {
            commandsListForm.addSelector(FORM_COMMAND, R.string.commands, names, 0);
        } else {
            if (loaded)
                commandsListForm.setWarningString(JLocale.getString(R.string.commands_not_found));
            else
                commandsListForm.setWaitingString(JLocale.getString(R.string.receiving_commands));
        }
        commandsListForm.invalidate(loaded);
    }

    private void requestCommandsForCurrentResource() {
        nodes = new String[0];
        names = new String[0];
        if (null != Jid.getResource(contact.getUserId(), null)) {
            jid = contact.getUserId();

        } else if (1 < contact.subcontacts.size()) {
            if (!Jid.isConference(protocol.getConnection().getMucServer(), contact.getUserId())) {
                String resource = commandsListForm.getSelectorString(FORM_RESOURCE);
                jid = contact.getUserId() + "/" + resource;
            } else {
                jid = contact.getUserId() + "/" + resourceConf;
            }

        } else if (1 == contact.subcontacts.size()) {
            XmppContact.SubContact sub = contact.subcontacts.get(0);
            if (StringConvertor.isEmpty(sub.resource)) {
                jid = contact.getUserId();
            } else {
                jid = contact.getUserId() + "/" + sub.resource;
            }

        } else {
            jid = contact.getUserId();
        }
        protocol.getConnection().requestCommandList(this);
    }

    void addItems(XmlNode query) {
        int count = (null == query) ? 0 : query.childrenCount();
        nodes = new String[count];
        names = new String[count];
        for (int i = 0; i < count; ++i) {
            XmlNode item = query.childAt(i);
            nodes[i] = StringConvertor.notNull(item.getAttribute("node"));
            names[i] = StringConvertor.notNull(item.getAttribute(XmlNode.S_NAME));
        }
        updateForm(true);
    }

    private int commandIndex;
    private String commandSessionId;
    private String commandId;

    public void formAction(BaseActivity activity, Forms form, boolean apply) {
        if (!apply) {
            form.back();
            return;
        }
        if (commandForm == null) {
            if (0 != nodes.length) {
                commandIndex = form.getSelectorValue(FORM_COMMAND);
                updateForm(false);
                protocol.getConnection().requestCommand(this, nodes[commandIndex]);
            } else {
                requestCommandsForCurrentResource();
                updateForm(false);
            }
        } else {
            execForm();
            updateForm(false);
        }
    }

    private void execForm() {
        String xml = "<iq type='set' to='" + Util.xmlEscape(jid) + "' id='"
                + Util.xmlEscape(commandId)
                + "'><command xmlns='http://jabber.org/protocol/commands'"
                + " node='" + nodes[commandIndex] + "'"
                + (null != commandSessionId ? " sessionid='" + commandSessionId + "'" : "")
                + ">"
                + commandForm.getXmlForm()
                + "</command></iq>";
        protocol.getConnection().requestRawXml(xml);
    }

    private String getCurrentNode() {
        return ((0 <= commandIndex) && (commandIndex < nodes.length))
                ? nodes[commandIndex] : "";
    }

    void loadCommandXml(XmlNode iqXml, String id) {
        XmlNode commandXml = iqXml.getFirstNode("command");
        if (null == commandXml) {
            return;
        }
        String xmlns = commandXml.getXmlns();
        if (!"http://jabber.org/protocol/commands".equals(xmlns)) {
            return;
        }
        if (!getCurrentNode().equals(commandXml.getAttribute("node"))) {
            return;
        }
        commandId = id;
        commandSessionId = commandXml.getAttribute("sessionid");
        XForm form = new XForm();
        commandsListForm.setCaption(names[commandIndex]);
        form.init(commandsListForm);
        form.loadXFromXml(commandXml, iqXml);
        commandForm = form;

        boolean showForm = (0 < commandForm.getForm().getSize());
        String status = commandXml.getAttribute("status");
        if (("canceled").equals(status) || ("completed").equals(status)) {
            String text = commandXml.getFirstNodeValue("note");
            protocol.getConnection().resetAdhoc();
            commandForm = null;
            if (!StringConvertor.isEmpty(text)) {
                commandsListForm.setWarningString(text);
                showForm = false;
            }
        }
        commandsListForm.invalidate(showForm);
    }

    @Override
    public void controlStateChanged(BaseActivity activity, String id) {
        if (FORM_RESOURCE.equals(id)) {
            requestCommandsForCurrentResource();
            updateForm(false);
        }
    }
}