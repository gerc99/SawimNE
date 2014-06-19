package protocol.icq.action;

import protocol.Contact;
import protocol.Group;
import protocol.TemporaryRoster;
import protocol.icq.Icq;
import protocol.icq.IcqContact;
import protocol.icq.PrivacyItem;
import protocol.icq.packet.*;
import ru.sawim.Options;
import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.modules.crypto.MD5;

import java.util.Vector;

public class ConnectAction extends IcqAction {

    public static final int STATE_ERROR = -1;
    public static final int STATE_INIT = 0;
    public static final int STATE_INIT_DONE = 1;
    public static final int STATE_AUTHKEY_REQUESTED = 2;
    public static final int STATE_CLI_IDENT_SENT = 3;
    public static final int STATE_CLI_DISCONNECT_SENT = 4;
    public static final int STATE_CLI_COOKIE_SENT = 5;
    public static final int STATE_CLI_WANT_CAPS_SENT = 6;
    public static final int STATE_CLI_WANT_CAPS_SENT2 = 7;
    public static final int STATE_CLI_CHECKROSTER_SENT = 8;
    public static final int STATE_CLI_STATUS_INFO_SENT = 9;
    public static final int STATE_CLI_REQOFFLINEMSGS_SENT = 10;
    public static final int STATE_CLI_ACKOFFLINEMSGS_SENT = 11;
    private static final short[] FAMILIES_AND_VER_LIST = {
            0x0022, 0x0001,
            0x0001, 0x0004,
            0x0013, 0x0004,
            0x0002, 0x0001,
            0x0025, 0x0001,
            0x0003, 0x0001,
            0x0015, 0x0001,
            0x0004, 0x0001,
            0x0006, 0x0001,
            0x0009, 0x0001,
            0x000a, 0x0001,
            0x000b, 0x0001};

    private Vector ignoreList = new Vector();
    private Vector invisibleList = new Vector();
    private Vector visibleList = new Vector();

    private int timestamp;
    private int count;
    private TemporaryRoster roster;

