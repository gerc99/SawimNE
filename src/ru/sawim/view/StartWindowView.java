package ru.sawim.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import protocol.Profile;
import protocol.StatusInfo;
import protocol.xmpp.XmppRegistration;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 29.08.13
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class StartWindowView extends Fragment {

    public static final String TAG = "StartWindowView";

    OnAddListener addListener;
    int accountID;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SawimApplication.returnFromAcc = true;
    }

    public void init(int accountID, OnAddListener addListener) {
        this.accountID = accountID;
        this.addListener = addListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.start_window, container, false);

        final boolean isEdit = accountID > -1;
        final EditText editLogin = (EditText) v.findViewById(R.id.edit_login);
        final EditText editPass = (EditText) v.findViewById(R.id.edit_password);
        final Button buttonOk = (Button) v.findViewById(R.id.ButtonOK);
        if (isEdit) {
            final Profile account = Options.getAccount(accountID);
            getActivity().setTitle(R.string.acc_edit);
            editLogin.setText(account.userId);
            editPass.setText(account.password);
            buttonOk.setText(R.string.save);
        } else {
            getActivity().setTitle(R.string.acc_add);
            buttonOk.setText(R.string.acc_add);
        }
        buttonOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String login = editLogin.getText().toString();
                String password = editPass.getText().toString();
                Profile account = new Profile();
                if (login.indexOf('@') + 1 > 0) //isServer
                    account.userId = login;
                else
                    account.userId = login + "@" + SawimApplication.DEFAULT_SERVER;

                if (StringConvertor.isEmpty(account.userId)) {
                    return;
                }
                account.password = password;

                if (login.length() > 0 && password.length() > 0) {
                    if (isEdit) {
                        int editAccountNum = Options.getAccountIndex(account);
                        account.isActive = Options.getAccountCount() <= editAccountNum
                                || Options.getAccount(editAccountNum).isActive;
                        addAccount(0, account);
                    } else {
                        account.isActive = true;
                        addAccount(Options.getAccountCount() + 1, account);
                    }
                    SawimApplication.getInstance().setStatus(RosterHelper.getInstance().getProtocol(account), StatusInfo.STATUS_ONLINE, "");
                    getFragmentManager().popBackStack();

                    if (addListener != null)
                        addListener.onAdd();
                }
            }
        });

        Button regJidButton = (Button) v.findViewById(R.id.reg_jid);
        regJidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                XmppRegistration xmppRegistration = new XmppRegistration();
                xmppRegistration.setListener(new XmppRegistration.OnAddAccount() {

                    @Override
                    public void addAccount(int num, Profile acc) {
                        StartWindowView.this.addAccount(num, acc);
                        back();
                        SawimApplication.getInstance().setStatus(RosterHelper.getInstance().getProtocol(acc), StatusInfo.STATUS_ONLINE, "");
                    }
                });
                xmppRegistration.init().show((BaseActivity) getActivity());
            }
        });
        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(JLocale.getString(R.string.app_name));
        boolean isEdit = accountID > -1;
        if (!isEdit) {
            if (RosterHelper.getInstance().getProtocolCount() > 0) {
                ((BaseActivity) getActivity()).recreateActivity();
            } else {
                if (SawimApplication.isManyPane())
                    getActivity().getSupportFragmentManager().popBackStack();
            }
        }
        getActivity().supportInvalidateOptionsMenu();
    }

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        RosterHelper.getInstance().setCurrentProtocol();
    }

    private void back() {
        if (SawimApplication.isManyPane())
            ((SawimActivity) getActivity()).recreateActivity();
        else
            getActivity().getSupportFragmentManager().popBackStack();
        Util.hideKeyboard(getActivity());
    }

    public interface OnAddListener {
        void onAdd();
    }
}