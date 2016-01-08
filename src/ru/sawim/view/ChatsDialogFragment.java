package ru.sawim.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import protocol.Contact;
import ru.sawim.R;
import ru.sawim.chat.Chat;
import ru.sawim.chat.ChatHistory;
import ru.sawim.models.RosterAdapter;
import ru.sawim.roster.RosterHelper;
import ru.sawim.roster.TreeNode;
import ru.sawim.widget.recyclerview.decoration.RecyclerItemClickListener;

/**
 * Created by gerc on 04.03.2015.
 */
public class ChatsDialogFragment extends DialogFragment {

    RosterAdapter chatsSpinnerAdapter;
    OnForceGoToChatListener forceGoToChatListener;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.chats_dialog, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setView(dialogView);
        RecyclerView lv = (RecyclerView) dialogView.findViewById(R.id.listView);
        chatsSpinnerAdapter = new RosterAdapter();
        chatsSpinnerAdapter.setType(RosterHelper.ACTIVE_CONTACTS);
        lv.setAdapter(chatsSpinnerAdapter);
        //lv.setFastScrollEnabled(true);
        lv.addOnItemTouchListener(new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View childView, int position) {
                TreeNode treeNode = chatsSpinnerAdapter.getItem(position);
                if (treeNode.getType() == TreeNode.CONTACT) {
                    if (forceGoToChatListener != null) {
                        forceGoToChatListener.onForceGoToChat(ChatHistory.instance.getChat((Contact) treeNode));
                    }
                    dismiss();
                }
            }

            @Override
            public void onItemLongPress(View childView, int position) {

            }
        }));
        chatsSpinnerAdapter.refreshList();
        AppCompatDialog dialog = dialogBuilder.create();
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
