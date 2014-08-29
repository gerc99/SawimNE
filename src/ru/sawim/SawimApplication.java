package ru.sawim;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Surface;
import android.view.WindowManager;
import de.duenndns.ssl.MemorizingTrustManager;
import protocol.Protocol;
import ru.sawim.chat.ChatHistory;
import ru.sawim.comm.JLocale;
import ru.sawim.io.DatabaseHelper;
import ru.sawim.io.FileSystem;
import ru.sawim.io.HomeDirectory;
import ru.sawim.io.StorageConvertor;
import ru.sawim.modules.Answerer;
import ru.sawim.modules.AutoAbsence;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.Emotions;
import ru.sawim.receiver.NetworkStateReceiver;
import ru.sawim.roster.RosterHelper;
import ru.sawim.service.SawimService;
import ru.sawim.service.SawimServiceConnection;
import ru.sawim.text.TextFormatter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.06.13
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
public class SawimApplication extends Application {

    public static final String LOG_TAG = SawimApplication.class.getSimpleName();

    public static String NAME;
    public static String VERSION;
    public static final String PHONE = "Android/" + android.os.Build.MODEL
            + "/" + android.os.Build.VERSION.RELEASE;
    public static final String DEFAULT_SERVER = "jabber.ru";

    public static boolean returnFromAcc = false;
    private boolean paused = true;
    private static int fontSize;
    public static boolean showStatusLine;
    public static int sortType;
    public static boolean hideIconsClient;
    public static int autoAbsenceTime;
    public static int gmtOffset;

    public static SawimApplication instance;
    private static DatabaseHelper dbHelper;
    private final SawimServiceConnection serviceConnection = new SawimServiceConnection();
    private final NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
    public boolean isBindService = false;

    private Handler uiHandler;
    private static final ExecutorService executor = Executors.newCachedThreadPool();

    public static SSLContext sc;

    public static SawimApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getBaseContext();
    }

    public static DatabaseHelper getDbHelper() {
        return dbHelper;
    }

    @Override
    public void onCreate() {
        instance = this;
        NAME = getString(R.string.app_name);
        VERSION = getVersion();
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getContext()));
        super.onCreate();
        dbHelper = new DatabaseHelper(getApplicationContext());
        uiHandler = new Handler(Looper.getMainLooper());

        startService();
        networkStateReceiver.updateNetworkState(this);

        instance.paused = false;

        HomeDirectory.init();
        Options.init();
        Scheme.load();
        updateOptions();
        Updater.startUIUpdater();
        gmtOffset = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000 * 60 * 60);
        try {
            gc();
            Emotions.instance.load();
            Answerer.getInstance().load();
            gc();
            sc = SSLContext.getInstance("TLS");
            MemorizingTrustManager mtm = new MemorizingTrustManager(this);
            sc.init(null, new X509TrustManager[] {mtm}, new SecureRandom());
            Options.loadAccounts();
            RosterHelper.getInstance().initAccounts();
            RosterHelper.getInstance().loadAccounts();
        } catch (Exception e) {
            DebugLog.panic("init", e);
            DebugLog.instance.activate();
        }
        DebugLog.startTests();
        TextFormatter.init();

        new Thread(new Runnable() {
            @Override
            public void run() {
                StorageConvertor.historyConvert();
                ChatHistory.instance.loadUnreadMessages();
            }
        },"loadMessage").start();
        if (RosterHelper.getInstance() != null) {
            RosterHelper.getInstance().autoConnect();
        }
    }

    public Handler getUiHandler() {
        return uiHandler;
    }

    public static ExecutorService getExecutor() {
        return executor;
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

    public void sendNotify(String title, String text, boolean silent) {
        Object[] parameters = new Object[]{title, text, silent};
        serviceConnection.send(Message.obtain(null, SawimService.SEND_NOTIFY, parameters));
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

    public static void updateOptions() {
        SawimResources.initIcons();
        fontSize = Options.getInt(JLocale.getString(R.string.pref_font_scheme));
        showStatusLine = Options.getBoolean(JLocale.getString(R.string.pref_show_status_line));
        hideIconsClient = Options.getBoolean(JLocale.getString(R.string.pref_hide_icons_clients));
        sortType = Options.getInt(R.array.sort_by_array, JLocale.getString(R.string.pref_cl_sort_by));
        autoAbsenceTime = Options.getInt(R.array.absence_array, JLocale.getString(R.string.pref_aa_time)) * 5 * 60;
    }

    public static boolean isManyPane() {
        int rotation = ((WindowManager) SawimApplication.getContext()
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getRotation();
        return !(rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) && isTablet();
    }

    public static boolean isTablet() {
        return getInstance().getResources().getBoolean(R.bool.is_tablet);
    }

    public void quit(boolean isForceClose) {
        int count = RosterHelper.getInstance().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
            p.disconnect(true);
        }
        RosterHelper.getInstance().safeSave();
        ChatHistory.instance.saveUnreadMessages();
        AutoAbsence.getInstance().online();
    }

    public static long getCurrentGmtTime() {
        return System.currentTimeMillis() / 1000;
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
        ru.sawim.modules.AutoAbsence.getInstance().userActivity();
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
}
