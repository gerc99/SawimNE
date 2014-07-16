package protocol.mrim;

import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.TemporaryRoster;
import protocol.net.ClientConnection;
import protocol.net.TcpSocket;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimException;
import ru.sawim.chat.Chat;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.chat.message.SystemNotice;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.RosterHelper;

import java.util.Vector;

public final class MrimConnection extends ClientConnection {
    private TcpSocket socket;
    private Mrim mrim;
    private int seqCounter = 0;

    private static final int MAX_TYPING_TASK_COUNT = 3;
    private TypingTask[] typingTasks = new TypingTask[MAX_TYPING_TASK_COUNT];
    private String typingTo = null;
    private MrimPacket pingPacket;

    private final Vector packets = new Vector();
    private UserInfo singleUserInfo;
    private Search search = null;
    private MrimContact lastContact = null;
    private MrimGroup lastGroup = null;

    private static final int MULTICHAT_MESSAGE = 0;
    private static final int MULTICHAT_GET_MEMBERS = 1;
    private static final int MULTICHAT_MEMBERS = 2;
    private static final int MULTICHAT_ADD_MEMBERS = 3;
    private static final int MULTICHAT_ATTACHED = 4;
    private static final int MULTICHAT_DETACHED = 5;
    private static final int MULTICHAT_DESTROYED = 6;
    private static final int MULTICHAT_INVITE = 7;

    private static final int CONTACT_OPER_SUCCESS = 0x0000;
    private static final int GET_CONTACTS_OK = 0x0000;
    private static final int GET_CONTACTS_ERROR = 0x0001;
    private static final int GET_CONTACTS_INTERR = 0x0002;

    private static final int FIRST_CONTACT_ID = 20;

    public static final int CONTACT_FLAG_REMOVED = 0x00000001;
    public static final int CONTACT_FLAG_SHADOW = 0x00000020;
    public static final int CONTACT_FLAG_GROUP = 0x00000002;
    public static final int STATUS_FLAG_INVISIBLE = 0x80000000;

    public static final int STATUS_OFFLINE = 0x00000000;
    public static final int STATUS_ONLINE = 0x00000001;
    public static final int STATUS_AWAY = 0x00000002;
    public static final int STATUS_UNDETERMINATED = 0x00000003;
    public static final int STATUS_CHAT = 0x00000004;
    public static final int STATUS_INVISIBLE = STATUS_FLAG_INVISIBLE | STATUS_ONLINE;

    public MrimConnection(Mrim protocol) {
        this.mrim = protocol;
    }

    private void setProgress(int progress) {
        mrim.setConnectingProgress(progress);
    }

    protected Protocol getProtocol() {
        return mrim;
    }

    private String getServer() throws SawimException {
        StringBuilder buffer = new StringBuilder();
        try {
            TcpSocket s = new TcpSocket();
            s.connectForReadingTo("mrim.mail.ru", 2042);
            int ch;
            while (true) {
                ch = s.read();
                if (-1 == ch) break;
                if (('0' <= ch && ch <= '9') || (ch == '.') || (ch == ':')) {
                    buffer.append((char) ch);
                }
            }
        } catch (Exception e) {
            throw new SawimException(120, 10);
        }
        return buffer.toString();
    }

    private int nextSeq() {
        return ++seqCounter;
    }

    private void sendPacket(MrimPacket packet) throws SawimException {
        if (0 == packet.getSeq()) {
            packet.setSeq(nextSeq());
        }
        byte[] outpacket = packet.toByteArray();
        socket.write(outpacket);
        socket.flush();
    }

    private MrimPacket getPacket() throws SawimException {
        byte[] header = new byte[4 * 7 + 16];
        socket.readFully(header);
        int seq = (int) Util.getDWordLE(header, 8);
        int cmd = (int) Util.getDWordLE(header, 12);
        int dataSize = (int) Util.getDWordLE(header, 16);
        byte[] data = new byte[dataSize];
        if (0 < dataSize) {
            socket.readFully(data);
        }
        return new MrimPacket(cmd, seq, data);
    }

