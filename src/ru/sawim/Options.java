package ru.sawim;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import protocol.Profile;
import protocol.StatusInfo;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.io.Storage;
import ru.sawim.modules.DebugLog;

import java.io.*;

public class Options {
    public static final String UNAVAILABLE_NESSAGE = "unavailable_message";
    public static final String OPTION_ANTISPAM_MSG = "antispam_msg";
    public static final String OPTION_ANTISPAM_HELLO = "antispam_hello";
    public static final String OPTION_ANTISPAM_ANSWER = "antispam_answer";
    public static final String OPTION_ANTISPAM_KEYWORDS = "antispam_keywords";

    public static final String OPTION_CL_SORT_BY = "cl_sort_by";
    public static final String OPTION_MESS_NOTIF = "mess_notify";
    public static final String OPTION_MESS_RINGTONE = "mess_ringtone";
    public static final String OPTION_CURRENT_PAGE = "current_page";
    public static final String OPTION_VIBRATION = "vibration";
    public static final String OPTION_COLOR_SCHEME = "color_scheme";
    public static final String OPTION_TYPING_MODE = "typing_mode";
    public static final String OPTION_MAX_MSG_COUNT = "max_msg_count";
    public static final String OPTION_FONT_SCHEME = "font_scheme";

    public static final String OPTION_ANSWERER = "answer";
    public static final String OPTION_HIDE_KEYBOARD = "hide_keyboard";
    public static final String OPTION_CL_HIDE_OFFLINE = "cl_hide_offline";
    public static final String OPTION_USER_GROUPS = "user_groups";
    public static final String OPTION_ANTISPAM_ENABLE = "antispam_enable";
    public static final String OPTION_SORT_UP_WITH_MSG = "sort_up_with_msg";
    public static final String OPTION_ALARM = "alarm";
    public static final String OPTION_SHOW_STATUS_LINE = "show_status_line";
    public static final String OPTION_NOTIFY_IN_AWAY = "notify_in_away";
    public static final String OPTION_BLOG_NOTIFY = "blog_notify";
    public static final String OPTION_SIMPLE_INPUT = "simple_input";

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;
    private static Profile profile;

    public static void init() {
        preferences = PreferenceManager.getDefaultSharedPreferences(SawimApplication.getContext());
        editor = preferences.edit();
        if (preferences.getAll().isEmpty()) {
            setDefaults();
        }
    }

    public static void safeSave() {
        editor.commit();
    }

    private static void setDefaults() {
        setString(Options.UNAVAILABLE_NESSAGE, "I'll be back");
        setString(Options.OPTION_CL_SORT_BY, SawimApplication.getContext().getString(R.string.sort_by_status));
        setBoolean(Options.OPTION_SORT_UP_WITH_MSG, true);
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE, false);
        setBoolean(Options.OPTION_HIDE_KEYBOARD, true);
        setBoolean(Options.OPTION_MESS_NOTIF, true);
        setString(Options.OPTION_MESS_RINGTONE, "content://settings/system/notification_sound");
        setString(Options.OPTION_TYPING_MODE, SawimApplication.getContext().getString(R.string.typing_both));
        setBoolean(Options.OPTION_BLOG_NOTIFY, true);
        setBoolean(Options.OPTION_NOTIFY_IN_AWAY, true);
        setString(Options.OPTION_MAX_MSG_COUNT, "100");
        setString(Options.OPTION_ANTISPAM_KEYWORDS, "http sms www @conf");
        setBoolean(Options.OPTION_ANSWERER, false);
        setBoolean(Options.OPTION_USER_GROUPS, true);
        setString(Options.OPTION_COLOR_SCHEME, "Light Holo");
        setInt(Options.OPTION_FONT_SCHEME, 16);
        setBoolean(Options.OPTION_SHOW_STATUS_LINE, false);

        setBoolean(OPTION_ALARM, true);

        safeSave();
    }

    public static String getString(String key) {
        return preferences.getString(key, "");
    }

    public static int getInt(String key) {
        return preferences.getInt(key, 0);
    }

    public static int getInt(int entriesResId, String key) {
        String[] entries = SawimApplication.getContext().getResources().getStringArray(entriesResId);
        String s = preferences.getString(key, entries[0]);
        for (int i = 0; i < entries.length; ++i) {
            if (entries[i].equals(s)) return i;
        }
        return 0;
    }

    public static boolean getBoolean(String key) {
        return preferences.getBoolean(key, false);
    }

    public static long getLong(String key) {
        return preferences.getLong(key, 0);
    }

    public static void setString(String key, String value) {
        editor.putString(key, value);
    }

    public static void setInt(String key, int value) {
        editor.putInt(key, value);
    }

    public static void setBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
    }

    public static void setLong(String key, long value) {
        editor.putLong(key, value);
    }

    public static Profile getAccount() {
        if (profile == null) {
            profile = new Profile();
        }
        return profile;
    }

    public static boolean hasAccount() {
        return profile != null && !StringConvertor.isEmpty(profile.userId);
    }

    public static void loadAccounts() {
        Storage s = new Storage("j-accounts");
        try {
            s.open();
            if (s.getNumRecords() > 0) {
                byte[] data = s.getRecord(1);
                if ((null == data) || (0 == data.length)) {
                    return;
                }
                Profile p = readProfile(data);
                if (!StringConvertor.isEmpty(p.userId)) {
                    profile = p;
                }
            }
        } catch (Exception e) {
            DebugLog.panic("load accounts", e);
        }
        s.close();
    }

    public static void saveAccount(Profile account) {
        if (StringConvertor.isEmpty(account.userId)) {
            return;
        }
        Storage s = new Storage("j-accounts");
        try {
            s.open();
            byte[] hash = writeAccount(account);
            if (s.getNumRecords() == 0) {
                s.addRecord(hash);
            } else {
                s.setRecord(1, hash);
            }
        } catch (Exception e) {
            DebugLog.panic("save account", e);
        }
        s.close();
    }

    private static byte[] writeAccount(Profile account) {
        if (account == null) return new byte[0];
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(StringConvertor.notNull(account.userId));
            dos.writeUTF(StringConvertor.notNull(account.password));
            dos.writeByte(account.statusIndex);
            dos.writeUTF(StringConvertor.notNull(account.statusMessage));
            dos.writeByte(account.xstatusIndex);
            dos.writeUTF(StringConvertor.notNull(account.xstatusTitle));
            dos.writeUTF(StringConvertor.notNull(account.xstatusDescription));
            byte[] hash = Util.decipherPassword(baos.toByteArray());
            baos.close();
            return hash;
        } catch (Exception e) {
            DebugLog.panic("write account" + account.userId, e);
            return new byte[0];
        }
    }

    private static Profile readProfile(byte[] data) {
        Profile p = new Profile();
        try {
            byte[] buf = Util.decipherPassword(data);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf);
            DataInputStream dis = new DataInputStream(bais);

            p.userId = dis.readUTF();
            p.password = dis.readUTF();
            p.statusIndex = dis.readByte();
            p.statusMessage = dis.readUTF();
            p.xstatusIndex = dis.readByte();
            p.xstatusTitle = dis.readUTF();
            p.xstatusDescription = dis.readUTF();
            if (0 < dis.available()) {
                if (!dis.readBoolean()) {
                    p.statusIndex = StatusInfo.STATUS_OFFLINE;
                }
            }
            bais.close();
        } catch (IOException ex) {
            DebugLog.panic("read account", ex);
        }
        return p;
    }
}
