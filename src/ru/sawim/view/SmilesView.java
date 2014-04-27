package ru.sawim.view;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.GridView;
import ru.sawim.models.SmilesAdapter;
import ru.sawim.modules.Emotions;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 13.05.13
 * Time: 18:10
 * To change this template use File | Settings | File Templates.
 */
public class SmilesView extends DialogFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);
        ColorDrawable colorDrawable = new ColorDrawable(Color.BLACK);
        colorDrawable.setAlpha(170);
        getDialog().getWindow().setBackgroundDrawable(colorDrawable);
        GridView gv = new GridView(getActivity());
        gv.setColumnWidth(Util.dipToPixels(getActivity(), 60));
        gv.setNumColumns(-1);
        gv.setAdapter(new SmilesAdapter(getActivity()));
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (RosterHelper.getInstance().getUpdateChatListener() != null)
                    RosterHelper.getInstance().getUpdateChatListener().pastText(Emotions.instance.getSmileCode(i));
                dismiss();
            }
        });
        return gv;
    }
}
