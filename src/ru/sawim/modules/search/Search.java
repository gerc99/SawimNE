package ru.sawim.modules.search;

import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.icq.Icq;
import protocol.mrim.Mrim;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListModel;
import ru.sawim.roster.RosterHelper;

import java.util.Vector;

public final class Search implements FormListener, ControlStateListener {
    final public static int UIN = 0;
    final public static int NICK = 1;
    final public static int FIRST_NAME = 2;
    final public static int LAST_NAME = 3;
    final public static int EMAIL = 4;
    final public static int CITY = 5;
    final public static int GENDER = 6;
    final public static int ONLY_ONLINE = 7;
    final public static int AGE = 8;
    final public static int LAST_INDEX = 9;

    private static final int USERID = 1000;
    private static final int GROUP = 1001;
    private static final int PROFILE = 1002;
    private static final int REQ_AUTH = 1020;

    private static final int MENU_ADD = 0;
    private static final int MENU_MESSAGE = 1;
    private static final int MENU_NEXT = 2;
    private static final int MENU_PREV = 3;

    private Forms searchForm;
    private VirtualList screen;

    private Group group;
    private boolean waitResults = false;
    private String preferredNick;

    private Vector results = new Vector();
    private Protocol protocol;
    private boolean icqFields;
    private byte type;
    private String[] searchParams = new String[Search.LAST_INDEX];
    private String xmppGate = null;

    private int currentResultIndex;
    private static final String ageList = "-|13-17|18-22|23-29|30-39|40-49|50-59|60-";
    private static final String[] ages = Util.explode(ageList, '|');

    private static final byte TYPE_FULL = 0;
    private static final byte TYPE_LITE = 1;

    private int searchId;

    public int getSearchId() {
        return searchId;
    }

    public Search(Protocol protocol) {
        this.protocol = protocol;
        icqFields = (protocol instanceof Icq);
        preferredNick = null;
    }

    public void controlStateChanged(BaseActivity activity, String id) {
        if (PROFILE == Integer.valueOf(id)) {
            String userid = searchForm.getTextFieldValue(USERID);
            if (StringConvertor.isEmpty(userid)) {
                return;
            }
            if ((null != xmppGate) && !userid.endsWith(xmppGate)) {
                userid = userid.replace('@', '%') + '@' + xmppGate;
            }
            Contact contact = protocol.createTempContact(userid);
            if (null != contact) {
                searchForm.back();
                protocol.showUserInfo(activity, contact);
            }
        }
    }

    public void show(BaseActivity activity) {
        type = TYPE_FULL;
        createSearchForm(false);
        searchForm.show(activity);
    }

    public void show(BaseActivity activity, String uin, boolean isConference) {
        type = TYPE_LITE;
        setSearchParam(Search.UIN, uin);
        createSearchForm(isConference);
        searchForm.show(activity);
    }

    private void showResults(BaseActivity activity) {
        results.removeAllElements();
        searchId = Util.uniqueValue();
        waitResults = true;
        showWaitScreen(activity);
        protocol.searchUsers(this);
    }

    public final void putToGroup(Group group) {
        this.group = group;
    }

    private Vector getGroups() {
        Vector all = protocol.getGroupItems();
        Vector groups = new Vector();
        for (int i = 0; i < all.size(); ++i) {
            Group g = (Group) all.elementAt(i);
            if (g.hasMode(Group.MODE_NEW_CONTACTS)) {
                groups.addElement(g);
            }
        }
        return groups;
    }

    public void addResult(UserInfo info) {
        results.addElement(info);
    }

    private UserInfo getCurrentResult() {
        return (UserInfo) results.elementAt(currentResultIndex);
    }

    private int getResultCount() {
        return results.size();
    }

    public void setXmppGate(String gate) {
        xmppGate = gate;
    }

    public String getSearchParam(int param) {
        return searchParams[param];
    }

    public void setSearchParam(int param, String value) {
        searchParams[param] = StringConvertor.isEmpty(value) ? null : value;
    }

