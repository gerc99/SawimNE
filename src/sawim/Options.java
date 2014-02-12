package sawim;

import protocol.Profile;
import protocol.StatusInfo;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.forms.PrivateStatusForm;
import sawim.io.Storage;
import sawim.modules.DebugLog;
import sawim.roster.RosterHelper;

import java.io.*;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Current record store format:
 * <p/>
 * Record #1: VERSION               (UTF8)
 * Record #2: OPTION KEY            (BYTE)
 * OPTION VALUE          (Type depends on key)
 * OPTION KEY            (BYTE)
 * OPTION VALUE          (Type depends on key)
 * OPTION KEY            (BYTE)
 * OPTION VALUE          (Type depends on key)
 * ...
 * <p/>
 * Option key            Option value
 * 0 -  63 (00XXXXXX)  UTF8
 * 64 - 127 (01XXXXXX)  INTEGER
 * 128 - 191 (10XXXXXX)  BOOLEAN
 * 192 - 224 (110XXXXX)  LONG
 * 225 - 255 (111XXXXX)  SHORT, BYTE-ARRAY (scrambled String)
 */

public class Options {
    ////////////////////// UTF8
    static final int OPTION_UIN1 = 0;
    public static final int UNAVAILABLE_NESSAGE = 1;
    static final int OPTION_UIN2 = 14;
    static final int OPTION_UIN3 = 15;
    static final int OPTION_NICK1 = 21;
    static final int OPTION_NICK2 = 22;
    static final int OPTION_NICK3 = 23;
    public static final int OPTION_ANTISPAM_MSG = 24;
    public static final int OPTION_ANTISPAM_HELLO = 25;
    public static final int OPTION_ANTISPAM_ANSWER = 26;
    public static final int OPTION_ANTISPAM_KEYWORDS = 29;

    ////////////////////// INTEGER
    public static final int OPTION_CL_SORT_BY = 65;
    public static final int OPTION_MESS_NOTIF_MODE = 66;
    public static final int OPTION_NOTIFY_VOLUME = 67;
    public static final int OPTION_ONLINE_NOTIF_MODE = 68;
    public static final int OPTION_VIBRATOR = 75;
    public static final int OPTION_COLOR_SCHEME = 73;
    public static final int OPTION_VISIBILITY_ID = 85;
    static final int OPTIONS_CURR_ACCOUNT = 86;
    public static final int OPTION_GMT_OFFSET = 87;
    public static final int OPTION_TYPING_MODE = 88;
    public static final int OPTION_LOCAL_OFFSET = 90;
    public static final int OPTION_CHAT_PRESENSEFONT_SCHEME = 92;
    public static final int OPTION_PRIVATE_STATUS = 93;
    public static final int OPTION_MAX_MSG_COUNT = 94;
    public static final int OPTION_AA_TIME  = 106;
    public static final int OPTION_FONT_SCHEME = 107;

    ////////////////////// BOOLEAN
    public static final int OPTION_ANSWERER = 128;
    public static final int OPTION_HIDE_KEYBOARD = 129;
    public static final int OPTION_CL_HIDE_OFFLINE = 130;
    public static final int OPTION_USER_GROUPS = 136;
    public static final int OPTION_HISTORY = 137;
    public static final int OPTION_WAKE_LOCK = 143;
    public static final int OPTION_SILENT_MODE = 150;
    public static final int OPTION_BRING_UP = 151;
    public static final int OPTION_ANTISPAM_ENABLE = 158;
    public static final int OPTION_HIDE_ICONS_CLIENTS = 160;
    public static final int OPTION_TF_FLAGS = 169;
    public static final int OPTION_SORT_UP_WITH_MSG = 171;
    public static final int OPTION_ALARM = 176;
    public static final int OPTION_SHOW_STATUS_LINE = 177;
    public static final int OPTION_NOTIFY_IN_AWAY = 179;
    public static final int OPTION_BLOG_NOTIFY = 180;
    public static final int OPTION_INSTANT_RECONNECTION = 181;
    public static final int OPTION_SIMPLE_INPUT = 182;

    ////////////////////// SHORT, BYTE-ARRAY (scrambled String)
    static final int OPTION_PASSWORD1 = 228;
    static final int OPTION_PASSWORD2 = 229;
    static final int OPTION_PASSWORD3 = 230;

