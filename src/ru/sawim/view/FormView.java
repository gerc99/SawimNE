package ru.sawim.view;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import ru.sawim.R;
import ru.sawim.models.form.FormAdapter;
import ru.sawim.models.form.Forms;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 04.06.13
 * Time: 21:30
 * To change this template use File | Settings | File Templates.
 */
public class FormView extends Fragment implements Forms.OnUpdateForm, View.OnClickListener {

    private FormAdapter adapter;
    private ListView list;
    private Button okButton;
    private Button cancelButton;

    @Override
    public void onAttach(Activity a) {
        super.onAttach(a);
        Forms.getInstance().setUpdateFormListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Forms.getInstance().setUpdateFormListener(null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        adapter = new FormAdapter(getActivity(), Forms.getInstance().controls);
        list.setAdapter(adapter);
        okButton = (Button) getActivity().findViewById(R.id.data_form_ok);
        okButton.setOnClickListener(this);

        cancelButton = (Button) getActivity().findViewById(R.id.data_form_cancel);
        cancelButton.setOnClickListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.form, container, false);
        list = (ListView) v.findViewById(R.id.listView);
        return v;
    }

    @Override
    public void updateForm() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    public void back() {
        getActivity().finish();
    }

    @Override
    public void onClick(View view) {
        if (view.equals(cancelButton)) {
            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), false);
            getActivity().finish();
        } else if (view.equals(okButton)) {
            new Thread() {
                public void run() {
                    FormView.this.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            Forms.getInstance().getFormListener().formAction(Forms.getInstance(), true);
                        }
                    });
                }
            }.start();
        }
    }

    public boolean onBackPressed() {
        if (Forms.getInstance().getBackPressedListener() == null) return true;
        return Forms.getInstance().getBackPressedListener().back();
    }
}
