package protocol.mrim;

import protocol.*;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.comm.Util;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListModel;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;
import ru.sawim.roster.RosterHelper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Vector;

public class Mrim extends Protocol {
    private MrimConnection connection = null;
    private MicroBlog microBlog;
    private static final ImageList statusIcons = ImageList.createImageList("/mrim-status.png");
    private static final int[] statusIconIndex = {1, 0, 3, 4, -1, -1, -1, -1, -1, -1, 5, -1, 2, -1, 1};

    public static Icon getPhoneContactIcon() {
        int phoneContactIndex = statusIconIndex[StatusInfo.STATUS_OFFLINE];
        if (6 < statusIcons.size()) {
            phoneContactIndex = statusIcons.size() - 1;
        }
        return statusIcons.iconAt(phoneContactIndex);
    }

    public MicroBlog getMicroBlog() {
        return microBlog;
    }

    private static final byte[] statuses = {
            StatusInfo.STATUS_OFFLINE,
            StatusInfo.STATUS_CHAT,
            StatusInfo.STATUS_ONLINE,
            StatusInfo.STATUS_AWAY,
            StatusInfo.STATUS_UNDETERMINATED,
            StatusInfo.STATUS_INVISIBLE};

    public Mrim() {
    }

    protected void initStatusInfo() {
        info = new StatusInfo(statusIcons, statusIconIndex, statuses);
        microBlog = new MicroBlog(this);
        xstatusInfo = Mrim.xStatus.getInfo();
        clientInfo = MrimClient.get();
    }

    protected String processUin(String uin) {
        return uin.toLowerCase();
    }

    public boolean isEmpty() {
        return super.isEmpty() || (getUserId().indexOf('@') <= 0);
    }

    public String getUniqueUserId(Contact contact) {
        return getUniqueUserId(contact.getUserId());
    }

    @Override
    public String getUniqueUserId(String userId) {
        if (userId.endsWith("@uin.icq")) {
            return userId.substring(0, userId.indexOf("@"));
        }
        return userId;
    }

    public void startConnection() {
        connection = new MrimConnection(this);
        connection.start();
    }

    public MrimConnection getConnection() {
        return connection;
    }

    public boolean isConnected() {
        return (null != connection) && connection.isConnected();
    }

    private Group getPhoneGroup() {
        MrimGroup phoneGroup = (MrimGroup) getGroupById(MrimGroup.PHONE_CONTACTS_GROUP);
        if (null != phoneGroup) {
            return phoneGroup;
        }
        phoneGroup = (MrimGroup) createGroup(JLocale.getString(R.string.phone_contacts));
        phoneGroup.setFlags(0);
        phoneGroup.setGroupId(MrimGroup.PHONE_CONTACTS_GROUP);
        addGroup(phoneGroup);
        return phoneGroup;
    }

    protected Contact createContact(String uin, String name) {
        name = (null == name) ? uin : name;
        if (-1 == uin.indexOf('@')) {
            if (0 < Util.strToIntDef(uin, 0)) {
                uin = uin + "@uin.icq";
            }
            if ("phone".equals(uin)) {
                return new MrimPhoneContact("");
            }
        }
        if (uin.endsWith("@chat.agent")) {
            return new MrimChatContact(uin, name);
        }
        return new MrimContact(uin, name);
    }

    protected void closeConnection() {
        MrimConnection c = connection;
        connection = null;
        if (null != c) {
            c.disconnect();
        }
    }

    protected void sendSomeMessage(PlainMessage msg) {
        connection.sendMessage(msg);
    }

    protected void s_sendTypingNotify(Contact to, boolean isTyping) {
        if (to.isSingleUserContact()) {
            connection.sendTypingNotify(to.getUserId(), isTyping);
        }
    }

    public boolean isMeVisible(Contact to) {
        if (to.inInvisibleList()) {
            return false;
        }
        if (to.inIgnoreList()) {
            return false;
        }
        return true;
    }

    public static final MrimXStatusInfo xStatus = new MrimXStatusInfo();

    protected void s_updateXStatus() {
        connection.setStatus();
    }

    protected void s_setPrivateStatus() {
        if (isConnected()) {
            connection.setStatus();
        }
    }

    public int getPrivateStatusMask() {
        return 0x00000000;
    }

    protected void s_searchUsers(Search cont) {
        String uin = cont.getSearchParam(Search.UIN);
        if ((null != uin) && (-1 == uin.indexOf('@'))) {
            UserInfo userInfo = new UserInfo(this);
            userInfo.uin = uin;
            if (null != userInfo.uin) {
                cont.addResult(userInfo);
            }
            cont.putToGroup(getPhoneGroup());
            cont.finished();
            return;
        }
        connection.searchUsers(cont);
    }

    protected void s_updateOnlineStatus() {
        connection.setStatus();
    }

    protected void s_addContact(Contact contact) {
        connection.addContact((MrimContact) contact);
    }

    public void requestAuth(String uin) {
        connection.requestAuth(uin, getUserId());
    }

    public void grandAuth(String uin) {
        connection.grandAuth(uin);
    }

    protected void denyAuth(String userId) {
    }

    protected void s_removeContact(Contact contact) {
        connection.removeContact((MrimContact) contact);
    }

    protected void s_addGroup(Group group) {
        connection.addGroup((MrimGroup) group);
    }

    public Group createGroup(String name) {
        return new MrimGroup(-1, 0, name);
    }

    protected void s_removeGroup(Group group) {
        connection.removeGroup((MrimGroup) group);
    }

    protected void s_renameGroup(Group group, String name) {
        group.setName(name);
        connection.renameGroup((MrimGroup) group);
    }

