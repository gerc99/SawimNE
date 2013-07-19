package ru.sawim;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import org.microemu.MIDletBridge;
import org.microemu.app.Common;
import org.microemu.cldc.file.FileSystem;
import org.microemu.util.AndroidRecordStoreManager;
import ru.sawim.service.SawimService;
import sawim.chat.ChatHistory;
import sawim.cl.ContactList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.LogManager;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 12.06.13
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
public class SawimApplication extends Application {

    public static SawimApplication instance;
    private final SawimServiceConnection serviceConnection = new SawimServiceConnection();
    public boolean useAbsence = false;
    public Common common;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    private final ExecutorService backgroundExecutor;
    private final Handler handler;

    public static SawimApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public SawimApplication() {
        handler = new Handler();
        backgroundExecutor = Executors
                .newSingleThreadExecutor(new ThreadFactory() {
                    @Override
                    public Thread newThread(Runnable runnable) {
                        Thread thread = new Thread(runnable,
                                "Background executor service");
                        thread.setPriority(Thread.MIN_PRIORITY);
                        thread.setDaemon(true);
                        return thread;
                    }
                });
    }

    @Override
    public void onCreate() {
        instance = this;
        new General().init();
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getApplicationContext()));
        //StrictMode.enableDefaults();
        super.onCreate();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        MIDletInit();
        new General().startApp();
        ChatHistory.instance.loadUnreadMessages();
        updateAppIcon();
        if (ContactList.getInstance().getManager() != null)
            ContactList.getInstance().autoConnect();
    }

    public void runInBackground(final Runnable runnable) {
        backgroundExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Exception e) {
                    ExceptionHandler.reportOnlyHandler(SawimApplication.this);
                }
            }
        });
    }

    public void runOnUiThread(final Runnable runnable) {
        handler.post(runnable);
    }

    private void MIDletInit() {
        common = new Common();
        MIDletBridge.setMicroEmulator(common);
        common.setRecordStoreManager(new AndroidRecordStoreManager(getApplicationContext()));
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            System.setProperty("video.snapshot.encodings", "yes");
        }
        FileSystem fs = new FileSystem();
        fs.registerImplementation();
        startService();
        networkStateReceiver.updateNetworkState(this);
        common.initMIDlet();
    }

    private void startService() {
        startService(new Intent(this, SawimService.class));
        registerReceiver(networkStateReceiver, networkStateReceiver.getFilter());
        bindService(new Intent(this, SawimService.class), serviceConnection, BIND_AUTO_CREATE);
    }

    public void quit() {
        unbindService(serviceConnection);
        unregisterReceiver(networkStateReceiver);
        stopService(new Intent(this, SawimService.class));
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
    }

    public boolean isNetworkAvailable() {
        return networkStateReceiver.isNetworkAvailable();
    }

    public void updateAppIcon() {
        serviceConnection.send(Message.obtain(null, SawimService.UPDATE_APP_ICON));
    }

    public String getVersion() {
        String version = "";
        try {
            PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pinfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            version = "unknown";
        }
        return version;
    }
}