    private static final Vector listOfProfiles = new Vector();

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
            return (Profile) listOfProfiles.elementAt(num);
        }
    }

    public static String getId(int id) {
        Profile p = (Profile) listOfProfiles.elementAt(id);
        return p.userId;
    }

    public static int getAccountIndex(Profile profile) {
        synchronized (listOfProfiles) {
            return Math.max(0, Util.getIndex(listOfProfiles, profile));
        }
    }

    public static void setCurrentAccount(int num) {
        num = Math.min(num, getAccountCount());
        Options.setInt(Options.OPTIONS_CURR_ACCOUNT, num);
    }

    public static int getCurrentAccount() {
        return Options.getInt(Options.OPTIONS_CURR_ACCOUNT);
    }

    public static void delAccount(int num) {
        RosterHelper.getInstance().removeProtocol(num);
        synchronized (listOfProfiles) {
            listOfProfiles.removeElementAt(num);
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
                s.open(false);
                for (; num < listOfProfiles.size(); ++num) {
                    Profile p = (Profile) listOfProfiles.elementAt(num);
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
                listOfProfiles.setElementAt(account, num);
            } else {
                num = listOfProfiles.size();
                listOfProfiles.addElement(account);
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
                listOfProfiles.removeAllElements();
                s.open(false);
                int accountCount = s.getNumRecords();
                for (int i = 0; i < accountCount; ++i) {
                    byte[] data = s.getRecord(i + 1);
                    if ((null == data) || (0 == data.length)) {
                        break;
                    }
                    Profile p = readProfile(data);
                    if (!StringConvertor.isEmpty(p.userId)) {
                        listOfProfiles.addElement(p);
                    }
                }
            }
        } catch (Exception e) {
            DebugLog.panic("load accounts", e);

            addProfile(OPTION_UIN1, OPTION_PASSWORD1, OPTION_NICK1);
            addProfile(OPTION_UIN2, OPTION_PASSWORD2, OPTION_NICK2);
            addProfile(OPTION_UIN3, OPTION_PASSWORD3, OPTION_NICK3);
        }
        s.close();
    }

    private static void saveAccount(int num, Profile account) {
        if (StringConvertor.isEmpty(account.userId)) {
            return;
        }
        Storage s = new Storage("j-accounts");
        try {
            s.open(true);
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

    private static void addProfile(int uinOpt, int passOpt, int nickOpt) {
        String uin = getString(uinOpt);
        if (!StringConvertor.isEmpty(uin)) {
            Profile p = new Profile();
            p.userId = uin;
            p.password = getString(passOpt);
            p.nick = getString(nickOpt);
            setAccount(getMaxAccountCount(), p);
            setString(uinOpt, "");
        }
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

    private static Object[] options = new Object[256];

    public static void loadOptions() {
        try {
            setDefaults();
            initAccounts();
            load();

        } catch (Exception e) {
            setDefaults();
        }
    }

    private static void initAccounts() {
        setInt(Options.OPTIONS_CURR_ACCOUNT, 0);
    }

    private static void setDefaults() {
        setString(Options.UNAVAILABLE_NESSAGE, "I'll be back");
        setBoolean(Options.OPTION_INSTANT_RECONNECTION, true);
        setBoolean(Options.OPTION_WAKE_LOCK, false);
        setInt(Options.OPTION_CL_SORT_BY, 0);
        setBoolean(Options.OPTION_SORT_UP_WITH_MSG, true);
        setBoolean(Options.OPTION_CL_HIDE_OFFLINE, false);
        setBoolean(Options.OPTION_HIDE_ICONS_CLIENTS, true);
        setBoolean(Options.OPTION_HIDE_KEYBOARD, true);
        setInt(Options.OPTION_MESS_NOTIF_MODE, 0);
        setInt(Options.OPTION_ONLINE_NOTIF_MODE, 0);
        setInt(Options.OPTION_TYPING_MODE, 0);
        setBoolean(Options.OPTION_BLOG_NOTIFY, true);
        setBoolean(Options.OPTION_NOTIFY_IN_AWAY, true);
        setInt(Options.OPTION_NOTIFY_VOLUME, 100);
        setBoolean(Options.OPTION_TF_FLAGS, false);
        setInt(Options.OPTION_MAX_MSG_COUNT, 100);
        setInt(Options.OPTION_VIBRATOR, 1);
        setString(Options.OPTION_ANTISPAM_KEYWORDS, "http sms www @conf");
        setInt(Options.OPTION_PRIVATE_STATUS, PrivateStatusForm.PSTATUS_NOT_INVISIBLE);
        setBoolean(Options.OPTION_ANSWERER, false);
        setBoolean(Options.OPTION_USER_GROUPS, true);
        setBoolean(Options.OPTION_HISTORY, false);
        setInt(Options.OPTION_COLOR_SCHEME, 1);
        setInt(Options.OPTION_FONT_SCHEME, 16);
        setInt(Options.OPTION_AA_TIME, 3);
        setInt(Options.OPTION_CHAT_PRESENSEFONT_SCHEME, 0);
        setBoolean(Options.OPTION_SHOW_STATUS_LINE, false);
        setInt(Options.OPTION_VISIBILITY_ID, 0);

        setBoolean(Options.OPTION_SILENT_MODE, false);
        setBoolean(Options.OPTION_BRING_UP, false);
        int time = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / (1000 * 60 * 60);
        setInt(Options.OPTION_GMT_OFFSET, time);
        setInt(Options.OPTION_LOCAL_OFFSET, 0);
        setBoolean(OPTION_ALARM, true);
    }

    private static void load() throws IOException {
        byte[] buf = Storage.loadSlot(Storage.SLOT_OPTIONS);
        if (buf == null) {
            return;
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);
        DataInputStream dis = new DataInputStream(bais);
        while (dis.available() > 0) {
            int optionKey = dis.readUnsignedByte();
            if (optionKey < 64) {
                setString(optionKey, dis.readUTF());
            } else if (optionKey < 128) {
                setInt(optionKey, dis.readInt());
            } else if (optionKey < 192) {
                setBoolean(optionKey, dis.readBoolean());
            } else if (optionKey < 224) {
                setLong(optionKey, dis.readLong());
            } else {
                byte[] optionValue = new byte[dis.readUnsignedShort()];
                dis.readFully(optionValue);
                optionValue = Util.decipherPassword(optionValue);
                setString(optionKey, StringConvertor.utf8beByteArrayToString(optionValue, 0, optionValue.length));
            }
        }
    }

    private static void save() throws IOException {
        DebugLog.profilerStart();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int key = 0; key < options.length; ++key) {
            if (null == options[key]) {
                continue;
            }
            dos.writeByte(key);
            if (key < 64) {
                dos.writeUTF((String) options[key]);
            } else if (key < 128) {
                dos.writeInt(((Integer) options[key]).intValue());
            } else if (key < 192) {
                dos.writeBoolean(((Boolean) options[key]).booleanValue());
            } else if (key < 224) {
                dos.writeLong(((Long) options[key]).longValue());
            } else if (key < 256) {
                String str = (String) options[key];
                byte[] optionValue = StringConvertor.stringToByteArrayUtf8(str);
                optionValue = Util.decipherPassword(optionValue);
                dos.writeShort(optionValue.length);
                dos.write(optionValue);
            }
        }
        DebugLog.profilerStep("make options");
        Storage.saveSlot(Storage.SLOT_OPTIONS, baos.toByteArray());
        DebugLog.profilerStep("safeSlot(OPTIONS)");
    }

    public static synchronized void safeSave() {
        long profiler = DebugLog.profilerStart();
        try {
            save();
        } catch (Exception e) {
            DebugLog.println("options: " + e.toString());
        }
        try {
            new ru.sawim.config.Options().store();
        } catch (Exception e) {
            DebugLog.println("options: " + e.toString());
        }
        DebugLog.profilerStep("safeSave", profiler);
    }

    public static String getString(int key) {
        String value = (String) options[key];
        return (null == value) ? "" : value;
    }

    public static int getInt(int key) {
        Integer value = (Integer) options[key];
        return (null == value) ? 0 : value;
    }

    public static boolean getBoolean(int key) {
        Boolean value = (Boolean) options[key];
        return (null != value) && value;
    }

    public static long getLong(int key) {
        Long value = (Long) options[key];
        return (null == value) ? 0 : value.longValue();
    }

    public static void setString(int key, String value) {
        options[key] = value;
    }

    public static void setInt(int key, int value) {
        options[key] = new Integer(value);
    }

    public static void setBoolean(int key, boolean value) {
        options[key] = new Boolean(value);
    }

    public static void setLong(int key, long value) {
        options[key] = new Long(value);
    }
}