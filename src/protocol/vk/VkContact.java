package protocol.vk;

import android.view.SubMenu;
import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;

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
    protected void initManageContactMenu(Protocol protocol, SubMenu menu) {
    }

    public void setOnlineStatus() {
        setStatus(StatusInfo.STATUS_ONLINE, null);
    }
}