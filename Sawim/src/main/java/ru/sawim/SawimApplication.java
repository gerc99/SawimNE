package ru.sawim;

import android.app.ActivityManager;
import android.app.Application;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.crashlytics.android.Crashlytics;
import com.github.anrwatchdog.ANRWatchDog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import de.duenndns.ssl.MemorizingTrustManager;
import io.fabric.sdk.android.Fabric;
import protocol.Protocol;
import protocol.xmpp.Xmpp;
import ru.sawim.chat.ChatHistory;
import ru.sawim.comm.JLocale;
import ru.sawim.db.RealmDb;
import ru.sawim.gcm.Preferences;
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

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import java.io.InputStream;
import java.security.SecureRandom;
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

    public static String NAME;
    public static String VERSION;
    public static final String PHONE = "Android/" + android.os.Build.MODEL
            + "/" + android.os.Build.VERSION.RELEASE;
    public static final String DEFAULT_SERVER = "jabber.ru";
    public static final String DEFAULT_CONFERENCE_SERVER = "conference." + DEFAULT_SERVER;
    public static final int AVATAR_SIZE = 56;

    public static boolean returnFromAcc = false;
    private boolean paused = true;
    private static int fontSize;
    public static boolean enableHistory;
    public static int sortType;
    public static boolean hideIconsClient;
    public static boolean showPicturesInChat;
    public static int autoAbsenceTime;

    private boolean canForegroundService;
    public static boolean checkPlayServices;

    private static SawimApplication instance;
    private final SawimServiceConnection serviceConnection = new SawimServiceConnection();
    private final NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();
    public boolean isBindService = false;

    private Handler uiHandler;
    private ExecutorService backgroundExecutor;
    public static SSLContext sc;
    //public RefWatcher refWatcher;
    private BroadcastReceiver registrationBroadcastReceiver;

    public static SawimApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getBaseContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        NAME = getString(R.string.app_name);
        VERSION = getVersion();
        //new ANRWatchDog().start();
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getContext()));
        Fabric.with(this, new Crashlytics());
        checkPlayServices = checkPlayServices();
        canForegroundService = !checkPlayServices;
         //    refWatcher = LeakCanary.install(this);
        RealmDb.init(getApplicationContext());

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
        Scheme.init();
        Options.loadAccounts();
        getExecutor().execute(new Runnable() {
            @Override
            public void run() {
                gc();
                Emotions.instance.load();
                Answerer.getInstance().load();
                try {
                    sc = SSLContext.getInstance("TLS");
                    MemorizingTrustManager mtm = new MemorizingTrustManager(SawimApplication.this);
                    sc.init(null, new X509TrustManager[]{mtm}, new SecureRandom());
                } catch (Exception e) {
                    DebugLog.panic("TLS init", e);
                }
                RosterHelper.getInstance().getProtocol();
                DebugLog.startTests();

                RosterHelper.getInstance().loadAccounts();

                StorageConvertor.historyConvert();
                gc();
            }
        });

        registrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(Preferences.SENT_TOKEN_TO_SERVER, false);

            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(registrationBroadcastReceiver,
                new IntentFilter(Preferences.REGISTRATION_COMPLETE));
    }
//adb shell am startservice -a com.google.android.gms.iid.InstanceID --es "CMD" "RST" -n ru.sawim.jp/ru.sawim.gcm.MyInstanceIDListenerService
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
        if (!canForegroundService) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
        }
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
        hideIconsClient = Options.getBoolean(JLocale.getString(R.string.pref_hide_icons_clients));
        showPicturesInChat = Options.getBoolean(JLocale.getString(R.string.pref_show_pictures_in_chat));
        sortType = Options.getInt(R.array.sort_by_array, JLocale.getString(R.string.pref_cl_sort_by));
        autoAbsenceTime = Options.getInt(R.array.absence_array, JLocale.getString(R.string.pref_aa_time)) * 5 * 60;
        if (Options.getBoolean(JLocale.getString(R.string.pref_history))
                && enableHistory != Options.getBoolean(JLocale.getString(R.string.pref_history))) {
            Protocol p = RosterHelper.getInstance().getProtocol();
            if (((Xmpp) p).getConnection() != null) {
                ((Xmpp) p).getConnection().enableMessageArchiveManager();
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
        Protocol p = RosterHelper.getInstance().getProtocol();
        if (p != null) {
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

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private static boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.i(LOG_TAG, "This device is not supported.");
            return false;
        }
        return true;
    }

    public boolean isCanForegroundService() {
        return canForegroundService;
    }

    public void setCanForegroundService(boolean canForegroundService) {
        this.canForegroundService = canForegroundService;
        if (!checkPlayServices || canForegroundService) {
            serviceConnection.send(Message.obtain(null, SawimService.START_FOREGROUND_SERVICE));
        } else {
            serviceConnection.send(Message.obtain(null, SawimService.STOP_FOREGROUND_SERVICE));
        }
    }
}
