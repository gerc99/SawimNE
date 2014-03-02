package ru.sawim;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import com.google.android.gcm.GCMBaseIntentService;
import protocol.Protocol;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppConnection;
import ru.sawim.activities.SawimActivity;
import ru.sawim.service.SawimService;
import sawim.chat.ChatHistory;
import sawim.roster.RosterHelper;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by Vitaly on 01.03.14.
 */
public class GCMIntentService extends GCMBaseIntentService implements XmppConnection.SessionManagementListener {

    public final static String CLIENT_ID = "284764164645";

    private String regId;

    public GCMIntentService() {
        super(CLIENT_ID);
        int count = RosterHelper.getInstance().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
            if (p instanceof Xmpp) {
                XmppConnection xmppConnection = ((Xmpp) p).getConnection();
                xmppConnection.setSessionManagementListener(this);
            }
        }
    }

    @Override
    protected void onMessage(Context context, Intent intent) {
        if (intent.hasExtra("message")) {
            String msg = intent.getExtras().getString("message");
            String from = intent.getExtras().getString("message_from");
            try {
                String message = URLDecoder.decode(msg, "UTF-8");
                Log.d("SawGCM", message);
                SawimApplication.getInstance().sendNotify(from, message);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onError(Context context, String s) {
        Log.i("SawGCM", "got error " + s);
    }

    @Override
    protected void onRegistered(Context context, String s) {
        Log.i("SawGCM", "regid=" + s);
        regId = s;
    }

    @Override
    protected void onUnregistered(Context context, String s) {
        regId = null;
    }

    @Override
    public void enabled(XmppConnection connection) {
        if (regId != null)
        connection.putPacketIntoQueue("<iq type='set'>" +
                "<register xmlns='http://sawim.ru/notifications#gcm' regid='"+ regId+ "' /></iq>");
        else
            Log.d("SawGCM", "device not registered for GCM yet");
    }
}
