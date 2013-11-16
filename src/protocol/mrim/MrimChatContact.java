package protocol.mrim;

import android.view.ContextMenu;
import android.view.Menu;
import protocol.ContactMenu;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.R;

import java.util.Vector;

public final class MrimChatContact extends MrimContact {

    public MrimChatContact(String uin, String name) {
        super(uin, name);
        setStatus(StatusInfo.STATUS_ONLINE, null);
    }

    private Vector members = new Vector();

    void setMembers(Vector inChat) {
        members = inChat;
    }

    Vector getMembers() {
        return members;
    }

    public boolean hasHistory() {
        return false;
    }

    public boolean isSingleUserContact() {
        return false;
    }

    protected void initContextMenu(Protocol protocol, ContextMenu contactMenu) {
        if (isTemp()) {
            contactMenu.add(Menu.FIRST, ContactMenu.USER_MENU_ADD_USER, 2, R.string.connect);
        } else {
            contactMenu.add(Menu.FIRST, ContactMenu.CONFERENCE_DISCONNECT, 2, R.string.leave_chat);
            contactMenu.add(Menu.FIRST, ContactMenu.USER_MENU_USERS_LIST, 2, R.string.list_of_users);
        }
        addChatItems(contactMenu);
    }
}


