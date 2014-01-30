package ru.sawim.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import ru.sawim.R;
import ru.sawim.SawimNotification;
import sawim.roster.RosterHelper;

public class SawimService extends Service {

    private static final String LOG_TAG = "SawimService";
    private final Messenger messenger = new Messenger(new IncomingHandler());

    public static final int UPDATE_APP_ICON = 1;
    public static final int SEND_NOTIFY = 2;
    public static final int SET_STATUS = 3;


    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onStart();");
        startForeground(R.string.app_name, SawimNotification.get(this));
        //musicReceiver = new MusicReceiver(this);
        //this.registerReceiver(musicReceiver, musicReceiver.getIntentFilter());
        //scrobbling finished
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy();");
        //this.unregisterReceiver(musicReceiver);
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return messenger.getBinder();
    }



    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case UPDATE_APP_ICON:
                        SawimService.this.startForeground(R.string.app_name, SawimNotification.get(SawimService.this));
                        break;
                    case SEND_NOTIFY:
                        //SawimNotification.sendNotify(SawimService.this, ((String[])msg.obj)[0], ((String[])msg.obj)[1]);
                        SawimService.this.startForeground(R.string.app_name, SawimNotification.get(SawimService.this));
                        break;
                    case SET_STATUS:
                        RosterHelper.getInstance().setStatus();
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