    protected void connect() throws SawimException {
        setProgress(0);
        DebugLog.println("go");

        String server = getServer();

        DebugLog.println("server " + server);

        setProgress(20);
        socket = new TcpSocket();
        String[] mrimHost = Util.explode(server, ':');
        socket.connectTo(mrimHost[0], Integer.parseInt(mrimHost[1]));
        DebugLog.println("send hello");

        MrimBuffer hi = new MrimBuffer();
        hi.putDWord(getPingInterval());
        sendPacket(new MrimPacket(MrimPacket.MRIM_CS_HELLO, hi));
        setProgress(40);

        MrimPacket packetKeepAlive = getPacket();
        int ping = Math.min((int) packetKeepAlive.getData().getDWord(), 120);
        setPingInterval(ping);

        DebugLog.println("recv hello " + getPingInterval());

        sendPacket(MrimPacket.getLoginPacket(mrim));
        setProgress(60);

        MrimPacket loginResult = getPacket();
        if (MrimPacket.MRIM_CS_LOGIN_ACK != loginResult.getCommand()) {

            DebugLog.println("mrim login resone " + loginResult.getData().getString());

            mrim.setPassword(null);
            throw new SawimException(111, 0);
        }
        setProgress(80);

        connect = true;
    }

    private int secondCounter = 0;
    private int outTypingCounter = 0;

    protected boolean processPacket() throws SawimException {
        MrimPacket toPacket = getPacketFromQueue();
        if (null != toPacket) {
            sendPacket(toPacket);
            return true;
        }
        if (0 < socket.available()) {
            MrimPacket packet = getPacket();
            try {
                processPacket(packet);
            } catch (SawimException e) {
                throw e;
            } catch (Exception e) {

                DebugLog.panic("mrim: processPacket", e);
                DebugLog.dump("mrim cmd " + packet.getCommand(), packet.toByteArray());

            }
            return true;
        }

        secondCounter++;
        if (4 == secondCounter) {
            inTypingTask(SawimApplication.getCurrentGmtTime());
            if (null != typingTo) {
                outTypingCounter++;
                if (9 == outTypingCounter) {
                    outTypingTask();
                    outTypingCounter = 0;
                }
            }
            secondCounter = 0;
        }
        return false;
    }

    private void putPacketIntoQueue(MrimPacket packet) {
        synchronized (packets) {
            packets.addElement(packet);
        }
    }

    private MrimPacket getPacketFromQueue() {
        synchronized (packets) {
            if (packets.isEmpty()) {
                return null;
            }
            MrimPacket packet = (MrimPacket) packets.firstElement();
            packets.removeElementAt(0);
            return packet;
        }
    }

    private void addMessage(String from, String msg, long flags, String date, boolean offline) {
        msg = StringConvertor.trim(msg);
        boolean isAuth = (0 != (MrimPacket.MESSAGE_FLAG_AUTHORIZE & flags));
        if (!isAuth && StringConvertor.isEmpty(msg)) {
            return;
        }
        try {
            if (isAuth && (5 < msg.length())) {
                MrimBuffer buffer = new MrimBuffer(Util.base64decode(msg.trim()));
                buffer.getDWord();
                buffer.getString();
                if (0 != (MrimPacket.MESSAGE_FLAG_OLD & flags)) {
                    msg = buffer.getString();
                } else {
                    msg = buffer.getUcs2String();
                }
            }
        } catch (Exception e) {
            msg = "";
        }
        if (-1 == from.indexOf('@')) {
            MrimContact contact = mrim.getContactByPhone(from);
            String fromEmail = from;

            if (null != contact) {
                PlainMessage message = new PlainMessage(contact.getUserId(),
                        mrim, SawimApplication.getCurrentGmtTime(), msg, false);
                message.setName("SMS");
                Chat chat = mrim.getChat(contact);
                if (contact instanceof MrimPhoneContact) {
                    chat.setWritable(false);
                }
                chat.addMessage(message, true, false, false);
                if (!(contact instanceof MrimPhoneContact)) {
                    fromEmail = contact.getUserId();
                }
            } else {
                if (SawimApplication.isPaused()) {
                    SawimApplication.maximize();
                }

                RosterHelper.getInstance().activateWithMsg(from + " (SMS):\n" + msg);
            }
            return;
        }
        if ((MrimPacket.MESSAGE_FLAG_AUTHORIZE & flags) != 0) {
            mrim.addMessage(new SystemNotice(mrim,
                    SystemNotice.SYS_NOTICE_AUTHREQ,
                    from, msg));

        } else if ((MrimPacket.MESSAGE_FLAG_NOTIFY & flags) == 0) {
            if ((MrimPacket.MESSAGE_FLAG_ALARM & flags) != 0) {
                msg = PlainMessage.CMD_WAKEUP;
            }
            mrim.addMessage(new PlainMessage(from, mrim,
                    (null == date) ? SawimApplication.getCurrentGmtTime() : Util.createGmtDate(date),
                    msg, offline));
        }
    }

