package ru.sawim;

import android.content.SharedPreferences;
import protocol.Profile;
import protocol.StatusInfo;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.forms.PrivateStatusForm;
import ru.sawim.io.Storage;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

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
    public static final String OPTION_VISIBILITY_ID = "visibility_id";
    static final String OPTIONS_CURR_ACCOUNT = "current_account";
    public static final String OPTION_GMT_OFFSET = "gmt_offset";
    public static final String OPTION_TYPING_MODE = "typing_mode";
    public static final String OPTION_PRIVATE_STATUS = "private_status";
    public static final String OPTION_MAX_MSG_COUNT = "max_msg_count";
    public static final String OPTION_AA_TIME = "aa_time";
    public static final String OPTION_FONT_SCHEME = "font_scheme";

    public static final String OPTION_ANSWERER = "answer";
    public static final String OPTION_HIDE_KEYBOARD = "hide_keyboard";
    public static final String OPTION_CL_HIDE_OFFLINE = "cl_hide_offline";
    public static final String OPTION_PUSH = "push";
    public static final String OPTION_USER_GROUPS = "user_groups";
    public static final String OPTION_HISTORY = "history";
    public static final String OPTION_WAKE_LOCK = "wake_lock";
    public static final String OPTION_BRING_UP = "bring_up";
    public static final String OPTION_ANTISPAM_ENABLE = "antispam_enable";
    public static final String OPTION_HIDE_ICONS_CLIENTS = "hide_icons_clients";
    public static final String OPTION_SORT_UP_WITH_MSG = "sort_up_with_msg";
    public static final String OPTION_ALARM = "alarm";
    public static final String OPTION_SHOW_STATUS_LINE = "show_status_line";
    public static final String OPTION_NOTIFY_IN_AWAY = "notify_in_away";
    public static final String OPTION_BLOG_NOTIFY = "blog_notify";
    public static final String OPTION_INSTANT_RECONNECTION = "instant_reconnection";
    public static final String OPTION_SIMPLE_INPUT = "simple_input";

    private static final String PREFS_NAME = "SAWIM:Settings";
    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    private static final List<Profile> listOfProfiles = new ArrayList<Profile>();

    public static void init() {
        preferences = SawimApplication.getContext().getSharedPreferences(PREFS_NAME, 0);
        editor = preferences.edit();
        //editor.clear();
        //editor.commit();
        if (preferences.getAll().isEmpty()) {
            initAccounts();
            setDefaults();
        }
    }

    public static synchronized void safeSave() {
        editor.commit();
    }

    private static void initAccounts() {
        setInt(Options.OPTIONS_CURR_ACCOUNT, 0);
    }

    private static void setDefaults() {
        setBoolean(Options.OPTION_PUSH, false);
        setString(Options.UNAVAILABLE_NESSAGE, "I'll be back");
        setInt(Options.OPTION_CURRENT_PAGE, 0);
        setBoolean(Options.OPTION_INSTANT_RECONNECTION, true);
        setBoolean(Options.OPTION_WAKE_LOCK, false);
        setInt(Options.OPTION_CL_SORT_BY, 0);
        setBoolean(Options.OPTION_SORT_UP_WITH_MSG, true);
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE, false);
        setBoolean(Options.OPTION_HIDE_ICONS_CLIENTS, true);
        setBoolean(Options.OPTION_HIDE_KEYBOARD, true);
        setBoolean(Options.OPTION_MESS_NOTIF, true);
        setInt(Options.OPTION_TYPING_MODE, 0);
        setBoolean(Options.OPTION_BLOG_NOTIFY, true);
        setBoolean(Options.OPTION_NOTIFY_IN_AWAY, true);
        setInt(Options.OPTION_MAX_MSG_COUNT, 100);
        setString(Options.OPTION_ANTISPAM_KEYWORDS, "http sms www @conf");
        setInt(Options.OPTION_PRIVATE_STATUS, PrivateStatusForm.PSTATUS_NOT_INVISIBLE);
        setBoolean(Options.OPTION_ANSWERER, false);
        setBoolean(Options.OPTION_USER_GROUPS, true);
        setBoolean(Options.OPTION_HISTORY, false);
        setInt(Options.OPTION_COLOR_SCHEME, 1);
        setInt(Options.OPTION_FONT_SCHEME, 16);
        setInt(Options.OPTION_AA_TIME, 15);
        setBoolean(Options.OPTION_SHOW_STATUS_LINE, false);
        setInt(Options.OPTION_VISIBILITY_ID, 0);

        setBoolean(Options.OPTION_BRING_UP, false);
        int time = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000 * 60 * 60);
        setInt(Options.OPTION_GMT_OFFSET, time);
        setBoolean(OPTION_ALARM, true);

        safeSave();
    }

    public static String getString(String key) {
        return preferences.getString(key, "");
    }

    public static int getInt(String key) {
        return preferences.getInt(key, 0);
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

    public static int getMaxAccountCount() {
        return 20;
    }

    public static int getAccountCount() {
        synchronized (listOfProfiles) {
            return listOfProfiles.size();
        }
    }

    public static Profile getAccount(int num) {
        synchronized (listOfProfiles) {
            if (listOfProfiles.size() <= num) {
                return new Profile();
            }
            return listOfProfiles.get(num);
        }
    }

    public static String getId(int id) {
        Profile p = listOfProfiles.get(id);
        return p.userId;
    }

    public static int getAccountIndex(Profile profile) {
        synchronized (listOfProfiles) {
            return Math.max(0, listOfProfiles.indexOf(profile));
        }
    }

    public static void setCurrentAccount(int num) {
        num = Math.min(num, getAccountCount());
        Options.setInt(Options.OPTIONS_CURR_ACCOUNT, num);
        safeSave();
    }

    public static int getCurrentAccount() {
        return Options.getInt(Options.OPTIONS_CURR_ACCOUNT);
    }

    public static void delAccount(int num) {
        RosterHelper.getInstance().removeProtocol(num);
        synchronized (listOfProfiles) {
            listOfProfiles.remove(num);
            int current = getCurrentAccount();
            if (current == num) {
                current = 0;
            }
            if (num < current) {
                current--;
            }
            if (listOfProfiles.size() < current) {
                current = 0;
            }
            setCurrentAccount(current);
            Storage s = new Storage("j-accounts");
            try {
                s.open();
                for (; num < listOfProfiles.size(); ++num) {
                    Profile p = listOfProfiles.get(num);
                    s.setRecord(num + 1, writeAccount(p));
                }
                for (; num < s.getNumRecords(); ++num) {
                    s.setRecord(num + 1, new byte[0]);
                }
            } catch (Exception ignored) {
            }
            s.close();
        }
    }

    public static void setAccount(int num, Profile account) {
        int size = getAccountCount();
        synchronized (listOfProfiles) {
            if (num < size) {
                listOfProfiles.remove(num);
                listOfProfiles.add(num, account);
            } else {
                num = listOfProfiles.size();
                listOfProfiles.add(account);
            }
            saveAccount(num, account);
        }
    }

    public static void saveAccount(Profile account) {
        synchronized (listOfProfiles) {
            int num = listOfProfiles.indexOf(account);
            if (0 <= num) {
                saveAccount(num, account);
            }
        }
    }

    public static void loadAccounts() {
        Storage s = new Storage("j-accounts");
        try {
            synchronized (listOfProfiles) {
                listOfProfiles.clear();
                s.open();
                int accountCount = s.getNumRecords();
                for (int i = 0; i < accountCount; ++i) {
                    byte[] data = s.getRecord(i + 1);
                    if ((null == data) || (0 == data.length)) {
                        break;
                    }
                    Profile p = readProfile(data);
                    if (!StringConvertor.isEmpty(p.userId)) {
                        listOfProfiles.add(p);
                    }
                }
            }
        } catch (Exception e) {
            DebugLog.panic("load accounts", e);
        }
        s.close();
    }

    private static void saveAccount(int num, Profile account) {
        if (StringConvertor.isEmpty(account.userId)) {
            return;
        }
        Storage s = new Storage("j-accounts");
        try {
            s.open();
            byte[] hash = writeAccount(account);
            if (num < s.getNumRecords()) {
                s.setRecord(num + 1, hash);
            } else {
                s.addRecord(hash);
            }
        } catch (Exception e) {
            DebugLog.panic("save account #" + num, e);
        }
        s.close();
    }

    private static byte[] writeAccount(Profile account) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeByte(account.protocolType);
            dos.writeUTF(StringConvertor.notNull(account.userId));
            dos.writeUTF(StringConvertor.notNull(account.password));
            dos.writeUTF(StringConvertor.notNull(account.nick));
            dos.writeByte(account.statusIndex);
            dos.writeUTF(StringConvertor.notNull(account.statusMessage));
            dos.writeByte(account.xstatusIndex);
            dos.writeUTF(StringConvertor.notNull(account.xstatusTitle));
            dos.writeUTF(StringConvertor.notNull(account.xstatusDescription));
            dos.writeBoolean(account.isActive);
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

            p.protocolType = dis.readByte();
            p.userId = dis.readUTF();
            p.password = dis.readUTF();
            p.nick = dis.readUTF();
            p.statusIndex = dis.readByte();
            p.statusMessage = dis.readUTF();
            p.xstatusIndex = dis.readByte();
            p.xstatusTitle = dis.readUTF();
            p.xstatusDescription = dis.readUTF();
            p.isActive = true;
            if (0 < dis.available()) {
                p.isActive = dis.readBoolean();
            }
            if (0 < dis.available()) {
                if (!dis.readBoolean()) {
                    p.statusIndex = StatusInfo.STATUS_OFFLINE;
                }
            }
            if (!p.isActive) {
                p.statusIndex = StatusInfo.STATUS_OFFLINE;
            }
            bais.close();
        } catch (IOException ex) {
            DebugLog.panic("read account", ex);
        }
        return p;
    }
}
