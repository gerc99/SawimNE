package protocol.jabber;

import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.models.form.VirtualListItem;
import ru.sawim.models.list.VirtualList;
import java.util.Vector;
import sawim.cl.ContactList;
import ru.sawim.models.list.VirtualListModel;
import sawim.util.JLocale;
import sawim.comm.*;
import ru.sawim.Scheme;
import protocol.*;
import ru.sawim.view.TextBoxView;


public final class ServiceDiscovery implements TextBoxView.TextBoxListener {

    private boolean isConferenceList = false;
    private int totalCount = 0;

    private Jabber jabber;
    private String serverJid;
    private TextBoxView serverBox;
    private TextBoxView searchBox;
    private boolean shortView;
    private Vector jids = new Vector();

    private VirtualList screen;
    private VirtualListModel model = new VirtualListModel();

    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_SET = 1;
    private static final int COMMAND_REGISTER = 2;
    private static final int COMMAND_SEARCH = 3;
    private static final int COMMAND_SET_SERVER = 4;
    private static final int COMMAND_HOME = 5;

    public void init(Jabber protocol) {
        screen = VirtualList.getInstance();
        jabber = protocol;
        serverBox = new TextBoxView();
        searchBox = new TextBoxView();
        screen.setModel(model);
        screen.setCaption(JLocale.getString("service_discovery"));
        screen.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                String jid = getCurrentJid(position);
                if (Jid.isConference(jid)) {
                    Contact c = jabber.createTempContact(jid);
                    jabber.addContact(c);
                } else if (Jid.isKnownGate(jid)) {
                    jabber.getConnection().register(jid);
                } else {
                    setServer(jid);
                }
            }

