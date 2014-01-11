package ru.sawim.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import ru.sawim.R;
import ru.sawim.activities.SawimActivity;
import sawim.chat.ChatHistory;
import sawim.roster.RosterHelper;

public class SawimService extends Service {
    public static final String ACTION_FOREGROUND = "FOREGROUND";

    private static final String LOG_TAG = "SawimService";

    private final Messenger messenger = new Messenger(new IncomingHandler());

    public static final int UPDATE_APP_ICON = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "onStart();");
        startForeground(R.string.app_name, getNotification());
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

    private Notification getNotification() {
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        int allUnread = ChatHistory.instance.getPersonalUnreadMessageCount(true);
        CharSequence stateMsg = "";

        final int icon;
        if (0 < allUnread) {
            icon = R.drawable.ic_tray_msg;
        } else if (RosterHelper.getInstance().isConnected()) {
            icon = R.drawable.ic_tray_on;
            stateMsg = getText(R.string.online);
        } else {
            icon = R.drawable.ic_tray_off;
            if (RosterHelper.getInstance().isConnecting()) {
                stateMsg = getText(R.string.connecting);
            } else {
                stateMsg = getText(R.string.offline);
            }
        }

        final Notification notification = new Notification(icon, null, 0);
        if (0 < unread) {
            notification.ledARGB = 0xff00ff00;
            notification.ledOnMS = 300;
            notification.ledOffMS = 1000;
            notification.flags |= android.app.Notification.FLAG_SHOW_LIGHTS;
            //notification.number = unread;
            stateMsg = String.format((String) getText(R.string.unread_messages), unread);
        }
        Intent notificationIntent = new Intent(this, SawimActivity.class);
        notificationIntent.setAction(SawimActivity.NOTIFY);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        stateMsg = ChatHistory.instance.getLastMessage(stateMsg.toString());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notification.setLatestEventInfo(this, getText(R.string.app_name), stateMsg, contentIntent);
        return notification;
    }

    private class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case UPDATE_APP_ICON:
                        SawimService.this.startForeground(R.string.app_name, getNotification());
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
