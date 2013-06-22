


package sawim;

import sawim.chat.ChatHistory;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.*;
import sawim.search.Search;
import sawim.ui.base.Scheme;
import sawim.util.JLocale;
import ru.sawim.activities.SawimActivity;

import javax.microedition.io.ConnectionNotFoundException;
import java.io.InputStream;

public class Sawim {
    public static final String NAME = "Sawim NE";
    public static final String VERSION = "1.0m";

    public static final String microeditionPlatform = getPhone();
    private boolean paused = true;

    private static Sawim instance = null;
    public static Sawim getSawim() {
        return instance;
    }

    private static String getPhone() {
        String dev = android.os.Build.MODEL
                + "/" + android.os.Build.VERSION.RELEASE;
        return "android/" + dev;
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }
    
    private static void platformRequestUrl(String url) throws ConnectionNotFoundException {
        if (-1 == url.indexOf(':')) {
            url = "xmpp:" + url;
        }
        if (url.startsWith("xmpp:")) {
            Search search = ContactList.getInstance().getManager().getCurrentProtocol().getSearchForm();
            search.show(Util.getUrlWithoutProtocol(url));
            return;
        }
    }

    public static java.io.InputStream getResourceAsStream(String name) {
        InputStream in;
        in = sawim.modules.fs.FileSystem.openSawimFile(name);
        if (null == in) {
            try {
                in = SawimActivity.getInstance().getAssets().open(name.substring(1));
            } catch (Exception ignored) {
            }
        }
        return in;
    }

    private void backgroundLoading() {
        Notify.getSound().initSounds();
        Sawim.gc();
        Emotions.instance.load();
        StringConvertor.load();
		Answerer.getInstance().load();
        Sawim.gc();
        DebugLog.startTests();
    }

    private void initBasic() {
        ru.sawim.config.HomeDirectory.init();
        JLocale.loadLanguageList();
        Scheme.load();
        Options.loadOptions();
        new ru.sawim.config.Options().load();
        JLocale.setCurrUiLanguage(Options.getString(Options.OPTION_UI_LANGUAGE));
        Scheme.setColorScheme(Options.getInt(Options.OPTION_COLOR_SCHEME));
    }

    private void initialize() {
        backgroundLoading();
        Options.loadAccounts();
        ContactList.getInstance().initUI();
        ContactList.getInstance().initAccounts();
        ContactList.getInstance().loadAccounts();
        sawim.modules.tracking.Tracking.loadTrackingFromRMS();
    }

    public void startApp() {
        if (!paused && (null != Sawim.instance)) {
            return;
        }
        Sawim.instance = this;
        wakeUp();
        initBasic();
        try {
            initialize();
        } catch (Exception e) {
            DebugLog.panic("init", e);
            DebugLog.activate();
        }
    }

    public void hideApp() {
        paused = true;
        AutoAbsence.instance.away();
    }

    public void quit() {
        ContactList cl = ContactList.getInstance();
        boolean wait;
        try {
            wait = cl.disconnect();
        } catch (Exception e) {
            return;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e1) {
            
        }
        cl.safeSave();
        if (wait) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                
            }
        }
        ChatHistory.instance.saveUnreadMessages();
    }

    public static boolean isPaused() {
        return instance.paused;
    }

    public static void maximize() {
        AutoAbsence.instance.online();
        wakeUp();
        sawim.modules.AutoAbsence.instance.userActivity();
        sawim.modules.AutoAbsence.instance.updateTime();
    }

    public static void minimize() {
        instance.hideApp();
        sawim.modules.AutoAbsence.instance.userActivity();
        sawim.modules.AutoAbsence.instance.updateTime();
    }

    public static void wakeUp() {
        instance.paused = false;
    }

    public static void gc() {
        System.gc();
        try {
            Thread.sleep(50);
        } catch (Exception e) {
        }
    }
}