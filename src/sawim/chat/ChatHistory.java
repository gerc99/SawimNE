package sawim.chat;

import android.graphics.drawable.BitmapDrawable;
import android.util.Log;
import protocol.Contact;
import protocol.Protocol;
import sawim.chat.message.Message;
import sawim.chat.message.PlainMessage;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.io.Storage;
import sawim.roster.RosterHelper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.*;

public final class ChatHistory {
    public final List<Chat> historyTable = new ArrayList<Chat>();
    public static final ChatHistory instance = new ChatHistory();

    private ChatHistory() {
        //super(JLocale.getString("chats"));
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
            return;
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
        if (!item.message.equals("")) return;
        historyTable.remove(item);
        item.clear();
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
                historyTable.remove(key);
                key.clear();
                key.getContact().updateChatState(null);
            }
        }
        RosterHelper.getInstance().markMessages(null);
    }

    private void removeChat(Chat chat) {
        if (null != chat) {
            clearChat(chat);
            //if (SawimApplication.getSawim().getDisplay().remove(chat)) {
            //    RosterHelper.getSawimActivity()._setActiveContact(null);
            //}
            //setCurrentItemIndex(getCurrItem());
            //invalidate();
        }
        if (0 < getSize()) {
            //restore();
        } else {
            RosterHelper.getInstance().updateRoster();
        }
    }

    private void clearChat(Chat chat) {
        //if (chat.isHuman() && !chat.getContact().isTemp()) {
        //    chat.removeReadMessages();
        //} else {
        unregisterChat(chat);
        //}
    }

    public void removeAll(Chat except) {
        for (int i = getTotal() - 1; 0 <= i; --i) {
            Chat chat = chatAt(i);
            if (except == chat) continue;
            clearChat(chat);
        }
        //setCurrentItemIndex(getCurrItem());
        if (0 < getSize()) {
            //    restore();

        } else {
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
                Contact newContact = p.getItemByUIN(contact.getUserId());
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

    public void updateChatList() {
        //invalidate();
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

    public String getLastMessage(String defMess) {
        Chat current = chatAt(getPreferredItem());
        if (current != null) {
            MessData md = current.getMessageDataByIndex(current.getMessData().size() - 1);
            return md.getNick() + "\n " + md.getText();
        }
        return defMess;
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

    /*private Chat getSelectedChat() {
        return (getCurrItem() < getSize()) ? chatAt(getCurrItem()) : null;
    }
    protected final void doKeyReaction(int keyCode, int actionCode, int type) {
        Chat chat = getSelectedChat();
        if ((KEY_PRESSED == type) && (NativeCanvas.CLEAR_KEY == keyCode)) {
            removeChat(chat);
            return;
        }
        if ((KEY_REPEATED == type) || (KEY_PRESSED == type)) {
            switch (actionCode) {
                case NativeCanvas.NAVIKEY_DOWN:
                    setCurrentItemIndex((getCurrItem() + 1) % getSize());
                    invalidate();
                    return;
                case NativeCanvas.NAVIKEY_UP:
                    setCurrentItemIndex((getCurrItem() + getSize() - 1) % getSize());
                    invalidate();
                    return;
            }
        }

        if (Clipboard.execHotKey(null == chat ? null : chat.getProtocol(),
                null == chat ? null : chat.getContact(), keyCode, type)) {
            return;
        }
        super.doKeyReaction(keyCode, actionCode, type);
    }

    public void showChatList(boolean forceGoToChat) {
        if (forceGoToChat) {
            forceGoToChat();
        }
        setCurrentItemIndex(getPreferredItem());
        show();
    }

	public void forceGoToChat() {
        if (getTotal() <= 0) return;
		Chat current = chatAt(getPreferredItem());
        if (0 < current.getUnreadMessageCount()) {
            current.updateRoster();
            return;
        }
	}
    
    public void showNextPrevChat(Chat item, boolean next) {
        int chatNum = historyTable.indexOf(item);
        if (-1 == chatNum) {
            return;
        }
        int nextChatNum = (chatNum + (next ? 1 : -1) + getTotal()) % getTotal();
        chatAt(nextChatNum).updateRoster();
    }

    private static final int MENU_SELECT = 1;
    private static final int MENU_DEL_CURRENT_CHAT = 2;
    private static final int MENU_DEL_ALL_CHATS_EXCEPT_CUR = 3;
    private static final int MENU_DEL_ALL_CHATS = 4;

    protected void doSawimAction(int action) {
        switch (action) {
            case NativeCanvas.Sawim_SELECT:
                getSelectedChat().updateRoster();
                return;

            case NativeCanvas.Sawim_BACK:
                back();
                return;

            case NativeCanvas.Sawim_MENU:
                showMenu(getMenu());
                return;
        }
        Chat chat = getSelectedChat();
        switch (action) {
            case MENU_SELECT:
                execSawimAction(NativeCanvas.Sawim_SELECT);
                return;

            case MENU_DEL_CURRENT_CHAT:
                removeChat(chat);
                break;

            case MENU_DEL_ALL_CHATS_EXCEPT_CUR:
                removeAll(chat);
                break;

            case MENU_DEL_ALL_CHATS:
                removeAll(null);
                break;
        }
    }

    protected final MenuModel getMenu() {
        MenuModel menu = new MenuModel();
        if (0 < getSize()) {
            menu.addItem("onContextItemSelected",                  MENU_SELECT);
            menu.addItem("delete_chat",             MENU_DEL_CURRENT_CHAT);
            menu.addItem("all_contact_except_this", MENU_DEL_ALL_CHATS_EXCEPT_CUR);
            menu.addItem("all_contacts",            MENU_DEL_ALL_CHATS);
        }
        menu.setActionListener(new Binder(this));
        return menu;
    }

    protected void drawItemData(GraphicsEx g, int index, int x, int y, int w, int h, int skip, int to) {
        for (int i = 0; i < leftIcons.length; ++i) {
            leftIcons[i] = null;
        }
        g.setThemeColor(THEME_TEXT);
        g.setFont(getDefaultFont());
        Chat chat = (Chat)historyTable.elementAt(index);
        chat.getContact().getLeftIcons(leftIcons);
        g.drawString(leftIcons, chat.getContact().getName(), null, x, y, w, h);
    }*/

    protected int getSize() {
        return historyTable.size();
    }

    public void saveUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.delete();
            s.open(true);
            for (int i = getTotal() - 1; 0 <= i; --i) {
                Chat chat = chatAt(i);

                int count = chat.getUnreadMessageCount();
                for (int j = 0; j < count; ++j) {
                    MessData message = chat.getUnreadMessage(j);
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    DataOutputStream o = new DataOutputStream(out);
                    o.writeUTF(chat.getProtocol().getUserId());
                    o.writeUTF(chat.getContact().getUserId());
                    o.writeUTF(message.getNick());
                    o.writeUTF(message.getText().toString());
                    o.writeLong(message.getTime());
                    s.addRecord(out.toByteArray());
                }
            }
        } catch (Exception ignored) {
            Log.e("saveUnreadMessages", ignored.getMessage());
        }
        s.close();
    }

    public void loadUnreadMessages() {
        Storage s = new Storage("unread");
        try {
            s.open(false);
            for (int i = 1; i <= s.getNumRecords(); ++i) {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(s.getRecord(i)));
                String account = in.readUTF();
                String userId = in.readUTF();
                String nick = in.readUTF();
                String text = in.readUTF();
                long time = in.readLong();
                Protocol protocol = RosterHelper.getInstance().getProtocol(account);
                if (null == protocol) {
                    continue;
                }
                PlainMessage msg = new PlainMessage(userId, protocol, time, text, true);
                if (!StringConvertor.isEmpty(nick)) {
                    msg.setName(nick);
                }
                protocol.addMessage(msg, true);
            }
        } catch (Exception ignored) {
        }
        s.close();
        s.delete();
    }
}