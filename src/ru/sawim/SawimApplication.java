package ru.sawim;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Message;
import org.microemu.MIDletBridge;
import org.microemu.app.Common;
import org.microemu.cldc.file.FileSystem;
import org.microemu.util.AndroidRecordStoreManager;
import ru.sawim.service.SawimService;
import ru.sawim.service.SawimServiceConnection;
import ru.sawim.text.TextFormatter;
import sawim.chat.ChatHistory;
import sawim.roster.RosterHelper;

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
    public Common common;
    private NetworkStateReceiver networkStateReceiver = new NetworkStateReceiver();

    public static SawimApplication getInstance() {
        return instance;
    }

    public static Context getContext() {
        return instance.getApplicationContext();
    }

    public SawimApplication() {
    }

    @Override
    public void onCreate() {
        instance = this;
        Thread.setDefaultUncaughtExceptionHandler(ExceptionHandler.inContext(getApplicationContext()));
        super.onCreate();
        MIDletInit();
        new General().startApp();
        TextFormatter.init();
        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatHistory.instance.loadUnreadMessages();
            }
        }).start();
        if (RosterHelper.getInstance() != null) {
            RosterHelper.getInstance().autoConnect();
            Thread.yield();
        }
        updateAppIcon();
    }

    private void MIDletInit() {
        common = new Common();
        MIDletBridge.setMicroEmulator(common);
        common.setRecordStoreManager(new AndroidRecordStoreManager(getApplicationContext()));
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

    public void updateAppIcon() {
        serviceConnection.send(Message.obtain(null, SawimService.UPDATE_APP_ICON));
    }

    public boolean isNetworkAvailable() {
        return networkStateReceiver.isNetworkAvailable();
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