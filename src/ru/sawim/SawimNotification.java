package ru.sawim;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import ru.sawim.ui.activity.SawimActivity;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.comm.JLocale;
import ru.sawim.roster.RosterHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by admin on 27.01.14.
 */
public class SawimNotification {

    private static final int VIBRA_OFF = 0;
    private static final int VIBRA_ON = 1;
    private static final int VIBRA_LOCKED_ONLY = 2;

    public static final int NOTIFY_ID = 1;
    public static final int ALARM_NOTIFY_ID = 2;
    private static final List<String> idsMap = new ArrayList<String>();
    private static final HashMap<Integer, NotificationCompat.Builder> notifiBuildersMap = new HashMap<>();

    public static Notification get(Context context, boolean silent) {
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

        Chat current = ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem());
        return getNotification(context, silent, icon, stateMsg, current);
    }

    private static Notification getNotification(Context context, boolean silent, final int icon, CharSequence stateMsg, Chat current) {
        int unread = ChatHistory.instance.getPersonalUnreadMessageCount(false);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, SawimActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.setAction(SawimActivity.NOTIFY);
        if (current != null) {
            notificationIntent.putExtra(SawimActivity.EXTRA_MESSAGE_FROM_ID, current.getContact().getUserId());
        }
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (0 < unread) {
            notification.setLights(0xff00ff00, 1000, 3000);
            if (silent) {
                if (Options.getBoolean(JLocale.getString(R.string.pref_vibration))) {
                    int dat = 70;
                    long[] pattern = {0,3 * dat, dat, dat};
                    notification.setVibrate(pattern);
                }
                String ringtone = Options.getString(JLocale.getString(R.string.pref_mess_ringtone), null);
                if (ringtone != null) {
                    notification.setSound(Uri.parse(ringtone));
                }
            }
        }
        if (0 < unread) {
            notification.setNumber(unread);
            stateMsg = String.format(context.getText(R.string.unread_messages).toString(), unread);
        }
        String nick = null;
        String senderName = null;
        if (current != null && current.lastMessage != null && current.getAllUnreadMessageCount() > 0) {
            nick = current.getContact().getName();
            senderName = current.lastMessageNick;
            stateMsg = current.lastMessage;
        }
        notification.setAutoCancel(true).setWhen(0)
                .setContentIntent(contentIntent)
                .setContentText(stateMsg)
                .setSmallIcon(icon)
                .setContentTitle(nick == null ? context.getString(R.string.app_name)
                        : context.getString(R.string.message_from) + " " + (senderName == null || current.getContact().isSingleUserContact() ? nick : senderName + "/" + nick));
        return notification.build();
    }

    public static void notification(Context context, boolean silent) {
        if (!idsMap.contains(String.valueOf(NOTIFY_ID))) {
            idsMap.add(String.valueOf(NOTIFY_ID));
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFY_ID, getNotification(context, silent, R.drawable.ic_tray_msg, "",
                        ChatHistory.instance.chatAt(ChatHistory.instance.getPreferredItem())));
    }

    public static void pushNotification(Context context, String jid, String message, int unread) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, SawimActivity.class);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        notificationIntent.setAction(SawimActivity.NOTIFY);
        notificationIntent.putExtra(SawimActivity.EXTRA_MESSAGE_FROM_ID, jid);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (0 < unread) {
            notification.setLights(0xff00ff00, 1000, 3000);
            /*if (silent) {
                if (Options.getBoolean(JLocale.getString(R.string.pref_vibration))) {
                    int dat = 70;
                    long[] pattern = {0,3 * dat, dat, dat};
                    notification.setVibrate(pattern);
                }
                String ringtone = Options.getString(JLocale.getString(R.string.pref_mess_ringtone), null);
                if (ringtone != null) {
                    notification.setSound(Uri.parse(ringtone));
                }
            }*/
        }
        notification.setNumber(unread);
        notification.setAutoCancel(true).setWhen(0)
                .setContentIntent(contentIntent)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_tray_msg)
                .setContentTitle(context.getString(R.string.app_name));

        if (!idsMap.contains(String.valueOf(NOTIFY_ID))) {
            idsMap.add(String.valueOf(NOTIFY_ID));
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(NOTIFY_ID, notification.build());
    }

    public static void alarm(String nick) {
        Context context = SawimApplication.getContext();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context);
        Intent notificationIntent = new Intent(context, SawimActivity.class);
        notificationIntent.setAction(SawimActivity.NOTIFY);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setLights(0xff00ff00, 1000, 3000);
        if (Options.getBoolean(JLocale.getString(R.string.pref_vibration))) {
                    int dat = 70;
                    long[] pattern = {0,3 * dat, dat, dat};
                    notification.setVibrate(pattern);
                }
                String ringtone = Options.getString(JLocale.getString(R.string.pref_mess_ringtone), null);
        if (ringtone != null) {
                    notification.setSound(Uri.parse(ringtone));
                }
        notification.setAutoCancel(true).setWhen(0)
                .setContentIntent(contentIntent)
                .setContentText(context.getString(R.string.wake_you_up))
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(nick);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(ALARM_NOTIFY_ID, notification.build());
    }

    public static void fileProgress(int id, String filename, int percent, String text) {
        final Context context = SawimApplication.getContext();
        Intent intent = new Intent(context, SawimActivity.class);
        intent.setAction(SawimActivity.NOTIFY_UPLOAD);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SawimActivity.NOTIFY_UPLOAD, id);
        PendingIntent contentIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = notifiBuildersMap.get(id);
        if (builder == null) {
            builder = new NotificationCompat.Builder(context);
            notifiBuildersMap.put(id, builder);
        }
        String idStr = String.valueOf(id);
        if (!idsMap.contains(idStr)) {
            idsMap.add(idStr);
        }

        builder.setContentIntent(contentIntent);
        builder.setOngoing(false);
        builder.setContentTitle(filename);
        if (percent != -1) {
            if (percent == 100) {
                builder.setTicker(JLocale.getString(R.string.sending_complete));
                builder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
                notifiBuildersMap.remove(id);
                builder.setAutoCancel(true);
            } else {
                builder.setTicker(JLocale.getString(R.string.sending_file));
                builder.setSmallIcon(android.R.drawable.stat_sys_upload);
                builder.setAutoCancel(false);
            }
            builder.setContentText(text).setProgress(100, percent, false);
        } else {
            builder.setSmallIcon(android.R.drawable.stat_sys_upload_done);
            builder.setTicker(text);
            builder.setAutoCancel(false);
            builder.setContentText(text).setProgress(0, 0, false);
            notifiBuildersMap.remove(id);
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(id, builder.build());
    }

    public static void captcha(String title) {
        int id = title.hashCode();
        Context context = SawimApplication.getContext();
        Intent intent = new Intent(context, SawimActivity.class);
        intent.setAction(SawimActivity.NOTIFY_CAPTCHA);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(SawimActivity.NOTIFY_CAPTCHA, title);
        PendingIntent contentIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentIntent(contentIntent);
        builder.setOngoing(false);
        builder.setContentTitle(title);
        builder.setTicker(JLocale.getString(R.string.captcha));
        builder.setContentText(JLocale.getString(R.string.captcha));
        builder.setSmallIcon(android.R.drawable.stat_notify_more);
        builder.setAutoCancel(true);
        String idStr = String.valueOf(id);
        if (!idsMap.contains(idStr)) {
            idsMap.add(idStr);
        }
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .notify(id, builder.build());
    }

    public static void clear(int id) {
        String idStr = String.valueOf(id);
        if (idsMap.isEmpty()) return;
        ((NotificationManager) SawimApplication.getContext().getSystemService(Context.NOTIFICATION_SERVICE)).cancel(id);
        idsMap.remove(idStr);
    }

}
