package ru.sawim.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import protocol.Contact;
import ru.sawim.R;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.models.RosterAdapter;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.MyListView;
import ru.sawim.widget.Util;

/**
 * Created by gerc on 04.03.2015.
 */
public class ChatsDialogFragment extends DialogFragment {

    RosterAdapter chatsSpinnerAdapter;
    OnForceGoToChatListener forceGoToChatListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.chats_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
        MyListView lv = (MyListView) dialogView.findViewById(R.id.listView);
        chatsSpinnerAdapter = new RosterAdapter();
        chatsSpinnerAdapter.setType(RosterHelper.ACTIVE_CONTACTS);
        lv.setAdapter(chatsSpinnerAdapter);
        lv.setFastScrollEnabled(true);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TreeNode treeNode = ((RosterAdapter) parent.getAdapter()).getItem(position);
                if (treeNode.getType() == TreeNode.CONTACT) {
                    if (forceGoToChatListener != null) {
                        forceGoToChatListener.onForceGoToChat(ChatHistory.instance.getChat((Contact) treeNode));
                    }
                    dismiss();
                }
            }
        });
        chatsSpinnerAdapter.refreshList();
        Dialog dialog = dialogBuilder.create();
        dialog.setCanceledOnTouchOutside(true);
        return dialog;
    }

    public void setForceGoToChatListener(OnForceGoToChatListener forceGoToChatListener) {
        this.forceGoToChatListener = forceGoToChatListener;
    }

    public void refreshList() {
        if (chatsSpinnerAdapter != null) {
            chatsSpinnerAdapter.refreshList();
        }
    }

    public interface OnForceGoToChatListener {
        void onForceGoToChat(Chat selectedChat);
    }
}
