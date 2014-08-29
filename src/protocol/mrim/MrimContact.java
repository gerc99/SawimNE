package protocol.mrim;

import android.view.ContextMenu;
import android.view.Menu;
import protocol.*;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.modules.DebugLog;
import ru.sawim.view.menu.MyMenu;

public class MrimContact extends Contact {
    private int contactId;
    private int flags;
    private String phones;
    public static final int CONTACT_INTFLAG_NOT_AUTHORIZED = 0x0001;

    public static final int USER_MENU_SEND_SMS = 1;

    public static final int CONTACT_FLAG_INVISIBLE = 0x04;
    public static final int CONTACT_FLAG_VISIBLE = 0x08;
    public static final int CONTACT_FLAG_IGNORE = 0x10;

    public void init(int contactId, String name, String phone, int groupId, int serverFlags, int flags) {
        setContactId(contactId);
        setName(name.length() > 0 ? name : userId);
        setGroupId(groupId);
        setFlags(flags);
        this.phones = phone;
        setBooleanValue(Contact.CONTACT_NO_AUTH, (CONTACT_INTFLAG_NOT_AUTHORIZED & serverFlags) != 0);
        setTempFlag(false);
        setOfflineStatus();
    }

    final void setFlags(int flags) {
        this.flags = flags;
        setBooleanValue(SL_VISIBLE, (flags & CONTACT_FLAG_VISIBLE) != 0);
        setBooleanValue(SL_INVISIBLE, (flags & CONTACT_FLAG_INVISIBLE) != 0);
        setBooleanValue(SL_IGNORE, (flags & CONTACT_FLAG_IGNORE) != 0);
    }

    public MrimContact(String uin, String name) {
        this.userId = uin;
        contactId = -1;
        setFlags(0);
        setGroupId(Group.NOT_IN_GROUP);
        this.setName(name);
        setOfflineStatus();
    }

    void setContactId(int id) {
        contactId = id;
    }

    int getContactId() {
        return contactId;
    }

    int getFlags() {
        return flags;
    }

    public void setClient(String cl) {
        DebugLog.println("client " + userId + " " + cl);
        MrimClient.createClient(this, cl);
    }

    public void addChatMenuItems(ContextMenu model) {
        if (isOnline() && Options.getBoolean(JLocale.getString(R.string.pref_alarm)) && isSingleUserContact()) {
            model.add(Menu.FIRST, ContactMenu.USER_MENU_WAKE, 2, R.string.wake);
        }
    }

    protected void initContextMenu(Protocol protocol, ContextMenu contactMenu) {
        addChatItems(contactMenu);
        if (!StringConvertor.isEmpty(phones)) {
            contactMenu.add(Menu.FIRST, USER_MENU_SEND_SMS, 2, R.string.send_sms);
        }
        addGeneralItems(protocol, contactMenu);
    }

    protected void initManageContactMenu(Protocol protocol, MyMenu menu) {
        if (protocol.isConnected()) {
            initPrivacyMenu(menu);
            if (isTemp()) {
                menu.add(SawimApplication.getContext().getString(R.string.add_user), ContactMenu.USER_MENU_ADD_USER);
            } else {
                if (protocol.getGroupItems().size() > 1) {
                    menu.add(SawimApplication.getContext().getString(R.string.move_to_group), ContactMenu.USER_MENU_MOVE);
                }
                if (!isAuth()) {
                    menu.add(SawimApplication.getContext().getString(R.string.requauth), ContactMenu.USER_MENU_REQU_AUTH);
                }
                menu.add(SawimApplication.getContext().getString(R.string.rename), ContactMenu.USER_MENU_RENAME);
            }
        }
        if ((protocol.isConnected() || isTemp()) && protocol.inContactList(this)) {
            menu.add(SawimApplication.getContext().getString(R.string.remove), ContactMenu.USER_MENU_USER_REMOVE);
        }
    }

    public void setMood(String moodCode, String title, String desc) {
        if (!StringConvertor.isEmpty(moodCode)) {
            DebugLog.println("mrim: mood " + getUserId() + " " + moodCode + " " + title);
        }
        String message = StringConvertor.trim(title + " " + desc);
        int x = Mrim.xStatus.createStatus(moodCode);

        setXStatus(x, message);
        if (XStatusInfo.XSTATUS_NONE == x) {
            setStatus(getStatusIndex(), message);
        }
    }

    String getPhones() {
        return phones;
    }

    void setPhones(String listOfPhones) {
        phones = listOfPhones;
    }
}