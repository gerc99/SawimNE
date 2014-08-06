package ru.sawim.modules.history;

import android.content.ContentValues;
import android.database.Cursor;
import protocol.Contact;
import ru.sawim.io.DatabaseHelper;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.Util;

import java.util.List;

public class HistoryStorage {

    private String protocolId;
    private String uniqueUserId;

    private static final String WHERE_ACC_CONTACT_ID = DatabaseHelper.ACCOUNT_ID + " = ? AND " + DatabaseHelper.CONTACT_ID + " = ?";
    private static final String WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID = DatabaseHelper.ACCOUNT_ID + " = ? AND " + DatabaseHelper.CONTACT_ID
            + "= ? AND " + DatabaseHelper.AUTHOR + "= ? AND " + DatabaseHelper.MESSAGE + " = ?";
    private static final String SELECT_COUNT = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_CHAT_HISTORY + " WHERE " + WHERE_ACC_CONTACT_ID;

    private HistoryStorage(String protocolId, String uniqueUserId) {
        this.protocolId = protocolId;
        this.uniqueUserId = uniqueUserId;
    }

    public static HistoryStorage getHistory(String protocolId, String uniqueUserId) {
        return new HistoryStorage(protocolId, uniqueUserId);
    }

    public void addText(MessData md) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.ACCOUNT_ID, protocolId);
        values.put(DatabaseHelper.CONTACT_ID, uniqueUserId);
        values.put(DatabaseHelper.SENDING_STATE, (int) md.getIconIndex());
        values.put(DatabaseHelper.INCOMING, md.isIncoming() ? 0 : 1);
        values.put(DatabaseHelper.AUTHOR, md.getNick());
        values.put(DatabaseHelper.MESSAGE, md.getText().toString());
        values.put(DatabaseHelper.DATE, md.getTime());
        values.put(DatabaseHelper.ROW_DATA, md.getRowData());
        SawimApplication.getDb().insert(DatabaseHelper.TABLE_CHAT_HISTORY, null, values);
    }

    public void updateText(MessData md) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.SENDING_STATE, (int) md.getIconIndex());
        SawimApplication.getDb().update(DatabaseHelper.TABLE_CHAT_HISTORY, values, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID, new String[]{protocolId, uniqueUserId, md.getNick(), md.getText().toString()});
    }

    public void deleteText(MessData md) {
        SawimApplication.getDb().delete(DatabaseHelper.TABLE_CHAT_HISTORY, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID, new String[]{protocolId, uniqueUserId, md.getNick(), md.getText().toString()});
    }

    public long getLastMessageTime() {
        long lastMessageTime = 0;
        Cursor cursor = SawimApplication.getDb().query(DatabaseHelper.TABLE_CHAT_HISTORY, null, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null, null, null);
        if (cursor.moveToLast()) {
            do {
                boolean isIncoming = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.INCOMING)) == 0;
                int sendingState = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SENDING_STATE));
                short rowData = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
                boolean isMessage = (rowData & MessData.PRESENCE) == 0 && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                if ((isMessage && sendingState == Message.NOTIFY_FROM_SERVER && !isIncoming) || isMessage) {
                    lastMessageTime = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.DATE));
                    break;
                }
            } while (cursor.moveToPrevious());
        }
        cursor.close();
        return lastMessageTime;
    }

    public MessData getLastMessage(Chat chat) {
        MessData lastMessage = null;
        Cursor cursor = SawimApplication.getDb().query(DatabaseHelper.TABLE_CHAT_HISTORY,
                null, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null, null, null);
        if (cursor.moveToLast()) {
            lastMessage = buildMessage(chat, cursor);
        }
        cursor.close();
        return lastMessage;
    }

    public static boolean hasMessage(Chat chat, Message message, int count) {
        String select = "SELECT * FROM (SELECT * FROM " + DatabaseHelper.TABLE_CHAT_HISTORY
                + " ORDER BY " + DatabaseHelper.ROW_AUTO_ID
                + " DESC LIMIT " + count + ") WHERE " + WHERE_ACC_CONTACT_ID
                + " ORDER BY " + DatabaseHelper.ROW_AUTO_ID + " DESC";
        MessData mess = chat.buildMessage(message, chat.getContact().isConference() ? message.getName() : chat.getFrom(message),
                false, Chat.isHighlight(message.getProcessedText(), chat.getContact().getMyName()));
        boolean hasMessage = false;
        Cursor cursor = SawimApplication.getDb().rawQuery(select, new String[]{chat.getProtocol().getUserId(), chat.getContact().getUserId()});
        if (cursor.moveToFirst()) {
            do {
                MessData messFromDataBase = buildMessage(chat, cursor);
                if (hasMessage(mess, messFromDataBase)) {
                    hasMessage = true;
                    break;
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        return hasMessage;
    }

    private static boolean hasMessage(MessData mess, MessData messFromDataBase) {
        return mess.getNick().equals(messFromDataBase.getNick())
                && mess.getText().toString().equals(messFromDataBase.getText().toString());
    }

    public void addNextListMessages(List<MessData> messDataList, final Chat chat, int limit, int offset, boolean addedAtTheBeginning) {
        final String selectCount = "SELECT * FROM " + DatabaseHelper.TABLE_CHAT_HISTORY
                + " WHERE " + WHERE_ACC_CONTACT_ID
                + " ORDER BY " + DatabaseHelper.ROW_AUTO_ID
                + " DESC LIMIT " + limit
                + " OFFSET " + offset;
        Cursor cursor = SawimApplication.getDb().rawQuery(selectCount, new String[]{protocolId, uniqueUserId});
        if (addedAtTheBeginning) {
            if (cursor.moveToFirst()) {
                do {
                    MessData mess = buildMessage(chat, cursor);
                    messDataList.add(0, mess);
                } while (cursor.moveToNext());
            }
        } else {
            if (cursor.moveToLast()) {
                do {
                    MessData mess = buildMessage(chat, cursor);
                    messDataList.add(mess);
                } while (cursor.moveToPrevious());
            }
        }
        cursor.close();
    }

    private static MessData buildMessage(Chat chat, Cursor cursor) {
        Contact contact = chat.getContact();
        int sendingState = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SENDING_STATE));
        boolean isIncoming = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.INCOMING)) == 0;
        String from = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AUTHOR));
        String text = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MESSAGE));
        long date = Util.createLocalDate(Util.getLocalDateString(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.DATE)), false));
        short rowData = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
        PlainMessage message;
        if (isIncoming) {
            message = new PlainMessage(from, chat.getProtocol(), date, text, true);
        } else {
            message = new PlainMessage(chat.getProtocol(), contact, date, text);
        }
        MessData messData;
        if (rowData == 0) {
            messData = chat.buildMessage(message, contact.isConference() ? from : chat.getFrom(message),
                    false, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        } else if ((rowData & MessData.ME) != 0 || (rowData & MessData.PRESENCE) != 0) {
            messData = new MessData(contact, message.getNewDate(), text, from, rowData, false);
        } else {
            messData = chat.buildMessage(message, contact.isConference() ? from : chat.getFrom(message),
                    rowData, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        }
        if (!message.isIncoming() && !messData.isMe()) {
            messData.setIconIndex((byte) sendingState);
        }
        return messData;
    }

    public int getHistorySize() {
        int num = 0;
        Cursor cursor = SawimApplication.getDb().rawQuery(SELECT_COUNT, new String[]{protocolId, uniqueUserId});
        if (cursor.moveToFirst()) {
            num = cursor.getInt(0);
        }
        cursor.close();
        return num;
    }

    public void removeHistory() {
        SawimApplication.getDb().delete(DatabaseHelper.TABLE_CHAT_HISTORY, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
    }
}