    private static final byte[] CLI_READY_DATA = {

            (byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x01, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x13, (byte) 0x00, (byte) 0x04, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x25, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x03, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x15, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x04, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x06, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x09, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x0a, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2,
            (byte) 0x00, (byte) 0x0b, (byte) 0x00, (byte) 0x01, (byte) 0x01, (byte) 0x10, (byte) 0x17, (byte) 0xf2
    };

    private static final int TIMEOUT = 20;
    private static final byte[] AIM_MD5_STRING = new byte[]{'A', 'O', 'L', ' ', 'I', 'n', 's', 't', 'a', 'n', 't', ' ',
            'M', 'e', 's', 's', 'e', 'n', 'g', 'e', 'r', ' ', '(', 'S', 'M', ')'};


    private String uin;
    private String password;

    private int state;
    private volatile boolean active;
    private String[] server;
    private byte[] cookie;

    private String[] getServerHostAndPort() {
        return new String[]{"login.icq.com", "5190"};
    }

    private boolean md5login;

    public ConnectAction(Icq icq) {
        super();
        this.uin = icq.getUserId();
        this.password = icq.getPassword();
        this.server = getServerHostAndPort();
        active();
        if (8 < password.length()) {
            this.password = password.substring(0, 8);
        }
        md5login = (-1 == uin.indexOf('@'));
    }

    private void setProgress(int progress) {
        getIcq().setConnectingProgress(progress);
    }

    public void init() throws SawimException {
        this.active = true;
        this.state = ConnectAction.STATE_INIT;
        active();
        getConnection().connectTo(server[0], Integer.parseInt(server[1]));
        state = ConnectAction.STATE_INIT_DONE;
        this.active = false;
        active();
    }

    public boolean forward(Packet packet) throws SawimException {
        if (null == getIcq()) {
            return false;
        }
        this.active = true;
        active();
        try {
            boolean consumed = false;
            if (ConnectAction.STATE_INIT_DONE == this.state) {

                if (packet instanceof ConnectPacket) {
                    ConnectPacket connectPacket = (ConnectPacket) packet;
                    if (ConnectPacket.SRV_CLI_HELLO == connectPacket.getType()) {
                        if (md5login) {
                            sendPacket(new ConnectPacket());
                            Util stream = new Util();
                            stream.writeWordBE(0x0001);
                            stream.writeLenAndUtf8String(uin);
                            stream.writeTLV(0x4b, new byte[0]);
                            sendPacket(new SnacPacket(0x0017, 0x0006, -1, stream.toByteArray()));
                            state = STATE_AUTHKEY_REQUESTED;
                        } else {
                            sendPacket(new ConnectPacket(uin, password));
                            state = ConnectAction.STATE_CLI_IDENT_SENT;
                        }
                        consumed = true;
                    }
                }

            } else if (STATE_AUTHKEY_REQUESTED == state) {
                if (packet instanceof SnacPacket) {
                    SnacPacket snacPacket = (SnacPacket) packet;
                    if ((0x0017 == snacPacket.getFamily())
                            && (0x0007 == snacPacket.getCommand())) {
                        Util stream = new Util();
                        stream.writeTLV(0x0001, uin.getBytes());

                        ArrayReader rbuf = snacPacket.getReader();
                        byte[] authkey = rbuf.getArray(rbuf.getWordBE());

                        byte[] passwordRaw = StringConvertor.stringToByteArray(password);

                        byte[] passkey = MD5.calculate(passwordRaw);

                        byte[] md5buf = new byte[authkey.length + passkey.length + AIM_MD5_STRING.length];
                        int md5marker = 0;
                        System.arraycopy(authkey, 0, md5buf, md5marker, authkey.length);
                        md5marker += authkey.length;
                        System.arraycopy(passkey, 0, md5buf, md5marker, passkey.length);
                        md5marker += passkey.length;
                        System.arraycopy(AIM_MD5_STRING, 0, md5buf, md5marker, AIM_MD5_STRING.length);

                        stream.writeTLV(0x0025, MD5.calculate(md5buf));

                        ConnectPacket.putVersion(stream, true);

                        sendPacket(new SnacPacket(0x0017, 0x0002, -1, stream.toByteArray()));
                        state = STATE_CLI_IDENT_SENT;
                    } else {

                        DebugLog.println("connect: family = 0x" + Integer.toHexString(snacPacket.getFamily())
                                + " command = 0x" + Integer.toHexString(snacPacket.getCommand()));

                        throw new SawimException(100, 0);
                    }
                }
                consumed = true;
            } else if (STATE_CLI_IDENT_SENT == state) {
                int errcode = -1;
                if (md5login) {
                    if (packet instanceof SnacPacket) {
                        SnacPacket snacPacket = (SnacPacket) packet;
                        if ((0x0017 == snacPacket.getFamily())
                                && (0x0003 == snacPacket.getCommand())) {
                            ArrayReader marker = snacPacket.getReader();
                            while (marker.isNotEnd()) {
                                int tlvType = marker.getTlvType();
                                byte[] tlvData = marker.getTlv();
                                switch (tlvType) {
                                    case 0x0001:
                                        getIcq().setRealUin(StringConvertor.byteArrayToAsciiString(tlvData));
                                        this.uin = getIcq().getUserId();
                                        break;
                                    case 0x0008:
                                        errcode = Util.getWordBE(tlvData, 0);
                                        break;
                                    case 0x0005:
                                        this.server = Util.explode(StringConvertor.byteArrayToAsciiString(tlvData), ':');
                                        break;
                                    case 0x0006:
                                        this.cookie = tlvData;
                                        break;
                                }
                            }
                        }
                    } else if (packet instanceof DisconnectPacket) {
                        consumed = true;
                    }
                } else {
                    if (packet instanceof DisconnectPacket) {
                        DisconnectPacket disconnectPacket = (DisconnectPacket) packet;

                        if (DisconnectPacket.TYPE_SRV_COOKIE == disconnectPacket.getType()) {
                            getIcq().setRealUin(disconnectPacket.getUin());
                            this.uin = getIcq().getUserId();
                            this.cookie = disconnectPacket.getCookie();
                            this.server = Util.explode(disconnectPacket.getServer(), ':');

                        } else if (DisconnectPacket.TYPE_SRV_GOODBYE == disconnectPacket.getType()) {
                            errcode = disconnectPacket.getError();
                        }
                        consumed = true;
                    }
                }

                if (-1 != errcode) {
                    int toThrow = 100;
                    switch (errcode) {

                        case 0x0001:
                            toThrow = 110;
                            break;

                        case 0x0004:
                        case 0x0005:
                            toThrow = 111;
                            break;

                        case 0x0007:
                        case 0x0008:
                            toThrow = 112;
                            break;

                        case 0x0015:
                        case 0x0016:
                            toThrow = 113;
                            break;

                        case 0x0018:
                        case 0x001d:
                            toThrow = 114;
                            break;
                    }
                    if (111 == toThrow) {
                        getIcq().setPassword(null);
                    }
                    throw new SawimException(toThrow, errcode);
                }

                if (consumed & (null != this.server) & (null != this.cookie)) {
                    getConnection().connectTo(server[0], Integer.parseInt(server[1]));
                    this.state = ConnectAction.STATE_CLI_DISCONNECT_SENT;
                }
            } else if (STATE_CLI_DISCONNECT_SENT == state) {
                if (packet instanceof ConnectPacket) {
                    ConnectPacket connectPacket = (ConnectPacket) packet;
                    if (connectPacket.getType() == ConnectPacket.SRV_CLI_HELLO) {
                        DebugLog.println("connect say 'hello'");
                        ConnectPacket reply = new ConnectPacket(this.cookie);
                        sendPacket(reply);
                        this.state = ConnectAction.STATE_CLI_COOKIE_SENT;
                        consumed = true;
                    }
                }
            } else if (STATE_CLI_COOKIE_SENT == state) {
                Util stream = new Util();

                for (int i = 0; i < FAMILIES_AND_VER_LIST.length; ++i) {
                    stream.writeWordBE(FAMILIES_AND_VER_LIST[i]);
                }
                sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_FAMILIES_COMMAND, SnacPacket.CLI_FAMILIES_COMMAND, stream.toByteArray()));
                this.state = ConnectAction.STATE_CLI_WANT_CAPS_SENT;

            } else if (STATE_CLI_WANT_CAPS_SENT == state) {

                if (packet instanceof SnacPacket) {
                    SnacPacket s = (SnacPacket) packet;
                    if ((SnacPacket.SERVICE_FAMILY == s.getFamily())
                            && (SnacPacket.SRV_MOTD_COMMAND == s.getCommand())) {
                        sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY,
                                SnacPacket.CLI_RATESREQUEST_COMMAND));
                        this.state = ConnectAction.STATE_CLI_WANT_CAPS_SENT2;
                    }
                }

            } else if (STATE_CLI_WANT_CAPS_SENT2 == state) {
                Util udata = null;
                udata = new Util();
                udata.writeWordBE(0x0001);
                udata.writeWordBE(0x0002);
                udata.writeWordBE(0x0003);
                udata.writeWordBE(0x0004);
                udata.writeWordBE(0x0005);
                sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_ACKRATES_COMMAND, 0x6F4E0000, udata.toByteArray()));
                sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_REQINFO_COMMAND, 0x2BDD0000));
                udata = new Util();
                udata.writeTLVWord(0x000B, 0x00FF);
                sendPacket(new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_REQLISTS_COMMAND, SnacPacket.CLI_REQLISTS_COMMAND, udata.toByteArray()));

                SnacPacket reply2;
                timestamp = getIcq().getSsiListLastChangeTime();
                count = getIcq().getSsiNumberOfItems();
                if ((-1 == timestamp) || (0 == count) || (0 == getIcq().getContactItems().size())) {
                    reply2 = new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_REQROSTER_COMMAND, 0x07630000);
                } else {
                    Util data = new Util();
                    data.writeDWordBE(timestamp);
                    data.writeWordBE(count);
                    reply2 = new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_CHECKROSTER_COMMAND, data.toByteArray());
                }
                sendPacket(reply2);

                roster = new TemporaryRoster(getIcq());
                getIcq().setContactListInfo(-1, 0);
                getIcq().setContactListStub();
                this.state = ConnectAction.STATE_CLI_CHECKROSTER_SENT;

            } else if (STATE_CLI_CHECKROSTER_SENT == state) {
                if (packet instanceof SnacPacket) {
                    consumed = loadContactList((SnacPacket) packet);
                }
            } else if (STATE_CLI_STATUS_INFO_SENT == state) {
                sendPacket(new ToIcqSrvPacket(0x00000000, this.uin, ToIcqSrvPacket.CLI_REQOFFLINEMSGS_SUBCMD, new byte[0], new byte[0]));
                getConnection().initPing();
                getConnection().setIcqConnected();
                this.state = ConnectAction.STATE_CLI_REQOFFLINEMSGS_SENT;
            }
            active();
            this.active = false;
            updateProgress();
            return consumed;
        } catch (SawimException e) {
            this.active = false;
            this.state = ConnectAction.STATE_ERROR;
            throw e;
        }
    }

    private void requestOtherContacts() throws SawimException {
        Util stream = new Util();
        stream.writeTLVByte(0x08, 1);
        sendPacket(new SnacPacket(SnacPacket.CONTACT_FAMILY, SnacPacket.CLI_REQBUDDY_COMMAND, stream.toByteArray()));
    }

    private void processOtherContacts(SnacPacket snacPacket) throws SawimException {
    }

    private void sendStatusData() throws SawimException {
        byte pstatus = getIcq().getIcqPrivateStatus();
        sendPacket(getIcq().getPrivateStatusPacket(pstatus));

        int x = getIcq().getProfile().xstatusIndex;
        String title = getIcq().getProfile().xstatusTitle;
        String desc = getIcq().getProfile().xstatusDescription;

        title = StringConvertor.notNull(title);
        desc = StringConvertor.notNull(desc);
        String text = (title + " " + desc).trim();
        sendPacket(getIcq().getNewXStatusPacket(x, text));
        sendPacket(getIcq().getStatusPacket());
        sendPacket(getIcq().getCapsPacket());
        sendPacket(getIcbmPacket());
    }

    private boolean loadContactList(SnacPacket snacPacket) throws SawimException {
        boolean consumed = false;
        if (SnacPacket.SSI_FAMILY != snacPacket.getFamily()) {
            return false;
        }
        boolean srvReplyRosterRcvd = false;
        boolean newRosterLoaded = false;
        if (SnacPacket.SRV_REPLYROSTEROK_COMMAND == snacPacket.getCommand()) {
            srvReplyRosterRcvd = true;
            newRosterLoaded = false;
            consumed = true;
            roster.useOld();
            getIcq().setRoster(roster.getGroups(), roster.mergeContacts());
            getIcq().setContactListInfo(timestamp, count);
        } else if (SnacPacket.SRV_REPLYROSTER_COMMAND == snacPacket.getCommand()) {
            if (1 != snacPacket.getFlags()) {
                srvReplyRosterRcvd = true;
                newRosterLoaded = true;
            }
            ArrayReader marker = snacPacket.getReader();
            marker.skip(1);
            count = marker.getWordBE();
            for (int i = 0; i < count; ++i) {
                int nameLen = marker.getWordBE();
                String userId = StringConvertor.utf8beByteArrayToString(
                        marker.getArray(nameLen), 0, nameLen);
                int groupId = marker.getWordBE();
                int id = marker.getWordBE();
                int type = marker.getWordBE();
                int len = marker.getWordBE();
                if (0x0000 == type) {
                    String nick = userId;
                    boolean noAuth = false;
                    int end = marker.getOffset() + len;
                    while (marker.getOffset() < end) {
                        int tlvType = marker.getTlvType();
                        if (0x0131 == tlvType) {
                            byte[] tlvData = marker.getTlv();
                            nick = StringConvertor.utf8beByteArrayToString(tlvData, 0, tlvData.length);
                        } else {
                            if (0x0066 == tlvType) {
                                noAuth = true;
                            }
                            marker.skipTlv();
                        }
                    }
                    try {
                        IcqContact item = (IcqContact) roster.makeContact(userId);
                        if (nick.equals(userId) && !StringConvertor.isEmpty(item.getName())) {
                            nick = item.getName();
                        }
                        item.init(id, groupId, nick, noAuth);
                        roster.addContact(item);
                    } catch (Exception e) {
                    }

                } else if (0x0001 == type) {
                    marker.skip(len);

                    if (0x0000 != groupId) {
                        Group grp = roster.getGroupById(groupId);
                        if (null == grp) {
                            grp = roster.makeGroup(userId);
                            grp.setGroupId(groupId);
                        } else {
                            grp.setName(userId);
                        }
                        if ("Not In List".equals(userId)) {
                            grp.setMode(Group.MODE_REMOVABLE | Group.MODE_BOTTOM);
                        } else {
                            grp.setMode(Group.MODE_FULL_ACCESS);
                        }
                        roster.addGroup(grp);
                    }

                } else if (0x0002 == type) {
                    marker.skip(len);
                    visibleList.addElement(new PrivacyItem(userId, id));

                } else if (0x0003 == type) {
                    marker.skip(len);
                    invisibleList.addElement(new PrivacyItem(userId, id));

                } else if (0x000E == type) {
                    marker.skip(len);
                    ignoreList.addElement(new PrivacyItem(userId, id));

                } else if (0x0004 == type) {
                    int end = marker.getOffset() + len;
                    while (marker.getOffset() < end) {
                        int tlvType = marker.getTlvType();
                        marker.skipTlv();

                        if (0x00CA == tlvType) {
                            getIcq().privateStatusId = id;
                        }
                    }
                } else {
                    marker.skip(len);
                }
            }
            timestamp = (int) marker.getDWordBE();
            consumed = true;
        }
        if (newRosterLoaded) {
            Vector contactItems = roster.mergeContacts();
            active();
            for (int i = 0; i < contactItems.size(); ++i) {
                IcqContact contact = (IcqContact) contactItems.elementAt(i);
                String userId = contact.getUserId();
                contact.setBooleanValue(Contact.SL_IGNORE, inList(ignoreList, userId));
                contact.setBooleanValue(Contact.SL_VISIBLE, inList(visibleList, userId));
                contact.setBooleanValue(Contact.SL_INVISIBLE, inList(invisibleList, userId));
            }
            getIcq().setPrivacyLists(ignoreList, invisibleList, visibleList);
            getIcq().setRoster(roster.getGroups(), contactItems);
            getIcq().setContactListInfo(timestamp, count);
        }
        if (srvReplyRosterRcvd) {
            DebugLog.memoryUsage("Connect memory usage");
            sendStatusData();
            sendPacket(new SnacPacket(SnacPacket.SSI_FAMILY, SnacPacket.CLI_ROSTERACK_COMMAND));
            sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_READY_COMMAND, ConnectAction.CLI_READY_DATA));
            this.state = ConnectAction.STATE_CLI_STATUS_INFO_SENT;
        }
        return consumed;
    }

    private SnacPacket getIcbmPacket() {
        long flags = 0x0003;
        flags |= 0x0B;

        Util icbm = new Util();
        icbm.writeWordBE(0x0000);
        icbm.writeDWordBE(flags);
        icbm.writeWordBE(8000);
        icbm.writeWordBE(0x03E7);
        icbm.writeWordBE(0x03E7);
        icbm.writeWordBE(0x0000);
        icbm.writeWordBE(0x0000);

        return new SnacPacket(SnacPacket.CLI_ICBM_FAMILY, SnacPacket.CLI_SETICBM_COMMAND, icbm.toByteArray());
    }

    private boolean inList(Vector list, String uin) {
        PrivacyItem item;
        for (int i = list.size() - 1; 0 <= i; --i) {
            item = (PrivacyItem) list.elementAt(i);
            if (uin.equals(item.userId)) {
                item.userId = uin;
                return true;
            }
        }
        return false;
    }

    public boolean isCompleted() {
        return (null == getIcq()) || getIcq().isConnected();
    }

    public boolean isError() {
        if (isCompleted()) {
            return false;
        }
        if (state == ConnectAction.STATE_ERROR) {
            return true;
        }
        if (!active && isNotActive(TIMEOUT)) {
            state = ConnectAction.STATE_ERROR;
            getConnection().processIcqException(new SawimException(118, 0));
        }
        return state == ConnectAction.STATE_ERROR;
    }

    private void updateProgress() {
        setProgress(getProgress());
    }

    public void initProgressBar() {
        setProgress(0);
    }

    private int getProgress() {
        switch (this.state) {
            case STATE_INIT:
                return 1;
            case STATE_INIT_DONE:
                return 12;
            case STATE_AUTHKEY_REQUESTED:
                return 25;
            case STATE_CLI_IDENT_SENT:
                return 37;
            case STATE_CLI_DISCONNECT_SENT:
                return 50;
            case STATE_CLI_COOKIE_SENT:
                return 62;
            case STATE_CLI_WANT_CAPS_SENT:
                return 68;
            case STATE_CLI_WANT_CAPS_SENT2:
                return 69;
            case STATE_CLI_CHECKROSTER_SENT:
                return 75;
            case STATE_CLI_STATUS_INFO_SENT:
                return 90;
            case STATE_CLI_REQOFFLINEMSGS_SENT:
                return 100;
        }
        return 2;
    }
}
