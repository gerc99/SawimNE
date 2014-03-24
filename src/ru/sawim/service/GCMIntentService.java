package ru.sawim.service;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import ru.sawim.receiver.GCMBroadcastReceiver;
import ru.sawim.SawimApplication;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by Vitaly on 01.03.14.
 */
public class GCMIntentService extends IntentService {

    public static final String LOG_TAG = GCMIntentService.class.getSimpleName();

    private String regId;

    public GCMIntentService() {
        super(LOG_TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                //sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                //sendNotification("Deleted messages on server: " + extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                if (intent.hasExtra("message")) {
                    String msg = intent.getExtras().getString("message");
                    String from = intent.getExtras().getString("message_from");
                    try {
                        String message = URLDecoder.decode(msg, "UTF-8");
                        Log.d(LOG_TAG, message);
                        SawimApplication.getInstance().sendNotify(from, message);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GCMBroadcastReceiver.completeWakefulIntent(intent);
    }
}