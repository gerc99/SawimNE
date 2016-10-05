package ru.sawim.modules.history;

import android.util.Log;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;
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
import ru.sawim.io.RosterStorage;
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
        addText(md.getId(), md.getIconIndex(), md.isIncoming(), md.getNick(), md.getText() == null ? "" : md.getText().toString(), md.getTime(), md.getRowData(), md.getServerMsgId());
    }

    public void addText(final List<MessData> messDataList) {
        Realm realm = RealmDb.realm();
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                for (MessData md : messDataList) {
                    Message message = new Message();
                    message.setContactId(uniqueUserId);
                    message.setMessageId(md.getId());
                    message.setIncoming(md.isIncoming());
                    message.setState(md.getIconIndex());
                    message.setAuthor(md.getNick());
                    message.setText(md.getText() == null ? "" : md.getText().toString());
                    message.setDate(md.getTime());
                    message.setData(md.getRowData());
                    realm.copyToRealmOrUpdate(message);
                }
            }
        });
        realm.close();
    }

    public void addText(final String id, final int iconIndex, final boolean isIncoming,
                                     final String nick, final String text, final long time, final short rowData, String serverMsgId) {
        Realm realm = RealmDb.realm();
        realm.executeTransaction(new Realm.Transaction() {
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
                realm.copyToRealmOrUpdate(message);
            }
        });
        realm.close();
    }

    public void updateState(final String messageId, final int state) {
        Realm realm = RealmDb.realm();
        realm.executeTransactionAsync(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                Message message = realm.where(Message.class).equalTo("contactId", uniqueUserId).equalTo("messageId", messageId).findFirst();
                message.setState(state);
            }
        });
        realm.close();
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
        List<Message> messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAllSorted("date", Sort.ASCENDING);
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
        List<Message> messages = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).findAllSorted("date", Sort.DESCENDING);
        boolean has = false;
        for (int i = messages.size() - 1; 0 <= i; --i) {
            Message localMessage = messages.get(i);
            short rowData = localMessage.getData();
            boolean isMessage = (rowData & MessData.PRESENCE) == 0 && (rowData & MessData.PROGRESS) == 0;
            if (isMessage) {
                if (msgText.equals(localMessage.getText()) && msgNick.equals(localMessage.getAuthor())) {
                    has = true;
                    break;
                }
            }
        }
        realmDb.close();
        return has;
    }

    public static List<Contact> getActiveContacts() {
        List<Contact> list = new ArrayList<>();
        Realm realmDb = RealmDb.realm();
        List<Message> messages = realmDb.where(Message.class).distinct("contactId");
        for (int i = messages.size() - 1; 0 <= i; --i) {
            Message localMessage = messages.get(i);
            String uniqueUserId = localMessage.getContactId();
            Protocol protocol = RosterHelper.getInstance().getProtocol();
            if (protocol != null) {
                Contact contact = protocol.getItemByUID(uniqueUserId);
                if (contact == null) {
                    ru.sawim.db.model.Contact localContact = realmDb.where(ru.sawim.db.model.Contact.class).contains("contactId", uniqueUserId).findFirst();
                    if (localContact != null) {
                        contact = RosterStorage.getContact(protocol, localContact);
                    } else {
                        contact = protocol.createTempContact(uniqueUserId, uniqueUserId.contains("/"));
                    }
                }
                if (contact != null) {
                    list.add(contact);
                }
            }
        }
        realmDb.close();
        return list;
    }

    public synchronized RealmResults<Message> getMessages(long timestamp) {
        Realm realmDb = RealmDb.realm();
        RealmQuery<Message> realmQuery;
        if (timestamp == 0) {
            realmQuery = realmDb.where(Message.class).equalTo("contactId", uniqueUserId);
        } else {
            realmQuery = realmDb.where(Message.class).equalTo("contactId", uniqueUserId).lessThan("date", timestamp);
        }
        final RealmResults<Message> messages = realmQuery.findAllSorted("date", Sort.DESCENDING);
        realmDb.close();
        return messages;
    }

    public synchronized List<MessData> addNextListMessages(final Chat chat, int limit, long timestamp) {
        List<MessData> list = new ArrayList<>();
        final RealmResults<Message> messages = getMessages(timestamp);
        List<Message> messagesList = messages.subList(0, Math.min(limit, messages.size()));
        for (int i = messagesList.size() - 1; 0 <= i; --i) {
            Message localMessage = messagesList.get(i);
            MessData messData = buildMessage(chat, localMessage);
            if (messData != null) {
                list.add(messData);
            }
        }
        return list;
    }

    public static MessData buildMessage(Chat chat, Message localMessage) {
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
                    false, localMessage.isIncoming() && Chat.isHighlight(message.getProcessedText(), contact.getMyName()), true);
        } else if ((rowData & MessData.PRESENCE) != 0 || (rowData & MessData.ME) != 0) {
            messData = new MessData(contact, message.getNewDate(), localMessage.getText(), localMessage.getAuthor(), rowData, false, true);
        } else {
            messData = Chat.buildMessage(contact, message, contact.isConference() ? localMessage.getAuthor() : chat.getFrom(message),
                    rowData, Chat.isHighlight(message.getProcessedText(), contact.getMyName()), true);
        }
        if (!message.isIncoming() && (messData != null && !messData.isMe())) {
            messData.setIconIndex(localMessage.getState());
        }
        return messData;
    }

    public static boolean isMessageExist(String userId, String messageId) {
        Realm realmDb = RealmDb.realm();
        Message message = realmDb.where(Message.class).equalTo("contactId", userId).equalTo("messageId", messageId).findFirst();
        boolean isMessageExist = message != null;
        realmDb.close();
        return isMessageExist;
    }

    public void addMessageToHistory(Contact contact, ru.sawim.chat.message.Message message, String from, boolean isSystemNotice) {
        addText(Chat.buildMessage(contact, message, from, isSystemNotice, Chat.isHighlight(message.getProcessedText(), contact.getMyName()), false));
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
            RosterStorage.updateFirstServerMsgId(contact);
            RosterHelper.getInstance().getProtocol().getStorage().updateUnreadMessagesCount(protocolId, uniqueUserId, (short) 0);
            Realm realmDb = RealmDb.realm();
            realmDb.executeTransaction(new Realm.Transaction() {
                @Override
                public void execute(Realm realm) {
                    realm.where(Message.class).equalTo("contactId", uniqueUserId).findAll().deleteAllFromRealm();
                }
            });
            realmDb.close();
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
