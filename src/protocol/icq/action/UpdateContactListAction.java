


package protocol.icq.action;

import protocol.Contact;
import protocol.Group;
import protocol.icq.Icq;
import protocol.icq.IcqContact;
import protocol.icq.packet.Packet;
import protocol.icq.packet.SnacPacket;
import ru.sawim.SawimException;
import ru.sawim.comm.ArrayReader;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;

import java.util.Vector;

public class UpdateContactListAction extends IcqAction {


    private static final int STATE_ERROR = -1;
    private static final int STATE_RENAME = 1;
    private static final int STATE_COMPLETED = 3;
    private static final int STATE_MOVE1 = 4;
    private static final int STATE_MOVE2 = 5;
    private static final int STATE_MOVE3 = 6;
    private static final int STATE_ADD = 18;
    private static final int STATE_DELETE_CONTACT = 7;
    private static final int STATE_DELETE_GROUP = 9;
    private static final int STATE_ADD_GROUP = 11;
    private static final int STATE_COMMIT = 13;


    public static final int ACTION_ADD = 1;
    public static final int ACTION_DEL = 2;
    public static final int ACTION_RENAME = 3;
    public static final int ACTION_MOVE = 4;
    private static final int ACTION_ADD_REQ_AUTH = 5;
    public static final int ACTION_MOVE_REQ_AUTH = 6;
    private static final int ACTION_MOVE_FROM_NIL = 7;


    public static final int TIMEOUT = 10;


    private Contact contact;
    private int contactFromId;
    private int contactToId;


    private Group gItem;
    private Group newGItem;

    private int action;
    private int state;

    private int errorCode;


    public UpdateContactListAction(Icq icq, Contact cItem, int _action) {
        this.action = _action;
        this.contact = cItem;
        this.contactFromId = ((IcqContact) cItem).getContactId();
        this.gItem = icq.getGroup(cItem);
    }

    public UpdateContactListAction(Group cItem, int _action) {
        this.action = _action;
        this.contact = null;
        this.gItem = cItem;
    }

    public UpdateContactListAction(Contact cItem, Group oldGroup, Group newGroup) {
        this.contact = cItem;
        this.contactFromId = ((IcqContact) cItem).getContactId();
        this.gItem = oldGroup;
        this.newGItem = newGroup;
        action = ACTION_MOVE;
        if ((null != gItem) && ("Not In List".equals(gItem.getName()))) {
            action = ACTION_MOVE_FROM_NIL;
        }
    }

    private void addItem() throws SawimException {
        byte[] buf = null;
        if (null == contact) {
            buf = packGroup(gItem);
            state = STATE_ADD_GROUP;

        } else {
            gItem = getIcq().getGroup(contact);
            contactToId = getIcq().createRandomId();
            buf = packContact(contact, contactToId, gItem.getId(), action == ACTION_ADD_REQ_AUTH);
            state = STATE_ADD;
        }
        sendSsiPacket(SnacPacket.CLI_ROSTERADD_COMMAND, buf);
    }

    private void sendSsiPacket(int cmd, byte[] buf) throws SawimException {
        sendPacket(new SnacPacket(SnacPacket.SSI_FAMILY,
                cmd, getConnection().getNextCounter(), buf));
    }

    public void init() throws SawimException {
        byte[] buf = null;

        if (ACTION_RENAME != action) {

            transactionStart();
        }

        switch (action) {

            case ACTION_RENAME:
                buf = (null != contact) ? packContact(contact, contactFromId, contact.getGroupId(), false) : packGroup(gItem);
                sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND, buf);
                state = STATE_RENAME;
                break;


            case ACTION_ADD:
            case ACTION_ADD_REQ_AUTH:
                addItem();
                break;


            case ACTION_MOVE_FROM_NIL:
            case ACTION_DEL:
                if (null != contact) {
                    buf = packContact(contact, contactFromId, gItem.getId(), false);
                    this.state = STATE_DELETE_CONTACT;
                } else {
                    buf = packGroup(gItem);
                    this.state = STATE_DELETE_GROUP;
                }

                sendSsiPacket(SnacPacket.CLI_ROSTERDELETE_COMMAND, buf);
                break;


            case ACTION_MOVE:
                sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND,
                        packContact(contact, contactFromId, gItem.getId(), false));

