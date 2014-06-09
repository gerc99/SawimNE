package ru.sawim.chat;

import protocol.*;
import protocol.Protocol;
import protocol.Contact;
import protocol.ServiceContact;
import ru.sawim.SawimApplication;
import ru.sawim.chat.message.Message;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.StringConvertor;
import ru.sawim.modules.history.HistoryStorage;
import ru.sawim.roster.RosterHelper;

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

    public String message;
    public int firstVisiblePosition;
    public int offset;
    public int dividerPosition = -1;
    public int lastVisiblePosition;

    public Chat(Protocol p, Contact item) {
        contact = item;
        protocol = p;
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
        addMessage(new MessData(contact, SawimApplication.getCurrentGmtTime(), text, caption, MessData.PROGRESS, false));
        if (RosterHelper.getInstance().getUpdateChatListener() != null)
            RosterHelper.getInstance().getUpdateChatListener().updateMessages();
    }

    public String getMyName() {
        if (contact instanceof ServiceContact) {
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

    public String getBlogPostId(String text) {
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
        return lastLine;
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

    public boolean isBlogBot() {
        return protocol.isBlogBot(contact.getUserId());
    }

    public boolean isHuman() {
        boolean service = isBlogBot() || protocol.isBot(contact);
        service |= Jid.isGate(contact.getUserId());
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

    public HistoryStorage getHistory() {
        if (null == history && hasHistory()) {
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

    public boolean empty() {
        return 0 == getMessCount();
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
        boolean notEmpty = 0 < messageCounter
                || 0 < otherMessageCounter
                || 0 < sysNoticeCounter;
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

    public void addTextToHistory(MessData md) {
        if (getHistory() == null || null == md) {
            return;
        }
        getHistory().addText(md);
    }

    public String getFrom(Message message) {
        String senderName = message.getName();
        if (null == senderName) {
            senderName = message.isIncoming()
                    ? contact.getName()
                    : getMyName();
        }
        return senderName;
    }

    public MessData buildMessage(Message message, String from, boolean isSystemNotice, boolean isHighlight) {
        boolean incoming = message.isIncoming();
        String messageText = message.getProcessedText();
        //messageText = StringConvertor.removeCr(messageText);
        if (StringConvertor.isEmpty(messageText)) {
            return null;
        }
        boolean isMe = messageText.startsWith(PlainMessage.CMD_ME);
        if (isMe) {
            messageText = messageText.substring(4);
            if (0 == messageText.length()) {
                return null;
            }
        }
        short flags = 0;
        if (incoming) {
            flags |= MessData.INCOMING;
        }
        if (isMe) {
            flags |= MessData.ME;
        }
        if (isSystemNotice) {
            flags |= MessData.SERVICE;
        }

        final MessData mData = new MessData(contact, message.getNewDate(), messageText, from, flags, isHighlight);
        if (!incoming && !mData.isMe()) {
            message.setVisibleIcon(mData);
        }
        return mData;
    }

    private void addTextToFormAndHistory(Message message, String from, boolean isSystemNotice, boolean isHighlight) {
        final MessData mData = buildMessage(message, from, isSystemNotice, isHighlight);
        addMessage(mData);
        addTextToHistory(mData);
    }

    public void addMessage(MessData mData) {
        messData.add(mData);
        if (RosterHelper.getInstance().getUpdateChatListener() != null) {
            RosterHelper.getInstance().getUpdateChatListener().addMessage(contact, mData);
            RosterHelper.getInstance().getUpdateChatListener().updateMessages();
        }
        boolean isConference = contact.isConference();
        if (isConference && mData.isMessage())
            RosterHelper.getInstance().setLastMessageTime(contact.getUserId(), mData.getTime());
    }

    public void addPresence(SystemNotice message) {
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        String messageText = message.getProcessedText();
        addMessage(new MessData(contact, message.getNewDate(), messageText, message.getName(), MessData.PRESENCE, false));
        if (!isVisibleChat()) {
            contact.updateChatState(this);
        }
    }

    public void addMessage(Message message, boolean isPlain, boolean isSystem, boolean isHighlight) {
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        boolean inc = !isVisibleChat();
        String from = getFrom(message);
        if (isPlain) {
            addTextToFormAndHistory(message, from, false, isHighlight);
            if (inc) {
                messageCounter = inc(messageCounter);
                if (!contact.isSingleUserContact() && !isHighlight) {
                    otherMessageCounter = inc(otherMessageCounter);
                    messageCounter--;
                }
            }
        } else if (isSystem) {
            SystemNotice notice = (SystemNotice) message;
            if (SystemNotice.SYS_NOTICE_AUTHREQ == notice.getSysnoteType()) {
                inc = true;
                authRequestCounter = inc(authRequestCounter);
            } else if (inc) {
                sysNoticeCounter = inc(sysNoticeCounter);
            }
            //MagicEye.addAction(protocol, contact.getUserId(), message.getText());
            addTextToFormAndHistory(message, from, true, isHighlight);
        }
        if (inc) {
            contact.updateChatState(this);
        }
    }

    public void addMyMessage(PlainMessage message) {
        String from = getFrom(message);
        if (getMessCount() <= 1)
            ChatHistory.instance.registerChat(this);
        resetUnreadMessages();
        addTextToFormAndHistory(message, from, true, false);
        if (RosterHelper.getInstance().getUpdateChatListener() != null)
            RosterHelper.getInstance().getUpdateChatListener().updateChat();
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