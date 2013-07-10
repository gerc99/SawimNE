package protocol;

import android.view.ContextMenu;
import android.view.Menu;
import ru.sawim.models.form.VirtualListItem;
import sawim.Clipboard;
import ru.sawim.models.list.VirtualListModel;
import DrawControls.icons.Icon;
import ru.sawim.General;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import ru.sawim.Scheme;
import ru.sawim.models.list.VirtualList;
import protocol.jabber.*;
import sawim.util.JLocale;

import java.util.List;

public final class StatusView {

    private Protocol protocol;
    private Contact contact;
    private String clientVersion;
    private VirtualListModel model;
    private VirtualList list;

    public static final int INFO_MENU_COPY     = 1;
    public static final int INFO_MENU_COPY_ALL = 2;

    public StatusView() {
    }

    public void addClient() {
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
            long now = General.getCurrentGmtTime();
            boolean today = (now - 24 * 60 * 60) < signonTime;
            if (contact.isOnline()) {
                addInfo("li_signon_time", Util.getLocalDateString(signonTime, today));
                addInfo("li_online_time", Util.longitudeToString(now - signonTime));
            } else {
                addInfo("li_signoff_time", Util.getLocalDateString(signonTime, today));
            }
        }
    }

    public void addPlain(Icon img, String str) {
        if (!StringConvertor.isEmpty(str)) {
            VirtualListItem line = model.createNewParser(true);
            if (null != img) {
                line.addIcon(img);
            }
            line.addDescription(str, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
            model.addPar(line);
        }
    }
    public void addStatusText(String text) {
        if (!StringConvertor.isEmpty(text)) {
            VirtualListItem line = model.createNewParser(true);
            line.addDescription(text, Scheme.THEME_PARAM_VALUE, Scheme.FONT_STYLE_PLAIN);
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
        list.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, INFO_MENU_COPY, 2, JLocale.getString("copy_text"));
                menu.add(Menu.FIRST, INFO_MENU_COPY_ALL, 2, JLocale.getString("copy_all_text"));
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case INFO_MENU_COPY:
                        VirtualListItem item = list.getModel().elements.get(listItem);
                        Clipboard.setClipBoardText(((item.getLabel() == null) ? "" : item.getLabel() + "\n") + item.getDescStr());
                        break;

                    case INFO_MENU_COPY_ALL:
                        StringBuffer s = new StringBuffer();
                        List<VirtualListItem> listItems = list.getModel().elements;
                        for (int i = 0; i < listItems.size(); ++i) {
                            s.append(listItems.get(i).getLabel()).append("\n")
                                    .append(listItems.get(i).getDescStr()).append("\n");
                        }
                        Clipboard.setClipBoardText(s.toString());
                        break;
                }
            }
        });
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

    public void showIt() {
        list.show();
    }
}

