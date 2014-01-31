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
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.view.PictureView;
import ru.sawim.view.menu.JuickMenu;
import sawim.Clipboard;
import sawim.modules.DebugLog;

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
        boolean isJuick = clickedString.substring(0, 1).equals("@") || clickedString.substring(0, 1).equals("#");
        if (isJuick) {
            new JuickMenu(SawimApplication.getCurrentActivity(), currentProtocol, currentContact, clickedString).show();
            return;
        }
        if (isLongTap || Jid.isConference(clickedString)) {
            CharSequence[] items = new CharSequence[2];
            items[0] = SawimApplication.getCurrentActivity().getString(R.string.copy);
            items[1] = SawimApplication.getCurrentActivity().getString(R.string.add_contact);
            final AlertDialog.Builder builder = new AlertDialog.Builder(SawimApplication.getCurrentActivity());
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
            if (!clickedString.startsWith("http://") && !clickedString.startsWith("https://"))
                clickedString = "http://" + clickedString;
            String url = clickedString.toLowerCase();
            if ((url.endsWith(".jpg"))
                    || (url.endsWith(".jpeg"))
                    || (url.endsWith(".png"))
                    || (url.endsWith(".gif"))
                    || (url.endsWith(".bmp"))) {
                PictureView pictureView = new PictureView();
                pictureView.setLink(clickedString);
                FragmentTransaction transaction = SawimApplication.getCurrentActivity().getSupportFragmentManager().beginTransaction();
                transaction.add(pictureView, PictureView.TAG);
                transaction.commitAllowingStateLoss();
            } else {
                Uri uri = Uri.parse(clickedString);
                Context context = SawimApplication.getCurrentActivity();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                context.startActivity(intent);
            }
        }
    }
}
