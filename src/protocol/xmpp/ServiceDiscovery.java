package protocol.xmpp;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import protocol.Contact;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.Config;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;
import ru.sawim.roster.RosterHelper;
import ru.sawim.view.TextBoxView;

import java.util.Vector;


public final class ServiceDiscovery implements TextBoxView.TextBoxListener {

    private boolean isConferenceList = false;
    private int totalCount = 0;

    private Xmpp xmpp;
    private String serverJid;
    private TextBoxView serverBox;
    private TextBoxView searchBox;
    private boolean shortView;
    private Vector jids = new Vector();
    private boolean isMucUsers = false;

    private VirtualList screen;
    private VirtualListModel model = new VirtualListModel();
    private static VirtualListItem groupItem;

    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_SET = 1;
    private static final int COMMAND_REGISTER = 2;
    private static final int COMMAND_ADHOC = 3;
    private static final int COMMAND_SEARCH = 4;
    private static final int COMMAND_SET_SERVER = 5;
    private static final int COMMAND_HOME = 6;

    public ServiceDiscovery() {
        serverBox = new TextBoxView();
        searchBox = new TextBoxView();
    }

    public void init(Xmpp protocol) {
        isMucUsers(false);
        screen = VirtualList.getInstance();
        xmpp = protocol;
        screen.setProtocol(xmpp);
        screen.setModel(model);
        screen.setCaption(JLocale.getString(R.string.service_discovery));
        groupItem = model.createNewParser(true);
        screen.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(BaseActivity activity, int position) {
                String jid = getCurrentJid(position);
                if (Jid.isConference(xmpp.getConnection().getMucServer(), jid)) {
                    Contact c = xmpp.createTempContact(jid);
                    xmpp.addContact(c);
                    xmpp.getConnection().sendPresence((XmppServiceContact) c);
                    Toast.makeText(activity, R.string.added, Toast.LENGTH_SHORT).show();
                } else if (Jid.isKnownGate(jid)) {
                    xmpp.getConnection().register(jid).show(activity);
                } else {
                    setServer(jid);
                }
            }

            @Override
            public boolean back() {
                if (isMucUsers) {
                    setServer("");
                    return true;
                }
                if (serverJid.equals("")) {
                    screen.updateModel();
                    return true;
                }
                setServer("");
                screen.updateModel();
                return false;
            }
        });
        screen.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.clear();
                String jid = getCurrentJid(listItem);
                if (Jid.isConference(xmpp.getConnection().getMucServer(), jid)) {
                    menu.add(Menu.FIRST, COMMAND_ADD, 2, JLocale.getString(R.string.service_discovery_add));

                } else if (Jid.isKnownGate(jid)) {
                    menu.add(Menu.FIRST, COMMAND_REGISTER, 2, JLocale.getString(R.string.register));

                } else {
                    menu.add(Menu.FIRST, COMMAND_SET, 2, JLocale.getString(R.string.select));
                    if (Jid.isGate(jid)) {
                        menu.add(Menu.FIRST, COMMAND_REGISTER, 2, JLocale.getString(R.string.register));
                    }
                    menu.add(Menu.FIRST, COMMAND_ADHOC, 2, JLocale.getString(R.string.adhoc));
                }
            }

            @Override
            public void onContextItemSelected(BaseActivity activity, int listItem, int action) {
                String jid = getCurrentJid(listItem);
                if (!StringConvertor.isEmpty(jid)) {
                    switch (action) {
                        case COMMAND_ADD:
                            Contact c = xmpp.createTempContact(jid);
                            xmpp.addContact(c);
                            RosterHelper.getInstance().activate(c);
                            break;

                        case COMMAND_SET:
                            setServer(jid);
                            break;

                        case COMMAND_REGISTER:
                            xmpp.getConnection().register(jid).show(activity);
                            break;

                        case COMMAND_ADHOC:
                            Contact contact = xmpp.createTempContact(jid);
                            AdHoc adhoc = new AdHoc(xmpp, (XmppContact) contact);
                            adhoc.show(activity);
                            break;
                    }
                }
            }
        });
        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, COMMAND_SEARCH, 2, JLocale.getString(R.string.service_discovery_search));
                menu.add(Menu.FIRST, COMMAND_SET_SERVER, 2, JLocale.getString(R.string.service_discovery_server));
                menu.add(Menu.FIRST, COMMAND_HOME, 2, JLocale.getString(R.string.service_discovery_home));
            }

            @Override
            public void onOptionsItemSelected(BaseActivity activity, MenuItem item) {
                switch (item.getItemId()) {
                    case COMMAND_SEARCH:
                        searchBox.setString("");
                        searchBox.setTextBoxListener(ServiceDiscovery.this);
                        searchBox.show(activity.getSupportFragmentManager(), JLocale.getString(R.string.service_discovery_search));
                        break;

                    case COMMAND_SET_SERVER:
                        serverBox.setTextBoxListener(ServiceDiscovery.this);
                        serverBox.show(activity.getSupportFragmentManager(), JLocale.getString(R.string.service_discovery_server));
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
            String rawJid = (String) jids.elementAt(num);
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
        if (Jid.isConference(xmpp.getConnection().getMucServer(), serverJid)) {
            return Jid.getResource(jid, jid);
        }
        return Jid.makeReadableJid(jid);
    }

    public void addItem(String name, String jid) {
        if (StringConvertor.isEmpty(jid)) {
            return;
        }
        String shortJid = makeShortJid(jid);
        String visibleJid = makeReadableJid(shortJid);
        VirtualListItem item = model.createNewParser(true);
        item.addLabel(20, visibleJid, Scheme.THEME_TEXT,
                shortView ? Scheme.FONT_STYLE_PLAIN : Scheme.FONT_STYLE_BOLD);
        if (!shortView) {
            if (StringConvertor.isEmpty(name)) {
                name = shortJid;
            }
            item.addDescription(20, name, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        }

        model.addPar(item);
        jids.addElement(shortJid);
        //if (0 == (jids.size() % 50)) {
        screen.updateModel();
        //}
    }

    public void showIt(BaseActivity activity) {
        if (StringConvertor.isEmpty(serverJid)) {
            setServer("");
        }
        screen.show(activity);
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
        Vector all = xmpp.getContactItems();
        boolean notEmpty = false;
        for (int i = 0; i < all.size(); ++i) {
            XmppContact contact = (XmppContact) all.elementAt(i);
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
        //    addUnique(SawimApplication.NAME, "jimm-sawim@conference.jabber.ru");
        VirtualListItem br = model.createNewParser(false);
        br.addBr();
        model.addPar(br);
        screen.updateModel();

        String domain = Jid.getDomain(xmpp.getUserId());
        addUnique(JLocale.getString(R.string.my_server), domain);
        addUnique(JLocale.getString(R.string.conferences_on_) + " " + domain, "conference." + domain);
    }

    public void isMucUsers(boolean isMucUsers) {
        this.isMucUsers = isMucUsers;
    }

    public void setServer(String jid) {
        jid = Jid.getNormalJid(jid);
        totalCount = 0;
        shortView = false;
        serverJid = jid;
        isConferenceList = (-1 == jid.indexOf('@')) && Jid.isConference(xmpp.getConnection().getMucServer(), '@' + jid);
        clear();
        if (0 == jid.length()) {
            /*groupItem.addGroup(0, SawimApplication.getContext().getString(R.string.my_conference), Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN, new VirtualListItem.OnGroupListListener() {
                @Override
                public void select() {
                    groupItem.opened = !groupItem.opened;
                    setServer("");
                }
            });
            model.addPar(groupItem);*/
            rebuild(groupItem);
            return;
        }
        if (Jid.isConference(xmpp.getConnection().getMucServer(), serverJid)) {
            shortView = true;
        }
        VirtualListItem wait = model.createNewParser(false);
        wait.addDescription(JLocale.getString(R.string.wait),
                Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(wait);
        screen.updateModel();
        xmpp.getConnection().requestDiscoItems(serverJid);
    }

    private void rebuild(VirtualListItem item) {
        Config conf = new Config().loadLocale("/jabber-services.txt");
        boolean conferences = true;
        //if (item.opened)
        //    addBookmarks();
        for (int i = 0; i < conf.getKeys().length; ++i) {
            if (conferences && !Jid.isConference(xmpp.getConnection().getMucServer(), conf.getKeys()[i])) {
                conferences = false;
                addBuildInList();
            }
            addUnique(conf.getValues()[i], conf.getKeys()[i]);
        }
        if (conferences) {
            addBuildInList();
        }
        screen.updateModel();
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
        screen.setCurrentItemIndex(index, true);
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        if (!ok) {
            return;
        }
        if (serverBox == box) {
            setServer(serverBox.getString());
            serverBox.setString(serverBox.getString());

        } else if (searchBox == box) {
            String text = searchBox.getString();
            if (isConferenceList) {
                text = text.toLowerCase();
            }
            int currentIndex = getJidIndex(screen.getCurrItem()) + 1;
            for (int i = currentIndex; i < jids.size(); ++i) {
                String jid = (String) jids.elementAt(i);
                if (-1 != jid.indexOf(text)) {
                    setCurrTextIndex(i);
                    break;
                }
            }
        }
    }
}