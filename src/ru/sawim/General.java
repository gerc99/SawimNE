package ru.sawim;

import android.content.Context;
import android.content.res.Resources;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.WindowManager;
import sawim.Options;
import sawim.Updater;
import sawim.chat.ChatHistory;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.*;
import sawim.roster.RosterHelper;
import sawim.search.Search;
import sawim.util.JLocale;

import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 24.05.13
 * Time: 18:52
 * To change this template use File | Settings | File Templates.
 */
public class General {

    public static final String NAME = SawimApplication.getInstance().getString(R.string.app_name);
    public static final String VERSION = SawimApplication.getInstance().getVersion();
    public static final String PHONE = "Android/" + android.os.Build.MODEL
            + "/" + android.os.Build.VERSION.RELEASE;
    public static final String DEFAULT_SERVER = "jabber.ru";

    private static General instance;
    public static boolean returnFromAcc = false;
    public static ActionBarActivity currentActivity;
    public static ActionBar actionBar;
    private static Resources resources;
    private boolean paused = true;
    private static int fontSize;
    public static boolean showStatusLine;
    public static int sortType;
    private float displayDensity;

    public static General getInstance() {
        return instance;
    }

    public void startApp() {
        instance = this;
        instance.paused = false;
        ru.sawim.config.HomeDirectory.init();
        Options.loadOptions();
        new ru.sawim.config.Options().load();
        JLocale.loadLanguageList();
        Scheme.load();
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
        updateOptions();
        Updater.startUIUpdater();
        try {
            Notify.getSound().initSounds();
            gc();
            Emotions.instance.load();
            StringConvertor.load();
            Answerer.getInstance().load();
            gc();

            Options.loadAccounts();
            RosterHelper.getInstance().initAccounts();
            RosterHelper.getInstance().loadAccounts();
            sawim.modules.tracking.Tracking.loadTrackingFromRMS();
        } catch (Exception e) {
            DebugLog.panic("init", e);
            DebugLog.instance.activate();
        }
        DebugLog.startTests();
        displayDensity = General.getResources(SawimApplication.getContext()).getDisplayMetrics().density;
    }

    public static void updateOptions() {
        SawimResources.initIcons();
        fontSize = Options.getInt(Options.OPTION_FONT_SCHEME);
        showStatusLine = Options.getBoolean(Options.OPTION_SHOW_STATUS_LINE);
        sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
    }

    public static boolean isManyPane() {
        int rotation = ((WindowManager) SawimApplication.getContext()
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        if (rotation == 0 && isTablet())
            return false;
        return isTablet();
    }

    public static boolean isTablet() {
        return getResources(SawimApplication.getContext()).getBoolean(R.bool.is_tablet);
    }

    public void quit(boolean isForceClose) {
        RosterHelper cl = RosterHelper.getInstance();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
        }
        cl.safeSave();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e1) {
        }
        ChatHistory.instance.saveUnreadMessages();
        AutoAbsence.getInstance().online();
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }

    public static void openUrl(String url) {
        Search search = RosterHelper.getInstance().getCurrentProtocol().getSearchForm();
        search.show(Util.getUrlWithoutProtocol(url), true);
    }

    public static java.io.InputStream getResourceAsStream(String name) {
        InputStream in;
        in = sawim.modules.fs.FileSystem.openSawimFile(name);
        if (null == in) {
            try {
                in = SawimApplication.getInstance().getAssets().open(name.substring(1));
            } catch (Exception ignored) {
            }
        }
        return in;
    }

    public static boolean isPaused() {
        return instance.paused;
    }

    public static void maximize() {
        AutoAbsence.getInstance().online();
        instance.paused = false;
    }

    public static void minimize() {
        sawim.modules.AutoAbsence.getInstance().userActivity();
        instance.paused = true;
    }

    public static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
    }

    public static int getFontSize() {
        return fontSize;
    }

    public static Resources getResources(Context c) {
        if (resources == null) {
            resources = (c == null) ? Resources.getSystem() : c.getResources();
        }
        return resources;
    }

    private static OnConfigurationChanged configurationChanged;

    public OnConfigurationChanged getConfigurationChanged() {
        return configurationChanged;
    }

    public void setConfigurationChanged(OnConfigurationChanged cC) {
        if (configurationChanged == null)
            configurationChanged = cC;
    }

    public float getDisplayDensity() {
        return displayDensity;
    }

    public interface OnConfigurationChanged {
        public void onConfigurationChanged();
    }
}
