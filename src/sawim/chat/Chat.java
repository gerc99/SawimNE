package sawim.chat;

import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.Jid;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import protocol.xmpp.XmppServiceContact;
import ru.sawim.General;
import sawim.Options;
import sawim.chat.message.Message;
import sawim.chat.message.PlainMessage;
import sawim.chat.message.SystemNotice;
import sawim.comm.StringConvertor;
import sawim.comm.Util;
import sawim.history.CachedRecord;
import sawim.history.HistoryStorage;
import sawim.roster.RosterHelper;

import java.util.ArrayList;
import java.util.List;

public final class Chat {
    private Protocol protocol;
    private Contact contact;
    private boolean writable = true;
    private HistoryStorage history;
    private List<MessData> messData = new ArrayList<MessData>();
    public static final String ADDRESS = ", ";
    private boolean visibleChat;

    public String message = "";
    public int scrollPosition;
    public int offset;
    public int dividerPosition;

    public Chat(Protocol p, Contact item) {
        contact = item;
        protocol = p;
        fillFromHistory();
    }

    void setContact(Contact item) {
        contact = item;
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
        addMessage(new MessData(contact, General.getCurrentGmtTime(), text, caption, MessData.PROGRESS, false));
    }

    public String getMyName() {
        if (contact instanceof XmppServiceContact) {
            String nick = contact.getMyName();
            if (null != nick) return nick;
        }
        return protocol.getNick();
    }

    public void activate() {
        RosterHelper.getInstance().activate(contact);
    }

    public void sendMessage(String message) {
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        //if (!contact.isSingleUserContact() && message.endsWith(", ")) {
        //    message = "";
        //}
        if (!StringConvertor.isEmpty(message)) {
            protocol.sendMessage(contact, message, true);
        }
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

    private String writeMessageTo(String nick) {
        if (null != nick) {
            if ('/' == nick.charAt(0)) {
                nick = ' ' + nick;
            }
            nick += Chat.ADDRESS;

        } else {
            nick = "";
        }
        return nick;
    }

    private boolean isBlogBot() {
        if (contact instanceof XmppContact) {
            return ((Xmpp) protocol).isBlogBot(contact.getUserId());
        }
        return false;
    }

    public boolean isHuman() {
        boolean service = isBlogBot() || protocol.isBot(contact);
        if (contact instanceof XmppContact) {
            service |= Jid.isGate(contact.getUserId());
        }
        return !service && contact.isSingleUserContact();
    }

    public String onMessageSelected(MessData md) {
        if (contact.isSingleUserContact()) {
            if (isBlogBot()) {
                return getBlogPostId(md.getText().toString());
            }
            return "";
        }
        String nick = ((null == md) || md.isFile()) ? null : md.getNick();
        return writeMessageTo(getMyName().equals(nick) ? null : nick);
    }

    public boolean hasHistory() {
        return contact.hasHistory();
    }

    private void fillFromHistory() {
        if (isBlogBot()) return;
        //if (Options.getBoolean(Options.OPTION_HISTORY)) {
        if (0 != getMessCount()) return;

        HistoryStorage hist = getHistory();
        if (hist == null) return;

        hist.openHistory();
        int recCount = hist.getHistorySize();
        if (0 == recCount) return;

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
            addTextToForm(message, contact.isConference() ? rec.from : getFrom(message), Chat.isHighlight(message.getProcessedText(), contact.getMyName()));
        }
        hist.closeHistory();
        //}
    }

    public HistoryStorage getHistory() {
        if ((null == history) && hasHistory()) {
            history = HistoryStorage.getHistory(contact);
        }
        return history;
    }

    public int getMessCount() {
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

    public int typeNewMessageIcon = Message.ICON_NONE;
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
        typeNewMessageIcon = Message.ICON_NONE;
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
        return Message.ICON_NONE;
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
        addToHistory(md.getText().toString(), md.isIncoming(), md.getNick(), md.getTime());
    }

    private boolean isHistory() {
        boolean useHist = Options.getBoolean(Options.OPTION_HISTORY);
        if (contact instanceof XmppServiceContact) {
            return useHist && contact.isHistory() == (byte) 1;
        }
        return useHist || contact.isHistory() == (byte) 1;
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

    private void addTextToForm(Message message, String from, boolean isHighlight) {
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

        final MessData mData = new MessData(contact, message.getNewDate(), messageText, from, flags, isHighlight);
        if (!incoming) {
            message.setVisibleIcon(mData);
        }
        addMessage(mData);
    }

    private void addMessage(MessData mData) {
        messData.add(mData);
    }

    public void addPresence(SystemNotice message) {
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        String messageText = message.getProcessedText();
        addMessage(new MessData(contact, message.getNewDate(), messageText, message.getName(), MessData.PRESENCE, false));
        if (!isVisibleChat()) {
            contact.updateChatState(this);
            ChatHistory.instance.updateChatList();
        }
    }

    public void addMessage(Message message, boolean toHistory, boolean isHighlight) {
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        boolean inc = !isVisibleChat();
        String from = getFrom(message);
        if (message instanceof PlainMessage) {
            addTextToForm(message, from, isHighlight);
            if (toHistory && isHistory()) {
                final String nick = from;
                addToHistory(message.getText(), true, nick, message.getNewDate());
            }
            if (inc) {
                messageCounter = inc(messageCounter);
                if (!contact.isSingleUserContact() && !isHighlight) {
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
            //MagicEye.addAction(protocol, contact.getUserId(), message.getText());
            addTextToForm(message, from, isHighlight);
        }
        if (inc) {
            contact.updateChatState(this);
            ChatHistory.instance.updateChatList();
        }
    }

    public void addMyMessage(PlainMessage message) {
        String from = getFrom(message);
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        resetUnreadMessages();
        addTextToForm(message, from, false);
        if (isHistory()) {
            addToHistory(message.getText(), false, from, message.getNewDate());
        }
    }

    public Contact getContact() {
        return contact;
    }

    MessData getUnreadMessage(int num) {
        int index = getMessCount() - getOtherMessageCount() + num;
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