    public void putMultiChatGetMembers(String to) {
        MrimBuffer out = MrimPacket.getMessageBuffer(to, "", 0x00400000);
        out.putDWord(4);
        out.putDWord(MULTICHAT_GET_MEMBERS);
        putPacketIntoQueue(new MrimPacket(MrimPacket.MRIM_CS_MESSAGE, out));
    }

    private void addMultiChatMessage(MrimBuffer packetData, long flags, String msg, String from) {
        long len = packetData.getDWord();
        int type = (int) packetData.getDWord();
        String chatName = packetData.getUcs2String();

        MrimChatContact chat = (MrimChatContact) mrim.getItemByUID(from);
        if (null == chat) {
            chat = (MrimChatContact) mrim.createTempContact(from);
            mrim.addTempContact(chat);
        }
        chat.setName(chatName);

        String email;
        switch (type) {
            case MULTICHAT_MESSAGE:
                email = packetData.getString();
                msg = StringConvertor.trim(msg);
                if (StringConvertor.isEmpty(msg)) {
                    return;
                }
                if ((MrimPacket.MESSAGE_FLAG_ALARM & flags) != 0) {
                    msg = PlainMessage.CMD_WAKEUP;
                }
                PlainMessage message = new PlainMessage(from, mrim,
                        SawimApplication.getCurrentGmtTime(), msg, false);
                message.setName(email);
                mrim.addMessage(message);
                return;

            case MULTICHAT_MEMBERS:
                packetData.getDWord();
                int count = (int) packetData.getDWord();
                Vector inChat = new Vector();
                for (; 0 < count; --count) {
                    inChat.addElement(packetData.getString());
                }
                chat.setMembers(inChat);
                return;

            case MULTICHAT_INVITE:
                email = packetData.getString();

                ru.sawim.modules.DebugLog.println("invite from " + email);

                return;
            case MULTICHAT_ATTACHED:
                email = packetData.getString();
                chat.getMembers().addElement(email);
                return;
            case MULTICHAT_DETACHED:
                email = packetData.getString();
                chat.getMembers().removeElement(email);
                return;
        }

        ru.sawim.modules.DebugLog.dump("type " + type, packetData.toByteArray());

    }

    private void inTypingTask(long now) {
        for (int i = 0; i < typingTasks.length; ++i) {
            if ((null != typingTasks[i]) && (typingTasks[i].time <= now)) {

                mrim.beginTyping(typingTasks[i].email, false);

                typingTasks[i] = null;
            }
        }
    }

    private void outTypingTask() {

        String to = typingTo;
        if (null != to) {
            int flags = MrimPacket.MESSAGE_FLAG_NORECV | MrimPacket.MESSAGE_FLAG_NOTIFY;
            MrimPacket packet = MrimPacket.getMessagePacket(to, " ", flags);
            packet.setSeq(nextSeq());
            putPacketIntoQueue(packet);
        }

    }

    private void beginTyping(MrimContact c, boolean flag) {
        long now = SawimApplication.getCurrentGmtTime();
        inTypingTask(now);
        for (int i = 0; i < typingTasks.length; ++i) {
            if (null == typingTasks[i]) {
                typingTasks[i] = new TypingTask(c.getUserId(), now + 11);
                mrim.beginTyping(c.getUserId(), true);
                return;
            }
        }

    }

