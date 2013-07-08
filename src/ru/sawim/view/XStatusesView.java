package ru.sawim.view;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import sawim.io.Storage;
import protocol.Protocol;
import protocol.icq.Icq;
import protocol.jabber.Jabber;
import protocol.mrim.Mrim;
import ru.sawim.R;
import ru.sawim.models.XStatusesAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 19.05.13
 * Time: 15:45
 * To change this template use File | Settings | File Templates.
 */
public class XStatusesView extends DialogFragment {
    private XStatusesAdapter statusesAdapter;
    protected String[] xst_titles = new String[100];
    protected String[] xst_descs  = new String[100];
    private Protocol protocol;

    public XStatusesView() {
        protocol = ContactList.getInstance().getCurrProtocol();
        load();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().setTitle(R.string.ms_xstatus_menu);
        View v = inflater.inflate(R.layout.xstatuses_view, container, false);
        statusesAdapter = new XStatusesAdapter(getActivity(), protocol);
        final ListView lv = (ListView) v.findViewById(R.id.xstatuses_view);
        statusesAdapter.setSelectedItem(protocol.getProfile().xstatusIndex + 1);
        lv.setCacheColorHint(0x00000000);
        lv.setAdapter(statusesAdapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int position, long l) {
                final Dialog dialogXStatusText = new Dialog(getActivity());
                dialogXStatusText.setContentView(R.layout.xtraz_text);
                dialogXStatusText.setTitle(protocol.getXStatusInfo().getName(position - 1));
                if (position == 0) {
                    setXStatus(-1, "", "");
                    dialogXStatusText.dismiss();
                    return;
                }

                final EditText editTitle = (EditText) dialogXStatusText.findViewById(R.id.xstatus_title_edit);
                final EditText editDesciption = (EditText) dialogXStatusText.findViewById(R.id.xstatus_description_edit);

                editTitle.setText(xst_titles[position - 1]);
                editDesciption.setText(xst_descs[position - 1]);

                Button buttonSave = (Button) dialogXStatusText.findViewById(R.id.xstatus_save_button);
                buttonSave.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        setXStatus(position - 1, editTitle.getText().toString(), editDesciption.getText().toString());
                        statusesAdapter.setSelectedItem(position - 1);
                        dialogXStatusText.dismiss();
                        dismiss();
                    }
                });
                dialogXStatusText.show();
            }
        });
        return v;
    }

    private void load() {
        try {
            Storage storage = new Storage(getProtocolId() + "-xstatus");
            storage.open(false);
            storage.loadXStatuses(xst_titles, xst_descs);
            storage.close();
        } catch (Exception ignored) {
        }
    }

    private String getProtocolId() {
        if (protocol instanceof Icq) {
            return "icq";
        }
        if (protocol instanceof Mrim) {
            return "mrim";
        }
        if (protocol instanceof Jabber) {
            return "jabber";
        }
        return "";
    }

    private final void setXStatus(int xstatus, String title, String desc) {
        if (0 <= xstatus) {
            xst_titles[xstatus] = StringConvertor.notNull(title);
            xst_descs[xstatus]  = StringConvertor.notNull(desc);
            try {
                Storage storage = new Storage(getProtocolId() + "-xstatus");
                storage.open(true);
                storage.saveXStatuses(xst_titles, xst_descs);
                storage.close();
            } catch (Exception ignored) {
            }
        }
        protocol.setXStatus(xstatus, title, desc);
    }
}
