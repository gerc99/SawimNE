package protocol.icq;

import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.icons.Icon;
import ru.sawim.icons.ImageList;
import ru.sawim.view.menu.MyMenu;

public class IcqContact extends Contact {
    private static final ImageList happyIcon = ImageList.createImageList("/happy.png");

    public boolean happyFlag;
    private short contactId;
    public static final int USER_MENU_REMOVE_ME = 8;

    public int getContactId() {
        return ((int) contactId) & 0xFFFF;
    }

    public void setContactId(int id) {
        contactId = (short) id;
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

    protected void initManageContactMenu(Protocol protocol, MyMenu menu) {
        boolean connected = protocol.isConnected();
        boolean temp = isTemp();
        boolean inList = protocol.inContactList(this);
        if (connected) {

            initPrivacyMenu(menu);

            if (temp) {
                menu.add(SawimApplication.getContext().getString(R.string.add_user), ContactMenu.USER_MENU_ADD_USER);
            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.add(SawimApplication.getContext().getString(R.string.move_to_group), ContactMenu.USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.add(SawimApplication.getContext().getString(R.string.requauth), ContactMenu.USER_MENU_REQU_AUTH);
                }
                if (inList) {
                    menu.add(SawimApplication.getContext().getString(R.string.rename), ContactMenu.USER_MENU_RENAME);
                }
            }
        }
        if (connected || (temp && inList)) {
            if (connected) {
                menu.add(SawimApplication.getContext().getString(R.string.remove_me), ContactMenu.USER_MENU_REMOVE_ME);
            }
            if (inList) {
                menu.add(SawimApplication.getContext().getString(R.string.remove), ContactMenu.USER_MENU_USER_REMOVE);
            }
        }
    }
}