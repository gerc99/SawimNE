package ru.sawim.chat;

import android.graphics.drawable.BitmapDrawable;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.chat.message.Message;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.Util;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

import java.util.*;

public final class ChatHistory {

    public static final ChatHistory instance = new ChatHistory();

    public final List<Chat> historyTable = new ArrayList<Chat>();

    private ChatHistory() {
    }

    public int getTotal() {
        return historyTable.size();
    }

    public void sort() {
        Collections.sort(historyTable, new Comparator<Chat>() {
            @Override
            public int compare(Chat c1, Chat c2) {
                return Util.compareNodes(c1.getContact(), c2.getContact());
            }
        });
    }

    public void addLayerToListOfChats(Protocol p, List<Object> items) {
        boolean hasLayer = false;
        items.add(p.getUserId());
        for (int i = 0; i < historyTable.size(); ++i) {
            Chat chat = chatAt(i);
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

    public Chat getChat(String id) {
        if (id == null) return null;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            if (id.equals(contactAt(i).getUserId())) {
                return chatAt(i);
            }
        }
        return null;
    }

    public int getUnreadMessageCount() {
        int count = 0;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            count += chatAt(i).getAllUnreadMessageCount();
        }
        return count;
    }

    public int getPersonalUnreadMessageCount(boolean all) {
        int count = 0;
        Chat chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (all || chat.isHuman() || !chat.getContact().isSingleUserContact()) {
                count += chat.getPersonalAndSysnoticeAndAuthUnreadMessageCount();
            }
        }
        return count;
    }

    public int getPreferredItem() {
        int current = -1;
        for (int i = 0; i < getTotal(); ++i) {
            Chat chat = chatAt(i);
            if (0 < chat.getAllUnreadMessageCount()) {
                current = i;
                break;
            }
        }
        return current;
    }

    private static final int[] SORTED_ICON_TYPES = new int[] {Message.ICON_IN_MSG_HI, Message.ICON_SYSREQ, Message.ICON_IN_MSG, Message.ICON_SYS_OK};
    private int getMoreImportant(int v1, int v2) {
        for (int iconType : SORTED_ICON_TYPES) {
            if (iconType == v1 || iconType == v2) {
                return iconType;
            }
        }
        return Message.ICON_NONE;
    }

    public BitmapDrawable getLastUnreadMessageIcon() {
        int icon = Message.ICON_NONE;
        /*for (int i = getTotal() - 1; 0 <= i; --i) {
            icon = getMoreImportant(icon, chatAt(i).getNewMessageIcon());
        }*/

        Chat chat = chatAt(getPreferredItem());
        if (chat != null) {
            icon = chat.getNewMessageIcon();
        }
        return Message.getIcon(icon);
    }

    public BitmapDrawable getUnreadMessageIcon(Contact contact) {
        int icon = Message.ICON_NONE;
        Chat chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (chat != null) {
                if (contact == chat.getContact()) {
                    icon = getMoreImportant(icon, chat.getNewMessageIcon());
                }
            }
        }
        return Message.getIcon(icon);
    }

    public BitmapDrawable getUnreadMessageIcon(Protocol p) {
        int icon = Message.ICON_NONE;
        Chat chat;
        for (int i = getTotal() - 1; 0 <= i; --i) {
            chat = chatAt(i);
            if (chat != null) {
                if (chat.getProtocol() == p) {
                    icon = getMoreImportant(icon, chat.getNewMessageIcon());
                }
            }
        }
        return Message.getIcon(icon);
    }

    public BitmapDrawable getUnreadMessageIcon(Vector contacts) {
        int icon = Message.ICON_NONE;
        Chat chat;
        for (int i = contacts.size() - 1; 0 <= i; --i) {
            chat = getChat((Contact) contacts.elementAt(i));
            if (chat != null) {
                icon = getMoreImportant(icon, chat.getNewMessageIcon());
            }
        }
        return Message.getIcon(icon);
    }

    public void registerChat(Chat item) {
        if (!contains(historyTable, item.getContact())) {
            historyTable.add(item);
            item.getContact().updateChatState(item);
        }
    }

    private static boolean contains(List<Chat> list, Contact contact) {
        int size = list.size();
        for (int i = 0; i < size; ++i) {
            if (list.get(i).getContact() == contact) {
                return true;
            }
        }
        return false;
    }

    public void unregisterChat(Chat item) {
        if (null == item) return;
        if (item.savedMessage != null) return;
        closeHistory(item);
        historyTable.remove(item);
        Contact c = item.getContact();
        c.updateChatState(null);
        item.getProtocol().ui_updateContact(c);
        if (0 < item.getAllUnreadMessageCount()) {
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
        if (!Options.getBoolean(JLocale.getString(R.string.pref_history))) {
            historyStorage.removeHistory();
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
        if (0 == getTotal()) {
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
}