package ru.sawim.view;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import sawim.Options;
import sawim.cl.ContactList;
import protocol.Protocol;
import ru.sawim.R;
import ru.sawim.models.StatusesAdapter;

public class StatusesView extends DialogFragment {
    private StatusesAdapter statusesAdapter;
    public static final int ADAPTER_STATUS = 0;
    public static final int ADAPTER_PRIVATESTATUS = 1;
    private int type;
    private Protocol protocol;

    public StatusesView(Protocol p, int type) {
        protocol = p;
        this.type = type;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.ms_status_menu);
        View v = inflater.inflate(R.layout.statuses_view, container, false);
        statusesAdapter = new StatusesAdapter(getActivity(), protocol, type);
        ListView lv = (ListView) v.findViewById(R.id.statuses_view);
        lv.setCacheColorHint(0x00000000);
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
                    protocol.setStatus(currItem, null);
                } else {
                    protocol.setPrivateStatus((byte)i);
                    Options.setInt(Options.OPTION_PRIVATE_STATUS, i);
                    Options.safeSave();
                }
                //statusesAdapter.setSelectedItem(i);
                dismiss();
            }
        });
        return v;
    }
}