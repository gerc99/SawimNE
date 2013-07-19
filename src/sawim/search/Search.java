

package sawim.search;

import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListModel;
import java.util.Vector;

import sawim.cl.*;
import sawim.comm.*;
//import sawim.ui.text.TextListController;
import sawim.util.*;
import protocol.*;
import protocol.icq.*;
import protocol.mrim.*;
import ru.sawim.R;
import ru.sawim.models.form.ControlStateListener;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;

public final class Search implements FormListener, ControlStateListener {
    final public static int UIN         = 0;
    final public static int NICK        = 1;
    final public static int FIRST_NAME  = 2;
    final public static int LAST_NAME   = 3;
    final public static int EMAIL       = 4;
    final public static int CITY        = 5;
    final public static int GENDER      = 6;
    final public static int ONLY_ONLINE = 7;
    final public static int AGE         = 8;
    final public static int LAST_INDEX  = 9;

    private static final int USERID = 1000;
    private static final int GROUP = 1001;
    private static final int PROFILE = 1002;
    private static final int REQ_AUTH = 1020;

    private static final int MENU_ADD     = 0;
    private static final int MENU_MESSAGE = 1;
    private static final int MENU_NEXT    = 2;
    private static final int MENU_PREV    = 3;

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
    private String jabberGate = null;

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
    public void controlStateChanged(int id) {
        if (PROFILE == id) {
            String userid = searchForm.getTextFieldValue(USERID);
            if (StringConvertor.isEmpty(userid)) {
                return;
            }
            if ((null != jabberGate) && !userid.endsWith(jabberGate)) {
                userid = userid.replace('@', '%') + '@' + jabberGate;
            }
            Contact contact = protocol.createTempContact(userid);
            if (null != contact) {
                protocol.showUserInfo(contact);
            }
        }
    }
    public void show() {
        type = TYPE_FULL;
        createSearchForm(false);
        searchForm.show();
    }
    public void show(String uin, boolean isConference) {
        type = TYPE_LITE;
        setSearchParam(Search.UIN, uin);
        createSearchForm(isConference);
        searchForm.show();
    }
    private void showResults() {
        results.removeAllElements();
        searchId = Util.uniqueValue();
        waitResults = true;
        showWaitScreen();
        protocol.searchUsers(this);
    }
    public final void putToGroup(Group group) {
        this.group = group;
    }
    private Vector getGroups() {
        Vector all = protocol.getGroupItems();
        Vector groups = new Vector();
        for (int i = 0; i < all.size(); ++i) {
            Group g = (Group)all.elementAt(i);
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

    public void setJabberGate(String gate) {
        jabberGate = gate;
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
        searchForm = new Forms((TYPE_LITE == type) ? "add_user" : "search_user", this);
        if (TYPE_LITE == type) {
            addUserIdItem();
            if (null != jabberGate) {
                searchForm.addString("transport", jabberGate);
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
                searchForm.addSelector(GROUP, "group", list, def);
            }
            boolean request_auth = !isConference;
            
            if (protocol instanceof Mrim) {
                request_auth = false;
            }
            
            if (request_auth) {
                searchForm.addCheckBox(REQ_AUTH, "requauth", true);
            }
            searchForm.addLink(PROFILE, JLocale.getString("info"));
            searchForm.setControlStateListener(this);
            return;
        }
        searchForm.addCheckBox(Search.ONLY_ONLINE, "only_online", false);
        addUserIdItem();
        searchForm.addTextField(Search.NICK, "nick", "");
        searchForm.addTextField(Search.FIRST_NAME, "firstname", "");
        searchForm.addTextField(Search.FIRST_NAME, "lastname", "");
        searchForm.addTextField(Search.CITY, "city", "");
        searchForm.addSelector(Search.GENDER, "gender", "female_male" + "|" + "female" + "|" + "male", 0);
        
        if (icqFields) {
            searchForm.addTextField(Search.EMAIL, "email", "");
        }
        searchForm.addSelector(Search.AGE, "age", ageList, 0);
    }

    private void activate() {
        drawResultScreen();
    }

    private void showWaitScreen() {
        screen.setCaption(JLocale.getString("search_user"));
        VirtualListModel model = new VirtualListModel();
        model.setInfoMessage(JLocale.getString("wait"));
        screen.setModel(model);
        screen.show();
    }
    private void drawResultScreen() {
        int resultCount = getResultCount();

        if (0 < resultCount) {
            screen.setCaption(JLocale.getString("results")
                    + " " + (currentResultIndex + 1) + "/" + resultCount);
            UserInfo userInfo = getCurrentResult();
            userInfo.setSeachResultFlag();
            userInfo.setProfileView(screen);
            userInfo.updateProfileView();

        } else {
            screen.setCaption(JLocale.getString("results") + " 0/0");
            VirtualListModel model = new VirtualListModel();
            model.setInfoMessage(JLocale.getString("no_results"));
            screen.setModel(model);
        }

        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, MENU_NEXT, 2, R.string.next);
                menu.add(Menu.FIRST, MENU_PREV, 2, R.string.prev);
            }

            @Override
            public void onOptionsItemSelected(FragmentActivity activity, MenuItem item) {
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
                menu.add(Menu.FIRST, MENU_ADD, 2, "add_to_list");
                menu.add(Menu.FIRST, MENU_MESSAGE, 2, "send_message");
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case MENU_ADD:
                        UserInfo info = getCurrentResult();
                        Search s = new Search(protocol);
                        s.preferredNick = info.getOptimalName();
                        s.show(info.uin, false);
                        break;

                    case MENU_MESSAGE:
                        UserInfo temp = getCurrentResult();
                        createContact(temp).activate(protocol);
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

    public void formAction(Forms form, boolean apply) {
        if (apply) {
            if (TYPE_FULL == type) {
                currentResultIndex = 0;
                setSearchParam(Search.UIN, searchForm.getTextFieldValue(USERID).trim());
                setSearchParam(Search.NICK,        searchForm.getTextFieldValue(Search.NICK));
                setSearchParam(Search.FIRST_NAME,  searchForm.getTextFieldValue(Search.FIRST_NAME));
                setSearchParam(Search.LAST_NAME,   searchForm.getTextFieldValue(Search.FIRST_NAME));
                setSearchParam(Search.CITY,        searchForm.getTextFieldValue(Search.CITY));
                setSearchParam(Search.GENDER,      Integer.toString(searchForm.getSelectorValue(Search.GENDER)));
                setSearchParam(Search.ONLY_ONLINE, searchForm.getCheckBoxValue(Search.ONLY_ONLINE) ? "1" : "0");
                setSearchParam(Search.AGE,         ages[searchForm.getSelectorValue(Search.AGE)]);
                
                if (icqFields) {
                    setSearchParam(Search.EMAIL, searchForm.getTextFieldValue(Search.EMAIL));
                }
                
                showResults();

            } else if (TYPE_LITE == type) {
                String userid = searchForm.getTextFieldValue(USERID).trim();
                userid = StringConvertor.toLowerCase(userid);
                if (StringConvertor.isEmpty(userid)) {
                    return;
                }
                
                if ((null != jabberGate) && !userid.endsWith(jabberGate)) {
                    userid = userid.replace('@', '%') + '@' + jabberGate;
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
                    ContactList.getInstance().activate(contact);
                    form.back();
                }
            }

        } else {
            form.back();
        }
    }
    private Contact createContact(UserInfo resultData) {
        String uin = StringConvertor.toLowerCase(resultData.uin.trim());
        
        if ((null != jabberGate) && !uin.endsWith(jabberGate)) {
            uin = uin.replace('@', '%') + '@' + jabberGate;
        }
        
        Contact contact = protocol.getItemByUIN(uin);
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