            @Override
            public boolean back() {
                if (serverJid == "") {
                    screen.clearAll();
                    return true;
                }
                setServer("");
                return false;
            }
        });
        screen.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.clear();
                String jid = getCurrentJid(listItem);
                if (Jid.isConference(jid)) {
                    menu.add(Menu.FIRST, COMMAND_ADD, 2, JLocale.getString("service_discovery_add"));

                } else if (Jid.isKnownGate(jid)) {
                    menu.add(Menu.FIRST, COMMAND_REGISTER, 2, JLocale.getString("register"));

                } else {
                    menu.add(Menu.FIRST, COMMAND_SET, 2, JLocale.getString("select"));
                    if (Jid.isGate(jid)) {
                        menu.add(Menu.FIRST, COMMAND_REGISTER, 2, JLocale.getString("register"));
                    }
                }
            }

            @Override
            public void onContextItemSelected(int listItem, int action) {
                String jid = getCurrentJid(listItem);
                if (!StringConvertor.isEmpty(jid)) {
                    switch (action) {
                        case COMMAND_ADD:
                            Contact c = jabber.createTempContact(jid);
                            jabber.addContact(c);
                            ContactList.getInstance().activate(c);
                            break;

                        case COMMAND_SET:
                            setServer(jid);
                            break;

                        case COMMAND_REGISTER:
                            jabber.getConnection().register(jid);
                            break;
                    }
                }
            }
        });
        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, COMMAND_SEARCH, 2, JLocale.getString("service_discovery_search"));
                menu.add(Menu.FIRST, COMMAND_SET_SERVER, 2, JLocale.getString("service_discovery_server"));
                menu.add(Menu.FIRST, COMMAND_HOME, 2, JLocale.getString("service_discovery_home"));
            }

            @Override
            public void onOptionsItemSelected(FragmentActivity activity, MenuItem item) {
                switch (item.getItemId()) {
                    case COMMAND_SEARCH:
                        searchBox.setString("");
                        searchBox.setTextBoxListener(ServiceDiscovery.this);
                        searchBox.show(activity.getSupportFragmentManager(), JLocale.getString("service_discovery_search"));
                        break;

                    case COMMAND_SET_SERVER:
                        serverBox.setString(serverJid);
                        serverBox.setTextBoxListener(ServiceDiscovery.this);
                        serverBox.show(activity.getSupportFragmentManager(), JLocale.getString("service_discovery_server"));
                        break;

                    case COMMAND_HOME:
                        setServer("");
                        break;
                }
            }
        });
    }

    private String getJid(int num) {
        if (num < jids.size()) {
            String rawJid = (String)jids.elementAt(num);
            if (rawJid.endsWith("@")) {
                return rawJid + serverJid;
            }
            return rawJid;
        }
        return "";
    }
    private int getJidIndex(int textIndex) {
        if (!model.isItemSelectable(textIndex)) return -1;
        int index = -1;
        for (int i = 0; i <= textIndex; ++i) {
            if (model.isItemSelectable(i)) index++;
        }
        return index;
    }
    private String getCurrentJid(int currItem) {
        int currentIndex = getJidIndex(currItem);
        return (-1 == currentIndex) ? "" : getJid(currentIndex);
    }

    private void addServer(boolean active) {
        if (0 < serverJid.length()) {
            VirtualListItem item = model.createNewParser(active);
            item.addDescription(serverJid, Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
            model.addPar(item);
            if (active) {
                jids.addElement(serverJid);
            }
        }
    }
    private void clear() {
        model.clear();
        jids.removeAllElements();
        addServer(false);
    }
    public void setTotalCount(int count) {
        model.clear();
        jids.removeAllElements();
        addServer(true);
        totalCount = count;
        shortView |= (totalCount > 400);
        screen.updateModel();
    }

    private String makeShortJid(String jid) {
        if (isConferenceList) {
            return jid.substring(0, jid.indexOf('@') + 1);
        }
        return jid;
    }
    private String makeReadableJid(String jid) {
        if (isConferenceList) {
            return jid;
        }
        if (Jid.isConference(serverJid)) {
            return Jid.getResource(jid, jid);
        }
        jid = Util.replace(jid, "@conference.jabber.ru", "@c.j.ru");
        return Util.replace(jid, "@conference.", "@c.");
    }

    public void addItem(String name, String jid) {
        if (StringConvertor.isEmpty(jid)) {
            return;
        }
        String shortJid = makeShortJid(jid);
        String visibleJid = makeReadableJid(shortJid);
        VirtualListItem item = model.createNewParser(true);
        item.addLabel(visibleJid, Scheme.THEME_TEXT,
                shortView ? Scheme.FONT_STYLE_PLAIN : Scheme.FONT_STYLE_BOLD);
        if (!shortView) {
            if (StringConvertor.isEmpty(name)) {
                name = shortJid;
            }
            item.addDescription(name, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        }

        model.addPar(item);
        jids.addElement(shortJid);
        if (0 == (jids.size() % 50)) {
            screen.updateModel();
        }
    }

    public void showIt() {
        if (StringConvertor.isEmpty(serverJid)) {
            setServer("");
        }
        screen.show();
    }
    public void update() {
        screen.updateModel();
    }

    private void addUnique(String text, String jid) {
        if (-1 == jids.indexOf(jid)) {
            addItem(text, jid);
        }
    }

    private void addBookmarks() {
        Vector all = jabber.getContactItems();
        boolean notEmpty = false;
        for (int i = 0; i < all.size(); ++i) {
            JabberContact contact = (JabberContact)all.elementAt(i);
            if (contact.isConference()) {
                addUnique(contact.getName(), contact.getUserId());
                notEmpty = true;
            }
        }
        if (notEmpty) {
            VirtualListItem br = model.createNewParser(false);
            br.addBr();
            model.addPar(br);
            screen.updateModel();
        }
    }
    private void addBuildInList() {
        addUnique("Sawim aspro", "jimm-sawim@conference.jabber.ru");
        VirtualListItem br = model.createNewParser(false);
        br.addBr();
        model.addPar(br);
        screen.updateModel();

        String domain = Jid.getDomain(jabber.getUserId());
        addUnique(JLocale.getString("my_server"), domain);
        addUnique(JLocale.getString("conferences_on_") + domain, "conference." + domain);
    }

    public void setServer(String jid) {
        jid = Jid.getNormalJid(jid);
        totalCount = 0;
        shortView = false;
        serverJid = jid;
        isConferenceList = (-1 == jid.indexOf('@')) && Jid.isConference('@' + jid);
        clear();
        if (0 == jid.length()) {
            Config conf = new Config().loadLocale("/jabber-services.txt");
            boolean conferences = true;
            addBookmarks();
            for (int i = 0; i < conf.getKeys().length; ++i) {
                if (conferences && !Jid.isConference(conf.getKeys()[i])) {
                    conferences = false;
                    addBuildInList();
                }
                addUnique(conf.getValues()[i], conf.getKeys()[i]);
            }
            if (conferences) {
                addBuildInList();
            }
            screen.updateModel();
            return;
        }
        if (Jid.isConference(serverJid)) {
            shortView = true;
        }
        VirtualListItem wait = model.createNewParser(false);
        wait.addDescription(JLocale.getString("wait"),
                Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(wait);
        screen.updateModel();
        jabber.getConnection().requestDiscoItems(serverJid);
    }

    void setError(String description) {
        clear();
        VirtualListItem error = model.createNewParser(false);
        error.addDescription(description, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(error);
        screen.updateModel();
    }

    private void setCurrTextIndex(int textIndex) {
        int index = 0;
        int currIndex = 0;
        for (int i = 0; i < model.getSize(); ++i) {
            if (model.isItemSelectable(i)) {
                if (textIndex == currIndex) {
                    index = i;
                    break;
                }
                currIndex++;
            }
        }
        screen.setCurrentItemIndex(index);
    }
    public void textboxAction(TextBoxView box, boolean ok) {
        if (!ok) {
            return;
        }
        if (serverBox == box) {
            setServer(serverBox.getString());

        } else if (searchBox == box) {
            String text = searchBox.getString();
            if (isConferenceList) {
                text = StringConvertor.toLowerCase(text);
            }
            int currentIndex = getJidIndex(screen.getCurrItem()) + 1;
            for (int i = currentIndex; i < jids.size(); ++i) {
                String jid = (String)jids.elementAt(i);
                if (-1 != jid.indexOf(text)) {
                    setCurrTextIndex(i);
                    break;
                }
            }
        }
    }
}