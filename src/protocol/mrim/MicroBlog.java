package protocol.mrim;

import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import protocol.Contact;
import ru.sawim.activities.VirtualListActivity;
import ru.sawim.view.TextBoxView;
import sawim.Clipboard;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import ru.sawim.models.list.VirtualListModel;
import ru.sawim.models.list.VirtualList;
import ru.sawim.Scheme;
import java.util.Vector;
import ru.sawim.models.form.VirtualListItem;
import sawim.util.JLocale;

public final class MicroBlog implements TextBoxView.TextBoxListener {
    private VirtualListModel model = new VirtualListModel();
    private Vector emails = new Vector();
    private Vector ids = new Vector();
    private Mrim mrim;
    private VirtualList list;

    private static final int MENU_WRITE     = 0;
    private static final int MENU_REPLY     = 1;
    private static final int MENU_COPY      = 2;
    private static final int MENU_CLEAN     = 3;
    private static final int MENU_USER_MENU = 4;

    public MicroBlog(Mrim mrim) {
        this.mrim = mrim;
    }

    public void activate() {
        list = VirtualList.getInstance();
        list.setCaption(JLocale.getString("microblog"));
        list.setModel(model);
        list.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                String to = "";
                int cur = position;
                if (cur < ids.size()) {
                    to = (String) ids.elementAt(cur);
                }
                write(to);
            }

            @Override
            public boolean back() {
                list.clearListeners();
                return true;
            }
        });
        list.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, MENU_WRITE, 2, JLocale.getString("message"));
                menu.add(Menu.FIRST, MENU_CLEAN, 2, JLocale.getString("clear"));
            }

            @Override
            public void onOptionsItemSelected(FragmentActivity activity, MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_WRITE:
                        write("");
                        break;

                    case MENU_CLEAN:
                        synchronized (this) {
                            emails.removeAllElements();
                            ids.removeAllElements();
                            model.clear();
                            list.updateModel();
                        }
                        break;
                }
            }
        });
        list.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, MENU_USER_MENU, 2, JLocale.getString("reply"));
                menu.add(Menu.FIRST, MENU_COPY, 2, JLocale.getString("copy_text"));
                //menu.add(Menu.FIRST, MENU_USER_MENU, 2, JLocale.getString("user_menu"));
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case MENU_REPLY:
                        String to = "";
                        int cur = listItem;
                        if (cur < ids.size()) {
                            to = (String)ids.elementAt(cur);
                        }
                        write(to);
                        break;

                    case MENU_COPY:
                        Clipboard.setClipBoardText(model.elements.get(listItem).getLabel() + "\n" + model.elements.get(listItem).getDescSpan().toString());
                        break;

                    /*case MENU_USER_MENU:
                        try {
                            int item = list.getCurrItem();
                            String uin = (String)emails.elementAt(item);
                            new ContactMenu(mrim, mrim.createTempContact(uin)).getContextMenu(menu);
                        } catch (Exception e) {
                        }
                        break;*/
                }
            }
        });
        list.show();
    }

    private void removeOldRecords() {
        final int maxRecordCount = 50;
        while (maxRecordCount < model.getSize()) {
            ids.removeElementAt(0);
            emails.removeElementAt(0);
            model.removeFirstText();
        }
    }

    public boolean addPost(String from, String nick, String post, String postid,
            boolean reply, long gmtTime) {
        if (StringConvertor.isEmpty(post) || ids.contains(postid)) {
            return false;
        }

        String date = Util.getLocalDateString(gmtTime, false);
        Contact contact = mrim.getItemByUIN(from);
        emails.addElement(from);
        ids.addElement(postid);

        VirtualListItem par = model.createNewParser(true);
        if (null != contact) {
            nick = contact.getName();
        }
        if (StringConvertor.isEmpty(nick)) {
            nick = from;
        }
        String label = nick;
        if (reply) {
            label = " (reply)";
        }
        par.addLabel(label + " " + date + ":", Scheme.THEME_MAGIC_EYE_NUMBER, Scheme.FONT_STYLE_PLAIN);
        par.addTextWithSmiles(post, Scheme.THEME_MAGIC_EYE_TEXT, Scheme.FONT_STYLE_PLAIN);

        model.addPar(par);
        //removeOldRecords();
        return true;
    }

    private TextBoxView postEditor;
    private String replayTo = "";

    private void write(String to) {
        replayTo = StringConvertor.notNull(to);
        postEditor = new TextBoxView();
        postEditor.setTextBoxListener(this);
        postEditor.show(VirtualListActivity.getInstance().getSupportFragmentManager(), StringConvertor.isEmpty(replayTo) ? "message" : "reply");
    }

    public void textboxAction(TextBoxView box, boolean ok) {
        MrimConnection c = mrim.getConnection();
        if (ok && mrim.isConnected() && (null != c)) {
            String text = postEditor.getString();
            if (!StringConvertor.isEmpty(text)) {
                c.postToMicroBlog(text, replayTo);
            }
            list.updateModel();
        }
    }
}