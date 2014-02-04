package sawim.modules.tracking;

import DrawControls.icons.Icon;
import android.graphics.drawable.Drawable;
import android.util.Log;
import protocol.Contact;
import protocol.Protocol;
import protocol.icq.Icq;
import protocol.mrim.Mrim;
import ru.sawim.SawimApplication;
import ru.sawim.activities.SawimActivity;
import sawim.chat.Chat;
import sawim.chat.message.PlainMessage;
import sawim.modules.Notify;
import sawim.roster.RosterHelper;
import sawim.util.JLocale;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

public final class Tracking {
    public static final byte FALSE = (byte) 0;
    public static final byte TRUE = (byte) 1;
    public final static int GLOBAL = 0;
    public final static int EVENT_ENTER = 1;
    public final static int EVENT_EXIT = 2;
    public final static int EVENT_STATUS = 3;
    public final static int EVENT_MESSAGE = 4;
    public final static int EVENT_TYPING = 5;
    public final static int EVENT_XSTATUS = 6;
    public final static int ACTION_CHAT = 10;
    public final static int ACTION_NOTICE = 11;
    public final static int ACTION_SOUND = 12;
    public final static int ACTION_VIBRA = 14;
    public final static int ACTION_MESSAGE = 15;
    public final static int ACTION_MESSAGE_TEXT = 16;
    public final static int ACTION_ICON = 17;
    public final static int ACTION_INCHAT = 18;
    public final static int ACTION_HISTORY = 19;
    public final static int ACTION_PRESENCE = 20;
    public final static int ACTION_OTHER_SOUND = 21;
    private final static String track_rms_name = "track";
    private final static int TRACK_PARAM_UIN = 0;
    private final static int TRACK_PARAM_EVENT_ID = 1;
    private final static int TRACK_PARAM_ACTION_ID = 2;
    private final static int TRACK_PARAM_ACTION_VALUE = 3;
    private final static int TRACK_PARAM_COUNT = 4;
    private static Vector actions = new Vector();

    private static void addTracking(Track track) {
        synchronized (actions) {
            actions.addElement(track);
        }
    }

    private static Track getTracking(int index) {
        synchronized (actions) {
            return (Track) actions.elementAt(index);
        }
    }

    private static void setTracking(Track track, int index) {
        synchronized (actions) {
            actions.setElementAt(track, index);
        }
    }

    private static Track getTrackingMessageByTrackMessageText(Track track) {
        int index = findTrackingByID(track.idEvent, track.idAction);
        if (index <= 0) return null;
        synchronized (actions) {
            return (Track) actions.elementAt(index - 1);
        }
    }