    private void processPacket(MrimPacket fromPacket) throws SawimException {
        int cmd = (int) fromPacket.getCommand();
        MrimBuffer packetData = fromPacket.getData();

        if (MrimPacket.MRIM_CS_MESSAGE_ACK == cmd) {
            long msgId = packetData.getDWord();
            long flags = packetData.getDWord();
            String from = packetData.getString();
            String msg = null;
            if ((0 != (MrimPacket.MESSAGE_FLAG_OLD & flags))
                    || (0 != (MrimPacket.MESSAGE_FLAG_AUTHORIZE & flags))) {
                msg = packetData.getString();
            } else {
                msg = packetData.getUcs2String();
            }


            if (from.endsWith("@chat.agent")) {
                packetData.getString();
                addMultiChatMessage(packetData, flags, msg, from);
                return;
            }

            MrimContact c = (MrimContact) mrim.getItemByUID(from);
            if (null != c) {
                beginTyping(c, (MrimPacket.MESSAGE_FLAG_NOTIFY & flags) != 0);
            }

            if ((MrimPacket.MESSAGE_FLAG_NORECV & flags) == 0) {
                putPacketIntoQueue(MrimPacket.getMessageRecvPacket(from, msgId));
            }
            addMessage(from, msg, flags, null, false);

        } else if (MrimPacket.MRIM_CS_OFFLINE_MESSAGE_ACK == cmd) {
            byte[] uidl = packetData.getUidl();
            String offmsg = packetData.getString();
            String[] mail = explodeMail(offmsg);
            String date = getMailValue(mail[0], "Date");
            String from = getMailValue(mail[0], "From");
            String msg = getMailMessage(mail);
            long flags = 0;
            try {
                flags = Integer.parseInt(getMailValue(mail[0], "X-MRIM-Flags"), 16);
            } catch (Exception e) {
            }
            addMessage(from, msg, flags, date, true);
            putPacketIntoQueue(MrimPacket.getDeleteOfflineMessagePacket(uidl));

        } else if (MrimPacket.MRIM_CS_MESSAGE_STATUS == cmd) {
            long status = packetData.getDWord();

            DebugLog.println("message status " + status);

            setMessageSended(fromPacket.getSeq(), status);

        } else if (MrimPacket.MRIM_CS_AUTHORIZE_ACK == cmd) {
            String uin = packetData.getString();
            mrim.setAuthResult(uin, true);

        } else if (MrimPacket.MRIM_CS_LOGOUT == cmd) {
            int resone = (int) packetData.getDWord();

            DebugLog.println("disconnect reason " + resone);

            throw new SawimException(110, resone);

        } else if (MrimPacket.MRIM_CS_USER_STATUS == cmd) {
            long status = packetData.getDWord();
            String xstatus = packetData.getString();
            String title = packetData.getUcs2String();
            String desc = packetData.getUcs2String();
            String user = packetData.getString();
            int clientCaps = (int) packetData.getDWord();
            String client = packetData.getString();
            MrimContact contact = (MrimContact) mrim.getItemByUID(user);
            if ((null != contact) && !(contact instanceof MrimPhoneContact)) {
                final int oldStatusIndex = contact.getStatusIndex();

                mrim.setContactStatus(contact, MrimConnection.getStatusIndexBy(status, xstatus), null);
                final int newStatusIndex = contact.getStatusIndex();
                contact.setMood(xstatus, title, desc);

                if (StatusInfo.STATUS_OFFLINE != newStatusIndex) {
                    contact.setClient(client);
                }

                mrim.ui_changeContactStatus(contact);
                if ((contact instanceof MrimChatContact) && contact.isOnline()) {
                    putMultiChatGetMembers(contact.getUserId());
                }
                if ((StatusInfo.STATUS_OFFLINE == newStatusIndex)
                        && (StatusInfo.STATUS_OFFLINE == oldStatusIndex)) {
                    //MagicEye.addAction(mrim, contact.getUserId(), "hiding_from_you");
                }
            }

        } else if (MrimPacket.MRIM_CS_ANKETA_INFO == cmd) {
            long searchStatus = packetData.getDWord();
            if (MrimPacket.MRIM_ANKETA_INFO_STATUS_OK == searchStatus) {
                int fieldNum = (int) packetData.getDWord();
                long maxResults = packetData.getDWord();
                long serverTime = packetData.getDWord();
                String[] fieldNames = new String[fieldNum];
                for (int i = 0; i < fieldNum; ++i) {
                    fieldNames[i] = packetData.getString().toLowerCase();
                }
                while (!packetData.isEOF()) {
                    UserInfo userInfo = (null == singleUserInfo)
                            ? new UserInfo(mrim, "")
                            : singleUserInfo;
                    setUserInfo(userInfo, fieldNames, packetData);
                    if (null != search) {
                        search.addResult(userInfo);
                    }
                    if (null != lastContact) {
                        if (StringConvertor.isEmpty(lastContact.getPhones())) {
                        } else if (StringConvertor.isEmpty(userInfo.homePhones)) {
                            userInfo.homePhones = lastContact.getPhones();
                        } else {
                            userInfo.homePhones += ", " + lastContact.getPhones();
                        }
                        lastContact = null;
                    }

                    if (null != singleUserInfo) {
                        singleUserInfo.setOptimalName();
                        singleUserInfo.updateProfileView();
                        singleUserInfo = null;
                    }
                }

                if (null != search) {
                    search.finished();
                    search = null;
                }

            } else {
                if (null != search) {
                    search.finished();
                }
            }
        } else if (MrimPacket.MRIM_CS_ADD_CONTACT_ACK == cmd) {
            final long result = packetData.getDWord();

            DebugLog.println("add result " + result);

            if (CONTACT_OPER_SUCCESS == result) {
                int id = (int) packetData.getDWord();
                if (0 <= id) {
                    if (null != lastGroup) {
                        lastGroup.setGroupId(id);
                    } else {
                        lastContact.setContactId(id);
                        if (!(lastContact instanceof MrimPhoneContact)) {
                            lastContact.setBooleanValue(Contact.CONTACT_NO_AUTH, true);
                        }
                        lastContact.setTempFlag(false);
                        mrim.ui_updateContact(lastContact);
                    }
                }
            }
            lastContact = null;

        } else if (MrimPacket.MRIM_CS_MODIFY_CONTACT_ACK == cmd) {
            DebugLog.println("contact updateRoster status " + packetData.getDWord());
        } else if (MrimPacket.MRIM_CS_CONTACT_LIST2 == cmd) {
            getContactList(packetData);

        } else if (MrimPacket.MRIM_MICROBLOG_RECORD == cmd) {
            long type = packetData.getDWord();
            String from = packetData.getString();
            String postid = packetData.getQWordAsString();
            long time = packetData.getDWord();
            String text = packetData.getUcs2String();
            String nick = packetData.getUcs2String();

            boolean reply = ((MRIM_BLOG_STATUS_REPLY & type) != 0);
            if (reply) {
                String alert = packetData.getUtf8String();
            }
            if (isConnected()) {
                boolean added = mrim.getMicroBlog().addPost(from, nick, text, postid, reply, time);
                if (added && !mrim.getUserId().equals(from)) {
                    //mrim.playNotification(Notify.NOTIFY_BLOG);
                }
            }
        } else {
            if (MrimPacket.MRIM_CS_NEW_EMAIL == cmd) {
                packetData.getDWord();
                String from = packetData.getUcs2String();
                String subject = packetData.getUcs2String();
                DebugLog.println("new message from " + from + "\n\n" + subject);

            } else if (0x1015 == cmd) {
                DebugLog.println("enviroment");
                while (!packetData.isEOF()) {
                    DebugLog.println("mrim: " + packetData.getString() + "=" + packetData.getUcs2String());
                }
            } else {
                DebugLog.println("mrim cmd 0x" + Integer.toHexString((int) cmd));
            }
        }
    }

