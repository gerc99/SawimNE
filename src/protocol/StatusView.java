

package protocol;

import ru.sawim.models.form.VirtualListItem;
import sawim.ui.text.VirtualListModel;
import DrawControls.icons.Icon;
import sawim.Sawim;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.ui.base.Scheme;
import sawim.ui.text.VirtualList;
//import sawim.ui.text.TextListController;
import protocol.jabber.*;


public final class StatusView {
    public static final int INFO_MENU_COPY     = 1;
    public static final int INFO_MENU_COPY_ALL = 2;
    public static final int INFO_MENU_GOTO_URL = 4;
    public static final int INFO_MENU_GET_X    = 5;
    private Protocol protocol;
    private Contact contact;
    private String clientVersion;
    
    private VirtualListModel model;
    private VirtualList list;

    public StatusView() {
    }

    public void addClient() {
        addBr();
        if ((ClientInfo.CLI_NONE != contact.clientIndex)
                && (null != protocol.clientInfo)) {
            addPlain(protocol.clientInfo.getIcon(contact.clientIndex),
                    (protocol.clientInfo.getName(contact.clientIndex)
                    + " " + contact.version).trim());
        }
        addPlain(null, clientVersion);
    }
    public void setClientVersion(String version) {
        clientVersion = version;
    }

    public void addTime() {
        if (!contact.isSingleUserContact()) {
            return;
        }
        if (contact instanceof JabberServiceContact) {
            return;
        }
        long signonTime = contact.chaingingStatusTime;
        if (0 < signonTime) {
            long now = Sawim.getCurrentGmtTime();
            boolean today = (now - 24 * 60 * 60) < signonTime;
            if (contact.isOnline()) {
                addInfo("li_signon_time", Util.getLocalDateString(signonTime, today));
                addInfo("li_online_time", Util.longitudeToString(now - signonTime));
            } else {
                addInfo("li_signoff_time", Util.getLocalDateString(signonTime, today));
            }
        }
    }
    
    public void addBr() {
        VirtualListItem line = model.createNewParser(false);
        line.addDescription(" \n", Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(line);
    }
    public void addPlain(Icon img, String str) {
        if (!StringConvertor.isEmpty(str)) {
            VirtualListItem line = model.createNewParser(true);
            if (null != img) {
                line.addIcon(img);
            }
            line.addDescriptionSelectable(str, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            model.addPar(line);
        }
    }
    public void addStatusText(String text) {
        if (!StringConvertor.isEmpty(text)) {
            VirtualListItem line = model.createNewParser(true);
            line.addDescriptionSelectable(text, Scheme.THEME_PARAM_VALUE, Scheme.FONT_STYLE_PLAIN);
            model.addPar(line);
        }
    }
    public void addInfo(String key, String value) {
        model.addParam(key, value);
    }
    
    public void addContactStatus() {
        byte status = contact.getStatusIndex();
        StatusInfo info = protocol.getStatusInfo();
        addStatus(info.getIcon(status), info.getName(status));
    }
    
    public void addXStatus() {
        XStatusInfo info = protocol.getXStatusInfo();
        int x = contact.getXStatusIndex();
        addStatus(info.getIcon(x), info.getName(x));
    }
    
    public void addStatus(Icon icon, String name) {
        addPlain(icon, name);
    }

    public void init(Protocol p, Contact c) {
        list = VirtualList.getInstance();
        model = new VirtualListModel();
        list.setModel(model);
        /*list.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(Menu menu) {
                menu.add(Menu.FIRST, INFO_MENU_COPY, 2, "copy_text");
                menu.add(Menu.FIRST, INFO_MENU_COPY_ALL, 2, "copy_all_text");
                //if ((1 < list.getCurrItem()) && Util.hasURL(model.getParText(list.getCurrItem()))) {
                //    menu.addItem("goto_url", INFO_MENU_GOTO_URL);
                //}

                if ((XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex())
                        && (protocol instanceof Icq)) {
                    menu.add(Menu.FIRST, INFO_MENU_GET_X, 2, "reqxstatmsg up");
                }
            }

            @Override
            public void onContextItemSelected(int itemId) {
                switch (itemId) {
                    case INFO_MENU_COPY:
                    case INFO_MENU_COPY_ALL:
                        //list.getController().copy(INFO_MENU_COPY_ALL == action);
                        list.restore();
                        break;

                    case INFO_MENU_GOTO_URL:
                        //ContactList.getInstance().gotoUrl(model.getParText(list.getCurrItem()));
                        break;

                    case INFO_MENU_GET_X:
                        ((Icq)protocol).requestXStatusMessage(contact);
                        list.restore();
                        break;
                }
            }
        });*/
        contact = c;
        protocol = p;
        clientVersion = null;
    }
    public void initUI() {
        model.clear();
        list.setCaption(contact.getName());
        addInfo(protocol.getUserIdName(), contact.getUserId());
    }
    public Contact getContact() {
        return contact;
    }
    public void update() {
        list.updateModel();
    }
    public void showIt() {
        list.show();
    }
}

