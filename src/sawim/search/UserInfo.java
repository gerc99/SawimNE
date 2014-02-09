package sawim.search;

import DrawControls.icons.Icon;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import protocol.Contact;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.icq.Icq;
import protocol.mrim.Mrim;
import protocol.net.TcpSocket;
import protocol.xmpp.Xmpp;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.models.list.VirtualList;
import ru.sawim.models.list.VirtualListItem;
import ru.sawim.models.list.VirtualListModel;
import sawim.Clipboard;
import sawim.ExternalApi;
import sawim.SawimException;
import sawim.comm.Util;
import sawim.forms.EditInfo;
import sawim.modules.DebugLog;
import sawim.modules.fs.FileBrowserListener;
import sawim.modules.fs.FileSystem;
import sawim.modules.fs.JSR75FileSystem;
import sawim.modules.photo.PhotoListener;
import sawim.util.JLocale;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class UserInfo implements PhotoListener, FileBrowserListener {
    private final Protocol protocol;
    private VirtualList profileView;
    private boolean avatarIsLoaded = false;
    private boolean searchResult = false;
    public byte[] avatar;
    public String status;
    public protocol.xmpp.XmlNode vCard;
    public final String realUin;

    public String localName, uin, nick, email, homeCity, firstName, lastName, homeState, homePhones, homeFax, homeAddress, cellPhone, homePage,
            interests, about, workCity, workState, workPhone, workFax, workAddress, workCompany, workDepartment, workPosition, birthDay;
    public int age;
    public byte gender;
    public boolean auth;

    public UserInfo(Protocol prot, String uin) {
        protocol = prot;
        realUin = uin;
    }

    public UserInfo(Protocol prot) {
        protocol = prot;
        realUin = null;
    }

    public void setProfileView(VirtualList view) {
        profileView = view;
    }

    public void createProfileView(String name) {
        localName = name;
        VirtualList textList = VirtualList.getInstance();
        textList.setCaption(localName);
        setProfileView(textList);
        if (!searchResult) {
            addMenu();
        }
        profileView.setModel(new VirtualListModel());
        profileView.setClickListListener(new VirtualList.OnClickListListener() {
            @Override
            public void itemSelected(int position) {

            }

            @Override
            public boolean back() {
                profileView.updateModel();
                return true;
            }
        });
    }

    public void showProfile() {
        profileView.show();
    }

    void setSeachResultFlag() {
        searchResult = true;
    }

    private static final int INFO_MENU_SAVE_AVATAR = 1;
    private static final int INFO_MENU_COPY = 2;
    private static final int INFO_MENU_COPY_ALL = 3;
    private static final int INFO_MENU_EDIT = 4;
    private static final int INFO_MENU_REMOVE_AVATAR = 5;
    private static final int INFO_MENU_ADD_AVATAR = 6;
    private static final int INFO_MENU_TAKE_AVATAR = 7;

    public void setOptimalName() {
        Contact contact = protocol.getItemByUIN(uin);
        if (null != contact) {
            String name = contact.getName();
            if (name.equals(contact.getUserId()) || name.equals(protocol.getUniqueUserId(contact))) {
                String newNick = getOptimalName();
                if (newNick.length() != 0) {
                    protocol.renameContact(contact, newNick);
                }
            }
        }
    }

    public synchronized void updateProfileView() {
        if (null == profileView) {
            return;
        }
        VirtualListModel profile = profileView.getModel();//new VirtualListModel();
        updateProfileView(profile);
        if ((null != uin) && !avatarIsLoaded) {
            avatarIsLoaded = true;
            boolean hasAvatarItem = false;
            hasAvatarItem |= (protocol instanceof Mrim);
            hasAvatarItem |= (protocol instanceof Icq);
            if (hasAvatarItem) {
                protocol.getAvatar(this);
            }
        }
        profileView.setModel(profile);
        profileView.updateModel();
    }

    private void updateProfileView(VirtualListModel profile) {
        profile.clear();

        profile.setHeader(R.string.main_info);
        profile.addParam(protocol.getUserIdName(), uin);

        profile.addParamImage(R.string.user_statuses, getStatusAsIcon());

        profile.addParam(R.string.nick, nick);
        profile.addParam(R.string.name, getName());
        profile.addParam(R.string.gender, getGenderAsString());
        if (0 < age) {
            profile.addParam(R.string.age, Integer.toString(age));
        }
        profile.addParam(R.string.email, email);
        if (auth) {
            profile.addParam(R.string.auth, JLocale.getString(R.string.yes));
        }
        profile.addParam(R.string.birth_day, birthDay);
        profile.addParam(R.string.cell_phone, cellPhone);
        profile.addParam(R.string.home_page, homePage);
        profile.addParam(R.string.interests, interests);
        profile.addParam(R.string.notes, about);

        profile.setHeader(R.string.home_info);
        profile.addParam(R.string.addr, homeAddress);
        profile.addParam(R.string.city, homeCity);
        profile.addParam(R.string.state, homeState);
        profile.addParam(R.string.phone, homePhones);
        profile.addParam(R.string.fax, homeFax);

        profile.setHeader(R.string.work_info);
        profile.addParam(R.string.title, workCompany);
        profile.addParam(R.string.depart, workDepartment);
        profile.addParam(R.string.position, workPosition);
        profile.addParam(R.string.addr, workAddress);
        profile.addParam(R.string.city, workCity);
        profile.addParam(R.string.state, workState);
        profile.addParam(R.string.phone, workPhone);
        profile.addParam(R.string.fax, workFax);

        profile.setHeader(R.string.avatar);
        profile.addAvatar(null, ru.sawim.widget.Util.avatarBitmap(avatar));
    }

    private void addMenu() {
        addContextMenu();
        profileView.setBuildOptionsMenu(new VirtualList.OnBuildOptionsMenu() {
            @Override
            public void onCreateOptionsMenu(Menu menu) {
                if (isEditable()) {
                    menu.add(Menu.FIRST, INFO_MENU_EDIT, 2, R.string.edit);
                    if (protocol instanceof Xmpp) {
                        menu.add(Menu.FIRST, INFO_MENU_TAKE_AVATAR, 2, R.string.take_photo);
                        if (sawim.modules.fs.FileSystem.isSupported()) {
                            menu.add(Menu.FIRST, INFO_MENU_ADD_AVATAR, 2, R.string.add_from_fs);
                        }
                        menu.add(Menu.FIRST, INFO_MENU_REMOVE_AVATAR, 2, R.string.remove);
                    }
                }
            }

            @Override
            public void onOptionsItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case INFO_MENU_EDIT:
                        new EditInfo(protocol, UserInfo.this).init().show();
                        break;

                    case INFO_MENU_TAKE_AVATAR:
                        ExternalApi.instance.setActivity(SawimApplication.getCurrentActivity());
                        ExternalApi.instance.startCamera(UserInfo.this, 640, 480);
                        break;

                    case INFO_MENU_REMOVE_AVATAR:
                        removeAvatar();
                        protocol.saveUserInfo(UserInfo.this);
                        updateProfileView();
                        break;

                    case INFO_MENU_ADD_AVATAR:
                        if (ExternalApi.instance.pickFile(UserInfo.this)) {
                            return;
                        }
                        break;
                }
            }
        });
    }

    private void addContextMenu() {
        profileView.setOnBuildContextMenu(new VirtualList.OnBuildContextMenu() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, int listItem) {
                menu.add(Menu.FIRST, INFO_MENU_COPY, 2, R.string.copy_text);
                menu.add(Menu.FIRST, INFO_MENU_COPY_ALL, 2, R.string.copy_all_text);
                if (avatar != null)
                    menu.add(Menu.FIRST, INFO_MENU_SAVE_AVATAR, 2, SawimApplication.getInstance().getString(R.string.save_avatar));
            }

            @Override
            public void onContextItemSelected(int listItem, int itemMenuId) {
                switch (itemMenuId) {
                    case INFO_MENU_COPY:
                        VirtualListItem item = profileView.getModel().elements.get(listItem);
                        Clipboard.setClipBoardText(((item.getLabel() == null) ? "" : item.getLabel() + "\n") + item.getDescStr());
                        break;

                    case INFO_MENU_COPY_ALL:
                        StringBuilder s = new StringBuilder();
                        List<VirtualListItem> listItems = profileView.getModel().elements;
                        for (int i = 0; i < listItems.size(); ++i) {
                            CharSequence label = listItems.get(i).getLabel();
                            CharSequence descStr = listItems.get(i).getDescStr();
                            if (label != null)
                                s.append(label).append("\n");
                            if (descStr != null)
                                s.append(descStr).append("\n");
                        }
                        Clipboard.setClipBoardText(s.toString());
                        break;

                    case INFO_MENU_SAVE_AVATAR:
                        byte[] buffer = avatar;
                        File file = new File(SawimApplication.PATH_AVATARS);
                        if (!file.exists())
                            file.mkdirs();

                        try {
                            if (buffer != null) {
                                String avatar = SawimApplication.PATH_AVATARS + realUin.replace("/", "%") + ".png";
                                FileOutputStream fos = new FileOutputStream(avatar);
                                fos.write(buffer);
                                fos.close();
                                Toast.makeText(SawimApplication.getContext(), R.string.saved_in + " " + avatar, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            DebugLog.println("Save avatar exception " + e.getMessage());
                            e.printStackTrace();
                        }
                        break;
                }
            }
        });
    }

    public void setProfileViewToWait() {
        VirtualListModel profile = profileView.getModel();
        profile.clear();
        profile.addParam(protocol.getUserIdName(), uin);
        profileView.updateModel();
        profile.setInfoMessage(JLocale.getString(R.string.wait));
        addContextMenu();
        profileView.setModel(profile);
        profileView.updateModel();
    }

    public boolean isEditable() {
        boolean isEditable = false;
        isEditable |= (protocol instanceof Icq);
        isEditable |= (protocol instanceof Xmpp);
        return isEditable && protocol.getUserId().equals(uin)
                && protocol.isConnected();
    }

    private Icon getStatusAsIcon() {
        if (protocol instanceof Icq) {
            byte statusIndex = StatusInfo.STATUS_NA;
            switch (Util.strToIntDef(status, -1)) {
                case 0:
                    statusIndex = StatusInfo.STATUS_OFFLINE;
                    break;
                case 1:
                    statusIndex = StatusInfo.STATUS_ONLINE;
                    break;
                case 2:
                    statusIndex = StatusInfo.STATUS_INVISIBLE;
                    break;
                default:
                    return null;
            }
            return protocol.getStatusInfo().getIcon(statusIndex);
        }
        return null;
    }


    public String getGenderAsString() {
        int[] g = {-1, R.string.female, R.string.male};
        return JLocale.getString(g[gender % 3]);
    }

    private String packString(String str) {
        return (null == str) ? "" : str.trim();
    }

    public String getName() {
        return packString(packString(firstName) + " " + packString(lastName));
    }

    public String getOptimalName() {
        String optimalName = packString(nick);
        if (optimalName.length() == 0) {
            optimalName = packString(getName());
        }
        if (optimalName.length() == 0) {
            optimalName = packString(firstName);
        }
        if (optimalName.length() == 0) {
            optimalName = packString(lastName);
        }
        return optimalName;
    }

    public void setAvatar(byte[] data) {
        avatar = null;
        if (null != data) {
            avatar = data;
        }
    }

    public void removeAvatar() {
        avatar = null;
        avatarIsLoaded = false;
        if (null != vCard) {
            vCard.removeNode("PHOTO");
        }
    }

    private String getImageType(byte[] data) {
        if (('P' == data[1]) && ('N' == data[2]) && ('G' == data[3])) {
            return "image/png";
        }
        return "image/jpeg";
    }

    public void setBinAvatar(byte[] data) {
        try {
            setAvatar(data);
            vCard.setValue("PHOTO", null, "TYPE", getImageType(data));
            vCard.setValue("PHOTO", null, "BINVAL", Util.base64encode(data));
        } catch (Exception ignored) {
        }
    }

    public void onFileSelect(InputStream fis, String fileName) throws SawimException {
        try {
            JSR75FileSystem file = FileSystem.getInstance();
            file.openFile(fileName);

            //java.io.InputStream fis = file.openInputStream();
            int size = (int) file.fileSize();
            if (size <= 30 * 1024 * 1024) {
                byte[] binAvatar = new byte[size];
                int readed = 0;
                while (readed < binAvatar.length) {
                    int read = fis.read(binAvatar, readed, binAvatar.length - readed);
                    if (-1 == read) break;
                    readed += read;
                }
                setBinAvatar(binAvatar);
                binAvatar = null;
            }

            TcpSocket.close(fis);
            file.close();
            fis = null;
            file = null;
        } catch (Throwable ignored) {
        }
        if (null != avatar) {
            protocol.saveUserInfo(this);
            updateProfileView();
        }
    }

    public void onDirectorySelect(String directory) {
    }

    public void processPhoto(byte[] data) {
        setBinAvatar(data);
        data = null;
        if (null != avatar) {
            protocol.saveUserInfo(this);
            updateProfileView();
        }
    }
}