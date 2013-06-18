


package sawim;

import DrawControls.icons.Icon;
import sawim.chat.ChatHistory;
import sawim.cl.ContactList;
import sawim.comm.Config;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.modules.*;
import sawim.search.Search;
import sawim.ui.base.Scheme;
import sawim.util.JLocale;
import org.microemu.MIDletBridge;
import ru.sawim.activities.SawimActivity;

import javax.microedition.io.ConnectionNotFoundException;
import java.io.InputStream;


public class Sawim implements Runnable {
    public static final String NAME = "Sawim NE";
    public static final String VERSION = "1.0m";
    public static String lastDate;
    //private Display display;

    private boolean locked = false;
    private long lastLockTime = 0;

    
    private static Sawim instance = null;
    public static Sawim getSawim() {
        return instance;
    }

    public static final String microeditionPlatform = getPhone();
    public static final String microeditionProfiles = getSystemProperty("microedition.profiles", null);
    public static final byte generalPhoneType = getGeneralPhone();

    private boolean paused = true;

    public static final byte PHONE_SE             = 0;
    public static final byte PHONE_SE_SYMBIAN     = 1;
    public static final byte PHONE_NOKIA          = 2;
    public static final byte PHONE_NOKIA_S40      = 3;
    public static final byte PHONE_NOKIA_S60      = 4;
    public static final byte PHONE_NOKIA_S60v8    = 5;
    public static final byte PHONE_NOKIA_N80      = 6;
    public static final byte PHONE_INTENT_JTE     = 7;
    public static final byte PHONE_JBED           = 8;
    public static final byte PHONE_SAMSUNG        = 9;
    public static final byte PHONE_ANDROID        = 10;
	public static final byte PHONE_ALCATEL        = 11;
	public static final byte PHONE_WTK        = 12;

    private static String getPhone() {
        final String platform = getSystemProperty("microedition.platform", null);
        
        if (null == platform) {
            try {
                Class.forName("com.nokia.mid.ui.DeviceControl");
                return "Nokia";
            } catch (Exception e) {
            }
        }
        
        
        String android = getSystemProperty("device.model", "")
                + "/" + getSystemProperty("device.software.version", "")
                + "/" +  getSystemProperty("device.id", "");
        if (2 < android.length()) {
            return "android/" + android;
        }
        
        return platform;
    }

    private static byte getGeneralPhone() {
        String device = getPhone();
        if (null == device) {
            return -1;
        }
        device = device.toLowerCase();
        
        
        if (-1 != device.indexOf("android")) {
            return PHONE_ANDROID;
        }
        
        if (device.indexOf("ericsson") != -1) {
            if ((-1 != getSystemProperty("com.sonyericsson.java.platform", "")
                        .toLowerCase().indexOf("sjp"))) {
                return PHONE_SE_SYMBIAN;
            }
            return PHONE_SE;
        }
        if (-1 != device.indexOf("platform=s60")) {
            return PHONE_NOKIA_S60;
        }
        if (device.indexOf("nokia") != -1) {
            if (device.indexOf("nokian80") != -1) {
                return PHONE_NOKIA_N80;
            }
            if (null != getSystemProperty("com.nokia.memoryramfree", null)) {
                
                return PHONE_NOKIA_S60;
            }
            String dir = getSystemProperty("fileconn.dir.private", "");
            
            if (-1 != dir.indexOf("/private/")) {
                
                return PHONE_NOKIA_S60;
            }
            if (-1 != device.indexOf(';')) {
                return PHONE_NOKIA_S60;
            }
            return PHONE_NOKIA_S40;
        }
        if (device.indexOf("samsung") != -1) {
            return PHONE_SAMSUNG;
        }
		if (device.indexOf("Alcatel-OT-806/1.0".toLowerCase()) != -1) {
		    return PHONE_ALCATEL;
        }
		if (device.indexOf("wtk") != -1) {
		    return PHONE_WTK;
        }
        if (device.indexOf("jbed") != -1) {
            return PHONE_JBED;
        }
        if (device.indexOf("intent") != -1) {
            return PHONE_INTENT_JTE;
        }
        
        return -1;
    }
    public static boolean isPhone(final byte phone) {
        
        if (PHONE_NOKIA_S60v8 == phone) {
            return (PHONE_NOKIA_S60 == generalPhoneType)
                    && (-1 == microeditionPlatform.indexOf(';'));
        }
        if (PHONE_NOKIA == phone) {
            return (PHONE_NOKIA_S40 == generalPhoneType)
                    || (PHONE_NOKIA_S60 == generalPhoneType)
                    || (PHONE_NOKIA_N80 == generalPhoneType);
        }
        if (PHONE_SE == phone) {
            return (PHONE_SE_SYMBIAN == generalPhoneType)
                    || (PHONE_SE == generalPhoneType);
        }
        
        return phone == generalPhoneType;
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000
                + Options.getInt(Options.OPTION_LOCAL_OFFSET) * 3600;
    }

