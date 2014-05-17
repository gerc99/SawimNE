package protocol.xmpp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import ru.sawim.Options;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.comm.StringConvertor;

import java.io.IOException;

public class XmppSession {

    private static final String PREFS_NAME = "XMPP:Settings";
    private static final String SENDER_ID = "284764164645";

    private static final String SESSION_ID = "SessionID";
    private static final String REBIND = "Rebind";
    private static final String ENABLED = "Enabled";
    private static final String PACKETS_IN = "PacketsIn";
    private static final String PACKETS_OUT = "PacketsOut";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    boolean isPlayServices;
    String regid;

    public XmppSession() {
        Context context = SawimApplication.getContext();
        preferences = context.getSharedPreferences(PREFS_NAME, 0);
        editor = preferences.edit();
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (Options.getBoolean(Options.OPTION_PUSH)) {
            isPlayServices = checkPlayServices(context);
            if (isPlayServices) {
                registerInBackground();
            } else {
                Log.i(XmppSession.class.getSimpleName(), "No valid Google Play Services APK found.");
            }
        }
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices(Context context) {
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
                    GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(SawimApplication.getContext());
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                Log.i(XmppSession.class.getSimpleName(), msg);
                return msg;
            }
        }.execute(null, null, null);
    }

    public void enable(final XmppConnection connection) {
        if (connection == null) return;
        if (!Options.getBoolean(Options.OPTION_PUSH)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connection.isSessionManagementEnabled() && !StringConvertor.isEmpty(regid)) {
                    connection.putPacketIntoQueue("<iq type='set'>" +
                            "<register xmlns='http://sawim.ru/notifications#gcm' regid='" + regid + "' /></iq>");
                }
            }
        }, "PushEnable").start();
    }

    public void enableRebind(final XmppConnection connection) {
        if (connection == null) return;
        if (!Options.getBoolean(Options.OPTION_PUSH)) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connection.rebindSupported) {
                    Log.i(XmppSession.class.getSimpleName(), "enableRebind regid = " + regid);
                    if (!isPlayServices) {
                        connection.putPacketIntoQueue("<iq type='set' id='p1:rebind'>" +
                                "<push xmlns='p1:push'><keepalive max='120'/><session duration='1440'/>"+
                                "<body send='all' groupchat='true' from='name'/>" +
                                "<offline>true</offline></push></iq>");
                    } else if (!StringConvertor.isEmpty(regid)) {
                        connection.putPacketIntoQueue("<iq type='set' id='p1:rebind'>" +
                                "<push xmlns='p1:push'><keepalive max='120'/><session duration='1440'/>" +
                                "<body send='all' groupchat='true' from='name'/>" +
                                "<offline>true</offline>" +
                                "<notification><type>gcm</type><id>" + regid + "</id></notification>" +
                                "<appid>ru.sawim</appid></push></iq>");
                    }
                }
            }
        }, "EnableRebind").start();
    }

    public void pushRegister(final XmppConnection connection) {
        if (connection == null) return;
        enable(connection);
        enableRebind(connection);
    }

    public void pushUnregister(final XmppConnection connection) {
        if (connection == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.writePacket("<iq type='set' id='123'><disable xmlns='p1:push'/></iq>");
                    connection.writePacket("<iq type='set'><unregister xmlns='http://sawim.ru/notifications#gcm'/></iq>");
                } catch (SawimException ignored) {
                }
            }
        }, "PushUnregister").start();
    }

    public void clear(final XmppConnection connection) {
        if (connection == null) return;
        editor.putBoolean(ENABLED + connection.fullJid_, false);
        editor.putBoolean(REBIND + connection.fullJid_, false);
        editor.putLong(PACKETS_IN + connection.fullJid_, 0);
        editor.putLong(PACKETS_OUT + connection.fullJid_, 0);
        editor.putString(SESSION_ID + connection.fullJid_, "");
        editor.commit();
    }

    public void save(XmppConnection connection) {
        editor.putBoolean(ENABLED + connection.fullJid_, connection.isSessionManagementEnabled());
        editor.putBoolean(REBIND + connection.fullJid_, connection.rebindSupported);
        editor.putLong(PACKETS_IN + connection.fullJid_, connection.packetsIn);
        editor.putLong(PACKETS_OUT + connection.fullJid_, connection.packetsOut);
        editor.putString(SESSION_ID + connection.fullJid_, connection.sessionId);
        editor.commit();
    }

    public void load(XmppConnection connection) {
        connection.setSessionManagementEnabled(preferences.getBoolean(ENABLED + connection.fullJid_, false));
        connection.packetsIn = preferences.getLong(PACKETS_IN + connection.fullJid_, 0);
        connection.packetsOut = preferences.getLong(PACKETS_OUT + connection.fullJid_, 0);
        connection.sessionId = preferences.getString(SESSION_ID + connection.fullJid_, "");
    }

    public boolean isStreamManagementSupported(String jid) {
        return preferences.getBoolean(REBIND + jid, false) || preferences.getBoolean(ENABLED + jid, false);
    }
}
