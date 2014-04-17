package protocol.xmpp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.comm.StringConvertor;
import ru.sawim.modules.DebugLog;

import java.io.IOException;

public class XmppSession {
    private static final String PREFS_NAME = "XMPP:Settings";
    private static final String SENDER_ID = "284764164645";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    GoogleCloudMessaging gcm;
    Context context;
    String regid;

    public XmppSession() {
        context = SawimApplication.getContext();
        preferences = context.getSharedPreferences(PREFS_NAME, 0);
        editor = preferences.edit();
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(context);
            registerInBackground();
        } else {
            Log.i(XmppSession.class.getSimpleName(), "No valid Google Play Services APK found.");
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.i(XmppSession.class.getSimpleName(), "This device is not supported.");
            return false;
        }
        return true;
    }


    /**
     * Registers the application with GCM servers asynchronously.
     * <p/>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;

                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }
        }.execute(null, null, null);
    }

    public void enable(final XmppConnection connection) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connection.isSessionManagementEnabled() && !StringConvertor.isEmpty(regid)) {

                    connection.putPacketIntoQueue("<iq type='set'>" +
                            "<register xmlns='http://sawim.ru/notifications#gcm' regid='" + regid + "' /></iq>");
                }
            }
        }).start();
    }

    public void enableRebind(final XmppConnection connection) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connection.rebindSupported && !StringConvertor.isEmpty(regid)) {
                    DebugLog.systemPrintln(regid);
                    connection.putPacketIntoQueue("<iq type='set' id='p1:rebind'>" +
                            "<push xmlns='p1:push'><keepalive max='120'/><session duration='1440'/>"+
                            "<body send='all' groupchat='true' from='name'/>" +
                            "<offline>true</offline>" +
                            "<notification><type>gcm</type><id>" + regid + "</id>" +
                            "</notification><appid>ru.sawim</appid></push></iq>");
                }
            }
        }).start();
    }

    public void clear(final XmppConnection connection) {
        if (connection.isSessionManagementEnabled() && !StringConvertor.isEmpty(regid)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        connection.writePacket("<iq type='set'>" +
                                "<unregister xmlns='http://sawim.ru/notifications#gcm'/></iq>");
                    } catch (SawimException ignored) {
                        ignored.printStackTrace();
                    }
                }
            }).start();
        }
        editor.putBoolean("Enabled" + connection.fullJid_, false);
        editor.putLong("PacketsIn" + connection.fullJid_, 0);
        editor.putLong("PacketsOut" + connection.fullJid_, 0);
        editor.putString("SessionID" + connection.fullJid_, "");
        editor.putString("SID" + connection.fullJid_, "");
        editor.commit();
    }

    public void save(XmppConnection connection) {
        editor.putBoolean("Enabled" + connection.fullJid_, connection.isSessionManagementEnabled());
        editor.putLong("PacketsIn" + connection.fullJid_, connection.packetsIn);
        editor.putLong("PacketsOut" + connection.fullJid_, connection.packetsOut);
        editor.putString("SessionID" + connection.fullJid_, connection.smSessionID);
        editor.putString("SID" + connection.fullJid_, connection.sessionId);
        editor.commit();
    }

    public void load(XmppConnection connection) {
        connection.setSessionManagementEnabled(preferences.getBoolean("Enabled" + connection.fullJid_, false));
        connection.packetsIn = preferences.getLong("PacketsIn" + connection.fullJid_, 0);
        connection.packetsOut = preferences.getLong("PacketsOut" + connection.fullJid_, 0);
        connection.smSessionID = preferences.getString("SessionID" + connection.fullJid_, "");
        connection.sessionId = preferences.getString("SID" + connection.fullJid_, "");
    }

    public boolean isStreamManagementSupported(String jid) {
        return preferences.getBoolean("Enabled" + jid, false);
    }
}