    public String[] getSearchParams() {
        return searchParams;
    }

    public void finished() {
        if (waitResults) {
            activate();
        }
        waitResults = false;
    }

    public void canceled() {
        if (waitResults) {
            searchId = -1;
        }
        waitResults = false;
    }

    private void addUserIdItem() {
        String userid = StringConvertor.notNull(getSearchParam(UIN));
        searchForm.addTextField(USERID, protocol.getUserIdName(), userid);
    }

    private void createSearchForm(boolean isConference) {
        screen = VirtualList.getInstance();
        searchForm = new Forms((TYPE_LITE == type) ? R.string.add_user : R.string.search_user, this, true);
        if (TYPE_LITE == type) {
            addUserIdItem();
            if (null != xmppGate) {
                searchForm.addString(R.string.transport, xmppGate);
            }
            Vector groups = getGroups();
            if (!groups.isEmpty()) {
                String[] list = new String[groups.size()];
                int def = 0;
                for (int i = 0; i < groups.size(); ++i) {
                    Group g = (Group) groups.elementAt(i);
                    list[i] = g.getName();
                    if (g == group) {
                        def = i;
                    }
                }
                searchForm.addSelector(GROUP, R.string.group, list, def);
            }
            boolean request_auth = !isConference;

            if (protocol instanceof Mrim) {
                request_auth = false;
            }

            if (request_auth) {
                searchForm.addCheckBox(REQ_AUTH, R.string.requauth, true);
            }
            searchForm.addButton(PROFILE, JLocale.getString(R.string.info));
            searchForm.setControlStateListener(this);
            return;
        }
        searchForm.addCheckBox(Search.ONLY_ONLINE, R.string.only_online, false);
        addUserIdItem();
        searchForm.addTextField(Search.NICK, R.string.nick, "");
        searchForm.addTextField(Search.FIRST_NAME, R.string.firstname, "");
        searchForm.addTextField(Search.FIRST_NAME, R.string.lastname, "");
        searchForm.addTextField(Search.CITY, R.string.city, "");
        int[] genderItems = {R.string.female_male, R.string.female, R.string.male};
        searchForm.addSelector(Search.GENDER, R.string.gender, genderItems, 0);

        if (icqFields) {
            searchForm.addTextField(Search.EMAIL, R.string.email, "");
        }
        searchForm.addSelector(Search.AGE, R.string.age, ageList, 0);
        searchForm.invalidate(true);
    }

    private void activate() {
        drawResultScreen();
    }

    private void showWaitScreen(BaseActivity activity) {
        screen.setCaption(JLocale.getString(R.string.search_user));
        VirtualListModel model = new VirtualListModel();
        model.setInfoMessage(JLocale.getString(R.string.wait));
        screen.updateModel();
        screen.setProtocol(protocol);
        screen.setModel(model);
        screen.show(activity);
    }