    private static final int MRIM_BLOG_STATUS_UPDATE = 0x00000001;
    private static final int MRIM_BLOG_STATUS_MUSIC = 0x00000002;
    private static final int MRIM_BLOG_STATUS_REPLY = 0x00000004;
    private static final int MRIM_BLOG_STATUS_NOTIFY = 0x00000010;

    private void setUserInfo(UserInfo userInfo, String[] fields, MrimBuffer in) {
        String username = null;
        String domain = null;
        for (int i = 0; i < fields.length; ++i) {
            String field = fields[i];
            if (field.equals("nickname")) {
                userInfo.nick = in.getUcs2String();

            } else if (field.equals("firstname")) {
                userInfo.firstName = in.getUcs2String();

            } else if (field.equals("lastname")) {
                userInfo.lastName = in.getUcs2String();

            } else if (field.equals("location")) {
                userInfo.homeAddress = in.getUcs2String();

            } else if (field.equals("domain")) {
                domain = in.getUtf8String();

            } else if (field.equals("username")) {
                username = in.getUtf8String();

            } else if (field.equals("birthday")) {
                userInfo.birthDay = in.getBirthdayString();

            } else if (field.equals("sex")) {
                byte[] gender = {0, 2, 1};
                userInfo.gender = gender[Util.strToIntDef(in.getUtf8String(), 0) % 3];

            } else {
                in.getString();
            }
        }
        userInfo.auth = true;
        userInfo.uin = username + "@" + domain;
    }

