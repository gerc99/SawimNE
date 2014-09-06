package ru.sawim;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import protocol.Profile;
import protocol.StatusInfo;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.io.Storage;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Options {

    static final String OPTIONS_CURR_ACCOUNT = "current_account";
    public static final String OPTION_PRIVATE_STATUS = "private_status";
    public static final String OPTION_CL_HIDE_OFFLINE = "cl_hide_offline";
    public static final String OPTION_CURRENT_PAGE = "current_page";
    public static final String OPTION_PRESENCE = "presence_";

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    private static final List<Profile> listOfProfiles = new ArrayList<Profile>();

    public static void init() {
        PreferenceManager.setDefaultValues(SawimApplication.getContext(), R.xml.preference, true);
        preferences = PreferenceManager.getDefaultSharedPreferences(SawimApplication.getContext());
        editor = preferences.edit();
    }

    public static synchronized void safeSave() {
        editor.commit();
    }

    public static String getString(String key) {
        return preferences.getString(key, "");
    }

    public static String getString(String key, String value) {
        return preferences.getString(key, value);
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
