package ru.sawim.gcm;

/**
 * Created by gerc on 24.01.2016.
 */

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import ru.sawim.BuildConfig;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimNotification;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

    private static final String CHANNEL_ID = BuildConfig.APPLICATION_ID;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private static void createChannel() {
        NotificationChannel сhannel = new NotificationChannel(CHANNEL_ID, SawimApplication.getInstance().getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
        //mChannel.setDescription(description);
        сhannel.setShowBadge(false);
        сhannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ((NotificationManager) SawimApplication.getInstance().getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(сhannel);
    }

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        int badge = 0;
        String msg = "";
        try {
            JSONObject aps = new JSONObject(data.getString("aps"));
            badge = aps.getInt("badge");
            msg = aps.getString("alert");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String fromJid = data.getString("from_jid");
        Log.d(TAG, msg);
        SawimNotification.pushNotification(this, fromJid, msg, badge);
    }
}