    private void drawResultScreen() {
        int resultCount = getResultCount();

        if (0 < resultCount) {
            screen.setCaption(JLocale.getString(R.string.results)
                    + " " + (currentResultIndex + 1) + "/" + resultCount);
            UserInfo userInfo = getCurrentResult();
            userInfo.setSearchResultFlag();
            userInfo.setProfileView(screen);
            userInfo.updateProfileView();

        } else {
            screen.setCaption(JLocale.getString(R.string.results) + " 0/0");
            VirtualListModel model = new VirtualListModel();
            model.setInfoMessage(JLocale.getString(R.string.no_results));
            screen.updateModel();
            screen.setProtocol(protocol);
            screen.setModel(model);
        }

        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, MENU_NEXT, 2, R.string.next);
                menu.add(Menu.FIRST, MENU_PREV, 2, R.string.prev);
            }

            @Override
            public void onOptionsItemSelected(BaseActivity activity, MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_NEXT:
                        nextOrPrev(true);
                        break;
                    case MENU_PREV:
                        nextOrPrev(false);
                        break;
                }
            }
        });

        screen.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, MENU_ADD, 2, R.string.add_to_list);
                menu.add(Menu.FIRST, MENU_MESSAGE, 2, R.string.send_message);
            }

            @Override
            public void onContextItemSelected(BaseActivity activity, int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case MENU_ADD:
                        UserInfo info = getCurrentResult();
                        Search s = new Search(protocol);
                        s.preferredNick = info.getOptimalName();
                        s.show(activity, info.uin, false);
                        break;

                    case MENU_MESSAGE:
                        UserInfo temp = getCurrentResult();
                        createContact(temp).activate(activity, protocol);
                        break;
                }
            }
        });
    }

    private void nextOrPrev(boolean next) {
        int size = getResultCount();
        if (0 < size) {
            if (1 < size) {
                getCurrentResult().setProfileView(null);
                getCurrentResult().removeAvatar();
            }
            currentResultIndex = ((next ? 1 : size - 1) + currentResultIndex) % size;
        }
        activate();
    }

    public void onContentMove(VirtualListModel sender, int direction) {
        nextOrPrev(1 == direction);
    }

    public void formAction(BaseActivity activity, Forms form, boolean apply) {
        if (apply) {
            if (TYPE_FULL == type) {
                currentResultIndex = 0;
                setSearchParam(Search.UIN, searchForm.getTextFieldValue(USERID).trim());
                setSearchParam(Search.NICK, searchForm.getTextFieldValue(Search.NICK));
                setSearchParam(Search.FIRST_NAME, searchForm.getTextFieldValue(Search.FIRST_NAME));
                setSearchParam(Search.LAST_NAME, searchForm.getTextFieldValue(Search.FIRST_NAME));
                setSearchParam(Search.CITY, searchForm.getTextFieldValue(Search.CITY));
                setSearchParam(Search.GENDER, Integer.toString(searchForm.getSelectorValue(Search.GENDER)));
                setSearchParam(Search.ONLY_ONLINE, searchForm.getCheckBoxValue(Search.ONLY_ONLINE) ? "1" : "0");
                setSearchParam(Search.AGE, ages[searchForm.getSelectorValue(Search.AGE)]);

                if (icqFields) {
                    setSearchParam(Search.EMAIL, searchForm.getTextFieldValue(Search.EMAIL));
                }

                showResults(activity);

            } else if (TYPE_LITE == type) {
                String userid = searchForm.getTextFieldValue(USERID).trim();
                userid = userid.toLowerCase();
                if (StringConvertor.isEmpty(userid)) {
                    return;
                }

                if ((null != xmppGate) && !userid.endsWith(xmppGate)) {
                    userid = userid.replace('@', '%') + '@' + xmppGate;
                }


                Contact contact = protocol.createTempContact(userid);
                if (null != contact) {
                    if (contact.isTemp()) {
                        String g = null;
                        if (!contact.isSingleUserContact()) {
                            g = contact.getDefaultGroupName();
                        }
                        if (null == g) {
                            g = searchForm.getSelectorString(GROUP);
                        }
                        contact.setName(preferredNick);
                        contact.setGroup(protocol.getGroup(g));
                        protocol.addContact(contact);
                        if (searchForm.getCheckBoxValue(REQ_AUTH)
                                && contact.isSingleUserContact()) {
                            protocol.requestAuth(contact);
                        }
                    }
                    RosterHelper.getInstance().activate(contact);
                }
            }
        }
        form.back();
    }

    private Contact createContact(UserInfo resultData) {
        String uin = resultData.uin.trim().toLowerCase();

        if ((null != xmppGate) && !uin.endsWith(xmppGate)) {
            uin = uin.replace('@', '%') + '@' + xmppGate;
        }

        Contact contact = protocol.getItemByUID(uin);
        if (null == contact) {
            contact = protocol.createTempContact(uin);
            contact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
            protocol.addTempContact(contact);
            contact.setOfflineStatus();
            contact.setName(resultData.getOptimalName());
        }
        return contact;
    }
}