    private static int findTrackingByID(int evt_index, int act_index) {
        int ret = -1;
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (track.idEvent == evt_index && track.idAction == act_index) {
                ret = i;
                break;
            }
        }
        return ret;
    }

    private static void deleteTracking(int index) {
        synchronized (actions) {
            actions.removeElementAt(index);
            actions.trimToSize();
        }
    }

    public static void addTrackList(String uin) {
        deleteTrackListFromTracking(uin);
        Vector list = TrackingForm.getList();
        if (list == null || list.size() <= 0) return;
        for (int i = 0; i < list.size(); i++) {
            TrackingForm.Line line = (TrackingForm.Line) list.elementAt(i);
            if (line.isEvent) continue;
            if (line.status_flag == 0) continue;

            Track track = new Track();
            track.uin = uin;
            track.idEvent = line.id_event;
            track.idAction = line.id_action;
            if (line.id_action == ACTION_MESSAGE_TEXT) {
                Log.e("ee", ""+TrackingForm.editText);
                track.valueAction = TrackingForm.editText;
            } else {
                track.valueAction = "";
            }
            addTracking(track);
        }
    }

    public static Vector getTrackList(String uin) {
        if (actions == null || actions.size() == 0) return null;
        Vector ret = new Vector();
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (!track.uin.equals(uin)) continue;
            ret.addElement(track);
        }
        return ret;
    }

    private static void deleteTrackListFromTracking(String uin) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (track.uin.equals(uin)) {
                deleteTracking(i);
                i--;
            }
        }
    }

    private static boolean existsTrackingRMS(RecordStore rms) {
        boolean ret = false;

        try {
            rms = RecordStore.openRecordStore(track_rms_name, false);
            if (rms.getNumRecords() == 0) {
                ret = false;
            } else {
                ret = true;
            }
            rms.closeRecordStore();
        } catch (RecordStoreException e) {
        }
        return ret;
    }

    public static void loadTrackingFromRMS() {
        RecordStore rms = null;
        try {

            rms = RecordStore.openRecordStore(track_rms_name, false);
            if (!existsTrackingRMS(rms)) return;
            int record_count = rms.getNumRecords();
            if (record_count == 0) return;
            for (int i = 1; i <= record_count; i = i + TRACK_PARAM_COUNT) {
                Track track = new Track();
                track.uin = getRMSRecordValue(rms, i + TRACK_PARAM_UIN);
                track.idEvent = Integer.valueOf(getRMSRecordValue(rms, i + TRACK_PARAM_EVENT_ID)).intValue();
                track.idAction = Integer.valueOf(getRMSRecordValue(rms, i + TRACK_PARAM_ACTION_ID)).intValue();

                track.valueAction = getRMSRecordValue(rms, i + TRACK_PARAM_ACTION_VALUE);
                addTracking(track);
            }
            rms.closeRecordStore();
        } catch (Exception e) {
        }
    }

    public static void saveTrackingToRMS() {
        RecordStore rms = null;
        try {
            RecordStore.deleteRecordStore(track_rms_name);
        } catch (RecordStoreException e) {
        }
        try {
            int record_count = actions.size();
            if (record_count == 0) return;
            rms = RecordStore.openRecordStore(track_rms_name, true);
            for (int i = 0; i < record_count; i++) {
                Track track = getTracking(i);
                addValueToRMSRecord(rms, track.uin);
                addValueToRMSRecord(rms, String.valueOf(track.idEvent));
                addValueToRMSRecord(rms, String.valueOf(track.idAction));
                if (track.valueAction != null)
                    addValueToRMSRecord(rms, track.valueAction);
            }
            rms.closeRecordStore();
        } catch (RecordStoreException e) {
        }
    }

    private static String getRMSRecordValue(RecordStore rms, int index) {
        String ret = "";
        try {
            byte[] data = rms.getRecord(index);
            if (data != null) {
                ret = utf8beByteArrayToString(data, 0, data.length);
            }
        } catch (RecordStoreException e) {
        }
        return ret;
    }

    private static String utf8beByteArrayToString(byte[] buf, int off, int len) {
        try {
            byte[] buf2 = new byte[len + 2];
            putWord(buf2, 0, len);
            System.arraycopy(buf, off, buf2, 2, len);
            ByteArrayInputStream bais = new ByteArrayInputStream(buf2);
            DataInputStream dis = new DataInputStream(bais);
            return dis.readUTF();
        } catch (Exception e) {
        }
        return "";
    }

    private static void putWord(byte[] buf, int off, int val) {
        buf[off] = (byte) ((val >> 8) & 0x000000FF);
        buf[++off] = (byte) ((val) & 0x000000FF);
    }

    private static void addValueToRMSRecord(RecordStore rms, String value) {
        try {
            byte[] buffer;
            buffer = stringToByteArray(value);
            rms.addRecord(buffer, 0, buffer.length);
        } catch (RecordStoreException e) {
        }
    }

    private static byte[] stringToByteArray(String val) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeUTF(val);
            byte[] raw = baos.toByteArray();
            byte[] result = new byte[raw.length - 2];
            System.arraycopy(raw, 2, result, 0, raw.length - 2);
            return result;
        } catch (Exception e) {
        }
        return val.getBytes();
    }

    public static byte isTrackingEvent(String uin, int event) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (track.uin.equals(uin))
                if (track.idEvent == event) {
                    return TRUE;
                }
        }
        return FALSE;
    }

    public static byte isTracking(String uin, int event) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (!track.uin.equals(uin)) continue;
            if (track.idEvent != event) continue;
            if (track.idAction == ACTION_MESSAGE_TEXT) continue;
            return TRUE;
        }
        return FALSE;
    }

    public static byte isTracking(String uin) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (!track.uin.equals(uin)) continue;
            if (track.idAction == ACTION_MESSAGE_TEXT) continue;
            return TRUE;
        }
        return FALSE;
    }

    public static byte isTracking() {
        if (actions == null || actions.size() == 0) return FALSE;
        return TRUE;
    }

    public static Drawable getTrackIcon(String uin) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (!track.uin.equals(uin)) continue;
            return TrackingForm.getTrackIcon();
        }
        return null;
    }

    public static void beginTrackAction(Contact item, int event) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (!track.uin.equals(item.getUserId())) continue;
            if (event != EVENT_XSTATUS) {
                if (track.idEvent != event) continue;
            } else {
                if (track.idEvent != EVENT_STATUS) continue;
            }
            int action = track.idAction;
            if (event == EVENT_ENTER) {
                beginTrackActionEnter(item, action, track);
            }
            if (event == EVENT_EXIT) {
                beginTrackActionExit(item, action);
            }
            if (event == EVENT_STATUS || event == EVENT_XSTATUS) {
                beginTrackActionStatus(item, event, action);
            }
            if (event == EVENT_MESSAGE) {
                beginTrackActionMessage(item, action);
            }
            if (event == EVENT_TYPING) {
                beginTrackActionTyping(item, action);
            }
        }
    }

    public static byte beginTrackActionItem(Contact item, int action) {
        for (int i = 0; i < actions.size(); i++) {
            Track track = getTracking(i);
            if (!track.uin.equals(item.getUserId())) continue;
            if (track.idAction == action) {
                return TRUE;
            }
        }
        return FALSE;
    }

    public static void beginTrackActionEnter(Contact item, int action, Track track) {
        Protocol protocol = item.getProtocol();
        Chat chat = protocol.getChat(item);
        Chat chat_ = new Chat(protocol, item);
        switch (action) {
            case ACTION_CHAT:
                ((SawimActivity) SawimApplication.getCurrentActivity()).openChat(chat.getProtocol(), chat.getContact(), true);
                break;
            case ACTION_NOTICE:
                RosterHelper.getInstance().activateWithMsg(JLocale.getString("track_form_title")
                        + " " + item.getName() + "\n" + item.getName() + " [" + item.getUserId() + "] " + JLocale.getString("track_action_online"));
                break;
            case ACTION_INCHAT:
                String notice = JLocale.getString("track_action_online");
                PlainMessage plainMsg = new PlainMessage(item.getUserId(), protocol, SawimApplication.getCurrentGmtTime(), notice, true);
                //plainMsg.setSendingState(Message.ICON_MSG_TRACK);
                chat.addMyMessage(plainMsg);
                break;
            case ACTION_SOUND:
                Notify.getSound().playSoundForExtra(Notify.NOTIFY_ONLINE);
                break;
            case ACTION_VIBRA:
                vibration();
                break;
            case ACTION_MESSAGE_TEXT:
                Track track_prev = getTrackingMessageByTrackMessageText(track);
                if (track_prev != null) {
                    if (track_prev.idAction == ACTION_MESSAGE) {
                        String str = track.valueAction;
                        if (str != null && str.length() != 0)
                            RosterHelper.getInstance().getCurrentProtocol().sendMessage(item, str, true);
                    }
                }
                break;
        }
    }

    private static void beginTrackActionExit(Contact item, int action) {
        Protocol protocol = item.getProtocol();
        Chat chat = protocol.getChat(item);
        Chat chat_ = new Chat(protocol, item);
        switch (action) {
            case ACTION_NOTICE:
                RosterHelper.getInstance().activateWithMsg(JLocale.getString("track_form_title")
                        + " " + item.getName() + "\n" + item.getName() + " [" + item.getUserId() + "] " + JLocale.getString("track_action_offline"));
                break;
            case ACTION_INCHAT:
                String notice = JLocale.getString("track_action_offline");
                PlainMessage plainMsg = new PlainMessage(item.getUserId(), protocol, SawimApplication.getCurrentGmtTime(), notice, true);
                //plainMsg.setSendingState(Message.ICON_MSG_TRACK);
                chat.addMyMessage(plainMsg);
                break;
            case ACTION_VIBRA:
                vibration();
                break;
        }
    }

    private static void beginTrackActionStatus(Contact item, int event, int action) {
        String status_name = "Not defined";
        String status_comm = "";
        String xstatus = status_name;
        Icon icon_status = null;
        Protocol protocol = item.getProtocol();
        Chat chat = protocol.getChat(item);
        Chat chat_ = new Chat(protocol, item);
        if (protocol instanceof Icq) {
            if (event == EVENT_STATUS) {
                status_comm = JLocale.getString("track_action_status") + " ";
                icon_status = protocol.getStatusInfo().getIcon(item.getStatusIndex());
                status_name = " [" + protocol.getStatusInfo().getName(item.getStatusIndex()) + "]";
            }

            if (event == EVENT_XSTATUS) {
                status_comm = JLocale.getString("track_action_xstatus") + " ";
                icon_status = protocol.getXStatusInfo().getIcon(item.getXStatusIndex());
                status_name = " [" + protocol.getXStatusInfo().getName(item.getXStatusIndex()) + "]";
            }
        }

        if (protocol instanceof Mrim) {
            if (event == EVENT_STATUS) {
                status_comm = JLocale.getString("track_action_status") + " ";
                icon_status = protocol.getStatusInfo().getIcon(item.getStatusIndex());
                status_name = " [" + protocol.getStatusInfo().getName(item.getStatusIndex()) + "]";
            }
            if (event == EVENT_XSTATUS) {
                status_comm = JLocale.getString("track_action_xstatus") + " ";
                icon_status = protocol.getStatusInfo().getIcon(item.getStatusIndex());
                status_name = " [" + protocol.getXStatusInfo().getName(item.getXStatusIndex()) + "]";
            }
        }

        switch (action) {
            case ACTION_INCHAT:
                PlainMessage plainMsg = new PlainMessage(item.getUserId(), protocol, SawimApplication.getCurrentGmtTime(), status_comm, true);
                //plainMsg.setSendingState(Message.ICON_MSG_TRACK);
                chat.addMyMessage(plainMsg);
                break;
            case ACTION_VIBRA:
                vibration();
                break;
        }
    }

    private static void beginTrackActionMessage(Contact item, int action) {
        switch (action) {
            case ACTION_VIBRA:
                vibration();
                break;
            case ACTION_SOUND:
                Notify.getSound().playSoundForExtra(Notify.NOTIFY_MESSAGE);
                break;
        }
    }

    private static void beginTrackActionTyping(Contact item, int action) {
        switch (action) {
            case ACTION_VIBRA:
                vibration();
                break;
            case ACTION_SOUND:
                Notify.getSound().playSoundForExtra(Notify.NOTIFY_TYPING);
                break;
        }
    }

    private static void vibration() {
        Notify.getSound().vibrate(500);
    }

    public static class Track {
        String uin;
        int idEvent;
        int idAction;
        String valueAction;
    }
}