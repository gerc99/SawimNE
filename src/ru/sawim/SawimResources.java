package ru.sawim;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import ru.sawim.icons.ImageList;

/**
 * Created by admin on 07.01.14.
 */
public class SawimResources {

    public static ImageList affiliationIcons = ImageList.createImageList("/jabber-affiliations.png");
    public static BitmapDrawable groupDownIcon;
    public static BitmapDrawable groupRightIcons;
    public static BitmapDrawable usersIcon;
    public static BitmapDrawable usersIconOn;
    public static BitmapDrawable typingIcon;
    public static Drawable listDivider;
    public static Drawable backgroundDrawableIn;
    public static Drawable backgroundDrawableOut;

    public static final Drawable MENU_ICON = SawimApplication.getInstance().getResources()
            .getDrawable(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);
    public static final BitmapDrawable AUTH_ICON = (BitmapDrawable) SawimApplication.getInstance().getResources()
            .getDrawable(android.R.drawable.stat_notify_error);
    public static final BitmapDrawable MESSAGE_ICON_CHECK = (BitmapDrawable) SawimApplication.getInstance().getResources()
            .getDrawable(R.drawable.msg_check);
    public static final BitmapDrawable AUTH_GRANT_ICON = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_auth_grant);
    public static final BitmapDrawable AUTH_REQ_ICON = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_auth_req);
    public static final BitmapDrawable MESSAGE_ICON = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_new_message);
    public static final BitmapDrawable PERSONAL_MESSAGE_ICON = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_new_personal_message);
    public static final Bitmap DEFAULT_AVATAR = BitmapFactory.decodeResource(SawimApplication.getContext().getResources(),
            R.drawable.avatar);
    public static final Bitmap DEFAULT_AVATAR_STATIC = BitmapFactory.decodeResource(SawimApplication.getContext().getResources(),
            R.drawable.online);
    public static final Drawable APP_ICON = SawimApplication.getInstance().getResources().getDrawable(R.drawable.ic_launcher);

    public static void initIcons() {
        usersIcon = null;
        usersIconOn = null;
        typingIcon = null;
        groupDownIcon = null;
        groupRightIcons = null;
        backgroundDrawableIn = null;
        backgroundDrawableOut = null;
        usersIconOn = (BitmapDrawable) SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.ic_participants_dark_on : R.drawable.ic_participants_on);
        usersIcon = (BitmapDrawable) SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.ic_participants_dark : R.drawable.ic_participants_light);
        typingIcon = (BitmapDrawable) SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.ic_typing_dark : R.drawable.ic_typing_light);
        groupDownIcon = (BitmapDrawable) SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.ic_collapsed_dark : R.drawable.ic_collapsed_light);
        groupRightIcons = (BitmapDrawable) SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.ic_expanded_dark : R.drawable.ic_expanded_light);
        listDivider = SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.abc_list_divider_holo_dark : R.drawable.abc_list_divider_holo_light);
        backgroundDrawableIn = SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.msg_in_dark : R.drawable.msg_in);
        backgroundDrawableOut = SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.msg_out_dark : R.drawable.msg_out);
    }
}