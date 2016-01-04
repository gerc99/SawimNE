package ru.sawim;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.github.anrwatchdog.ANRWatchDog;
import com.squareup.leakcanary.LeakCanary;
import com.squareup.leakcanary.RefWatcher;

import de.duenndns.ssl.MemorizingTrustManager;
import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.Xmpp;
import ru.sawim.chat.ChatHistory;
import ru.sawim.comm.JLocale;
import ru.sawim.io.*;
import ru.sawim.modules.Answerer;
import ru.sawim.modules.AutoAbsence;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.Emotions;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.receiver.NetworkStateReceiver;
import ru.sawim.roster.RosterHelper;
import ru.sawim.service.SawimService;
import ru.sawim.service.SawimServiceConnection;
import ru.sawim.text.TextFormatter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.06.13
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
public class SawimApplication extends Application {

    public static final String LOG_TAG = SawimApplication.class.getSimpleName();

    public static final String DATABASE_NAME = "sawim.db";
    public static final int DATABASE_VERSION = 10;

    public static String NAME;
    public static String VERSION;
    public static final String PHONE = "Android/" + android.os.Build.MODEL
            + "/" + android.os.Build.VERSION.RELEASE;
    public static final String DEFAULT_SERVER = "jabber.ru";
    public static final int AVATAR_SIZE = 56;

    public static boolean returnFromAcc = false;
    private boolean paused = true;
    private static int fontSize;
    public static boolean showStatusLine;
    public static boolean enableHistory;
    public static int sortType;
    public static boolean hideIconsClient;
    public static int autoAbsenceTime;

    private static SawimApplication instance;
    private final SawimServiceConnection serviceConnection = new SawimServiceConnection();
    private final NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
    public boolean isBindService = false;
    public static final HashMap<String, String> actionQueue = new HashMap<>();

    private DatabaseHelper databaseHelper;

    private Handler uiHandler;
    private ExecutorService backgroundExecutor;

    public static SSLContext sc;

    public RefWatcher refWatcher;

