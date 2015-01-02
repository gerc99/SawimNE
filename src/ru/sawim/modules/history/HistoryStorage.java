package ru.sawim.modules.history;

import android.content.ContentValues;
import android.database.Cursor;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.io.SawimProvider;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

import java.util.List;

public class HistoryStorage {

    private static final String WHERE_ACC_CONTACT_ID = SawimProvider.ACCOUNT_ID + " = ? AND " + SawimProvider.CONTACT_ID + " = ?";
    private static final String WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID = SawimProvider.ACCOUNT_ID + " = ? AND " + SawimProvider.CONTACT_ID
            + "= ? AND " + SawimProvider.AUTHOR + "= ? AND " + SawimProvider.MESSAGE + " = ?";

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
            values.put(SawimProvider.ACCOUNT_ID, protocolId);
            values.put(SawimProvider.CONTACT_ID, uniqueUserId);
            values.put(SawimProvider.SENDING_STATE, iconIndex);
            values.put(SawimProvider.INCOMING, isIncoming ? 0 : 1);
            values.put(SawimProvider.AUTHOR, nick);
            values.put(SawimProvider.MESSAGE, text);
            values.put(SawimProvider.DATE, time);
            values.put(SawimProvider.ROW_DATA, rowData);
            SawimApplication.getContext().getContentResolver().insert(SawimProvider.HISTORY_RESOLVER_URI, values);
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public void updateText(MessData md) {
        try {
            ContentValues values = new ContentValues();
            values.put(SawimProvider.SENDING_STATE, md.getIconIndex());
            SawimApplication.getContext().getContentResolver()
                    .update(SawimProvider.HISTORY_RESOLVER_URI, values, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID,
                            new String[]{protocolId, uniqueUserId, md.getNick(), md.getText().toString()});
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public void deleteLastMessage() {
        String WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID = WHERE_ACC_CONTACT_ID + " = ? AND " + SawimProvider.ROW_AUTO_ID + "= ?";
        SawimApplication.getContext().getContentResolver()
                .delete(SawimProvider.HISTORY_RESOLVER_URI, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID,
                        new String[]{protocolId, uniqueUserId, Integer.toString(getLastId())});
    }

    public void deleteText(MessData md) {
        SawimApplication.getContext().getContentResolver()
                .delete(SawimProvider.HISTORY_RESOLVER_URI, WHERE_ACC_CONTACT_AUTHOR_MESSAGE_ID,
                        new String[]{protocolId, uniqueUserId, md.getNick(), md.getText().toString()});
    }

    public synchronized long getLastMessageTime() {
        long lastMessageTime = 0;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_RESOLVER_URI, null, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null);
            if (cursor.moveToLast()) {
                do {
                    boolean isIncoming = cursor.getInt(cursor.getColumnIndex(SawimProvider.INCOMING)) == 0;
                    int sendingState = cursor.getInt(cursor.getColumnIndex(SawimProvider.SENDING_STATE));
                    short rowData = cursor.getShort(cursor.getColumnIndex(SawimProvider.ROW_DATA));
                    boolean isMessage = (rowData & MessData.PRESENCE) == 0
                            && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                    if ((isMessage && sendingState == Message.NOTIFY_FROM_SERVER && !isIncoming) || isMessage) {
                        lastMessageTime = cursor.getLong(cursor.getColumnIndex(SawimProvider.DATE));
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
            cursor = SawimApplication.getContext().getContentResolver().query(SawimProvider.HISTORY_RESOLVER_URI, null,
                    null, null, null);
            cursor.moveToLast();
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return cursor;
    }

    public synchronized static boolean hasLastMessage(Chat chat, Message message) {
        Contact contact = chat.getContact();
        boolean hasMessage = false;
        Cursor cursor = null;
        try {
            MessData mess = Chat.buildMessage(contact, message, contact.isConference() ? message.getName() : chat.getFrom(message),
                    false, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
            cursor = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_RESOLVER_URI, null, WHERE_ACC_CONTACT_ID,
                            new String[]{chat.getProtocol().getUserId(), contact.getUserId()}, SawimProvider.ROW_AUTO_ID + " DESC LIMIT 1");
            if (cursor.moveToFirst()) {
                MessData messFromDataBase = buildMessage(chat, cursor);
                hasMessage = hasMessage(mess, messFromDataBase);
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

    public synchronized boolean addNextListMessages(List<MessData> messDataList, final Chat chat, int limit, int offset, boolean addedAtTheBeginning) {
        boolean isAdded;
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_RESOLVER_URI, null, WHERE_ACC_CONTACT_ID,
                            new String[]{protocolId, uniqueUserId}, SawimProvider.ROW_AUTO_ID + " DESC LIMIT " + limit + " OFFSET " + offset);
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
        int sendingState = cursor.getInt(cursor.getColumnIndex(SawimProvider.SENDING_STATE));
        boolean isIncoming = cursor.getInt(cursor.getColumnIndex(SawimProvider.INCOMING)) == 0;
        String from = cursor.getString(cursor.getColumnIndex(SawimProvider.AUTHOR));
        String text = cursor.getString(cursor.getColumnIndex(SawimProvider.MESSAGE));
        long date = Util.createLocalDate(Util.getLocalDateString(cursor.getLong(cursor.getColumnIndex(SawimProvider.DATE)), false));
        short rowData = cursor.getShort(cursor.getColumnIndex(SawimProvider.ROW_DATA));
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
            cursor = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_RESOLVER_URI, null, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId}, null);
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
            cursor = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_RESOLVER_URI, null, WHERE_ACC_CONTACT_ID,
                            new String[]{protocolId, uniqueUserId}, SawimProvider.ROW_AUTO_ID + " DESC LIMIT 1");
            if (cursor.moveToFirst()) {
                id = cursor.getInt(cursor.getColumnIndex(SawimProvider.ROW_AUTO_ID));
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
            updateUnreadMessagesCount(0);
            SawimApplication.getContext().getContentResolver()
                    .delete(SawimProvider.HISTORY_RESOLVER_URI, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
        } catch (Exception e) {
            DebugLog.panic(e);
        }
    }

    public void updateUnreadMessagesCount(int count) {
        Cursor c = null;
        try {
            c = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_UNREAD_MESSAGES_RESOLVER_URI, null, WHERE_ACC_CONTACT_ID,
                            new String[]{protocolId, uniqueUserId}, null);
            if (c.getCount() > 0) {
                ContentValues values = new ContentValues();
                values.put(SawimProvider.UNREAD_MESSAGES_COUNT, count);
                SawimApplication.getContext().getContentResolver()
                        .update(SawimProvider.HISTORY_UNREAD_MESSAGES_RESOLVER_URI, values, WHERE_ACC_CONTACT_ID, new String[]{protocolId, uniqueUserId});
            } else {
                ContentValues values = new ContentValues();
                values.put(SawimProvider.ACCOUNT_ID, protocolId);
                values.put(SawimProvider.CONTACT_ID, uniqueUserId);
                values.put(SawimProvider.UNREAD_MESSAGES_COUNT, count);
                SawimApplication.getContext().getContentResolver().insert(SawimProvider.HISTORY_UNREAD_MESSAGES_RESOLVER_URI, values);
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (c != null) {
                c.close();
            }
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

    public static void loadUnreadMessages() {
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getContext().getContentResolver()
                    .query(SawimProvider.HISTORY_UNREAD_MESSAGES_RESOLVER_URI, null, null, null, null);
            if (cursor.moveToFirst()) {
                do {
                    String account = cursor.getString(cursor.getColumnIndex(SawimProvider.ACCOUNT_ID));
                    String userId = cursor.getString(cursor.getColumnIndex(SawimProvider.CONTACT_ID));
                    short unreadMessageCount = cursor.getShort(cursor.getColumnIndex(SawimProvider.UNREAD_MESSAGES_COUNT));
                    if (unreadMessageCount == 0) {
                        continue;
                    }
                    Protocol protocol = RosterHelper.getInstance().getProtocol(account);
                    if (protocol != null) {
                        Contact contact = protocol.getItemByUID(userId);
                        if (contact == null) {
                            contact = protocol.createTempContact(userId);
                        }
                        Chat chat = protocol.getChat(contact);
                        chat.setOtherMessageCounter(unreadMessageCount);
                        contact.updateChatState(chat);
                    }
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            DebugLog.panic(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}