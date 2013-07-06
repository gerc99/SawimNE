package protocol.icq;

import DrawControls.icons.Icon;
import DrawControls.icons.ImageList;
import android.view.Menu;
import android.view.SubMenu;
import protocol.Contact;
import protocol.Protocol;
import ru.sawim.R;

public class IcqContact extends Contact {
    private static final ImageList happyIcon = ImageList.createImageList("/happy.png");

    public boolean happyFlag;
    private short contactId;
    public static final int USER_MENU_REMOVE_ME = 8;

    public int getContactId() {
        return ((int)contactId) & 0xFFFF;
    }
    public void setContactId(int id) {
        contactId = (short)id;
    }

    public Icon getHappyIcon() {
        return happyFlag ? happyIcon.iconAt(0) : null;
    }

    public void init(int id, int groupId, String name, boolean noAuth) {
        setContactId(id);
        setGroupId(groupId);
        setName(name);
        setBooleanValue(Contact.CONTACT_NO_AUTH, noAuth);
        setTempFlag(false);
        setOfflineStatus();
        
        setBooleanValue(Contact.SL_IGNORE, false);
        setBooleanValue(Contact.SL_VISIBLE, false);
        setBooleanValue(Contact.SL_INVISIBLE, false);
        
    }

    public IcqContact(String uin) {
        this.userId = uin;
        setOfflineStatus();
    }
    
    public final void setOfflineStatus() {
        super.setOfflineStatus();
        happyFlag = false;
    }

    public final void setXStatusMessage(String text) {
        setXStatus(getXStatusIndex(), text);
    }

    protected void initManageContactMenu(Protocol protocol, SubMenu menu) {
        boolean connected = protocol.isConnected();
        boolean temp = isTemp();
        boolean inList = protocol.inContactList(this);
        if (connected) {
            
            initPrivacyMenu(menu);
            
            if (temp) {
                menu.add(Menu.FIRST, USER_MENU_ADD_USER, 2, R.string.add_user);
            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.add(Menu.FIRST, USER_MENU_MOVE, 2, R.string.move_to_group);
                }
                if (!isAuth()) {
                    menu.add(Menu.FIRST, USER_MENU_REQU_AUTH, 2, R.string.requauth);
                }
                if (inList) {
                    menu.add(Menu.FIRST, USER_MENU_RENAME, 2, R.string.rename);
                }
            }
        }
        if (connected || (temp && inList)) {
            if (connected) {
                menu.add(Menu.FIRST, USER_MENU_REMOVE_ME, 2, R.string.remove_me);
            }
            if (inList) {
                menu.add(Menu.FIRST, USER_MENU_USER_REMOVE, 2, R.string.remove);
            }
        }
    }
}