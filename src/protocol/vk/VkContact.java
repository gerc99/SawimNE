package protocol.vk;

import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.view.menu.MyMenu;

/**
 * Created with IntelliJ IDEA.
 * <p/>
 * Date: 16.01.13 22:02
 *
 * @author vladimir
 */
public class VkContact extends Contact {
    private int uid;

    VkContact(int uid) {
        this.uid = uid;
        this.userId = "" + uid;
    }

    int getUid() {
        return uid;
    }

    @Override
    protected void initManageContactMenu(Protocol protocol, MyMenu menu) {
    }

    public void setOnlineStatus() {
        setStatus(StatusInfo.STATUS_ONLINE, null);
    }
}