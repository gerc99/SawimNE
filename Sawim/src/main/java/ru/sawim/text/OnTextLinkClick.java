package ru.sawim.text;

import android.support.v7.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import protocol.Protocol;
import protocol.xmpp.Jid;
import ru.sawim.Clipboard;
import ru.sawim.R;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.roster.RosterHelper;
import ru.sawim.ui.fragment.PictureFragment;
import ru.sawim.ui.fragment.menu.JuickMenu;
import ru.sawim.ui.fragment.tasks.HtmlTask;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 15.11.13
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
public class OnTextLinkClick implements TextLinkClickListener {

    private String currentContact;

    public void setContact(String currentContact) {
        this.currentContact = currentContact;
    }

    @Override
    public void onTextLinkClick(View textView, final String clickedString, boolean isLongTap) {
        if (clickedString.length() == 0) return;
        final BaseActivity activity = (BaseActivity) textView.getContext();
        boolean isJuick = clickedString.startsWith("@") || clickedString.startsWith("#");
        boolean isJID = Jid.isJID(clickedString);
        if (isJuick) {
            if (currentContact != null) {
                new JuickMenu(activity, currentContact, clickedString).show();
            }
            return;
        }
        if (isLongTap || isJID) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(true);
            builder.setTitle(R.string.url_menu);
            ArrayList<CharSequence> items = new ArrayList<>();
            items.add(activity.getString(R.string.copy));
            if (isJID) {
                items.add(activity.getString(R.string.add_contact));
            }
            builder.setItems(items.toArray(new CharSequence[items.size()]), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Clipboard.setClipBoardText(activity, clickedString);
                            break;
                        case 1:
                            Protocol protocol = RosterHelper.getInstance().getProtocol();
                            protocol.getSearchForm().show(activity,
                                    Util.getUrlWithoutProtocol(clickedString), true);
                            break;
                    }
                }
            });
            try {
                builder.create().show();
            } catch (Exception e) {
                // WindowManager$BadTokenException will be caught and the app would not display
                DebugLog.panic("onTextLinkClick", e);
            }
        } else {
            String clickedStringWithHttp = Util.isUrl(clickedString) ? clickedString : "http://" + clickedString;
            if (clickedStringWithHttp.toLowerCase().startsWith(HtmlTask.PIK4U)
                    || (clickedStringWithHttp.toLowerCase().startsWith("https://db.tt/"))
                    || Util.isImageFile(clickedStringWithHttp)) {
                PictureFragment pictureFragment = new PictureFragment();
                pictureFragment.setLink(clickedStringWithHttp);
                FragmentTransaction transaction = (activity).getSupportFragmentManager().beginTransaction();
                transaction.add(pictureFragment, PictureFragment.TAG);
                transaction.commitAllowingStateLoss();
            } else {
                Uri uri = Uri.parse(clickedStringWithHttp);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, activity.getPackageName());
                try {
                    activity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                }
            }
        }
    }
}