    private String getMailValue(String header, String key) {
        int pos = header.indexOf(key);
        if (-1 == pos) {
            return "";
        }
        int end = header.indexOf('\n', pos);
        return header.substring(pos + key.length() + 1, end).trim();
    }

    private String[] explodeMail(String mail) {
        int delemiter = mail.indexOf("\n\n");
        String[] m = new String[2];
        m[0] = mail.substring(0, delemiter + 1);
        m[1] = mail.substring(delemiter + 2);
        return m;
    }

    private String getMailMessage(String[] mail) {
        String body = mail[1];
        if (-1 != getMailValue(mail[0], "Content-Type").indexOf("multipart/")) {
            String boundary = getMailValue(mail[0], "Boundary");
            int start = body.indexOf("--" + boundary) + 2 + boundary.length();
            int end = body.indexOf("--" + boundary, start);
            mail = explodeMail(body.substring(start, end));
            body = mail[1];
        }

        if (-1 != getMailValue(mail[0], "Content-Transfer-Encoding").indexOf("base64")) {
            byte[] data = Util.base64decode(body);
            MrimBuffer buffer = new MrimBuffer(data);
            body = buffer.getUcs2StringZ(data.length);
        }
        return body;
    }

    protected void closeSocket() {
        socket.close();
    }

    protected void ping() throws SawimException {
        if (isConnected()) {
            if (null == pingPacket) {
                pingPacket = new MrimPacket(MrimPacket.MRIM_CS_PING);
            }
            sendPacket(pingPacket);
        }
    }

