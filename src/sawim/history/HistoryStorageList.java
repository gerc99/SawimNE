package sawim.history;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.widget.Toast;
import protocol.jabber.Jabber;
import ru.sawim.activities.SawimActivity;
import ru.sawim.activities.VirtualListActivity;
import ru.sawim.models.form.VirtualListItem;
import sawim.ui.base.Scheme;
import sawim.ui.text.VirtualList;
import sawim.ui.text.VirtualListModel;
import java.util.*;
import javax.microedition.rms.*;
import sawim.*;
import sawim.util.JLocale;
import sawim.comm.*;
//import sawim.ui.text.TextListController;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;


public final class HistoryStorageList implements Runnable, FormListener {

    private HistoryStorage history;
    private Forms frmFind;
    private static final int tfldFind = 1000;
    private static final int find_backwards = 1010;
    private static final int find_case_sensitiv = 1011;
    private static final int NOT_FOUND = 1;

    private static final int CACHE_SIZE = 50;
    private Hashtable cachedRecords = new Hashtable();
    private Thread searching = null;

    private VirtualList currMsg = VirtualList.getInstance();
    private VirtualList allMsg = VirtualList.getInstance();
    private HistoryExport export = null;

    public void show(HistoryStorage storage) {
        allMsg.setCaption(JLocale.getString("history"));
        allMsg.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                if (getSize() > 0) {
                    menu.add(Menu.FIRST, MENU_FIND, 2, JLocale.getString("find"));
                    menu.add(Menu.FIRST, MENU_CLEAR, 2, JLocale.getString("clear"));
                    menu.add(Menu.FIRST, MENU_COPY_TEXT, 2, JLocale.getString("copy_text"));
                    menu.add(Menu.FIRST, MENU_INFO, 2, JLocale.getString("history_info"));

                    if (sawim.modules.fs.FileSystem.isSupported()) {
                        menu.add(Menu.FIRST, MENU_EXPORT, 2, JLocale.getString("export"));
                    }
                }
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                select(itemMenuId, listItem);
            }
        });
        allMsg.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
                showMessText(position).show();
            }

            @Override
            public boolean back() {
                closeHistoryView();
                return true;
            }
        });
        history = storage;
        history.openHistory();

        int size = getSize();
        if (0 != size) {
            allMsg.setCurrentItemIndex(size - 1);
            onCursorMove(size - 1);
        }
        buildListMessages();
        allMsg.show();
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

    protected void onCursorMove(int index) {
        CachedRecord record = getCachedRecord(index);
        if (null != record) {
            allMsg.setCaption(record.from + " " + record.date);
        }
    }

    private void select(int action, int currItem) {
        switch (action) {
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
                CharSequence[] items = new CharSequence[3];
                items[0] = JLocale.getString("currect_contact");
                items[1] = JLocale.getString("all_contact_except_this");
                items[2] = JLocale.getString("all_contacts");
                AlertDialog.Builder builder = new AlertDialog.Builder(SawimActivity.getInstance());
                builder.setTitle(JLocale.getString("history"));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                history.removeHistory();
                                clearCache();
                                break;
                            case 1:
                                history.clearAll(true);
                                break;
                            case 2:
                                history.clearAll(false);
                                clearCache();
                                break;
                        }
                    }
                });
                builder.create().show();
                break;

            case MENU_COPY_TEXT:
                int index = currItem;
                if (-1 == index) return;
                CachedRecord record = getCachedRecord(index);
                if (null == record) return;
                SawimUI.setClipBoardText((record.type == 0),
                        record.from, record.date, record.text);
                break;

            case MENU_INFO:
                RecordStore rs = history.getRS();
                try {
                    String sb = JLocale.getString("hist_cur") + ": " + getSize()  + "\n"
                            + JLocale.getString("hist_size") + ": " + (rs.getSize() / 1024) + "\n"
                            + JLocale.getString("hist_avail") + ": " + (rs.getSizeAvailable() / 1024) + "\n";
                    Toast.makeText(VirtualListActivity.getInstance(), sb, Toast.LENGTH_SHORT);
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

    public void onContentMove(VirtualListModel sender, int direction) {
        moveInList(direction);
    }

    private static final int MENU_FIND       = 1;
    private static final int MENU_CLEAR      = 2;
    private static final int MENU_COPY_TEXT  = 3;
    private static final int MENU_INFO       = 4;
    private static final int MENU_EXPORT     = 5;
    private static final int MENU_EXPORT_ALL = 6;

    
    private void moveInList(int offset) {
        allMsg.setCurrentItemIndex(allMsg.getCurrItem() + offset);
    }

    public void run() {
        Thread it = Thread.currentThread();
        searching = it;
        
        String text = frmFind.getTextFieldValue(tfldFind);
        int textIndex = find(text, allMsg.getCurrItem(),
                frmFind.getCheckBoxValue(find_case_sensitiv),
                frmFind.getCheckBoxValue(find_backwards));

        if (0 <= textIndex) {
            allMsg.setCurrentItemIndex(textIndex);
            onCursorMove(textIndex);

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
        }
    }
    
    private VirtualList showMessText(int currItem) {
        if (currItem >= getSize()) return null;
        CachedRecord record = history.getRecord(currItem);
        currMsg.setCaption(record.from);

        currMsg.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, MENU_COPY_TEXT, 2, JLocale.getString("copy_text"));
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                select(MENU_COPY_TEXT, listItem);
            }
        });

        VirtualListModel msgText = new VirtualListModel();
        VirtualListItem parser = msgText.createNewParser(false);
        parser.addDescription(record.date + ":", Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
        parser.addTextWithSmiles(record.text, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);
        msgText.addPar(parser);

        currMsg.setModel(msgText);
        return currMsg;
    }

    private final int getSize() {
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
    }*/

    private void buildListMessages() {
        for (int i = 0; i < getSize(); ++i) {
            CachedRecord record = getCachedRecord(i);
            if ((null == record) || (null == record.getShortText())) return;

            VirtualListModel msgText = new VirtualListModel();
            VirtualListItem parser = msgText.createNewParser(false);
            parser.addLabel(record.date + ":", Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
            parser.addTextWithSmiles(record.getShortText(), (record.type == 0) ? Scheme.THEME_CHAT_INMSG
                    : Scheme.THEME_CHAT_OUTMSG, Scheme.FONT_STYLE_PLAIN);
            msgText.addPar(parser);

            allMsg.setModel(msgText);
        }
    }

    void exportDone() {
        export = null;
        //invalidate();
    }
    
}


