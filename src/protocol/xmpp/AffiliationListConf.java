package protocol.xmpp;

import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;
import ru.sawim.view.TextBoxView;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.roster.RosterHelper;
import sawim.util.JLocale;

import java.util.Vector;

public final class AffiliationListConf implements FormListener, TextBoxView.TextBoxListener {

    private Xmpp xmpp;
    private String serverJid;
    private TextBoxView searchBox;
    private Vector jids = new Vector();
    private Vector descriptions = new Vector();
    private Vector reasons = new Vector();
    private String myNick;

    private VirtualList screen = VirtualList.getInstance();
    private VirtualListModel model = new VirtualListModel();

    private static final int COMMAND_ADD = 0;
    private static final int COMMAND_SEARCH = 1;

    public void init(Xmpp protocol) {
        xmpp = protocol;
        searchBox = new TextBoxView();
        screen.setCaption(JLocale.getString("conf_aff_list"));
        screen.setModel(model);
        screen.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                String jid = getCurrentJid(position);
                showOptionsForm(jid, getCurrentReason(position));
            }

            @Override
            public boolean back() {
                screen.updateModel();
                return true;
            }
        });

        screen.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, COMMAND_ADD, 2, "service_discovery_add");
                menu.add(Menu.FIRST, COMMAND_SEARCH, 2, "service_discovery_search");
            }

            @Override
            public void onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case COMMAND_ADD:
                        showOptionsForm("", "");
                        break;

                    case COMMAND_SEARCH:
                        searchBox.setTextBoxListener(AffiliationListConf.this);
                        searchBox.show(SawimApplication.getCurrentActivity().getSupportFragmentManager(), "service_discovery_search");
                        break;
                }
            }
        });
    }

    private int getJidIndex(int textIndex) {
        int index = -1;
        for (int i = 0; i <= textIndex; ++i) {
            index++;
        }
        return index;
    }

    private String getJid(int num) {
        if (num < jids.size()) {
            String rawJid = (String) jids.elementAt(num);
            return rawJid;
        }
        return "";
    }

    private String getCurrentJid(int currItem) {
        int currentIndex = getJidIndex(currItem);
        return (-1 == currentIndex) ? "" : getJid(currentIndex);
    }

    private String getReason(int num) {
        if (num < reasons.size()) {
            String rawJid = (String) reasons.elementAt(num);
            return rawJid;
        }
        return "";
    }

    private String getCurrentReason(int currItem) {
        int currentIndex = getJidIndex(currItem) - 1;
        return (-1 == currentIndex) ? "" : getReason(currentIndex);
    }

    private void addServer(boolean active) {
        if (0 < serverJid.length()) {
            VirtualListItem item = model.createNewParser(active);
            item.addDescription(serverJid, Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
            model.addPar(item);
            screen.updateModel();
            if (active) {
                jids.addElement(serverJid);
            }
        }
    }

    private void clear() {
        model.clear();
        jids.removeAllElements();
        reasons.removeAllElements();
        descriptions.removeAllElements();
        addServer(false);
    }

    public void setTotalCount() {
        model.clear();
        jids.removeAllElements();
        reasons.removeAllElements();
        descriptions.removeAllElements();
        addServer(true);
    }

    public void addItem(String reasone, String jid) {
        if (StringConvertor.isEmpty(jid)) {
            return;
        }
        VirtualListItem item = model.createNewParser(true);
        item.addLabel(jid, Scheme.THEME_CHAT_INMSG,
                Scheme.FONT_STYLE_BOLD);
        if (StringConvertor.isEmpty(reasone)) {
            reasone = "";
        }
        item.addDescription(reasone, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(item);
        screen.updateModel();
        jids.addElement(jid);
        reasons.addElement(reasone);
    }

    public void showIt() {
        if (StringConvertor.isEmpty(serverJid)) {
            setServer("", "");
        }
        screen.show();
    }

    public void setServer(String jid, String myN) {
        jid = Jid.getNormalJid(jid);
        myNick = myN;
        serverJid = jid;
        clear();
        VirtualListItem wait = model.createNewParser(false);
        wait.addDescription(JLocale.getString("wait"),
                Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        model.addPar(wait);
        screen.updateModel();
    }

    private void setCurrTextIndex(int textIndex) {
        int index = 0;
        int currIndex = 0;
        for (int i = 0; i < model.getSize(); ++i) {
            if (textIndex == currIndex) {
                index = i;
                break;
            }
            currIndex++;
        }
        screen.setCurrentItemIndex(index, true);
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        if (!ok) {
            return;
        }
        if (searchBox == box) {
            String text = searchBox.getString();
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

    private static Forms enterData = null;
    private static final int JID = 0;
    private static final int AFFILIATION = 1;
    private static final int REASON = 2;
    private static final String affiliationList = "ow" + "ner" + "|" + "ad" + "min" + "|" + "mem" + "ber" + "|" + "ou" + "tcast" + "|" + "n" + "o" + "ne";
    private static final String[] affiliationI = Util.explode(affiliationList, '|');
    private String affiliation;

    public void setAffiliation(String affil) {
        affiliation = affil;
    }

    private final int getAffiliation() {
        if (("o" + "wner").equals(affiliation)) {
            return 0;
        } else if (("a" + "dmin").equals(affiliation)) {
            return 1;
        } else if (("m" + "ember").equals(affiliation)) {
            return 2;
        } else {
            return 3;
        }
    }

    private void showOptionsForm(String jid, String reason) {
        enterData = new Forms("conf_aff_list", this, true);
        enterData.addTextField(JID, "jid", jid);
        enterData.addSelector(AFFILIATION, "affiliation", affiliationList, getAffiliation());
        enterData.addTextField(REASON, "reason", reason);
        enterData.show();
    }

    public void formAction(Forms form, boolean apply) {
        if (enterData == form) {
            if (apply) {
                try {
                    String reason = enterData.getTextFieldValue(REASON);
                    xmpp.getConnection().setAffiliationListConf(serverJid,
                            enterData.getTextFieldValue(JID),
                            affiliationI[enterData.getSelectorValue(AFFILIATION)],
                            reason);
                } catch (Exception e) {
                }
                RosterHelper.getInstance().updateRoster();
            }
            enterData.back();
            enterData = null;
        }
    }
}