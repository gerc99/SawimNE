package ru.sawim.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimNotification;

public class SawimService extends Service {

    private static final String LOG_TAG = SawimService.class.getSimpleName();
    private final Messenger messenger = new Messenger(new IncomingHandler());

    public static final int UPDATE_CONNECTION_STATUS = 1;
    public static final int UPDATE_APP_ICON = 2;
    public static final int SEND_NOTIFY = 3;
    public static final int SET_STATUS = 4;

    @Override
    public void onCreate() {
        super.onCreate();
        SawimService.this.startForeground(R.string.app_name, SawimNotification.get(SawimService.this, false));
        Log.i(LOG_TAG, "onStart();");
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy();");
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return messenger.getBinder();
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            try {
                switch (msg.what) {
                    case UPDATE_CONNECTION_STATUS:
                        //updateLock();
                        break;
                    case UPDATE_APP_ICON:
                        SawimService.this.startForeground(R.string.app_name, SawimNotification.get(SawimService.this, false));
                        break;
                    case SEND_NOTIFY:
                        //SawimNotification.sendNotify(SawimService.this, (String)((Object[])msg.obj)[0], (String)((Object[])msg.obj)[1]);
                        SawimService.this.startForeground(R.string.app_name, SawimNotification.get(SawimService.this, (boolean)msg.obj));
                        break;
                    case SET_STATUS:
                        final Protocol protocol = (Protocol) ((Object[]) msg.obj)[0];
                        final int statusIndex = (int) ((Object[]) msg.obj)[1];
                        final String statusMsg = (String) ((Object[]) msg.obj)[2];
                        protocol.setStatus(statusIndex, statusMsg);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
