package protocol.xmpp;

import ru.sawim.modules.search.UserInfo;

public class AvatarLoader implements Runnable {
    private byte[] avatarBytes = null;
    private UserInfo userInfo;
    private XmlNode bs64photo;

    public AvatarLoader(UserInfo userInfo, XmlNode bs64photo) {
        this.userInfo = userInfo;
        this.bs64photo = bs64photo;
    }

    public void run() {
        avatarBytes = userInfo.isEditable()
                ? bs64photo.getBinValue()
                : bs64photo.popBinValue();
        bs64photo = null;
        try {
            if (null != avatarBytes) {
                userInfo.setAvatar(avatarBytes);
                avatarBytes = null;
                userInfo.updateProfileView();
            }
        } catch (OutOfMemoryError outOfMemoryError) {
            outOfMemoryError.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}


