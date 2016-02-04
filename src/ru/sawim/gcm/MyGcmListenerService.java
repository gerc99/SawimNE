package ru.sawim.gcm;

/**
 * Created by gerc on 24.01.2016.
 */

import android.os.Bundle;
import android.util.Log;
import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import ru.sawim.SawimNotification;

public class MyGcmListenerService extends GcmListenerService {

    private static final String TAG = "MyGcmListenerService";

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
