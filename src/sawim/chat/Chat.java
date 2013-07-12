package sawim.chat;

import ru.sawim.General;
import sawim.Options;
import ru.sawim.General;
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
import sawim.modules.MagicEye;

import java.util.ArrayList;
import java.util.List;

public final class Chat {
    private Protocol protocol;
    private Contact contact;
    private boolean writable = true;
    private HistoryStorage history;
    private boolean showStatus = true;
    private List<MessData> messData = new ArrayList<MessData>();
    private boolean visibleChat;
    public static final String ADDRESS = ", ";
    public int position;

    public Chat(Protocol p, Contact item) {
        contact = item;
        protocol = p;
        fillFromHistory();
    }

    void setContact(Contact item) {
        contact = item;
    }

    public void updateStatus() {
        showStatus = true;
        showStatusPopup();
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

    public final void setWritable(boolean wr) {
        writable = wr;
    }

    public final boolean getWritable() {
        return writable;
    }

    public void beginTyping(boolean type) {
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

    public void addFileProgress(String caption, String text) {
        long time = General.getCurrentGmtTime();
        short flags = MessData.PROGRESS;
        final MessData mData = new MessData(time, text, caption, flags, Message.ICON_NONE);
        if (General.getInstance().getUpdateChatListener() == null) {
            removeOldMessages();
            messData.add(mData);
        } else {
            General.getInstance().getUpdateChatListener().addMessage(this, mData);
        }
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
        return Message.ICON_OUT_MSG;
    }

    public String getMyName() {
        if (contact instanceof JabberServiceContact) {
            String nick = contact.getMyName();
            if (null != nick) return nick;
        }
        return protocol.getNick();
    }

    public void activate() {
        if (showStatus) {
            showStatusPopup();
        }
        ContactList.getInstance().activate(contact);
    }

    public void sendMessage(String message) {
        ChatHistory.instance.registerChat(this);
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

    public boolean hasHistory() {
        return contact.hasHistory();
    }
    private int getMessCount() {
            return messCount();
    }
    private void fillFromHistory() {
        if (!hasHistory()) {
            return;
        }
        if (isBlogBot()) {
            return;
        }
        if (Options.getBoolean(Options.OPTION_HISTORY)) {
            if (0 != getMessCount()) {
                return;
            }
            HistoryStorage hist = getHistory();
            hist.openHistory();
            int recCount = hist.getHistorySize();
            if (0 == recCount) {
                return;
            }

            for (int i = 0; i < recCount; ++i) {
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
                addTextToForm(message, getFrom(message));
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

    public int messCount() {
        return messData.size();
    }

    public MessData getMessageDataByIndex(int index) {
        return messData.get(index);
    }

    public void clear() {
        messData.clear();
    }

    public void removeMessages(final int limit) {
        if (messData.size() < limit) {
            return;
        }
        if ((0 < limit) && (0 < messData.size())) {
            while (limit < messData.size()) {
                messData.remove(0);
            }
        } else {
            ChatHistory.instance.unregisterChat(Chat.this);
        }
    }

    public void removeOldMessages() {
        removeMessages(Options.getInt(Options.OPTION_MAX_MSG_COUNT));
    }

    public void removeMessagesAtCursor(int currPoss) {
        removeMessages(messData.size() - currPoss - 1);
    }

    public boolean empty() {
        return 0 == getMessCount();
    }

    public long getLastMessageTime() {
        if (0 == getMessCount()) {
            return 0;
        }
        MessData md = getMessageDataByIndex(getMessCount() - 1);
        return md.getTime();
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
    public int getAuthRequestCounter() {
        return authRequestCounter;
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

    private short inc(short val) {
        return (short) ((val < Short.MAX_VALUE) ? (val + 1) : val);
    }
    private byte inc(byte val) {
        return (byte) ((val < Byte.MAX_VALUE) ? (val + 1) : val);
    }

    private void addToHistory(String msg, boolean incoming, String nick, long time) {
        if (hasHistory()) {
            getHistory().addText(msg, incoming, nick, time);
        }
    }

    public void addTextToHistory(MessData md) {
        if (!hasHistory()) {
            return;
        }
        if ((null == md) || (null == md.getText())) {
            return;
        }
        addToHistory(md.getText(), md.isIncoming(), md.getNick(), md.getTime());
    }

    private boolean isHistory() {
        boolean useHist = Options.getBoolean(Options.OPTION_HISTORY);
        if (contact instanceof JabberServiceContact) {
            return useHist && contact.isHistory() == (byte)1;
        }
        return useHist || contact.isHistory() == (byte)1;
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

    private void addTextToForm(Message message, String from) {
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
        final MessData mData = new MessData(message.getNewDate(), messageText, from, flags, getIcon(message, incoming));
        if (!incoming) {
            message.setVisibleIcon(mData);
        }
        if (General.getInstance().getUpdateChatListener() == null) {
            removeOldMessages();
            messData.add(mData);
        } else {
            General.getInstance().getUpdateChatListener().addMessage(this, mData);
        }
    }

    public void addPresence(SystemNotice message) {
        ChatHistory.instance.registerChat(this);
        String messageText = message.getProcessedText();
        final MessData mData = new MessData(message.getNewDate(), messageText, message.getName(), MessData.PRESENCE, Message.ICON_NONE);
        if (General.getInstance().getUpdateChatListener() == null) {
            removeOldMessages();
            messData.add(mData);
        } else {
            General.getInstance().getUpdateChatListener().addMessage(this, mData);
        }
        if (!isVisibleChat()) {
            contact.updateChatState(this);
            ChatHistory.instance.updateChatList();
        }
    }

    public void addMessage(Message message, boolean toHistory) {
        ChatHistory.instance.registerChat(this);
        boolean inc = !isVisibleChat();
        if (message instanceof PlainMessage) {
            addTextToForm(message, getFrom(message));
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
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                inc = true;
                authRequestCounter = inc(authRequestCounter);
            } else if (inc) {
                sysNoticeCounter = inc(sysNoticeCounter);
            }
            MagicEye.addAction(protocol, contact.getUserId(), message.getText());
            addTextToForm(message, getFrom(message));
        }
        if (inc) {
            contact.updateChatState(this);
            ChatHistory.instance.updateChatList();
        }
    }

    public void addMyMessage(PlainMessage message) {
        ChatHistory.instance.registerChat(this);
        resetUnreadMessages();
        addTextToForm(message, getFrom(message));
        if (isHistory()) {
            addToHistory(message.getText(), false, getFrom(message), message.getNewDate());
        }
    }

    public Contact getContact() {
        return contact;
    }

    MessData getUnreadMessage(int num) {
        int index = getMessCount() - getUnreadMessageCount() + num;
        return getMessageDataByIndex(index);
    }

    public List<MessData> getMessData() {
        return messData;
    }

    public boolean isVisibleChat() {
        return visibleChat;
    }

    public void setVisibleChat(boolean visibleChat) {
        this.visibleChat = visibleChat;
    }
}