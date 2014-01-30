package ru.sawim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import ru.sawim.activities.SawimActivity;
import sawim.chat.ChatHistory;
import sawim.roster.RosterHelper;

/**
 * Created by admin on 27.01.14.
 */
public class SawimNotification {

    public static final int NOTIFY_ID = 1;

    public static Notification get(Context context) {
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        int allUnread = ChatHistory.instance.getPersonalUnreadMessageCount(true);
        CharSequence stateMsg = "";

        final int icon;
        if (0 < allUnread) {
            icon = R.drawable.ic_tray_msg;
        } else if (RosterHelper.getInstance().isConnected()) {
            icon = R.drawable.ic_tray_on;
            stateMsg = context.getText(R.string.online);
        } else {
            icon = R.drawable.ic_tray_off;
            if (RosterHelper.getInstance().isConnecting()) {
                stateMsg = context.getText(R.string.connecting);
            } else {
                stateMsg = context.getText(R.string.offline);
            }
        }

        long when = 0;
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, SawimActivity.class);
        notificationIntent.setAction(SawimActivity.NOTIFY);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (0 < unread) {
            notification.setLights(0xff00ff00, 300, 1000);
        }
        if (0 < allUnread) {
            notification.setNumber(unread);
            stateMsg = String.format((String) context.getText(R.string.unread_messages), unread);
        }
        stateMsg = ChatHistory.instance.getLastMessage(stateMsg.toString());
        notification.setAutoCancel(true);
        notification.setWhen(when);
        //notification.setDefaults(android.app.Notification.DEFAULT_ALL);
        notification.setContentIntent(contentIntent);
        notification.setContentTitle(context.getString(R.string.app_name));
        notification.setContentText(stateMsg);
        notification.setSmallIcon(icon);
        return notification.build();
    }

    /*public static void sendNotify(Context context, final String title, final String text) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        long when = 0;
        int icon = R.drawable.ic_tray_msg;
        long[] vibraPattern = {0, 500, 250, 500};
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        int allUnread = ChatHistory.instance.getPersonalUnreadMessageCount(true);
        if (0 < allUnread) {
            NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
            CharSequence contentTitle = context.getText(R.string.notify_title);
            CharSequence contentText = null;
            Intent notificationIntent = new Intent(context, SawimActivity.class);
            notificationIntent.setAction(SawimActivity.NOTIFY);
            PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
            //Intent replyIntent = new Intent(context, SawimActivity.class);
            //replyIntent.setAction(SawimActivity.NOTIFY_REPLY);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //replyIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            //PendingIntent piReply = PendingIntent.getActivity(context, 0, replyIntent, PendingIntent.FLAG_ONE_SHOT);
            //notification.setVibrate(vibraPattern);
            if (0 < unread) {
                notification.setLights(0xff00ff00, 300, 1000);
                //notification.number = unread;
                contentText = String.format((String) context.getText(R.string.unread_messages), unread);
            }
            contentText = ChatHistory.instance.getLastMessage(contentText.toString());
            notification.setWhen(when);
            notification.setDefaults(android.app.Notification.DEFAULT_ALL);
            notification.setContentIntent(contentIntent);
            notification.setContentTitle(contentTitle);
            notification.setContentText(contentText);
            notification.setSmallIcon(icon);
            //NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
            //style.addLine(text).setBigContentTitle(title).setSummaryText(contentText);
            //notification.addAction(android.R.drawable.sym_action_chat, context.getText(R.string.reply), piReply);
            //notification.setStyle(style);
            notificationManager.notify(NOTIFY_ID, notification.build());
        }
    }*/

    public static void clear(Context context) {
        SawimApplication.getInstance().updateAppIcon();
        //NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        //notificationManager.cancel(NOTIFY_ID);
    }
}
