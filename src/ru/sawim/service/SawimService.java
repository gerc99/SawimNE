package ru.sawim.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimNotification;
import ru.sawim.chat.ChatHistory;
import ru.sawim.roster.RosterHelper;

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
        Log.i(LOG_TAG, "onStart();");

        new Thread(new Runnable() {
            @Override
            public void run() {
                ChatHistory.instance.loadUnreadMessages();
            }
        },"loadUnreadMessage").start();
        if (RosterHelper.getInstance() != null) {
            RosterHelper.getInstance().autoConnect();
        }
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "onDestroy();");
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
                        break;
                    case UPDATE_APP_ICON:
                        break;
                    case SEND_NOTIFY:
                        SawimNotification.sendNotify(SawimService.this, (String)((Object[])msg.obj)[0], (String)((Object[])msg.obj)[1], (boolean)((Object[])msg.obj)[2]);
                        break;
                    case SET_STATUS:
                        final Protocol protocol = (Protocol) ((Object[]) msg.obj)[0];
                        final int statusIndex = (int) ((Object[]) msg.obj)[1];
                        final String statusMsg = (String) ((Object[]) msg.obj)[2];
                        new AsyncTask<Void, Void, Void>() {
                            @Override
                            protected Void doInBackground(Void... params) {
                                protocol.setStatus(statusIndex, statusMsg);
                                return null;
                            }
                        }.execute(null, null, null);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
