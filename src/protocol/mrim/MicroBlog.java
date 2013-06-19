



package protocol.mrim;

import sawim.ui.text.VirtualListModel;
import sawim.ui.text.VirtualList;
import DrawControls.icons.*;
import sawim.chat.message.Message;
import sawim.ui.base.Scheme;
import java.util.Vector;

import sawim.comm.*;
//import sawim.ui.text.TextListController;
import sawim.util.*;
import protocol.*;
import ru.sawim.models.form.VirtualListItem;

public final class MicroBlog/* extends TextListController*/ {
    private VirtualListModel model = new VirtualListModel();
    private Vector emails = new Vector();
    private Vector ids = new Vector();
    private Mrim mrim;
    private boolean hasNewMessage;
    private final VirtualList list;

    public MicroBlog(Mrim mrim) {
        this.mrim = mrim;
        list = VirtualList.getInstance();
        list.setCaption(JLocale.getString("microblog"));
        list.setModel(model);
        /*list.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(Menu menu) {
                menu.add(Menu.FIRST, MENU_WRITE, 2, "message");
                menu.add(Menu.FIRST, MENU_REPLY, 2, "reply");
                menu.add(Menu.FIRST, MENU_USER_MENU, 2, "user_menu");
                menu.add(Menu.FIRST, MENU_COPY, 2, "copy_text");
                menu.add(Menu.FIRST, MENU_CLEAN, 2, "clear");
            }

            @Override
            public void onContextItemSelected(int action) {
                switch (action) {
                    case MENU_WRITE:
                        list.restore();
                        write("");
                        break;

                    case MENU_REPLY:
                        String to = "";
                        int cur = list.getCurrItem();
                        if (cur < ids.size()) {
                            to = (String)ids.elementAt(cur);
                        }
                        write(to);
                        break;

                    case MENU_COPY:
                        //list.getController().copy(false);
                        list.restore();
                        break;

                    case MENU_CLEAN:
                        synchronized (this) {
                            emails.removeAllElements();
                            ids.removeAllElements();
                            model.clear();
                            list.updateModel();
                        }
                        list.restore();
                        break;

                    case MENU_USER_MENU:
                        try {
                            int item = list.getCurrItem();
                            String uin = (String)emails.elementAt(item);
                            //list.showMenu(ContactList.getInstance().getContextMenu(mrim,
                            //        mrim.createTempContact(uin)));
                        } catch (Exception e) {
                        }
                        break;
                }
            }
        });*/
    }

    public void activate() {
        list.show();
    }


    public Icon getIcon() {
        return hasNewMessage ? Message.msgIcons.iconAt(Message.ICON_IN_MSG_HI) : null;
    }

    private void removeOldRecords() {
        final int maxRecordCount = 50;
        /*while (maxRecordCount < model.getSize()) {
            ids.removeElementAt(0);
            emails.removeElementAt(0);
            list.removeFirstText();
        }*/
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
        par.addDescription(nick, Scheme.THEME_MAGIC_EYE_USER, Scheme.FONT_STYLE_PLAIN);
        if (reply) {
            par.addDescription(" (reply)", Scheme.THEME_MAGIC_EYE_USER, Scheme.FONT_STYLE_PLAIN);
        }
        par.addDescription(" " + date + ":", Scheme.THEME_MAGIC_EYE_NUMBER, Scheme.FONT_STYLE_PLAIN);
    //    par.addTextWithSmiles(post, Scheme.THEME_MAGIC_EYE_TEXT, Scheme.FONT_STYLE_PLAIN);

        model.addPar(par);
        removeOldRecords();
        //if (this != Sawim.getSawim().getDisplay().getCurrentDisplay()) {
        //    hasNewMessage = true;
        //}
        list.updateModel();
        return true;
    }

    private static final int MENU_WRITE     = 0;
    private static final int MENU_REPLY     = 1;
    private static final int MENU_COPY      = 2;
    private static final int MENU_CLEAN     = 3;
    private static final int MENU_USER_MENU = 4;

    //private TextBoxView postEditor;
    private String replayTo = "";

    private void write(String to) {
        replayTo = StringConvertor.notNull(to);
        //postEditor = new TextBoxView().create(StringConvertor.isEmpty(replayTo)
        //        ? "message" : "reply", 250);
        //postEditor.setTextBoxListener(this);
        //postEditor.show();
    }

    /*public void textboxAction(TextBoxView box, boolean ok) {
        MrimConnection c = mrim.getConnection();
        if (ok && mrim.isConnected() && (null != c)) {
            String text = postEditor.getString();
            if (!StringConvertor.isEmpty(text)) {
                c.postToMicroBlog(text, replayTo);
                list.setAllToBottom();
            }
            list.restore();
        }
    }*/
}