    public static SawimApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getBaseContext();
    }

    @Override
    public void onCreate() {
        instance = this;
        NAME = getString(R.string.app_name);
        VERSION = getVersion();
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getContext()));
        super.onCreate();
        //new ANRWatchDog().start();
        refWatcher = LeakCanary.install(this);
        databaseHelper = new DatabaseHelper(getApplicationContext());
        uiHandler = new Handler(Looper.getMainLooper());
        backgroundExecutor = Executors
                .newCachedThreadPool(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable,
                                "Background executor service");
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
        startService();
        networkStateReceiver.updateNetworkState(this);

        instance.paused = false;

        HomeDirectory.init();
        Options.init();
        initOptions();
        updateOptions();
        Updater.startUIUpdater();

        try {
            gc();
            Emotions.instance.load();
            Answerer.getInstance().load();
            gc();
            sc = SSLContext.getInstance("TLS");
            MemorizingTrustManager mtm = new MemorizingTrustManager(SawimApplication.this);
            sc.init(null, new X509TrustManager[] {mtm}, new SecureRandom());
            Options.loadAccounts();
            RosterHelper.getInstance().initAccounts();
            RosterHelper.getInstance().loadAccounts();
            loadChats();
        } catch (Exception e) {
            DebugLog.panic("init", e);
        }
        DebugLog.startTests();
        TextFormatter.init();

        StorageConvertor.historyConvert();
        int count = RosterHelper.getInstance().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
            p.getStorage().loadUnreadMessages();
        }

    }

    private void loadChats() {
        int count = RosterHelper.getInstance().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
            List<Contact> contacts = HistoryStorage.getActiveContacts();
            for (Contact contact : contacts) {
                ChatHistory.instance.registerChat(p.getChat(contact));
            }
        }
    }

    public Handler getUiHandler() {
        return uiHandler;
    }

    public static ExecutorService getExecutor() {
        return getInstance().backgroundExecutor;
    }

    private void startService() {
        if (!isRunService()) {
            startService(new Intent(this, SawimService.class));
        }
        if (!isBindService) {
            isBindService = true;
            registerReceiver(networkStateReceiver, networkStateReceiver.getFilter());
            bindService(new Intent(this, SawimService.class), serviceConnection, BIND_AUTO_CREATE);
        }
    }

    public void stopService() {
        if (isBindService) {
            isBindService = false;
            unbindService(serviceConnection);
            unregisterReceiver(networkStateReceiver);
        }
        if (isRunService()) {
            stopService(new Intent(this, SawimService.class));
        }
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
    }

    public void updateConnectionState() {
        serviceConnection.send(Message.obtain(null, SawimService.UPDATE_CONNECTION_STATUS));
    }

    public void updateAppIcon() {
        serviceConnection.send(Message.obtain(null, SawimService.UPDATE_APP_ICON));
    }

    public void setStatus(Protocol p, int statusIndex, String statusMsg) {
        Object[] objects = new Object[]{p, statusIndex, statusMsg};
        serviceConnection.send(Message.obtain(null, SawimService.SET_STATUS, objects));
    }

    public void sendNotify(boolean silent) {
        serviceConnection.send(Message.obtain(null, SawimService.SEND_NOTIFY, silent));
    }

    public boolean isNetworkAvailable() {
        return networkStateReceiver.isNetworkAvailable();
    }

    private String getVersion() {
        String version = "";
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pinfo.versionName + "(" + pinfo.versionCode + ")";
        } catch (PackageManager.NameNotFoundException e) {
            version = "unknown";
        }
        return version;
    }

    public static void initOptions() {
        enableHistory = Options.getBoolean(JLocale.getString(R.string.pref_history));
    }

    public static void updateOptions() {
        SawimResources.initIcons();
        fontSize = Options.getInt(JLocale.getString(R.string.pref_font_scheme));
        showStatusLine = Options.getBoolean(JLocale.getString(R.string.pref_show_status_line));
        hideIconsClient = Options.getBoolean(JLocale.getString(R.string.pref_hide_icons_clients));
        sortType = Options.getInt(R.array.sort_by_array, JLocale.getString(R.string.pref_cl_sort_by));
        autoAbsenceTime = Options.getInt(R.array.absence_array, JLocale.getString(R.string.pref_aa_time)) * 5 * 60;
        if (Options.getBoolean(JLocale.getString(R.string.pref_history))
                && enableHistory != Options.getBoolean(JLocale.getString(R.string.pref_history))) {
            for (Protocol p : RosterHelper.getInstance().getProtocols()) {
                if (p instanceof Xmpp) {
                    if (((Xmpp) p).getConnection() != null) {
                        ((Xmpp) p).getConnection().enableMessageArchiveManager();
                    }
                }
            }
        }
    }

    public static boolean isManyPane() {
        return SawimApplication.getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE && isTablet();
    }

    public static boolean isTablet() {
        return getInstance().getResources().getBoolean(R.bool.is_tablet);
    }

    public void quit(boolean isForceClose) {
        HistoryStorage.saveUnreadMessages();
        AutoAbsence.getInstance().online();
        int count = RosterHelper.getInstance().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
            p.disconnect(false);
        }
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis();
    }

    public static java.io.InputStream getResourceAsStream(String name) {
        InputStream in = new FileSystem().openSawimFile(name);
        if (null == in) {
            try {
                in = SawimApplication.getInstance().getAssets().open(name.substring(1));
            } catch (Exception ignored) {
            }
        }
        return in;
    }

    private boolean isRunService() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> runningServiceInfoList = manager.getRunningServices(Integer.MAX_VALUE);
        if (runningServiceInfoList != null) {
            for (ActivityManager.RunningServiceInfo service : runningServiceInfoList) {
                if (SawimService.class.getCanonicalName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isPaused() {
        return instance.paused;
    }

    public static void maximize() {
        AutoAbsence.getInstance().online();
        instance.paused = false;
    }

    public static void minimize() {
        AutoAbsence.getInstance().userActivity();
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

    public static DatabaseHelper getDatabaseHelper() {
        return instance.databaseHelper;
    }
}
