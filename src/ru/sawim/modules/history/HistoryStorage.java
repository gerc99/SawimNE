package ru.sawim.modules.history;

import android.content.ContentValues;
import android.database.Cursor;
import protocol.Contact;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.io.DatabaseHelper;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryStorage {

    private static final String WHERE_ACC_CONTACT_ID = DatabaseHelper.ACCOUNT_ID + " = ? AND " + DatabaseHelper.CONTACT_ID + " = ?";
    private static final String WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID = DatabaseHelper.ACCOUNT_ID + " = ? AND " + DatabaseHelper.CONTACT_ID
            + "= ? AND " + DatabaseHelper.AUTHOR + "= ? AND " + DatabaseHelper.MESSAGE + " = ?";

    private String protocolId;
    private String uniqueUserId;

    private HistoryStorage(String protocolId, String uniqueUserId) {
        this.protocolId = protocolId;
        this.uniqueUserId = uniqueUserId;
    }

    public synchronized static HistoryStorage getHistory(String protocolId, String uniqueUserId) {
        return new HistoryStorage(protocolId, uniqueUserId);
    }

    public void addText(MessData md) {
        addText(md.getIconIndex(), md.isIncoming(), md.getNick(), md.getText().toString(), md.getTime(), md.getRowData());
    }

    public void addText(int iconIndex, boolean isIncoming, String nick, String text, long time, short rowData) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.ACCOUNT_ID, protocolId);
            values.put(DatabaseHelper.CONTACT_ID, uniqueUserId);
            values.put(DatabaseHelper.SENDING_STATE, iconIndex);
            values.put(DatabaseHelper.INCOMING, isIncoming ? 0 : 1);
            values.put(DatabaseHelper.AUTHOR, nick);
            values.put(DatabaseHelper.MESSAGE, text);
            values.put(DatabaseHelper.DATE, time);
            values.put(DatabaseHelper.ROW_DATA, rowData);
            SawimApplication.getDatabaseHelper().getWritableDatabase().insert(DatabaseHelper.TABLE_CHAT_HISTORY, null, values);
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public void updateText(MessData md) {
        try {
            ContentValues values = new ContentValues();
            values.put(DatabaseHelper.SENDING_STATE, md.getIconIndex());
            SawimApplication.getDatabaseHelper().getWritableDatabase().update(DatabaseHelper.TABLE_CHAT_HISTORY, values, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID,
                    new String[]{protocolId, uniqueUserId, md.getNick(), md.getText().toString()});
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public void deleteLastMessage() {
        String WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID = WHERE_ACC_CONTACT_ID + " = ? AND " + DatabaseHelper.ROW_AUTO_ID + "= ?";
        SawimApplication.getDatabaseHelper().getWritableDatabase().delete(DatabaseHelper.TABLE_CHAT_HISTORY, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID,
                new String[]{protocolId, uniqueUserId, Integer.toString(getLastId())});
    }

    public void deleteText(MessData md) {
        SawimApplication.getDatabaseHelper().getWritableDatabase().delete(DatabaseHelper.TABLE_CHAT_HISTORY, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID,
                new String[]{protocolId, uniqueUserId, md.getNick(), md.getText().toString()});
    }

    public synchronized List<Integer> getSearchMessagesIds(String search) {
        List<Integer> ids = new ArrayList<>();
        if (search == null || search.isEmpty()) return ids;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null, null, null);
            if (cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    String messTxt = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MESSAGE));
                    if (messTxt.toLowerCase().contains(search)) {
                        ids.add(i);
                    }
                }
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return ids;
    }

    public synchronized long getMessageTime(boolean last) {
        long lastMessageTime = 0;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID,
                    new String[]{protocolId, uniqueUserId}, null, null, DatabaseHelper.DATE + (last ? " DESC" : " ASC"));
            if (cursor.moveToLast()) {
                do {
                    boolean isIncoming = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.INCOMING)) == 0;
                    int sendingState = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SENDING_STATE));
                    short rowData = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
                    boolean isMessage = (rowData & MessData.PRESENCE) == 0
                            && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                    if ((isMessage && sendingState == Message.NOTIFY_FROM_SERVER && !isIncoming) || isMessage) {
                        lastMessageTime = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.DATE));
                        break;
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return lastMessageTime;
    }

    public Cursor getLastCursor() {
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null,
                    null, null, null, null, null);
            cursor.moveToLast();
        } catch (Exception e) {
            DebugLog.panic(e);
        }
        return cursor;
    }

    public synchronized boolean hasLastMessage(Chat chat, Message message) {
        Contact contact = chat.getContact();
        boolean hasMessage = false;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID,
                    new String[]{protocolId, uniqueUserId}, null, null, DatabaseHelper.DATE + " DESC");
            if (cursor.moveToLast()) {
                do {
                    short rowData = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
                    MessData mess = Chat.buildMessage(contact, message, contact.isConference() ? message.getName() : chat.getFrom(message),
                            false, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
                    MessData messFromDataBase = buildMessage(chat, cursor);
                    boolean isMessage = (rowData & MessData.PRESENCE) == 0
                            && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                    if (isMessage) {
                        hasMessage = hasMessage(mess, messFromDataBase);
                        if (hasMessage) {
                            return true;
                        }
                    }
                } while (cursor.moveToPrevious());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return hasMessage;
    }

    private static boolean hasMessage(MessData mess, MessData messFromDataBase) {
        return mess.getNick().equals(messFromDataBase.getNick())
                && mess.getText().toString().equals(messFromDataBase.getText().toString());
    }

    public boolean addNextListMessages(List<MessData> messDataList, final Chat chat, int limit, int offset, boolean addedAtTheBeginning) {
        boolean isAdded;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID,
                    new String[]{protocolId, uniqueUserId}, null, null, DatabaseHelper.DATE + " DESC LIMIT " + limit + " OFFSET " + offset);
            if (addedAtTheBeginning) {
                isAdded = cursor.moveToFirst();
                if (isAdded) {
                    do {
                        MessData mess = buildMessage(chat, cursor);
                        messDataList.add(0, mess);
                    } while (cursor.moveToNext());
                }
            } else {
                isAdded = cursor.moveToLast();
                if (isAdded) {
                    do {
                        MessData mess = buildMessage(chat, cursor);
                        messDataList.add(mess);
                    } while (cursor.moveToPrevious());
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return isAdded;
    }

    private static MessData buildMessage(Chat chat, Cursor cursor) {
        Contact contact = chat.getContact();
        int sendingState = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SENDING_STATE));
        boolean isIncoming = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.INCOMING)) == 0;
        String from = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AUTHOR));
        String text = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MESSAGE));
        long date = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.DATE));
        short rowData = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
        PlainMessage message;
        if (isIncoming) {
            message = new PlainMessage(from, chat.getProtocol(), date, text, true);
        } else {
            message = new PlainMessage(chat.getProtocol(), contact.getUserId(), date, text);
        }
        MessData messData;
        if (rowData == 0) {
            messData = Chat.buildMessage(contact, message, contact.isConference() ? from : chat.getFrom(message),
                    false, isIncoming ? Chat.isHighlight(message.getProcessedText(), contact.getMyName()): false);
        } else if ((rowData & MessData.ME) != 0 || (rowData & MessData.PRESENCE) != 0) {
            messData = new MessData(contact, message.getNewDate(), text, from, rowData);
        } else {
            messData = Chat.buildMessage(contact, message, contact.isConference() ? from : chat.getFrom(message),
                    rowData, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        }
        if (!message.isIncoming() && !messData.isMe()) {
            messData.setIconIndex((byte) sendingState);
        }
        return messData;
    }

    public synchronized int getHistorySize() {
        Cursor cursor = null;
        int count = 0;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null, null, null);
            count = cursor.getCount();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return count;
    }

    public int getLastId() {
        int id = -1;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getWritableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID,
                    new String[]{protocolId, uniqueUserId}, null, null, DatabaseHelper.ROW_AUTO_ID + " DESC LIMIT 1");
            if (cursor.moveToFirst()) {
                id = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ROW_AUTO_ID));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return id;
    }

    public void removeHistory() {
        try {
            RosterHelper.getInstance().getProtocol(protocolId).getStorage().updateUnreadMessagesCount(protocolId, uniqueUserId, 0);
            SawimApplication.getDatabaseHelper().getWritableDatabase().delete(DatabaseHelper.TABLE_CHAT_HISTORY, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public static void saveUnreadMessages() {
        for (int i = ChatHistory.instance.getTotal() - 1; 0 <= i; --i) {
            Chat chat = ChatHistory.instance.chatAt(i);
            int unreadMessageCount = chat.getAllUnreadMessageCount();
            if (unreadMessageCount == 0) {
                if (!Options.getBoolean(JLocale.getString(R.string.pref_history))) {
                    HistoryStorage historyStorage = chat.getHistory();
                    historyStorage.removeHistory();
                }
            }
        }
    }
}