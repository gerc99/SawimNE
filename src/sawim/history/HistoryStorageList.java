package sawim.history;

import sawim.ui.text.TextList;
import sawim.ui.text.TextListModel;
import java.util.*;
import javax.microedition.rms.*;
import sawim.*;
import sawim.cl.*;
import sawim.ui.base.*;
import sawim.util.JLocale;
import sawim.comm.*;
//import sawim.ui.text.TextListController;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;


public final class HistoryStorageList extends TextList implements Runnable, FormListener {

    private HistoryStorage history;
    private Forms frmFind;
    private static final int tfldFind = 1000;
    private static final int find_backwards = 1010;
    private static final int find_case_sensitiv = 1011;
    private static final int NOT_FOUND = 1;

    private static final int CACHE_SIZE = 50;
    private Hashtable cachedRecords = new Hashtable();
    private Thread searching = null;

    //private MenuModel msgMenu = new MenuModel();
    private TextList msg = TextList.getInstance();
    private HistoryExport export = null;

    public HistoryStorageList(HistoryStorage storage) {
        setCaption(JLocale.getString("history"));
        history = storage;
        history.openHistory();

        int size = getSize();
        if (0 != size) {
            //setCurrentItemIndex(size - 1);
        }
    }

    void closeHistoryView() {
        clearCache();
        history.closeHistoryView();
        searching = null;
    }

    public int getHistorySize() {
        return history.getHistorySize();
    }
    private CachedRecord getCachedRecord(int num) {
        Integer key = new Integer(num);
        CachedRecord cachedRec = (CachedRecord)cachedRecords.get(key);
        if (null == cachedRec) {
            trimCache();
            cachedRec = history.getRecord(num);
            if (null != cachedRec) {
                cachedRecords.put(key, cachedRec);
            }
        }
        return cachedRec;
    }
    private void trimCache() {
        if (cachedRecords.size() > CACHE_SIZE) {
            cachedRecords.clear();
        }
    }
    
    private void clearCache() {
        cachedRecords.clear();
        Sawim.gc();
    }

    protected void onCursorMove() {
        CachedRecord record = getCachedRecord(getCurrItem());
        if (null != record) {
            setCaption(record.from + " " + record.date);
        }
    }
    protected void select(int action) {
        /*switch (action) {
            case NativeCanvas.Sawim_SELECT:
                showMessText().show();
                return;

            case NativeCanvas.Sawim_BACK:
                back();
                closeHistoryView();
                return;

            case NativeCanvas.Sawim_MENU:
                showMenu(getMenu());
                return;
        }*/
        switch (action) {
            case MENU_GOTO_URL:
                //ContactList.getInstance().gotoUrl(getCachedRecord(getCurrItem()).text);
                break;

            case MENU_SELECT:
                showMessText().show();
                break;

            case MENU_FIND:
                if (null == frmFind) {
                    frmFind = Forms.getInstance();
                    frmFind.init("find", this);
                    frmFind.addTextField(tfldFind, "text_to_find", "");
                    frmFind.addCheckBox(find_backwards, "find_backwards", true);
                    frmFind.addCheckBox(find_case_sensitiv, "find_case_sensitiv", false);
                }
                frmFind.remove(NOT_FOUND);
                frmFind.show();
                break;

            case MENU_CLEAR:
                /*MenuModel menu = new MenuModel();
                menu.addItem("currect_contact",         MENU_DEL_CURRENT);
                menu.addItem("all_contact_except_this", MENU_DEL_ALL_EXCEPT_CUR);
                menu.addItem("all_contacts",            MENU_DEL_ALL);
                menu.setActionListener(new Binder(this));
                showMenu(menu);*/
                break;

            case MENU_DEL_CURRENT:
                history.removeHistory();
                clearCache();
                restore();
                break;

            case MENU_DEL_ALL_EXCEPT_CUR:
                history.clearAll(true);
                restore();
                break;

            case MENU_DEL_ALL:
                history.clearAll(false);
                clearCache();
                restore();
                break;

            case MENU_COPY_TEXT:
                int index = getCurrItem();
                if (-1 == index) return;
                CachedRecord record = getCachedRecord(index);
                if (null == record) return;
                SawimUI.setClipBoardText((record.type == 0),
                        record.from, record.date, record.text);
                restore();
                break;

            case MENU_INFO:
                RecordStore rs = history.getRS();
                try {
                    String sb = JLocale.getString("hist_cur") + ": " + getSize()  + "\n"
                            + JLocale.getString("hist_size") + ": " + (rs.getSize() / 1024) + "\n"
                            + JLocale.getString("hist_avail") + ": " + (rs.getSizeAvailable() / 1024) + "\n";
                    //new Popup(this, sb).show();
                } catch (Exception ignored) {
                }
                break;

            
            case MENU_EXPORT:
                export = new HistoryExport(this);
                export.export(history);
                break;

            case MENU_EXPORT_ALL:
                export = new HistoryExport(this);
                export.export(null);
                break;
            
        }
    }

    public void onContentMove(TextListModel sender, int direction) {
        moveInList(direction);
    }

