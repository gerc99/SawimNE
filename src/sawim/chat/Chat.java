


package sawim.chat;

import DrawControls.icons.Icon;
import ru.sawim.activities.ChatActivity;
import ru.sawim.activities.SawimActivity;
import sawim.SawimUI;
import sawim.Options;
import sawim.chat.message.Message;
import sawim.chat.message.PlainMessage;
import sawim.chat.message.SystemNotice;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.history.CachedRecord;
import sawim.history.HistoryStorage;
import protocol.Contact;
import protocol.Protocol;
import protocol.jabber.Jabber;
import protocol.jabber.JabberContact;
import protocol.jabber.JabberServiceContact;
import protocol.jabber.Jid;

import java.util.ArrayList;
import java.util.List;

public final class Chat {
    private Protocol protocol;
    private Contact contact;
    private boolean writable = true;
    private HistoryStorage history;
    private Icon[] statusIcons = new Icon[7];
    private List<MessData> messData = new ArrayList<MessData>();
    private boolean showStatus = true;
    private static boolean selectMode;
    private boolean visibleChat;

    public final int getSize() {
        return messData.size();
    }

    private MessData getMessageDataByIndex(int index) {
        return messData.get(index);
    }

    public final void setWritable(boolean wr) {
        writable = wr;
    }

    public Chat(Protocol p, Contact item) {
        contact = item;
        protocol = p;
        fillFromHistory();
    }

    void setContact(Contact item) {
        contact = item;
    }

    private void markItem(int item) {
        MessData mData = getMessageDataByIndex(item);
        mData.setMarked(!mData.isMarked());
        selectMode = hasSelectedItems();
    }

    private boolean hasSelectedItems() {
        for (int i = 0; i < messData.size(); ++i) {
            MessData md = getMessageDataByIndex(i);
            if (md.isMarked()) {
                return true;
            }
        }
        return false;
    }

    private int isSelectedItems() {
        int size = 0;
        for (int i = 0; i < messData.size(); ++i) {
            MessData md = getMessageDataByIndex(i);
            if (md.isMarked()) {
                ++size;
            }
        }
        return size;
    }

    private void updateStatusIcons() {
        for (int i = 0; i < statusIcons.length; ++i) {
            statusIcons[i] = null;
        }
        contact.getLeftIcons(statusIcons);

    }
    public void updateStatus() {
        updateStatusIcons();
        showStatus = true;
        showStatusPopup();
    }

    public static final String ADDRESS = ", ";

    public String writeMessageTo(String nick) {
        if (null != nick) {
            if ('/' == nick.charAt(0)) {
                nick = ' ' + nick;
            }
            nick += ADDRESS;

        } else {
            nick = "";
        }
        return nick;
    }

    private String getBlogPostId(String text) {
        if (StringConvertor.isEmpty(text)) {
            return null;
        }
        String lastLine = text.substring(text.lastIndexOf('\n') + 1);
        if (0 == lastLine.length()) {
            return null;
        }
        if ('#' != lastLine.charAt(0)) {
            return null;
        }
        int numEnd = lastLine.indexOf(' ');
        if (-1 != numEnd) {
            lastLine = lastLine.substring(0, numEnd);
        }
        return lastLine + " ";
    }
    private boolean isBlogBot() {
        if (contact instanceof JabberContact) {
            return ((Jabber) protocol).isBlogBot(contact.getUserId());
        }
        return false;
    }
    public boolean isHuman() {
        boolean service = isBlogBot() || protocol.isBot(contact);
        if (contact instanceof JabberContact) {
            service |= Jid.isGate(contact.getUserId());
        }
        return !service && contact.isSingleUserContact();
    }

    public Protocol getProtocol() {
        return protocol;
    }

