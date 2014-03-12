package protocol.xmpp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import ru.sawim.SawimApplication;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class XmppSession {
    private SharedPreferences _prefs;
    private final String PREFS_NAME = "XMPP:Settings";
    private Context _context;
    private SharedPreferences.Editor _editor;
    private XmppConnection connection;

    String SENDER_ID = "284764164645";

    GoogleCloudMessaging gcm;
    Context context;

    String regid;

    public XmppSession(Context context, XmppConnection connection) {
        _context = context;
        this.connection = connection;
        _prefs = _context.getSharedPreferences(PREFS_NAME, 0);
        _editor = _prefs.edit();
        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
       /* if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(_context);
            registerInBackground();
        } else {
            Log.i(XmppSession.class.getSimpleName(), "No valid Google Play Services APK found.");
        }*/
    }
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(_context);
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.i(XmppSession.class.getSimpleName(), "This device is not supported.");
            return false;
        }
        return true;
    }


    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
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

    public void enable() {
        connection.putPacketIntoQueue("<iq type='set'>" +
                "<register xmlns='http://sawim.ru/notifications#gcm' regid='"+ regid+ "' /></iq>");
    }


    public void save() {
        _editor.putString("JID", connection.fullJid_);
        _editor.putBoolean("Enabled", connection.isSessionManagementEnabled());
        _editor.putLong("PacketsIn", connection.packetsIn);
        _editor.putLong("PacketsOut", connection.packetsOut);
        _editor.putString("SessionID", connection.smSessionID);
        _editor.commit();
    }

    public void load() {
        String jid = _prefs.getString("JID", "");
        if (jid.equals(connection.fullJid_)) {
            connection.setSessionManagementEnabled( _prefs.getBoolean("Enabled", false));
            connection.packetsIn = _prefs.getLong("PacketsIn", 0);
            connection.packetsOut = _prefs.getLong("PacketsOut", 0);
            connection.smSessionID = _prefs.getString("SessionID", "");
        }
    }
}
