package protocol.xmpp;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;

public class XmppSession {
    private static final String PREFS_NAME = "XMPP:Settings";

    private static final String SM_ENABLED = "Enabled";
    private static final String SM_SESSION_ID = "SessionID";
    private static final String SM_PACKETS_IN = "PacketsIn";
    private static final String SM_PACKETS_OUT = "PacketsOut";

    private static final String REBIND_ENABLED = "Rebind";
    private static final String REBIND_SESSION_ID = "Rebind_SessionId";

    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;

    public XmppSession() {
        Context context = SawimApplication.getContext();
        preferences = context.getSharedPreferences(PREFS_NAME, 0);
        editor = preferences.edit();
    }

    public void enableRebind(final XmppConnection connection) {
        if (connection == null) return;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (connection.rebindSupported) {
                    Log.i(XmppSession.class.getSimpleName(), "enableRebind");
                    connection.putPacketIntoQueue("<iq type='set' id='p1:rebind'>" +
                            "<push xmlns='p1:push'><keepalive max='120'/><session duration='1440'/>"+
                            "<body send='all' groupchat='true' from='name'/>" +
                            "<offline>true</offline></push></iq>");
                }
            }
        }, "EnableRebind").start();
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

        if (XmppConnection.DEBUGLOG) {
            Log.d("sawim-session", "Clear session for " + accountId);
        }
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

        if (XmppConnection.DEBUGLOG) {
            Log.d("sawim-session", "Saved session for " + accountId);
            Log.d("sawim-session", "smEnabled = " + connection.smEnabled);
            Log.d("sawim-session", "smSessionId = " + connection.smSessionId);
            Log.d("sawim-session", "rebindEnabled = " + connection.rebindEnabled);
            Log.d("sawim-session", "rebindSessionId = " + connection.rebindSessionId);
        }
    }

    public void load(XmppConnection connection) {
        String accountId = connection.fullJid_;

        connection.rebindEnabled = preferences.getBoolean(REBIND_ENABLED + accountId, false);
        connection.rebindSessionId = preferences.getString(REBIND_SESSION_ID + accountId, "");

        connection.smEnabled = preferences.getBoolean(SM_ENABLED + accountId, false);
        connection.smPacketsIn = preferences.getLong(SM_PACKETS_IN + accountId, 0);
        connection.smPacketsOut = preferences.getLong(SM_PACKETS_OUT + accountId, 0);
        connection.smSessionId = preferences.getString(SM_SESSION_ID + accountId, "");

        if (XmppConnection.DEBUGLOG) {
            Log.d("sawim-session", "Loaded session for " + accountId);
            Log.d("sawim-session", "smEnabled = " + connection.smEnabled);
            Log.d("sawim-session", "smSessionId = " + connection.smSessionId);
            Log.d("sawim-session", "rebindEnabled = " + connection.rebindEnabled);
            Log.d("sawim-session", "rebindSessionId = " + connection.rebindSessionId);
        }
    }

    public boolean isStreamManagementSupported(String accountId) {
        if (XmppConnection.DEBUGLOG) {
            Log.d("sawim-session", "Checking session supporting for " + accountId);
            Log.d("sawim-session", "smEnabled = " + preferences.getBoolean(SM_ENABLED + accountId, false));
            Log.d("sawim-session", "rebindEnabled = " + preferences.getBoolean(REBIND_ENABLED + accountId, false));
        }
        return preferences.getBoolean(REBIND_ENABLED + accountId, false) ||
               preferences.getBoolean(SM_ENABLED + accountId, false);
    }
}
