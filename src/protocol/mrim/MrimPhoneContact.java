package protocol.mrim;

import android.view.ContextMenu;
import android.view.Menu;
import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.icons.Icon;

public class MrimPhoneContact extends MrimContact {
    static final String PHONE_UIN = "phone";

    public MrimPhoneContact(String phones) {
        super(PHONE_UIN, PHONE_UIN);
        setPhones(phones);
        setName(phones);
        setBooleanValue(Contact.CONTACT_NO_AUTH, false);
    }

    int getFlags() {
        return 0x100000;
    }

    public Icon getLeftIcon(Protocol p) {
        return Mrim.getPhoneContactIcon();
    }

    public void activate(BaseActivity activity, Protocol p) {
        if (hasChat()) {
            p.getChat(this).activate();
        } else {
            new ContactMenu(p, this).doAction(activity, USER_MENU_SEND_SMS);
        }
    }

    protected void initContextMenu(Protocol protocol, ContextMenu menu) {
        menu.add(Menu.FIRST, USER_MENU_SEND_SMS, 2, JLocale.getString(R.string.send_sms));
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_USER_INFO, 2, JLocale.getString(R.string.info));
        if ((protocol.getGroupItems().size() > 1) && !isTemp()) {
            menu.add(Menu.FIRST, ContactMenu.USER_MENU_MOVE, 2, JLocale.getString(R.string.move_to_group));
        }
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_USER_REMOVE, 2, JLocale.getString(R.string.remove));
        menu.add(Menu.FIRST, ContactMenu.USER_MENU_RENAME, 2, JLocale.getString(R.string.rename));
    }

    public boolean isVisibleInContactList() {
        return true;
    }
}


