package protocol.xmpp;

import android.content.Context;
import android.content.SharedPreferences;

public class XmppSession {
    private SharedPreferences _prefs;
    private final String PREFS_NAME = "XMPP:Settings";
    private Context _context;
    private SharedPreferences.Editor _editor;
    private XmppConnection connection;

    public XmppSession(Context context, XmppConnection connection) {
        _context = context;
        this.connection = connection;
        _prefs = _context.getSharedPreferences(PREFS_NAME, 0);
        _editor = _prefs.edit();
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