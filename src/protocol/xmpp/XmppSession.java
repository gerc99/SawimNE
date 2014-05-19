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

    private static final String SM_ENABLED = "Enabled";
    private static final String SM_SESSION_ID = "SessionID";
    private static final String SM_PACKETS_IN = "PacketsIn";
    private static final String SM_PACKETS_OUT = "PacketsOut";

    private static final String REBIND_ENABLED = "Rebind";
    private static final String REBIND_SESSION_ID = "Rebind_SessionId";

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
        if (connection == null) {
            return;
        }

        String accountId = connection.fullJid_;

        editor.putBoolean(REBIND_ENABLED + accountId, false);
        editor.putString(REBIND_SESSION_ID + accountId, "");

        editor.putBoolean(SM_ENABLED + accountId, false);
        editor.putLong(SM_PACKETS_IN + accountId, 0);
        editor.putLong(SM_PACKETS_OUT + accountId, 0);
        editor.putString(SM_SESSION_ID + accountId, "");

        editor.commit();

        Log.d("sawim-session", "Clear session for " + accountId);
    }

    public void save(XmppConnection connection) {
        String accountId = connection.fullJid_;

        editor.putBoolean(REBIND_ENABLED + accountId, connection.rebindEnabled);
        editor.putString(REBIND_SESSION_ID + accountId, connection.rebindSessionId);

        editor.putBoolean(SM_ENABLED + accountId, connection.smEnabled);
        editor.putLong(SM_PACKETS_IN + accountId, connection.smPacketsIn);
        editor.putLong(SM_PACKETS_OUT + accountId, connection.smPacketsOut);
        editor.putString(SM_SESSION_ID + accountId, connection.smSessionId);

        editor.commit();

        Log.d("sawim-session", "Saved session for " + accountId);
        Log.d("sawim-session", "smEnabled = " + connection.smEnabled);
        Log.d("sawim-session", "smSessionId = " + connection.smSessionId);
        Log.d("sawim-session", "rebindEnabled = " + connection.rebindEnabled);
        Log.d("sawim-session", "rebindSessionId = " + connection.rebindSessionId);
    }

    public void load(XmppConnection connection) {
        String accountId = connection.fullJid_;

        connection.rebindEnabled = preferences.getBoolean(REBIND_ENABLED + accountId, false);
        connection.rebindSessionId = preferences.getString(REBIND_SESSION_ID + accountId, "");

        connection.smEnabled = preferences.getBoolean(SM_ENABLED + accountId, false);
        connection.smPacketsIn = preferences.getLong(SM_PACKETS_IN + accountId, 0);
        connection.smPacketsOut = preferences.getLong(SM_PACKETS_OUT + accountId, 0);
        connection.smSessionId = preferences.getString(SM_SESSION_ID + accountId, "");

        Log.d("sawim-session", "Loaded session for " + accountId);
        Log.d("sawim-session", "smEnabled = " + connection.smEnabled);
        Log.d("sawim-session", "smSessionId = " + connection.smSessionId);
        Log.d("sawim-session", "rebindEnabled = " + connection.rebindEnabled);
        Log.d("sawim-session", "rebindSessionId = " + connection.rebindSessionId);
    }

    public boolean isStreamManagementSupported(String accountId) {
        Log.d("sawim-session", "Checking session supporting for " + accountId);
        Log.d("sawim-session", "smEnabled = " + preferences.getBoolean(SM_ENABLED + accountId, false));
        Log.d("sawim-session", "rebindEnabled = " + preferences.getBoolean(REBIND_ENABLED + accountId, false));

        return preferences.getBoolean(REBIND_ENABLED + accountId, false) ||
               preferences.getBoolean(SM_ENABLED + accountId, false);
    }
}