    private static final int MENU_SELECT     = 0;
    private static final int MENU_FIND       = 1;
    private static final int MENU_CLEAR      = 2;
    private static final int MENU_DEL_CURRENT        = 40;
    private static final int MENU_DEL_ALL_EXCEPT_CUR = 41;
    private static final int MENU_DEL_ALL            = 42;
    private static final int MENU_COPY_TEXT  = 3;
    private static final int MENU_INFO       = 4;
    private static final int MENU_EXPORT     = 5;
    private static final int MENU_EXPORT_ALL = 6;
    private static final int MENU_GOTO_URL   = 7;

    /*protected MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        if (getSize() > 0) {
            menu.addItem("onContextItemSelected",       MENU_SELECT);
            menu.addEllipsisItem("find",  MENU_FIND);
            menu.addEllipsisItem("clear", MENU_CLEAR);
            menu.addItem("copy_text",    MENU_COPY_TEXT);
            menu.addItem("history_info", MENU_INFO);
            
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.addItem("export",       MENU_EXPORT);
                
            }
            
        }
        menu.setActionListener(new Binder(this));
        return menu;
    }*/

    
    private void moveInList(int offset) {
        //setCurrentItemIndex(getCurrItem() + offset);
        showMessText().restore();
    }
    public void run() {
        Thread it = Thread.currentThread();
        searching = it;
        
        String text = frmFind.getTextFieldValue(tfldFind);
        int textIndex = find(text, getCurrItem(),
                frmFind.getCheckBoxValue(find_case_sensitiv),
                frmFind.getCheckBoxValue(find_backwards));

        if (0 <= textIndex) {
            //setCurrentItemIndex(textIndex);
            restore();

        } else if (searching == it) {
            frmFind.addString(NOT_FOUND, text + "\n" + JLocale.getString("not_found"));
        }
    }

    private int find(String text, int fromIndex, boolean caseSens, boolean back) {
        Thread it = Thread.currentThread();
        int size = history.getHistorySize();
        if ((fromIndex < 0) || (fromIndex >= size)) return -1;
        if (!caseSens) text = StringConvertor.toLowerCase(text);

        int step = back ? -1 : +1;
        int updater = 100;
        for (int index = fromIndex; ; index += step) {
            if ((index < 0) || (index >= size)) break;
            CachedRecord record = history.getRecord(index);
            String searchText = caseSens
                    ? record.text
                    : StringConvertor.toLowerCase(record.text);
            if (searchText.indexOf(text) != -1) {
                return index;
            }

            if (0 != updater) {
                updater--;
            } else {
                updater = 100;
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
                if (it != searching) {
                    return -1;
                }
            }
        }
        return -1;
    }

    public void formAction(Forms form, boolean apply) {
        if (apply) {
            frmFind.remove(NOT_FOUND);
            new Thread(this).start();

        } else {
            searching = null;
            restore();
        }
    }

    
    private TextList showMessText() {
        if (getCurrItem() >= getSize()) return null;
        CachedRecord record = history.getRecord(getCurrItem());
        msg.setCaption(record.from);

    /*    msgMenu.clean();
        msgMenu.addItem("copy_text", MENU_COPY_TEXT);
        if (record.containsUrl()) {
            msgMenu.addItem("goto_url",  MENU_GOTO_URL);
        }
        msgMenu.setActionListener(new Binder(this));


        msg.lock();
        msg.setAllToTop();
        TextListModel msgText = new TextListModel();
        VirtualListItem parser = msgText.createNewParser(false);
        parser.addDescription(record.date + ":", THEME_TEXT, FONT_STYLE_BOLD);
        parser.doCRLF();
        parser.addTextWithSmiles(record.text, THEME_TEXT, FONT_STYLE_PLAIN);
        msgText.addPar(parser);

        msg.setController(new TextListController(msgMenu, -1));
        msg.setModel(msgText);*/
        return msg;
    }

    
    protected final int getSize() {
        return getHistorySize();
    }

    /*protected void paint(GraphicsEx g) {
        super.paint(g);
        
        HistoryExport he = export;
        if (null != he) {
            int progressHeight = getDefaultFont().getHeight();
            int y = (getClientHeight() - 2 * progressHeight) / 2;
            int w = getWidth();
            g.setClip(0, 0, getWidth(), getHeight());
            g.setThemeColor(Scheme.THEME_BACKGROUND);
            g.fillRect(0, y - 1, w, progressHeight * 2 + 1);
            g.setThemeColor(Scheme.THEME_TEXT);
            g.drawRect(0, y - 1, w, progressHeight * 2 + 1);
            g.setFont(getDefaultFont());
            g.drawString(he.contact, 2, y, w - 4, progressHeight);
            g.fillRect(0, y + progressHeight, w * he.currentMessage / he.messageCount, progressHeight);
        }
        
    }
    protected void drawItemData(GraphicsEx g, int index, int x1, int y1, int w, int h, int skip, int to) {
        CachedRecord record = getCachedRecord(index);
        if ((null == record) || (null == record.getShortText())) return;
        Font font = getDefaultFont();
        g.setFont(font);
        g.setThemeColor((record.type == 0) ? THEME_CHAT_INMSG : THEME_CHAT_OUTMSG);
        g.drawString(record.getShortText(), x1, y1 + (h - font.getHeight()) / 2,
                Graphics.TOP | Graphics.LEFT);
    }*/

    void exportDone() {
        export = null;
        //invalidate();
    }
    
}