    private static final int ACTION_FT_CANCEL = 900;
    private static final int ACTION_REPLY = 901;
    private static final int ACTION_ADD_TO_HISTORY = 902;
    private static final int ACTION_COPY_TEXT = 903;
	private static final int ACTION_QUOTE = 1003;
    private static final int ACTION_GOTO_URL = 904;
    private static final int ACTION_DEL_CHAT = 905;
    
    /*protected MenuModel getMenu() {
        if (selectMode) {
            MenuModel menu = new MenuModel();
			menu.addItem("copy_text", ACTION_COPY_TEXT);
			menu.addItem("quote", ACTION_QUOTE);
            menu.setActionListener(new Binder(this));
            return menu;
        }
        boolean accessible = writable && (contact.isSingleUserContact() || contact.isOnline());
        MessData md = getCurrentMsgData();
        MenuModel menu = new MenuModel();
        
        if ((null != md) && md.isFile()) {
            menu.addItem("cancel", ACTION_FT_CANCEL);
        }
        
        if (0 < authRequestCounter) {
            menu.addItem("grant", Contact.USER_MENU_GRANT_AUTH);
            menu.addItem("deny", Contact.USER_MENU_DENY_AUTH);
        }

        if (!contact.isSingleUserContact()) {
            menu.addItem("list_of_users", Contact.USER_MENU_USERS_LIST);
        }
        if ((null != md) && md.isURL()) {
            menu.addItem("goto_url", ACTION_GOTO_URL);
        }

        if (accessible) {
            if (sawim.modules.fs.FileSystem.isSupported()) {
                menu.addItem("ft_name", Contact.USER_MENU_FILE_TRANS);
            }
            if (FileTransfer.isPhotoSupported()) {
                menu.addItem("ft_cam", Contact.USER_MENU_CAM_TRANS);
            }
        }

        menu.addItem("copy_text", ACTION_COPY_TEXT);
		menu.addItem("quote", ACTION_QUOTE);
        if (accessible) {
            if (!SawimUI.isClipBoardEmpty()) {
                menu.addItem("paste", Contact.USER_MENU_PASTE);
            }
        }
    //    contact.addChatMenuItems(menu);

        if (!contact.isAuth()) {
            menu.addItem("requauth", Contact.USER_MENU_REQU_AUTH);
        }
        
        if (!contact.isSingleUserContact() && contact.isOnline()) {
            menu.addItem("leave_chat", Contact.CONFERENCE_DISCONNECT);
        }
        menu.addItem("delete_chat", ACTION_DEL_CHAT);
        menu.setActionListener(new Binder(this));
        return menu;
    }
    */
    public void beginTyping(boolean type) {
        updateStatusIcons();
    }

