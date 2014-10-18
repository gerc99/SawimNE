package ru.sawim.modules;

import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.*;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;

import java.util.List;
import java.util.TimeZone;

public final class DebugLog {
    public static final DebugLog instance = new DebugLog();
    private static final int MENU_COPY = 0;
    private static final int MENU_COPY_ALL = 1;
    private static final int MENU_CLEAN = 2;
    private static long profilerTime;
    private VirtualListModel model = new VirtualListModel();
    private VirtualList list = null;

    public static void memoryUsage(String str) {
        long size = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        size = (size + 512) / 1024;
        println(str + " = " + size + "kb.");
    }

    public static void systemPrintln(String text) {
        System.out.println(text);
    }

    public static void println(String text) {
        System.out.println(text);
        //instance.print(text);
    }

    public static void panic(String str) {
        try {
            throw new Exception();
        } catch (Exception e) {
            panic(str, e);
        }
    }

    public static void assert0(String str, String result, String heed) {
        assert0(str, (result != heed) && !result.equals(heed));
    }

    public static void assert0(String str, boolean result) {
        if (result) {
            try {
                throw new Exception();
            } catch (Exception e) {
                println("assert: " + str);
                e.printStackTrace();
            }
        }
    }

    public static void panic(String str, Throwable e) {
        Log.e("DebugLog", str, e);
        e.printStackTrace();
    }

    public static void panic(Throwable e) {
        Log.e("DebugLog", "panic", e);
        //ExceptionHandler.reportOnlyHandler(SawimApplication.getInstance().getApplicationContext()).uncaughtException(null, e);
    }

    public static long profilerStart() {
        profilerTime = System.currentTimeMillis();
        return profilerTime;
    }

    public static long profilerStep(String str, long startTime) {
        long now = System.currentTimeMillis();
        println("profiler: " + str + ": " + (now - startTime));
        return now;
    }

    public static void profilerStep(String str) {
        long now = System.currentTimeMillis();
        println("profiler: " + str + ": " + (now - profilerTime));
        profilerTime = now;
    }

    public static void startTests() {
        //println("1329958015 " + Util.createGmtTime(2012, 02, 23, 4, 46, 55));

        println("TimeZone info");
        java.util.TimeZone tz = java.util.TimeZone.getDefault();
        println("TimeZone offset: " + tz.getRawOffset());
        println("Daylight: " + tz.useDaylightTime());
        println("ID: " + tz.getID());

        int t2 = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000 * 60 * 60);
        println("GMT " + t2);
    }

    public static void dump(String comment, byte[] data) {
        StringBuilder sb = new StringBuilder();
        sb.append("dump: ").append(comment).append(":\n");
        for (int i = 0; i < data.length; ++i) {
            String hex = Integer.toHexString(((int) data[i]) & 0xFF);
            if (1 == hex.length()) sb.append(0);
            sb.append(hex);
            sb.append(" ");
            if (i % 16 == 15) sb.append("\n");
        }
        println(sb.toString());
    }

    private void init() {
        list = VirtualList.getInstance();
        list.setCaption(JLocale.getString(R.string.debug_log));
        list.setModel(model);
        list.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, MENU_COPY, 2, JLocale.getString(R.string.copy_text));
                menu.add(Menu.FIRST, MENU_COPY_ALL, 2, JLocale.getString(R.string.copy_all_text));
            }

            @Override
            public void onContextItemSelected(BaseActivity activity, int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case MENU_COPY:
                        VirtualListItem item = list.getModel().elements.get(listItem);
                        Clipboard.setClipBoardText(activity, ((item.getLabel() == null) ? "" : item.getLabel() + "\n") + item.getDescStr());
                        break;

                    case MENU_COPY_ALL:
                        StringBuilder s = new StringBuilder();
                        List<VirtualListItem> listItems = list.getModel().elements;
                        for (int i = 0; i < listItems.size(); ++i) {
                            CharSequence label = listItems.get(i).getLabel();
                            CharSequence descStr = listItems.get(i).getDescStr();
                            if (label != null)
                                s.append(label).append("\n");
                            if (descStr != null)
                                s.append(descStr).append("\n");
                        }
                        Clipboard.setClipBoardText(activity, s.toString());
                        break;
                }
            }
        });
        list.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                menu.add(Menu.FIRST, MENU_CLEAN, 2, JLocale.getString(R.string.clear));
            }

            @Override
            public void onOptionsItemSelected(BaseActivity activity, MenuItem item) {
                switch (item.getItemId()) {
                    case MENU_CLEAN:
                        model.clear();
                        startTests();
                        list.updateModel();
                        break;
                }
            }
        });
    }

    public void activate(BaseActivity activity) {
        init();
        list.show(activity);
    }

    private void removeOldRecords() {
        final int maxRecordCount = 50;
        while (maxRecordCount < model.getSize()) {
            model.removeFirstText();
        }
    }

    private synchronized void print(String text) {
        VirtualListItem record = model.createNewParser(true);
        String date = Util.getLocalDateString(SawimApplication.getCurrentGmtTime(), true);
        record.addLabel(date + ": ", Scheme.THEME_NUMBER,
                Scheme.FONT_STYLE_PLAIN);
        record.addDescription(text, Scheme.THEME_TEXT, Scheme.FONT_STYLE_PLAIN);

        model.addPar(record);
        removeOldRecords();
    }

    private long freeMemory() {
        for (int i = 0; i < 10; ++i) {
            SawimApplication.gc();
        }
        return Runtime.getRuntime().freeMemory();
    }
}