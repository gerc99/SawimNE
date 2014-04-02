package ru.sawim.text;

import android.app.AlertDialog;
import android.content.Context;
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
import ru.sawim.SawimApplication;
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
    public void onTextLinkClick(View textView, String clickedString, boolean isLongTap) {
        if (clickedString.length() == 0) return;
        boolean isJuick = clickedString.startsWith("@") || clickedString.startsWith("#");
        if (isJuick) {
            new JuickMenu(BaseActivity.getCurrentActivity(), currentProtocol, currentContact, clickedString).show();
            return;
        }
        if (isLongTap || Jid.isJID(clickedString)) {
            CharSequence[] items = new CharSequence[2];
            items[0] = BaseActivity.getCurrentActivity().getString(R.string.copy);
            items[1] = BaseActivity.getCurrentActivity().getString(R.string.add_contact);
            final AlertDialog.Builder builder = new AlertDialog.Builder(BaseActivity.getCurrentActivity());
            builder.setCancelable(true);
            builder.setTitle(R.string.url_menu);
            final String finalClickedString = clickedString;
            builder.setItems(items, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            Clipboard.setClipBoardText(finalClickedString);
                            break;
                        case 1:
                            SawimApplication.openUrl(finalClickedString);
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
            if (!Util.isUrl(clickedString))
                clickedString = "http://" + clickedString;
            if (clickedString.toLowerCase().startsWith(HtmlTask.PIK4U)
                    || (clickedString.toLowerCase().startsWith("https://db.tt/"))
                    || Util.isImageFile(clickedString)) {
                PictureView pictureView = new PictureView();
                pictureView.setLink(clickedString);
                FragmentTransaction transaction = BaseActivity.getCurrentActivity().getSupportFragmentManager().beginTransaction();
                transaction.add(pictureView, PictureView.TAG);
                transaction.commitAllowingStateLoss();
            } else {
                Uri uri = Uri.parse(clickedString);
                Context context = BaseActivity.getCurrentActivity();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                context.startActivity(intent);
            }
        }
    }
}