    private void getContactList(MrimBuffer packetData) {
        long resultState = packetData.getDWord();
        if (GET_CONTACTS_OK != resultState) {
            return;
        }
        TemporaryRoster roster = new TemporaryRoster(mrim);
        mrim.setContactListStub();

        int groupCount = (int) packetData.getDWord();
        String groupMask = packetData.getString();
        String contactMask = packetData.getString();

        DebugLog.println("group mask " + groupMask);
        DebugLog.println("contact mask " + contactMask);

        for (int groupId = 0; groupId < groupCount; ++groupId) {
            int flags = (int) packetData.getDWord();
            String name = packetData.getUcs2String();
            packetData.skipFormattedRecord(groupMask, 2);
            if ((CONTACT_FLAG_REMOVED & flags) != 0) continue;

            MrimGroup g = (MrimGroup) roster.getGroupById(groupId);
            if (null == g) {
                g = (MrimGroup) roster.makeGroup(name);
                g.setGroupId(groupId);
            }
            g.setFlags(flags);
            g.setName(name);
            roster.addGroup(g);
        }
        boolean hasPhones = false;
        for (int cotactId = FIRST_CONTACT_ID; !packetData.isEOF(); ++cotactId) {
            int flags = (int) packetData.getDWord();
            int groupId = (int) packetData.getDWord();
            String userid = packetData.getString();
            String name = packetData.getUcs2String();
            int serverFlags = (int) packetData.getDWord();
            int status = (int) packetData.getDWord();
            String phone = packetData.getString();

            String xstatus = packetData.getString();
            String title = packetData.getUcs2String();
            String desc = packetData.getUcs2String();
            long unkVal = packetData.getDWord();
            String client = packetData.getString();


            String id = packetData.getQWordAsString();
            long time = packetData.getDWord();
            String blogPost = packetData.getUcs2String();

            packetData.skipFormattedRecord(contactMask, 7 + 5 + 4);
            if ((CONTACT_FLAG_REMOVED & flags) != 0) continue;

            MrimContact contact;
            if (MrimPhoneContact.PHONE_UIN.equals(userid)) {
                if (StringConvertor.isEmpty(phone)) continue;
                contact = new MrimPhoneContact(phone);
                groupId = MrimGroup.PHONE_CONTACTS_GROUP;

            } else {
                contact = (MrimContact) roster.makeContact(userid);
            }
            contact.init(cotactId, name, phone, groupId, serverFlags, flags);
            mrim.setContactStatus(contact, MrimConnection.getStatusIndexBy(status, xstatus), null);
            contact.setMood(xstatus, title, desc);
            hasPhones |= (MrimGroup.PHONE_CONTACTS_GROUP == contact.getGroupId());

            mrim.getMicroBlog().addPost(userid, name, blogPost, id, false, time);

            contact.setClient(client);

            roster.addContact(contact);
            if ((contact instanceof MrimChatContact) && contact.isOnline()) {
                putMultiChatGetMembers(contact.getUserId());
            }
        }
        if (hasPhones) {
            MrimGroup phoneGroup = (MrimGroup) roster.makeGroup(JLocale.getString(R.string.phone_contacts));
            phoneGroup.setName(JLocale.getString(R.string.phone_contacts));
            phoneGroup.setFlags(0);
            phoneGroup.setGroupId(MrimGroup.PHONE_CONTACTS_GROUP);
            roster.addGroup(phoneGroup);
        }
        if (isConnected()) {
            mrim.setRoster(roster.getGroups(), roster.mergeContacts());
            setProgress(100);
        }
    }

    public void disconnect() {
        connect = false;
        mrim = null;
    }

    void sendMessage(PlainMessage message) {
        boolean notify = true;
        typingTo = null;

        String text = message.getText();
        String to = message.getRcvrUin();
        int flags = 0;

        if (text.startsWith(PlainMessage.CMD_WAKEUP)) {
            notify = false;
            flags = MrimPacket.MESSAGE_FLAG_ALARM;
            //text = "/wakeup Wake up and be free!\n" +
            //        "Please, updateRoster your client on http://sawim.net.ru/";
        }

        MrimPacket packet = MrimPacket.getMessagePacket(to, text, flags);
        packet.setSeq(nextSeq());
        message.setMessageId(packet.getSeq());
        putPacketIntoQueue(packet);

        if (notify) {
            addMessage(message);
        }
    }

    void sendTypingNotify(String to, boolean isTyping) {
        if (isTyping) {
            typingTo = to;
            outTypingTask();

        } else {
            typingTo = null;
        }
    }

    void requestAuth(String to, String myUin) {
        MrimBuffer data = new MrimBuffer();
        data.putDWord(2);
        data.putString(myUin);
        data.putUcs2String("Hi!");
        putPacketIntoQueue(MrimPacket.getMessagePacket(to, Util.base64encode(data.toByteArray()),
                MrimPacket.MESSAGE_FLAG_AUTHORIZE + MrimPacket.MESSAGE_FLAG_NORECV));
    }

    void sendSms(String to, String msg) {
        String phone = (to.charAt(0) == '+') ? to
                : ('+' + ((to.charAt(0) == '8') ? '7' + to.substring(1) : to));
        MrimBuffer out = new MrimBuffer();
        out.putDWord(0);
        out.putString(phone);
        out.putUcs2String(msg);
        putPacketIntoQueue(new MrimPacket(MrimPacket.MRIM_CS_SMS, out));
    }

    void addGroup(MrimGroup group) {
        lastGroup = group;
        String name = group.getName();
        putPacketIntoQueue(MrimPacket.getAddContactPacket(CONTACT_FLAG_GROUP, 0, "", name, "", ""));
    }

    void addContact(MrimContact contact) {
        lastContact = contact;
        putPacketIntoQueue(MrimPacket.getAddContactPacket(contact.getFlags(),
                contact.getGroupId(),
                contact.getUserId(), contact.getName(), contact.getPhones(), ""));
    }

