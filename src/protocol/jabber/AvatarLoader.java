package protocol.jabber;


import DrawControls.icons.Image;
import DrawControls.icons.ImageList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import sawim.Sawim;
import sawim.search.UserInfo;


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
            if ((null != avatarBytes)) {
                userInfo.setAvatar(avatarBytes);
                avatarBytes = null;
                userInfo.updateProfileView();
            }
        } catch (OutOfMemoryError ignored) {
        } catch (Exception ignored) {
        }
    }
}


