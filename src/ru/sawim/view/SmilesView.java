package ru.sawim.view;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import sawim.modules.Emotions;
import ru.sawim.R;
import ru.sawim.models.SmilesAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 13.05.13
 * Time: 18:10
 * To change this template use File | Settings | File Templates.
 */
public class SmilesView extends DialogFragment {
    SmilesAdapter smilesAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        smilesAdapter = new SmilesAdapter(getActivity());
        GridView gv = new GridView(getActivity());
        gv.setNumColumns(5);
        gv.setAdapter(smilesAdapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(ChatView.PASTE_TEXT);
                intent.putExtra("text", Emotions.instance.getSmileCode(i));
                getActivity().sendBroadcast(intent);
                dismiss();
            }
        });
        return gv;
    }
}