    void removeContact(MrimContact contact) {
        int flags = contact.getFlags() | CONTACT_FLAG_REMOVED;
        putPacketIntoQueue(MrimPacket.getModifyContactPacket(
                contact.getContactId(), flags, 0,
                contact.getUserId(), contact.getName(), contact.getPhones()));
    }

    void removeGroup(MrimGroup group) {
        int flags = CONTACT_FLAG_GROUP | CONTACT_FLAG_REMOVED;
        putPacketIntoQueue(MrimPacket.getModifyContactPacket(
                group.getId(), flags, 0,
                group.getName(), group.getName(), ""));
    }

    void updateContact(MrimContact contact) {
        putPacketIntoQueue(MrimPacket.getModifyContactPacket(
                contact.getContactId(), contact.getFlags(), contact.getGroupId(),
                contact.getUserId(), contact.getName(), contact.getPhones()));
    }

    void renameGroup(MrimGroup group) {
        int flags = group.getFlags() | CONTACT_FLAG_GROUP;
        putPacketIntoQueue(MrimPacket.getModifyContactPacket(
                group.getId(), flags, 0,
                group.getName(), group.getName(), ""));
    }

    void setStatus() {
        putPacketIntoQueue(MrimPacket.getSetStatusPacket(mrim));
    }

    void grandAuth(String uin) {
        putPacketIntoQueue(MrimPacket.getAutorizePacket(uin));
    }

    public void postToMicroBlog(String text, String reply) {
        MrimBuffer out = new MrimBuffer();
        if (StringConvertor.isEmpty(reply)) {
            out.putDWord(0x01);
            out.putUcs2String(text);
        } else {
            out.putDWord(0x14);
            out.putUcs2String(text);
            out.putStringAsQWord(reply);
        }
        putPacketIntoQueue(new MrimPacket(MrimPacket.MRIM_MICROBLOG_ADD_RECORD, out));
    }

    void searchUsers(Search cont) {
        this.search = cont;
        putPacketIntoQueue(MrimPacket.getUserSearchRequestPacket(cont.getSearchParams()));
    }

    UserInfo getUserInfo(MrimContact contact) {
        lastContact = contact;
        String[] userInfo = new String[Search.LAST_INDEX];
        userInfo[Search.UIN] = contact.getUserId();
        singleUserInfo = new UserInfo(mrim);
        putPacketIntoQueue(MrimPacket.getUserSearchRequestPacket(userInfo));
        return singleUserInfo;
    }

    private void setMessageSended(long msgId, long status) {
        if (MrimPacket.MESSAGE_DELIVERED == status) {
            markMessageSended(msgId, PlainMessage.NOTIFY_FROM_CLIENT);
        }
    }

    private static final int[] statusCodes = {
            STATUS_OFFLINE,
            STATUS_ONLINE,
            STATUS_AWAY,
            STATUS_CHAT,
            -1,
            -1,
            -1,
            -1,
            -1,
            -1,
            STATUS_AWAY,
            -1,
            STATUS_INVISIBLE
                    - 1};
    private static final String[] xstatusCodes = {
            "",
            "",
            "STATUS_AWAY",
            "status_chat",
            "",
            "",
            "",
            "",
            "",
            "",
            "status_dnd",
            "",
            ""};

    private static byte getStatusIndexBy(long status, String xstatus) {
        for (byte i = 0; i < xstatusCodes.length; ++i) {
            if (StringConvertor.isEmpty(xstatusCodes[i])) {
                continue;
            }
            if (xstatusCodes[i].equals(xstatus)) {
                return i;
            }
        }
        for (byte i = 0; i < statusCodes.length; ++i) {
            if (statusCodes[i] == status) {
                return i;
            }
        }
        if (STATUS_UNDETERMINATED == status) {
            return StatusInfo.STATUS_UNDETERMINATED;
        }
        return StatusInfo.STATUS_ONLINE;
    }

    static String getNativeXStatus(byte statusIndex) {
        return xstatusCodes[statusIndex];
    }

    static int getNativeStatus(byte statusIndex) {
        return statusCodes[statusIndex];
    }
}