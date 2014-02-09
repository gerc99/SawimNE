package sawim.history;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.models.form.FormListener;
import ru.sawim.models.form.Forms;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;
import sawim.Clipboard;
import sawim.comm.StringConvertor;
import sawim.util.JLocale;

import javax.microedition.rms.RecordStore;
import java.util.Hashtable;


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

    private VirtualList allMsg;
    private VirtualListModel listMessages = new VirtualListModel();
    private HistoryExport export = null;

    public void show(HistoryStorage storage) {
        allMsg = VirtualList.getInstance();
        allMsg.setCaption(JLocale.getString(R.string.history));
        allMsg.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                if (getSize() > 0) {
                    menu.add(Menu.FIRST, MENU_COPY_TEXT, 2, JLocale.getString(R.string.copy_text));
                }
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                select(itemMenuId, listItem);
            }
        });
        allMsg.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, MENU_FIND, 2, JLocale.getString(R.string.find));
                menu.add(Menu.FIRST, MENU_CLEAR, 2, JLocale.getString(R.string.clear));
                menu.add(Menu.FIRST, MENU_INFO, 2, JLocale.getString(R.string.history_info));

                if (sawim.modules.fs.FileSystem.isSupported()) {
                    menu.add(Menu.FIRST, MENU_EXPORT, 2, JLocale.getString(R.string.export));
                }
            }

            @Override
            public void onOptionsItemSelected(MenuItem item) {
                select(item.getItemId(), 0);
            }
        });
        allMsg.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {
            }

            @Override
            public boolean back() {
                closeHistoryView();
                allMsg.updateModel();
                return true;
            }
        });
        history = storage;
        history.openHistory();

        int size = getSize();
        if (0 != size && !history.getContact().isConference()) {
            allMsg.setCurrentItemIndex(size - 1, false);
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
        Integer key = num;
        CachedRecord cachedRec = (CachedRecord) cachedRecords.get(key);
        if (null == cachedRec) {
            //trimCache();
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
        SawimApplication.gc();
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
                    frmFind = new Forms(R.string.find, this, true);
                    frmFind.addTextField(tfldFind, R.string.text_to_find, "");
                    frmFind.addCheckBox(find_backwards, R.string.find_backwards, true);
                    frmFind.addCheckBox(find_case_sensitiv, R.string.find_case_sensitiv, false);
                }
                frmFind.remove(NOT_FOUND);
                frmFind.show();
                break;

            case MENU_CLEAR:
                CharSequence[] items = new CharSequence[3];
                items[0] = JLocale.getString(R.string.currect_contact);
                items[1] = JLocale.getString(R.string.all_contact_except_this);
                items[2] = JLocale.getString(R.string.clear_all_contacts);
                AlertDialog.Builder builder = new AlertDialog.Builder(SawimApplication.getCurrentActivity());
                builder.setCancelable(true);
                builder.setTitle(JLocale.getString(R.string.history));
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                history.removeHistory();
                                clearCache();
                                allMsg.back();
                                break;
                            case 1:
                                history.clearAll(true);
                                break;
                            case 2:
                                history.clearAll(false);
                                clearCache();
                                allMsg.back();
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
                Clipboard.setClipBoardText(record.text);
                break;

            case MENU_INFO:
                RecordStore rs = history.getRS();
                if (rs == null) break;
                try {
                    String sb = JLocale.getString(R.string.hist_cur) + ": " + getSize() + "\n"
                            + JLocale.getString(R.string.hist_size) + ": " + (rs.getSize() / 1024) + "\n"
                            + JLocale.getString(R.string.hist_avail) + ": " + (rs.getSizeAvailable() / 1024) + "\n";
                    Toast.makeText(SawimApplication.getCurrentActivity(), sb, Toast.LENGTH_SHORT).show();
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

    private static final int MENU_FIND = 1;
    private static final int MENU_CLEAR = 2;
    private static final int MENU_COPY_TEXT = 3;
    private static final int MENU_INFO = 4;
    private static final int MENU_EXPORT = 5;
    private static final int MENU_EXPORT_ALL = 6;


    private void moveInList(int offset) {
        allMsg.setCurrentItemIndex(allMsg.getCurrItem() + offset, true);
    }

    public void run() {
        Thread it = Thread.currentThread();
        searching = it;

        String text = frmFind.getTextFieldValue(tfldFind);
        int textIndex = find(text, allMsg.getCurrItem(),
                frmFind.getCheckBoxValue(find_case_sensitiv),
                frmFind.getCheckBoxValue(find_backwards));

        if (0 <= textIndex) {
            allMsg.setCurrentItemIndex(textIndex, true);
            onCursorMove(textIndex);

        } else if (searching == it) {
            frmFind.addString_(NOT_FOUND, text + "\n" + JLocale.getString(R.string.not_found));
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
            form.back();
        } else {
            searching = null;
        }
    }

    private final int getSize() {
        return getHistorySize();
    }

    private void buildListMessages() {
        for (int i = 0; i < getSize(); ++i) {
            CachedRecord record = getCachedRecord(i);
            if ((null == record) || (null == record.text)) return;

            VirtualListItem parser = listMessages.createNewParser(false);
            String from = history.getContact().isConference() ? record.from + " " : "";
            parser.addLabel(from + record.date + ":", Scheme.THEME_TEXT, Scheme.FONT_STYLE_BOLD);
            parser.addDescription(record.text, (record.type == 0) ? Scheme.THEME_CHAT_INMSG
                    : Scheme.THEME_CHAT_OUTMSG, Scheme.FONT_STYLE_PLAIN);
            listMessages.addPar(parser);
            allMsg.setModel(listMessages);
            allMsg.updateModel();
        }
    }

    void exportDone() {
        export = null;
        //invalidate();
    }
}