    protected void s_moveContact(Contact contact, Group to) {
        contact.setGroup(to);
        getConnection().updateContact((MrimContact) contact);
    }

    protected void s_renameContact(Contact contact, String name) {
        contact.setName(name);
        getConnection().updateContact((MrimContact) contact);
    }

    public void sendSms(String phone, String text) {
        getConnection().sendSms(phone, text);
    }

    public MrimContact getContactByPhone(String phone) {
        for (int i = getContactItems().size() - 1; i >= 0; i--) {
            MrimContact contact = (MrimContact) getContactItems().elementAt(i);
            String phones = contact.getPhones();
            if ((null != phones) && (-1 != phones.indexOf(phone))) {
                return contact;
            }
        }
        return null;
    }

    protected Contact loadContact(DataInputStream dis) throws Exception {
        int contactId = dis.readInt();
        String uin = dis.readUTF();
        String name = dis.readUTF();
        String phones = dis.readUTF();
        int groupId = dis.readInt();
        final int serverFlags = 0;
        byte booleanValues = dis.readByte();
        int flags = dis.readInt();
        MrimContact c = (MrimContact) createContact(uin, name);
        c.setPhones(phones);
        c.init(contactId, name, phones, groupId, serverFlags, flags);
        c.setBooleanValues(booleanValues);
        return c;
    }

    protected void saveContact(DataOutputStream out, Contact contact) throws Exception {
        MrimContact mrimContact = (MrimContact) contact;
        if (contact instanceof MrimPhoneContact) return;
        out.writeByte(0);
        out.writeInt(mrimContact.getContactId());
        out.writeUTF(contact.getUserId());
        out.writeUTF(contact.getName());
        out.writeUTF(StringConvertor.notNull(mrimContact.getPhones()));
        out.writeInt(contact.getGroupId());
        out.writeByte(contact.getBooleanValues());
        out.writeInt(mrimContact.getFlags());
    }

    public void getAvatar(UserInfo userInfo) {
        new GetAvatar().getAvatar(userInfo);
    }

    public String getUserIdName() {
        return "E-mail";
    }

    public void saveUserInfo(UserInfo userInfo) {
    }

    protected void doAction(BaseActivity activity, Contact c, int action) {
        MrimContact contact = (MrimContact) c;
        switch (action) {
            case MrimContact.USER_MENU_SEND_SMS:
                new ru.sawim.forms.SmsForm(this, contact.getPhones()).show(activity);
                break;

            case ContactMenu.CONFERENCE_DISCONNECT:
                new ContactMenu(this, c).doAction(activity, ContactMenu.USER_MENU_USER_REMOVE);
                break;

            case ContactMenu.USER_MENU_PS_VISIBLE:
            case ContactMenu.USER_MENU_PS_INVISIBLE:
            case ContactMenu.USER_MENU_PS_IGNORE:
                int flags = contact.getFlags();
                switch (action) {
                    case ContactMenu.USER_MENU_PS_VISIBLE:
                        flags ^= MrimContact.CONTACT_FLAG_VISIBLE;
                        break;
                    case ContactMenu.USER_MENU_PS_INVISIBLE:
                        flags ^= MrimContact.CONTACT_FLAG_INVISIBLE;
                        break;
                    case ContactMenu.USER_MENU_PS_IGNORE:
                        flags ^= MrimContact.CONTACT_FLAG_IGNORE;
                        break;
                }
                contact.setFlags(flags);
                getConnection().updateContact(contact);
                RosterHelper.getInstance().updateRoster();
                break;

        }
        if (ContactMenu.USER_MENU_USERS_LIST == action) {
            VirtualListModel list = new VirtualListModel();
            Vector members = ((MrimChatContact) c).getMembers();
            for (int i = 0; i < members.size(); ++i) {
                list.addItem((String) members.elementAt(i), false);
            }
            VirtualList tl = VirtualList.getInstance();
            tl.setCaption(JLocale.getString(R.string.list_of_users));
            tl.setProtocol(this);
            tl.setModel(list);
            tl.show(activity);
            tl.updateModel();

        } else if (ContactMenu.USER_MENU_ADD_USER == action) {
            if (isConnected()) {
                addContact(contact);
                getConnection().putMultiChatGetMembers(contact.getUserId());
            }
        }
    }

    public void showUserInfo(BaseActivity activity, Contact contact) {
        UserInfo data;
        if (contact instanceof MrimPhoneContact) {
            data = new UserInfo(this);
            data.nick = contact.getName();
            data.homePhones = ((MrimContact) contact).getPhones();
            data.createProfileView(contact.getName());
            data.updateProfileView();

        } else if (isConnected()) {
            data = getConnection().getUserInfo((MrimContact) contact);
            data.createProfileView(contact.getName());
            data.setProfileViewToWait();

        } else {
            data = new UserInfo(this, contact.getUserId());
            data.uin = contact.getUserId();
            data.nick = contact.getName();
            data.homePhones = ((MrimContact) contact).getPhones();
            data.createProfileView(contact.getName());
            data.updateProfileView();
        }
        data.showProfile(activity);
    }

    public void showStatus(BaseActivity activity, Contact contact) {
        if (contact instanceof MrimPhoneContact) {
            return;
        }
        StatusView statusView = RosterHelper.getInstance().getStatusView();
        statusView.init(this, contact);
        statusView.initUI();

        if (XStatusInfo.XSTATUS_NONE != contact.getXStatusIndex()) {
            statusView.addXStatus();
            statusView.addStatusText(contact.getXStatusText());
        } else {
            statusView.addContactStatus();
            statusView.addStatusText(contact.getStatusText());
        }
        statusView.addClient();
        statusView.addTime();
        statusView.showIt(activity);
    }
}
