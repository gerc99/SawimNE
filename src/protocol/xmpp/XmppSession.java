package protocol.xmpp;

import android.content.Context;
import android.content.SharedPreferences;
import ru.sawim.SawimApplication;

final class XmppSession {
    private static final String PREFS_NAME = "XMPP:Session";
    private static final String SESSION_ID = "SessionId";
    private static final String USER_ID = "UserId";

    private String sessionId;
    private String userId;

    private boolean isRestored = false;

    XmppSession() {
        loadSessionData();
    }

    boolean isEmpty() {
        return userId.isEmpty() || sessionId.isEmpty();
    }

    boolean isRestored() {
        return isRestored;
    }

    boolean isSessionFor(String userId) {
        return !sessionId.isEmpty() && this.userId.equals(userId);
    }

    String getSessionId() {
        return sessionId;
    }

    String getUserId() {
        return userId;
    }

    void setSessionData(String userId, String sessionId) {
        if (userId == null || sessionId == null) {
            throw new IllegalArgumentException("Empty session data");
        }

        this.userId = userId;
        this.sessionId = sessionId;

        saveSessionData();
    }

    void setAsRestored() {
        isRestored = true;
    }

    void resetSessionData() {
        userId = "";
        sessionId = "";

        saveSessionData();
    }

    private void saveSessionData() {
        SharedPreferences.Editor editor = getPreferencesEditor();
        editor.putString(SESSION_ID, sessionId);
        editor.putString(USER_ID, userId);
        editor.commit();
    }

    private void loadSessionData() {
        SharedPreferences preferences = getPreferences();
        sessionId = preferences.getString(SESSION_ID, "");
        userId = preferences.getString(USER_ID, "");
    }

    private SharedPreferences.Editor getPreferencesEditor() {
        SharedPreferences preferences = getPreferences();
        return preferences.edit();
    }

    private SharedPreferences getPreferences() {
        Context context = SawimApplication.getContext();
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