    private int getSeVersion() {
        String sJava = getSystemProperty("com.sonyericsson.java.platform", "");
        
        
        
        if ((null != sJava) && sJava.startsWith("JP-")) {
            int major = 0;
            int minor = 0;
            int micro = 0;


            if (sJava.length() >= 4) {
                major = sJava.charAt(3) - '0';
            }
            if (sJava.length() >= 6) {
                minor = sJava.charAt(5) - '0';
            }
            if (sJava.length() >= 8) {
                micro = sJava.charAt(7) - '0';
            }


            if ((0 <= major) && (major <= 9)
                    && (0 <= minor) && (minor <= 9)
                    && (0 <= micro) && (micro <= 9)) {
                return major * 100 + minor * 10 + micro;
            }
        }
        return 0;
    }
    public static boolean hasMemory(int requared) {
        
        if (isPhone(PHONE_SE)) {
            return true;
        }
        if (isPhone(PHONE_NOKIA_S60)) {
            return true;
        }
        if (isPhone(PHONE_JBED)) {
            return true;
        }
        if (isPhone(PHONE_INTENT_JTE)) {
            return true;
        }
        
        if (isPhone(PHONE_ANDROID)) {
            return true;
        }
        
        
        Sawim.gc();
        long free = Runtime.getRuntime().freeMemory();
        return (requared < free);
    }

    public static String getAppProperty(String key, String defval) {
        String res = null;
        try {
            res = MIDletBridge.getAppProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }
    public static boolean isSetAppProperty(String key) {
        String res = getAppProperty(key, "");
        return "yes".equals(res) || "true".equals(res);
    }
    private static String getSystemProperty(String key, String defval) {
        String res = null;
        try {
            res = System.getProperty(key);
        } catch (Exception e) {
        }
        return StringConvertor.isEmpty(res) ? defval : res;
    }

    
    public static boolean isS60v5() {
        String platform = StringConvertor.notNull(Sawim.microeditionPlatform);
        return -1 != platform.indexOf("sw_platform_version=5.");
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
        
        /*if (url.equals("sawim:update")) {
            StringBuffer url_ = new StringBuffer();
            url_.append("http://sawim.net.ru/go.xhtml?act=update&lang=");
            url_.append(JLocale.getCurrUiLanguage());
            url_.append("&protocols=ICQ,MRIM,JABBER&cdata=");
            url_.append(Config.loadResource("build.dat"));
            url = url_.toString();
        }*/
    }
    public static void openUrl(String url) {
        try {
            platformRequestUrl(url);
        } catch (Exception e) {
            
        }
    }
    public static void platformRequestAndExit(String url) {
        try {
            platformRequestUrl(url);
        } catch (Exception e) {
        }
    }

    public static java.io.InputStream getResourceAsStream(String name) {
        InputStream in = null;
        
        in = sawim.modules.fs.FileSystem.openSawimFile(name);
        if (null == in) {
            try {
                in = SawimActivity.getInstance().getAssets().open(name.substring(1));
            } catch (Exception ignored) {
            }
        }
        return in;
    }

    public void run() {
        try {
            backgroundLoading();
        } catch (Exception ignored) {
        }
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

    private void restore(Object screen) {
        if (null == screen) {
            return;
        }
        AutoAbsence.instance.online();
        wakeUp();
    }

    public void startApp() {
        if (!paused && (null != Sawim.instance)) {
            return;
        }
        Sawim.instance = this;
        locked = false;
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
        locked = false;
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
        if (instance.paused) {
            return true;
        }
        return /*instance.display.isPaused()*/false;
    }

    public static boolean isLocked() {
        return instance.locked;
    }

    public static void lockSawim() {
        final long now = Sawim.getCurrentGmtTime();
        final int WAITING_INTERVAL = 3; 
        if (instance.lastLockTime + WAITING_INTERVAL  < now) {
            instance.locked = true;

            AutoAbsence.instance.away();
            
        }
    }
    public static void unlockSawim() {
        instance.lastLockTime = Sawim.getCurrentGmtTime();
        instance.locked = false;
        ContactList.getInstance().activate();
        AutoAbsence.instance.online();
    }

    public static void maximize() {
        //instance.restore(instance.display.getCurrentDisplay());
        wakeUp();
    }
    public static void minimize() {
        instance.hideApp();
        //instance.display.hide();
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