                this.state = STATE_MOVE1;
                break;
        }
        active();
    }

    private boolean processPaket(Packet packet) throws SawimException {

        SnacPacket snacPacket = null;
        if (packet instanceof SnacPacket) {
            snacPacket = (SnacPacket) packet;
        } else {
            return false;
        }

        if (SnacPacket.SSI_FAMILY != snacPacket.getFamily()) {
            return false;
        }
        if (SnacPacket.CLI_ROSTERDELETE_COMMAND == snacPacket.getCommand()) {
            ArrayReader buf = snacPacket.getReader();
            int length = buf.getWordBE();
            String uin = StringConvertor.byteArrayToAsciiString(
                    buf.getArray(length), 0, length);
            return (null != contact) && uin.equals(contact.getUserId());
        }
        if (SnacPacket.SRV_UPDATEACK_COMMAND != snacPacket.getCommand()) {
            return false;
        }


        int retCode = snacPacket.getReader().getWordBE();
        switch (retCode) {
            case 0x002:
                errorCode = 154;
                break;
            case 0x003:
                errorCode = 155;
                break;
            case 0x00A:
                errorCode = 156;
                break;
            case 0x00C:
                errorCode = 157;
                break;
            case 0x00D:
                errorCode = 158;
                break;
        }
        if ((0x00A == retCode) && (ACTION_MOVE_FROM_NIL == action)) {
            errorCode = 0;
            state = STATE_COMPLETED;
            contact.setGroup(newGItem);
            contact.setTempFlag(false);
            getIcq().addContact(contact);
            return true;
        }
        if ((0x00A == retCode) && (ACTION_DEL == action)) {
            errorCode = 0;
            state = STATE_COMPLETED;
            return true;
        }
        if (0 != errorCode) {

            DebugLog.println("updateRoster action = " + action
                    + " state = " + state
                    + " ret code = " + retCode);


            getIcq().showException(new SawimException(errorCode, 0));
            state = STATE_ERROR;
            return true;
        }

        switch (state) {
            case STATE_ADD_GROUP:
                if (0 < getIcq().getGroupItems().size()) {
                    sendGroupsList();
                    this.state = STATE_COMMIT;
                } else {
                    transactionCommit();
                    this.state = STATE_COMPLETED;
                }
                break;

            case STATE_ADD:
                if (0 == retCode) {
                    sendGroup(gItem);
                    ((IcqContact) contact).setContactId(contactToId);
                    contact.setBooleanValue(IcqContact.CONTACT_NO_AUTH, action == ACTION_ADD_REQ_AUTH);
                    this.state = STATE_COMPLETED;
                }
                transactionCommit();
                if ((0 != retCode) && (action == ACTION_ADD)) {
                    action = ACTION_ADD_REQ_AUTH;
                    transactionStart();
                    addItem();
                }
                break;

            case STATE_RENAME:
                this.state = STATE_COMPLETED;
                break;


            case STATE_MOVE1:
                sendSsiPacket(SnacPacket.CLI_ROSTERDELETE_COMMAND,
                        packContact(contact, contactFromId, gItem.getId(), false));

                this.state = STATE_MOVE2;
                break;

            case STATE_MOVE2:
                contactToId = getIcq().createRandomId();
                sendSsiPacket(SnacPacket.CLI_ROSTERADD_COMMAND,
                        packContact(contact, contactToId, newGItem.getId(), action == ACTION_MOVE_REQ_AUTH));
                this.state = STATE_MOVE3;
                break;

            case STATE_MOVE3:
                ((IcqContact) contact).setContactId(contactToId);
                sendGroup(newGItem);
                this.state = STATE_COMMIT;
                break;

            case STATE_DELETE_CONTACT:
                sendGroup(gItem);
                this.state = STATE_COMMIT;
                break;

            case STATE_DELETE_GROUP:
                sendGroupsList();
                this.state = STATE_COMMIT;
                break;


            case STATE_COMMIT:
                transactionCommit();
                this.state = STATE_COMPLETED;
                break;
        }

        active();
        return true;
    }

    private void transactionStart() throws SawimException {
        sendSsiPacket(SnacPacket.CLI_ADDSTART_COMMAND, new byte[0]);
    }

    private void transactionCommit() throws SawimException {
        sendSsiPacket(SnacPacket.CLI_ADDEND_COMMAND, new byte[0]);
    }

    private void sendGroupsList() throws SawimException {
        sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND, packGroups());
    }

    private void sendGroup(Group group) throws SawimException {
        sendSsiPacket(SnacPacket.CLI_ROSTERUPDATE_COMMAND, packGroup(group));
    }


    public boolean forward(Packet packet) throws SawimException {
        boolean result = processPaket(packet);

        if (result && (0 != errorCode)) {
            if ((ACTION_MOVE != action) && (ACTION_RENAME != action)) {
                transactionCommit();
            }
            active();
        }

        return result;
    }


    public boolean isCompleted() {
        return (this.state == UpdateContactListAction.STATE_COMPLETED);
    }

    public boolean isError() {
        if (this.state == ConnectAction.STATE_ERROR) return true;
        if (isNotActive(TIMEOUT) || (errorCode != 0)) {
            this.state = ConnectAction.STATE_ERROR;
        }
        return (this.state == ConnectAction.STATE_ERROR);
    }

    private byte[] packContact(Contact cItem, int contactId, int groupId, boolean auth) {
        Util stream = new Util();

        stream.writeLenAndUtf8String(cItem.getUserId());
        stream.writeWordBE(groupId);
        stream.writeWordBE(contactId);
        stream.writeWordBE(0);


        Util addData = new Util();


        if ((ACTION_DEL != action) && (ACTION_MOVE_FROM_NIL != action)) {
            addData.writeWordBE(0x0131);
            addData.writeLenAndUtf8String(cItem.getName());
        }


        if (auth) {
            addData.writeTLV(0x0066, null);
        }


        stream.writeWordBE(addData.size());
        stream.writeByteArray(addData.toByteArray());


        return stream.toByteArray();
    }

    private byte[] packGroup(Group gItem) {
        Util stream = new Util();

        stream.writeLenAndUtf8String(gItem.getName());
        stream.writeWordBE(gItem.getId());
        stream.writeWordBE(0);
        stream.writeWordBE(1);


        Vector items = gItem.getContacts();
        int size = items.size();
        if (size != 0) {

            stream.writeWordBE(size * 2 + 4);


            stream.writeWordBE(0x00c8);
            stream.writeWordBE(size * 2);
            for (int i = 0; i < size; ++i) {
                IcqContact item = (IcqContact) items.elementAt(i);
                stream.writeWordBE(item.getContactId());
            }
        } else {
            stream.writeWordBE(0);
        }

        return stream.toByteArray();
    }

    private byte[] packGroups() {
        Util stream = new Util();

        Vector gItems = getIcq().getGroupItems();
        int size = gItems.size();
        stream.writeLenAndUtf8String("");
        stream.writeWordBE(0);
        stream.writeWordBE(0);
        stream.writeWordBE(1);
        stream.writeWordBE(size * 2 + 4);
        stream.writeWordBE(0xc8);
        stream.writeWordBE(size * 2);

        for (int i = 0; i < size; ++i) {
            stream.writeWordBE(((Group) gItems.elementAt(i)).getId());
        }

        return stream.toByteArray();
    }
}


