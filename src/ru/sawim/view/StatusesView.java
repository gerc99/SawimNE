package ru.sawim.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import protocol.Protocol;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.models.StatusesAdapter;
import ru.sawim.widget.Util;

public class StatusesView extends DialogFragment {
    public static final int ADAPTER_STATUS = 0;
    public static final int ADAPTER_PRIVATESTATUS = 1;
    private int type;
    private Protocol protocol;

    public StatusesView init(Protocol p, int type) {
        protocol = p;
        this.type = type;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Context context = getActivity();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.statuses_view, null);
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        dialogBuilder.setTitle(R.string.ms_status_menu);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setInverseBackgroundForced(Util.isNeedToInverseDialogBackground());
        StatusesAdapter statusesAdapter = new StatusesAdapter(getActivity(), protocol, type);
        ListView lv = (ListView) dialogView.findViewById(R.id.statuses_view);
        if (type == ADAPTER_STATUS)
            statusesAdapter.setSelectedItem(protocol.getProfile().statusIndex);
        else
            statusesAdapter.setSelectedItem(protocol.getPrivateStatus());
        lv.setAdapter(statusesAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (type == ADAPTER_STATUS) {
                    int currItem = protocol.getStatusInfo().applicableStatuses[i];
                    SawimApplication.getInstance().setStatus(protocol, currItem, null);
                } else {
                    protocol.setPrivateStatus((byte) i);
                    Options.setInt(Options.OPTION_PRIVATE_STATUS, i);
                    Options.safeSave();
                }
                dismiss();
            }
        });

        return dialogBuilder.create();
    }
}