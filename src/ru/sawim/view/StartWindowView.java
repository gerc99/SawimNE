package ru.sawim.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import protocol.Profile;
import protocol.Registration;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.roster.RosterHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 29.08.13
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class StartWindowView extends Fragment {

    public static final String TAG = "StartWindowView";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SawimApplication.returnFromAcc = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View dialogLogin = inflater.inflate(R.layout.login, container, false);
        final TextView loginText = (TextView) dialogLogin.findViewById(R.id.acc_login_text);
        final EditText editLogin = (EditText) dialogLogin.findViewById(R.id.edit_login);
        final TextView serverText = (TextView) dialogLogin.findViewById(R.id.acc_server_text);
        final EditText editServer = (EditText) dialogLogin.findViewById(R.id.edit_server);
        final EditText editPass = (EditText) dialogLogin.findViewById(R.id.edit_password);
        final Button buttonOk = (Button) dialogLogin.findViewById(R.id.ButtonOK);

        loginText.setText("ID");
        serverText.setVisibility(TextView.VISIBLE);
        editServer.setVisibility(EditText.VISIBLE);
        final Profile account = Options.getAccount();
        getActivity().setTitle(R.string.acc_edit);
        if (StringConvertor.isEmpty(account.userId)) {
            editServer.setText(SawimApplication.DEFAULT_SERVER);
        } else {
            editLogin.setText(account.userId.substring(0, account.userId.indexOf('@')));
            editServer.setText(account.userId.substring(account.userId.indexOf('@') + 1));
        }
        editPass.setText(account.password);
        buttonOk.setText(R.string.save);
        editLogin.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        buttonOk.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String login = editLogin.getText().toString();
                String server = editServer.getText().toString();
                String password = editPass.getText().toString();
                Profile account = Options.getAccount();
                if (login.indexOf('@') + 1 > 0) //isServer
                    account.userId = login;
                else
                    account.userId = login + "@" + server;
                if (StringConvertor.isEmpty(account.userId)) {
                    return;
                }
                account.password = password;
                addAccount(account);
                if (login.length() > 0 && password.length() > 0) {
                    ((SawimActivity) getActivity()).recreateActivity();
                } else {
                    if (SawimApplication.isManyPane())
                        getActivity().getSupportFragmentManager().popBackStack();
                }
            }
        });

        Button regJidButton = (Button) dialogLogin.findViewById(R.id.reg_jid);
        regJidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Registration xmppRegistration = new Registration();
                xmppRegistration.setListener(new Registration.OnAddAccount() {

                    @Override
                    public void addAccount(Profile acc) {
                        addAccount(acc);
                    }
                });
                xmppRegistration.init().show((BaseActivity) getActivity());
            }
        });
        return dialogLogin;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(JLocale.getString(R.string.app_name));
        getActivity().supportInvalidateOptionsMenu();
    }

    public void addAccount(Profile acc) {
        Options.saveAccount(acc);
        RosterHelper.getInstance().setCurrentProtocol();
    }

    private void back() {
        if (SawimApplication.isManyPane())
            ((SawimActivity) getActivity()).recreateActivity();
        else
            getActivity().getSupportFragmentManager().popBackStack();
    }
}