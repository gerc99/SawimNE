package ru.sawim;

import DrawControls.icons.ImageList;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

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

    public static final BitmapDrawable authIcon = (BitmapDrawable) SawimApplication.getInstance().getResources()
            .getDrawable(android.R.drawable.stat_notify_error);
    public static final BitmapDrawable messageIconCheck = (BitmapDrawable) SawimApplication.getInstance().getResources()
            .getDrawable(R.drawable.msg_check);
    public static final BitmapDrawable authGrantIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_auth_grant);
    public static final BitmapDrawable authReqIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_auth_req);
    public static final BitmapDrawable messageIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_new_message);
    public static final BitmapDrawable personalMessageIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
            getDrawable(R.drawable.ic_new_personal_message);

    public static void initIcons() {
        usersIcon = null;
        usersIconOn = null;
        typingIcon = null;
        groupDownIcon = null;
        groupRightIcons = null;
        listDivider = null;
        usersIconOn = (BitmapDrawable) SawimApplication.getInstance().getResources()
                .getDrawable(Scheme.isBlack() ? R.drawable.ic_participants_dark_on : R.drawable.ic_participants_on);
        usersIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
                getDrawable(Scheme.isBlack() ? R.drawable.ic_participants_dark : R.drawable.ic_participants_light);
        typingIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
                getDrawable(Scheme.isBlack() ? R.drawable.ic_typing_dark : R.drawable.ic_typing_light);
        groupDownIcon = (BitmapDrawable) SawimApplication.getInstance().getResources().
                getDrawable(Scheme.isBlack() ? R.drawable.ic_collapsed_dark : R.drawable.ic_collapsed_light);
        groupRightIcons = (BitmapDrawable) SawimApplication.getInstance().getResources().
                getDrawable(Scheme.isBlack() ? R.drawable.ic_expanded_dark : R.drawable.ic_expanded_light);
        listDivider = SawimApplication.getInstance().getResources().
                getDrawable(Scheme.isBlack() ? R.drawable.abc_list_divider_holo_dark : R.drawable.abc_list_divider_holo_light);
    }
}