package ru.sawim.modules.history;

import android.util.Log;

import io.realm.Realm;
import io.realm.Sort;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.chat.MessData;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.db.RealmDb;
import ru.sawim.db.model.Message;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryStorage {

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
        addText(md.getId(), md.getIconIndex(), md.isIncoming(), md.getNick(), md.getText().toString(), md.getTime(), md.getRowData(), md.getServerMsgId());
    }

    public void addText(final String id, final int iconIndex, final boolean isIncoming,
                                     final String nick, final String text, final long time, final short rowData, String serverMsgId) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Message message = new Message();
                message.setContactId(uniqueUserId);
                message.setMessageId(id);
                message.setIncoming(isIncoming);
                message.setState(iconIndex);
                message.setAuthor(nick);
                message.setText(text);
                message.setDate(time);
                message.setData(rowData);
                realm.copyToRealm(message);
                Log.e("save message", text+" "+id);
            }
        });
        realm.close();
    }

    public void updateState(final String messageId, final int state) {
        RealmDb.realm().executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Message message = new Message();
                message.setContactId(uniqueUserId);
                message.setMessageId(messageId);
                message.setState(state);
                realm.copyToRealmOrUpdate(message);
            }
        });
    }

    public List<Integer> getSearchMessagesIds(String search) {
        List<Integer> ids = new ArrayList<>();
        if (search == null || search.isEmpty()) return ids;
        Realm realmDb = RealmDb.realm();
        List<Message> messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAll();
        for (int i = messages.size() - 1; 0 <= i; --i) {
            if (messages.get(i).getText().toLowerCase().contains(search)) {
                ids.add(i);
            }
        }
        realmDb.close();
        return ids;
    }

    public synchronized long getMessageTime(boolean last) {
        long lastMessageTime = 0;
        Realm realmDb = RealmDb.realm();
        List<Message> messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAll();
        if (last) {
            for (int i = messages.size() - 1; 0 <= i; --i) {
                Message message = messages.get(i);
                short rowData = message.getData();
                boolean isMessage = (rowData & MessData.PRESENCE) == 0
                        && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                if ((isMessage && message.getState() == ru.sawim.chat.message.Message.NOTIFY_FROM_SERVER && !message.isIncoming()) || isMessage) {
                    lastMessageTime = message.getDate();
                    break;
                }
            }
        } else {
            for (int i = 0; i < messages.size(); i++) {
                Message message = messages.get(i);
                short rowData = message.getData();
                boolean isMessage = (rowData & MessData.PRESENCE) == 0
                        && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                if ((isMessage && message.getState() == ru.sawim.chat.message.Message.NOTIFY_FROM_SERVER && !message.isIncoming()) || isMessage) {
                    lastMessageTime = message.getDate();
                    break;
                }
            }
        }
        realmDb.close();
        return lastMessageTime;
    }

    public synchronized boolean hasLastMessage(Chat chat, ru.sawim.chat.message.Message message) {
        String msgNick = chat.getFrom(message);
        String msgText = MessData.formatCmdMe(msgNick, message.getText());
        Realm realmDb = RealmDb.realm();
        List<Message> messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAll();
        for (int i = messages.size() - 1; 0 <= i; --i) {
            Message localMessage = messages.get(i);
            short rowData = localMessage.getData();
            boolean isMessage = (rowData & MessData.PRESENCE) == 0 && (rowData & MessData.PROGRESS) == 0;
            if (isMessage) {
                if (msgText.equals(localMessage.getText()) && msgNick.equals(localMessage.getAuthor())) {
                    return true;
                }
            }
        }
        realmDb.close();
        return false;
    }

    public static List<Contact> getActiveContacts() {
        List<Contact> list = new ArrayList<>();
        Realm realmDb = RealmDb.realm();
        List<Message> messages = realmDb.where(Message.class).findAll();
        for (int i = messages.size() - 1; 0 <= i; --i) {
            Message localMessage = messages.get(i);
            String uniqueUserId = localMessage.getContactId();
            Protocol protocol = RosterHelper.getInstance().getProtocol();
            if (protocol != null) {
                Contact contact = protocol.getItemByUID(uniqueUserId);
                if (contact == null) {
                    contact = protocol.createContact(uniqueUserId, uniqueUserId, false);
                }
                if (contact != null) {
                    list.add(contact);
                }
            }
        }
        realmDb.close();
        return list;
    }

    public List<MessData> addNextListMessages(final Chat chat, int limit, long timestamp) {
        List<MessData> list = new ArrayList<>();
        List<Message> messages;
        Realm realmDb = RealmDb.realm();
        if (timestamp == 0) {
            messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAllSorted("date", Sort.DESCENDING);
        } else {
            messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).lessThan("date", timestamp).findAllSorted("date", Sort.DESCENDING);
        }
        if (messages != null) {
            messages = messages.subList(0, Math.min(limit, messages.size()));
            for (int i = messages.size() - 1; 0 <= i; --i) {
                Message localMessage = messages.get(i);
                MessData messData = buildMessage(chat, localMessage);
                if (messData != null) {
                    list.add(messData);
                }
            }
        }
        realmDb.close();
        return list;
    }

    private static MessData buildMessage(Chat chat, Message localMessage) {
        Contact contact = chat.getContact();
        short rowData = localMessage.getData();
        //String serverMsgId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SERVER_MSG_ID));
        PlainMessage message;
        if (localMessage.isIncoming()) {
            message = new PlainMessage(localMessage.getAuthor(), RosterHelper.getInstance().getProtocol(), localMessage.getMessageId(), localMessage.getDate(), localMessage.getText(), true);
        } else {
            message = new PlainMessage(RosterHelper.getInstance().getProtocol(), contact.getUserId(), localMessage.getMessageId(), localMessage.getDate(), localMessage.getText());
        }
        //message.setServerMsgId(serverMsgId);
        MessData messData;
        if (rowData == 0) {
            messData = Chat.buildMessage(contact, message, contact.isConference() ? localMessage.getAuthor() : chat.getFrom(message),
                    false, localMessage.isIncoming() && Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        } else if ((rowData & MessData.PRESENCE) != 0 || (rowData & MessData.ME) != 0) {
            messData = new MessData(contact, message.getNewDate(), localMessage.getText(), localMessage.getAuthor(), rowData, false);
        } else {
            messData = Chat.buildMessage(contact, message, contact.isConference() ? localMessage.getAuthor() : chat.getFrom(message),
                    rowData, Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        }
        if (!message.isIncoming() && !messData.isMe()) {
            messData.setIconIndex(localMessage.getState());
        }
        return messData;
    }

    /*public static List<PlainMessage> getNotSendedMessages() {
        List<PlainMessage> list = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = SawimApplication.getDatabaseHelper().getReadableDatabase().query(DatabaseHelper.TABLE_CHAT_HISTORY, null,
                    DatabaseHelper.INCOMING + " = 1 AND " + DatabaseHelper.SENDING_STATE + " = -1", null, null, null, null, null);
            if (cursor.moveToLast()) {
                do {
                    String protocolId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ACCOUNT_ID));
                    String uniqueUserId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CONTACT_ID));
                    String messId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.ID));
                    int sendingState = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.SENDING_STATE));
                    boolean isIncoming = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.INCOMING)) == 0;
                    String from = cursor.getString(cursor.getColumnIndex(DatabaseHelper.AUTHOR));
                    String text = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MESSAGE));
                    long date = cursor.getLong(cursor.getColumnIndex(DatabaseHelper.DATE));
                    short rowData = cursor.getShort(cursor.getColumnIndex(DatabaseHelper.ROW_DATA));
                    //String serverMsgId = cursor.getString(cursor.getColumnIndex(DatabaseHelper.SERVER_MSG_ID));
                    boolean isMessage = (rowData & MessData.PRESENCE) == 0
                            && (rowData & MessData.SERVICE) == 0 && (rowData & MessData.PROGRESS) == 0;
                    if (isMessage) {
                        Protocol protocol = RosterHelper.getInstance().getProtocol();
                        PlainMessage message = new PlainMessage(protocol, uniqueUserId, date, text);
                        message.setMessageId(messId);
                        list.add(message);
                    }
                } while (cursor.moveToPrevious());
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return list;
    }*/

    public static boolean isMessageExist(String id) {
        Realm realmDb = RealmDb.realm();
        boolean isMessageExist = realmDb.where(Message.class).equalTo("messageId", id).findFirst() != null;
        realmDb.close();
        return isMessageExist;
    }

    public void addMessageToHistory(Contact contact, ru.sawim.chat.message.Message message, String from, boolean isSystemNotice) {
        addText(Chat.buildMessage(contact, message, from, isSystemNotice, Chat.isHighlight(message.getProcessedText(), contact.getMyName())));
    }

    public int getHistorySize() {
        Realm realmDb = RealmDb.realm();
        int count = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAll().size();
        realmDb.close();
        return count;
    }

    public void removeHistory() {
        try {
            Contact contact = RosterHelper.getInstance().getProtocol().getItemByUID(uniqueUserId);
            contact.firstServerMsgId = "";
            //RosterStorage.updateFirstServerMsgId(contact);
            RosterHelper.getInstance().getProtocol().getStorage().updateUnreadMessagesCount(protocolId, uniqueUserId, (short) 0);
            Realm realmDb = RealmDb.realm();
            realmDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(Message.class).equalTo("contactId", uniqueUserId).findAll().deleteAllFromRealm();
                }
            });
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
