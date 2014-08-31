package ru.sawim.text;

import android.app.AlertDialog;
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
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.Util;
import ru.sawim.modules.DebugLog;
import ru.sawim.view.PictureView;
import ru.sawim.view.menu.JuickMenu;
import ru.sawim.view.tasks.HtmlTask;

/**
 * Created with IntelliJ IDEA.
 * User: admin
 * Date: 15.11.13
 * Time: 17:43
 * To change this template use File | Settings | File Templates.
 */
public class TextLinkClick implements TextLinkClickListener {

    private Protocol currentProtocol;
    private String currentContact;

    public TextLinkClick(Protocol currentProtocol, String currentContact) {
        this.currentProtocol = currentProtocol;
        this.currentContact = currentContact;
    }

    @Override
    public void onTextLinkClick(View textView, final String clickedString, boolean isLongTap) {
        if (clickedString.length() == 0) return;
        final BaseActivity activity = (BaseActivity) textView.getContext();
        boolean isJuick = clickedString.startsWith("@") || clickedString.startsWith("#");
        if (isJuick) {
            new JuickMenu(activity, currentProtocol, currentContact, clickedString).show();
            return;
        }
        if (isLongTap || Jid.isJID(clickedString)) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setCancelable(true);
            builder.setTitle(R.string.url_menu);
            CharSequence[] items = new CharSequence[2];
            items[0] = activity.getString(R.string.copy);
            items[1] = activity.getString(R.string.add_contact);
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Clipboard.setClipBoardText(activity, clickedString);
                            break;
                        case 1:
                            currentProtocol.getSearchForm().show(activity,
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
                PictureView pictureView = new PictureView();
                pictureView.setLink(clickedStringWithHttp);
                FragmentTransaction transaction = (activity).getSupportFragmentManager().beginTransaction();
                transaction.add(pictureView, PictureView.TAG);
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