    public static boolean isHighlight(String text, String nick) {
        if (null == nick) {
            return false;
        }
        for (int index = text.indexOf(nick); -1 != index; index = text.indexOf(nick, index + 1)) {
            if (0 < index) {
                char before = text.charAt(index - 1);
                if ((' ' != before) && ('\n' != before) && ('\t' != before)) {
                    continue;
                }
            }
            if (index + nick.length() + 2 < text.length()) {
                char after = (char) Math.min(text.charAt(index + nick.length()),
                        text.charAt(index + nick.length() + 1));
                if ((' ' != after) && ('\n' != after) && ('\t' != after)) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }
    
    public MessData addFileProgress(String caption, String text) {
        /*long time = Sawim.getCurrentGmtTime();
        short flags = MessData.PROGRESS;
        VirtualListItem parser = createParser();
        parser.addDescription(text, THEME_TEXT, FONT_STYLE_PLAIN);
        parser.addProgress(THEME_TEXT);
        Par par = parser.getPar();
        MessData mData = new MessData(time, "", caption, flags, Message.ICON_NONE);
		mData.setFontSet(GraphicsEx.chatFontSet);
		mData.setPar(par);
        synchronized (this) {
            messData.add(mData);
            setCurrentItemIndex(getSize() - 1);
            removeOldMessages();
        }
        ChatHistory.instance.registerChat(this);
        return mData; */
        return null;
    }

    public void changeFileProgress(MessData mData, String caption, String text) {
        /*final int width = getMinScreenMetrics() - 3;
        VirtualListItem parser = new List<VirtualListItem>(mData.par, getFontSet(), width);
        parser.addDescription(text, THEME_TEXT, FONT_STYLE_PLAIN);

        long time = Sawim.getCurrentGmtTime();
        short flags = MessData.PROGRESS;
        synchronized (this) {
            int index = Util.getIndex((Vector) messData, mData);
            if ((0 < getSize()) && (0 <= index)) {
                lock();
                mData.init(time, text, caption, flags, Message.ICON_NONE);
                parser.commit();
                unlock();
            }
        } */
    }

    public int getIcon(Message message, boolean incoming) {
        if (message instanceof SystemNotice) {
            int type = ((SystemNotice)message).getSysnoteType();
            if (SystemNotice.SYS_NOTICE_MESSAGE == type) {
                return Message.ICON_NONE;
            }
            return Message.ICON_SYSREQ;
        }
        if (incoming) {
            if (!contact.isSingleUserContact()
                    && !isHighlight(message.getProcessedText(), getMyName())) {
                return Message.ICON_IN_MSG;
            }
            return Message.ICON_IN_MSG_HI;
        }
        return message.iconIndex;
    }

    public String getMyName() {
        if (contact instanceof JabberServiceContact) {
            String nick = contact.getMyName();
            if (null != nick) return nick;
        }
        return protocol.getNick();
    }
    private String getFrom(Message message) {
        String senderName = message.getName();
        if (null == senderName) {
            senderName = message.isIncoming()
                    ? contact.getName()
                    : getMyName();
        }
        return senderName;
    }
    private void addTextToForm(Message message) {
        String from = getFrom(message);
        boolean incoming = message.isIncoming();

        String messageText = message.getProcessedText();
        messageText = StringConvertor.removeCr(messageText);
        if (StringConvertor.isEmpty(messageText)) {
            return;
        }
        boolean isMe = messageText.startsWith(PlainMessage.CMD_ME);
        if (isMe) {
            messageText = messageText.substring(4);
            if (0 == messageText.length()) {
                return;
            }
        }
		short flags = 0;
        if (incoming) {
            flags |= MessData.INCOMING;
        }
        if (isMe) {
            flags |= MessData.ME;
        }
        if (Util.hasURL(messageText)) {
            flags |= MessData.URLS;
        }
        if (message instanceof SystemNotice) {
            flags |= MessData.SERVICE;
        }
		final MessData mData = new MessData(message, message.getNewDate(), messageText, from, flags, getIcon(message, incoming));
        if (!incoming) {
            message.setVisibleIcon(mData);
        }
        SawimActivity.getInstance().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messData.add(mData);
                removeOldMessages();
            }
        });
    }

    public void activate() {
        resetSelected();
        if (showStatus) {
            showStatusPopup();
        }
        ContactList.getInstance()._setActiveContact(contact);
    }
    
    public void sendMessage(String message) {
        ChatHistory.instance.registerChat(Chat.this);
        if (!contact.isSingleUserContact() && message.endsWith(", ")) {
            message = "";
        }
        if (!StringConvertor.isEmpty(message)) {
            protocol.sendMessage(contact, message, true);
        }
    }
    
    private void showStatusPopup() {
        showStatus = false;
    }

    final static private int MAX_HIST_LAST_MESS = 5;

    private boolean hasHistory() {
        return contact.hasHistory();
    }
    private void fillFromHistory() {
        if (!hasHistory()) {
            return;
        }
        if (isBlogBot()) {
            return;
        }
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            if (0 != getSize()) {
                return;
            }
            HistoryStorage hist = getHistory();
            hist.openHistory();
            int recCount = hist.getHistorySize();
            if (0 == recCount) {
                return;
            }

            int loadOffset = Math.max(recCount - MAX_HIST_LAST_MESS, 0);
            for (int i = loadOffset; i < recCount; ++i) {
                CachedRecord rec = hist.getRecord(i);
                if (null == rec) {
                    continue;
                }
                long date = Util.createLocalDate(rec.date);
                PlainMessage message;
                if (rec.isIncoming()) {
                    message = new PlainMessage(rec.from, protocol, date, rec.text, true);
                } else {
                    message = new PlainMessage(protocol, contact, date, rec.text);
                }
                addTextToForm(message);
            }
            hist.closeHistory();
        }
    }

    public HistoryStorage getHistory() {
        if ((null == history) && hasHistory()) {
            history = HistoryStorage.getHistory(contact);
        }
        return history;
    }

    private void addToHistory(String msg, boolean incoming, String nick, long time) {
        if (hasHistory()) {
            getHistory().addText(msg, incoming, nick, time);
        }
    }

    private void addTextToHistory(MessData md) {
        if (!hasHistory()) {
            return;
        }
        if ((null == md) || (null == md.getText())) {
            return;
        }
        addToHistory(md.getText(), md.isIncoming(), md.getNick(), md.getTime());
    }

    public void clear() {
        messData.clear();
    }

    private void removeMessages(int limit) {
        if (getSize() < limit) {
            return;
        }
        if ((0 < limit) && (0 < getSize())) {
            while (limit < messData.size()) {
                messData.remove(0);
            }
        } else {
            ChatHistory.instance.unregisterChat(this);
        }
    }

    private void removeOldMessages() {
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT));
    }

    public void removeReadMessages() {
        removeMessages(getUnreadMessageCount());
    }

    private void resetSelected() {
        selectMode = false;
        for (int i = 0; i < messData.size(); ++i) {
            getMessageDataByIndex(i).setMarked(false);
        }
    }
	private String quoteSelected(MessData md) {
        StringBuffer sb = new StringBuffer();
        String msg = md.getText();
        if (md.isMe()) {
            msg = "*" + md.getNick() + " " + msg;
        }
        sb.append(SawimUI.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
        sb.append("\n");
        return 0 == sb.length() ? null : sb.toString();
    }

    private String quoteAllSelected() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < messData.size(); ++i) {
            MessData md = getMessageDataByIndex(i);
            if (md.isMarked()) {
                String msg = md.getText();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(SawimUI.serialize(md.isIncoming(), md.getNick() + " " + md.strTime, msg));
                sb.append("\n");
            }
        }
        return 0 == sb.length() ? null : sb.toString();
    }

    private String copySelected(MessData md) {
        String msg = md.getText();
        if (md.isMe()) {
            msg = "*" + md.getNick() + " " + msg;
        }
        return msg + "\n";
    }

    private String copyAllSelected() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < messData.size(); ++i) {
            MessData md = getMessageDataByIndex(i);
            if (md.isMarked()) {
                String msg = md.getText();
                if (md.isMe()) {
                    msg = "*" + md.getNick() + " " + msg;
                }
                sb.append(msg);
                sb.append("\n");
            }
        }
        return 0 == sb.length() ? null : sb.toString();
    }

    public boolean empty() {
        return (0 == messData.size()) && (0 == getSize());
    }

    public long getLastMessageTime() {
        if (0 == messData.size()) {
            return 0;
        }
        MessData md = messData.get(messData.size() - 1);
        return md.getTime();
    }

    public boolean isVisibleChat() {
        return visibleChat;
    }

    private short messageCounter = 0;
    private short otherMessageCounter = 0;
    private byte sysNoticeCounter = 0;
    private byte authRequestCounter = 0;

    public void resetAuthRequests() {
        boolean notEmpty = (0 < authRequestCounter);
        authRequestCounter = 0;
        if (notEmpty) {
            contact.updateChatState(this);
            protocol.markMessages(contact);
        }
    }

    public void resetUnreadMessages() {
        boolean notEmpty = (0 < messageCounter)
                || (0 < otherMessageCounter)
                || (0 < sysNoticeCounter);
        messageCounter = 0;
        otherMessageCounter = 0;
        sysNoticeCounter = 0;
        if (notEmpty) {
            contact.updateChatState(this);
            protocol.markMessages(contact);
        }
    }

    public int getUnreadMessageCount() {
        return messageCounter + sysNoticeCounter + authRequestCounter
                + otherMessageCounter;
    }
    public int getPersonalUnreadMessageCount() {
        return messageCounter + sysNoticeCounter + authRequestCounter;
    }
	public int getOtherMessageCount() {
	    return sysNoticeCounter + authRequestCounter
                + otherMessageCounter;
	}

    public final int getNewMessageIcon() {
        if (0 < messageCounter) {
            return Message.ICON_IN_MSG_HI;
        } else if (0 < authRequestCounter) {
            return Message.ICON_SYSREQ;
        } else if (0 < otherMessageCounter) {
            return Message.ICON_IN_MSG;
        } else if (0 < sysNoticeCounter) {
            return Message.ICON_SYS_OK;
        }
        return -1;
    }

	private boolean isHistory() {
	    boolean useHist = Options.getBoolean(Options.OPTION_HISTORY);
		if (contact instanceof JabberServiceContact) {
		    return useHist && contact.isHistory() == (byte)1;
		}
		return useHist || contact.isHistory() == (byte)1;
	}
	
    public void addMyMessage(PlainMessage message) {
        ChatHistory.instance.registerChat(this);
        resetUnreadMessages();
        addTextToForm(message);
        if (isHistory()) {
            addToHistory(message.getText(), false, getFrom(message), message.getNewDate());
        }
    }

    private short inc(short val) {
        return (short) ((val < Short.MAX_VALUE) ? (val + 1) : val);
    }
    private byte inc(byte val) {
        return (byte) ((val < Byte.MAX_VALUE) ? (val + 1) : val);
    }
    public void addMessage(Message message, boolean toHistory) {
        ChatHistory.instance.registerChat(this);
        boolean inc = !isVisibleChat();
        if (message instanceof PlainMessage) {
            addTextToForm(message);
            if (toHistory && isHistory()) {
                final String nick = getFrom(message);
                addToHistory(message.getText(), true, nick, message.getNewDate());
            }
            if (inc) {
                messageCounter = inc(messageCounter);
                if (!contact.isSingleUserContact()
                        && !isHighlight(message.getProcessedText(), getMyName())) {
                    otherMessageCounter = inc(otherMessageCounter);
                    messageCounter--;
                }
            }
        } else if (message instanceof SystemNotice) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_PRESENCE != notice.getSysnoteType()) {
				if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
					inc = true;
					authRequestCounter = inc(authRequestCounter);
				} else if (inc) {
					sysNoticeCounter = inc(sysNoticeCounter);
				}
				//MagicEye.addAction(protocol, contact.getUserId(), message.getDescStr());
			}
			
            addTextToForm(message);
        }
        if (inc) {
            contact.updateChatState(this);
            ChatHistory.instance.updateChatList();
        }
    }

    public Contact getContact() {
        return contact;
    }

    MessData getUnreadMessage(int num) {
        int index = messData.size() - getUnreadMessageCount() + num;
        return messData.get(index);
    }

    public List<MessData> getMessData() {
        return messData;
    }

    public void setVisibleChat(boolean visibleChat) {
        this.visibleChat = visibleChat;
    }

    public String onMessageSelected(MessData md) {
        if (contact.isSingleUserContact()) {
            if (isBlogBot()) {
                return getBlogPostId(md.getText());
            }
            return "";
        }
        String nick = ((null == md) || md.isFile()) ? null : md.getNick();
        return writeMessageTo(getMyName().equals(nick) ? null : nick);
    }
}