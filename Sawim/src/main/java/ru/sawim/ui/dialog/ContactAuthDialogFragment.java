package ru.sawim.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;

import protocol.Contact;
import protocol.ContactMenu;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.chat.Chat;
import ru.sawim.comm.JLocale;
import ru.sawim.roster.RosterHelper;
import ru.sawim.ui.activity.BaseActivity;

public class ContactAuthDialogFragment extends DialogFragment {

    private Chat newChat;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Protocol protocol = RosterHelper.getInstance().getProtocol();
        final Contact contact = newChat.getContact();
        final BaseActivity activity = (BaseActivity) getActivity();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        dialogBuilder.setMessage(JLocale.getString(R.string.grant) + " " + contact.getName() + "?");
        dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new ContactMenu(contact).doAction(activity, ContactMenu.USER_MENU_GRANT_AUTH);
                activity.supportInvalidateOptionsMenu();
                RosterHelper.getInstance().updateRoster();
            }
        });
        dialogBuilder.setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new ContactMenu(contact).doAction(activity, ContactMenu.USER_MENU_DENY_AUTH);
                activity.supportInvalidateOptionsMenu();
                RosterHelper.getInstance().updateRoster();
            }
        });
        Dialog dialog = dialogBuilder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public void setNewChat(Chat newChat) {
        this.newChat = newChat;
    }
}
