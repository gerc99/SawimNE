package ru.sawim.chat;

import android.graphics.drawable.BitmapDrawable;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.chat.message.Message;
import ru.sawim.comm.Util;
import ru.sawim.io.Storage;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public final class ChatHistory {
    public final List<Chat> historyTable = new ArrayList<Chat>();
    public static final ChatHistory instance = new ChatHistory();

    private ChatHistory() {
    }

    public int getTotal() {
        return historyTable.size();
    }

    public void sort() {
        Util.sortChats(historyTable);
    }

    public void addLayerToListOfChats(Protocol p, List<Object> items) {
        boolean hasLayer = false;
        items.add(p.getUserId());
        for (int i = 0; i < ChatHistory.instance.historyTable.size(); ++i) {
            Chat chat = ChatHistory.instance.chatAt(i);
            if (chat.getProtocol() == p) {
                items.add(chat);
                hasLayer = true;
            }
        }
        if (RosterHelper.getInstance().getProtocolCount() == 1) {
            items.remove(0);
            return;
        }
        if (!hasLayer) {
            items.remove(items.size() - 1);
        }
    }

    public Chat chatAt(int index) {
        if ((index < historyTable.size()) && (index >= 0))
            return historyTable.get(index);
        return null;
    }

    public Contact contactAt(int index) {
        return chatAt(index).getContact();
    }

    public Chat getChat(Contact c) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            if (c == contactAt(i)) {
                return chatAt(i);
            }
        }
        return null;
    }

    public Chat getChatById(String id) {
        int size = historyTable.size();
        for (int i = 0; i < size; ++i) {
            Chat chat = chatAt(i);
            if (chat.getContact().getUserId() == id) {
                return chat;
            }
        }
        return null;
    }

    public int getUnreadMessageCount() {
        int count = 0;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            count += chatAt(i).getUnreadMessageCount();
        }
        return count;
    }

    public int getPersonalUnreadMessageCount(boolean all) {
        int count = 0;
        Chat chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (all || chat.isHuman() || !chat.getContact().isSingleUserContact()) {
                count += chat.getPersonalUnreadMessageCount();
            }
        }
        return count;
    }

    public int getOtherMessageCount() {
        int count = 0;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            count += chatAt(i).getOtherMessageCount();
        }
        return count;
    }

    private byte getMoreImportant(int v1, int v2) {
        if ((Message.ICON_IN_MSG_HI == v1) || (Message.ICON_IN_MSG_HI == v2)) {
            return Message.ICON_IN_MSG_HI;
        }
        if ((Message.ICON_SYSREQ == v1) || (Message.ICON_SYSREQ == v2)) {
            return Message.ICON_SYSREQ;
        }
        if ((Message.ICON_IN_MSG == v1) || (Message.ICON_IN_MSG == v2)) {
            return Message.ICON_IN_MSG;
        }
        if ((Message.ICON_SYS_OK == v1) || (Message.ICON_SYS_OK == v2)) {
            return Message.ICON_SYS_OK;
        }
        return -1;
    }

    public BitmapDrawable getUnreadMessageIcon() {
        byte icon = -1;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            icon = getMoreImportant(icon, chatAt(i).getNewMessageIcon());
        }
        return Message.getIcon(icon);
    }

    public BitmapDrawable getUnreadMessageIcon(Protocol p) {
        byte icon = -1;
        Chat chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (chat.getProtocol() == p) {
                icon = getMoreImportant(icon, chat.getNewMessageIcon());
            }
        }
        return Message.getIcon(icon);
    }

    public BitmapDrawable getUnreadMessageIcon(Vector contacts) {
        byte icon = -1;
        Contact c;
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            c = (Contact) contacts.elementAt(i);
            icon = getMoreImportant(icon, c.getUnreadMessageIcon());
        }
        return Message.getIcon(icon);
    }

    public void registerChat(Chat item) {
        if (!contains(historyTable, item.getContact().getUserId())) {
            historyTable.add(item);
            item.getContact().updateChatState(item);
        }
    }

    private static boolean contains(List<Chat> list, String id) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            if (list.get(i).getContact().getUserId() == id) {
                return true;
            }
        }
        return false;
    }

    public void unregisterChat(Chat item) {
        if (null == item) return;
        if (item.message != null) return;
        closeHistory(item);
        historyTable.remove(item);
        Contact c = item.getContact();
        c.updateChatState(null);
        item.getProtocol().ui_updateContact(c);
        if (0 < item.getUnreadMessageCount()) {
            RosterHelper.getInstance().markMessages(c);
        }
    }

    public void unregisterChats(Protocol p) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Chat key = chatAt(i);
            if (key.getProtocol() == p) {
                closeHistory(key);
                historyTable.remove(key);
                key.getContact().updateChatState(null);
            }
        }
        RosterHelper.getInstance().markMessages(null);
    }

    private void closeHistory(Chat chat) {
        HistoryStorage historyStorage = chat.getHistory();
        boolean hasHistory = historyStorage != null && historyStorage.getHistorySize() > 0;
        if (hasHistory) {
            historyStorage.closeHistory();
        }
    }

    private void clearChat(Chat chat) {
        unregisterChat(chat);
    }

    public void removeAll(Chat except) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Chat chat = chatAt(i);
            if (except == chat) continue;
            clearChat(chat);
        }
        if (0 == getSize()) {
            RosterHelper.getInstance().updateRoster();
        }
    }

    public void restoreContactsWithChat(Protocol p) {
        int total = getTotal();
        for (int i = 0; i < total; ++i) {
            Contact contact = contactAt(i);
            Chat chat = chatAt(i);
            if (p != chat.getProtocol()) {
                continue;
            }
            if (!p.inContactList(contact)) {
                Contact newContact = p.getItemByUID(contact.getUserId());
                if (null != newContact) {
                    chat.setContact(newContact);
                    contact.updateChatState(null);
                    newContact.updateChatState(chat);
                    continue;
                }
                if (contact.isSingleUserContact()) {
                    contact.setTempFlag(true);
                    contact.setGroup(null);
                } else {
                    if (null == p.getGroup(contact)) {
                        contact.setGroup(p.getGroup(contact.getDefaultGroupName()));
                    }
                }
                p.addTempContact(contact);
            }
        }
    }

    public int getItemChat(Contact currentContact) {
        int current = 0;
        for (int i = 0; i < historyTable.size(); ++i) {
            Chat chat = chatAt(i);
            if (currentContact.getUserId() == chat.getContact().getUserId()) {
                current = i;
            }
        }
        return current;
    }

    public CharSequence getLastMessage(CharSequence defMess) {
        Chat current = chatAt(getPreferredItem());
        if (current != null) {
            HistoryStorage historyStorage = current.getHistory();
            boolean hasHistory = historyStorage != null && historyStorage.getHistorySize() > 0;
            if (hasHistory) {
                MessData md = historyStorage.getLastMessage(current);
                return md.getText();
            }
        }
        return defMess;
    }

    public String getLastMessageNick() {
        Chat current = chatAt(getPreferredItem());
        if (current != null) {
            HistoryStorage historyStorage = current.getHistory();
            boolean hasHistory = historyStorage != null && historyStorage.getHistorySize() > 0;
            if (hasHistory) {
                MessData md = historyStorage.getLastMessage(current);
                return md.getNick();
            }
        }
        return null;
    }

    public int getPreferredItem() {
        for (int i = 0; i < historyTable.size(); ++i) {
            if (0 < chatAt(i).getPersonalUnreadMessageCount()) {
                return i;
            }
        }
        //Contact currentContact = RosterHelper.getSawimActivity().getCurrentContact();
        int current = -1;
        for (int i = 0; i < historyTable.size(); ++i) {
            Chat chat = chatAt(i);
            if (0 < chat.getUnreadMessageCount()) {
                return i;
            }
            //if (currentContact == chat.getContact()) {
            //    current = i;
            //}
        }
        return current;
    }

    protected int getSize() {
        return historyTable.size();
    }

    public void saveUnreadMessages() {
        Storage.delete("unread");
        Storage s = new Storage("unread");
        try {
            s.open();
            for (int i = getTotal() - 1; 0 <= i; --i) {
                Chat chat = chatAt(i);
                int unreadMessageCount = chat.getUnreadMessageCount();
                if (unreadMessageCount == 0 && !Options.getBoolean(Options.OPTION_HISTORY)) {
                    HistoryStorage historyStorage = chat.getHistory();
                    boolean hasHistory = historyStorage != null && historyStorage.getHistorySize() > 0;
                    if (hasHistory) {
                        historyStorage.removeHistory();
                    }
                }
                if (unreadMessageCount > 0) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataOutputStream o = new DataOutputStream(out);
                    o.writeUTF(chat.getProtocol().getUserId());
                    o.writeUTF(chat.getContact().getUserId());
                    o.writeShort(unreadMessageCount);
                    s.addRecord(out.toByteArray());
                }
            }
        } catch (Exception e) {
        }
        s.close();
    }

    public void loadUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.open();
            for (int i = 1; i <= s.getNumRecords(); ++i) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(s.getRecord(i)));
                String account = in.readUTF();
                String userId = in.readUTF();
                short unreadMessageCount = in.readShort();
                Protocol protocol = RosterHelper.getInstance().getProtocol(account);
                Contact contact = protocol.getItemByUID(userId);
                Chat chat = protocol.getChat(contact);
                chat.setMessageCounter(unreadMessageCount);
                contact.updateChatState(chat);
            }
        } catch (Exception ignored) {
        }
        s.close();
        Storage.delete("unread");
    }
}