package protocol.vk;

import protocol.Contact;
import protocol.Group;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.chat.message.PlainMessage;
import ru.sawim.icons.ImageList;
import ru.sawim.modules.search.Search;
import ru.sawim.modules.search.UserInfo;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 16.01.13 22:02
 *
 * @author vladimir
 */
public class Vk extends Protocol {
    private VkConnection connection = null;

    public Vk() {
    }

    @Override
    public String getUserIdName() {
        return "Id";
    }

    private static final byte[] statuses = {
            StatusInfo.STATUS_OFFLINE,
            StatusInfo.STATUS_ONLINE};

    @Override
    protected void initStatusInfo() {
        ImageList icons = createStatusIcons();
        final int[] statusIconIndex = {1, 0, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 1};
        info = new StatusInfo(icons, statusIconIndex, statuses);
    }

    private ImageList createStatusIcons() {
        ImageList icons = ImageList.createImageList("/vk-status.png");
        if (0 < icons.size()) {
            return icons;
        }
        return ImageList.createImageList("/jabber-status.png");
    }

    @Override
    protected void requestAuth(String userId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void grandAuth(String userId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void denyAuth(String userId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_setPrivateStatus() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_renameContact(Contact contact, String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_moveContact(Contact contact, Group to) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_removeGroup(Group group) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_renameGroup(Group group, String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_addGroup(Group group) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean isConnected() {
        VkConnection c = connection;
        return (null != c) && c.isConnected();
    }

    @Override
    public void startConnection() {
        connection = new VkConnection(this);
        connection.login();
    }

    @Override
    protected void closeConnection() {
        VkConnection c = connection;
        connection = null;
        if (null != c) {
            c.logout();
        }
    }

    @Override
    public Group createGroup(String name) {
        return new Group(name);
    }

    @Override
    protected Contact createContact(String uin, String name) {
        int id;
        try {
            id = Integer.parseInt(uin);
        } catch (java.lang.NumberFormatException e) {
            //RosterHelper.getInstance().activateWithMsg(SawimApplication.getContext().getString(R.string.enter_id));
            return null;
        }
        VkContact c = new VkContact(id);
        c.setName(name);
        return c;
    }

    @Override
    protected void s_searchUsers(Search cont) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_updateOnlineStatus() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void s_updateXStatus() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void saveUserInfo(UserInfo info) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected void sendSomeMessage(PlainMessage msg) {
        connection.sendMessage(msg);
    }
}
