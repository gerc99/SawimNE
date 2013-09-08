package ru.sawim.view;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import protocol.Profile;
import protocol.jabber.JabberRegistration;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.roster.Roster;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.start_window, container, false);
        Button regJidButton = (Button) v.findViewById(R.id.reg_jid);
        regJidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                JabberRegistration jabberRegistration = new JabberRegistration();
                jabberRegistration.setListener(new JabberRegistration.OnAddAccount() {

                    @Override
                    public void addAccount(int num, Profile acc) {
                        addAccount(num, acc);
                    }
                });
                jabberRegistration.init().show(getActivity());
            }
        });
        Button signInJabberButton = (Button) v.findViewById(R.id.sign_in_jabber);
        signInJabberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LoginDialog(Profile.protocolTypes[Profile.PROTOCOL_JABBER]).show(getActivity().getSupportFragmentManager(), "login");
            }
        });
        Button signIntoOtherNetworksButton = (Button) v.findViewById(R.id.sign_into_other_networks);
        signIntoOtherNetworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(true);
                builder.setTitle(R.string.acc_sel_protocol);
                builder.setItems(Profile.protocolNames, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        new LoginDialog(Profile.protocolTypes[item]).show(getActivity().getSupportFragmentManager(), "login");
                    }
                });
                builder.create().show();
            }
        });

        return v;
    }

    private void addAccountAuthenticator(String id) {
        Account account = new Account(id, SawimApplication.getContext().getString(R.string.app_name));
        AccountManager am = AccountManager.get(getActivity());
        boolean accountCreated = am.addAccountExplicitly(account, null, null);
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null && accountCreated) {
            AccountAuthenticatorResponse response = extras.getParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, id);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, SawimApplication.getContext().getString(R.string.app_name));
            if (response != null)
                response.onResult(result);
        }
    }

    public void addAccount(int num, Profile acc) {
        addAccountAuthenticator(acc.userId);
        Options.setAccount(num, acc);
        Roster.getInstance().setCurrentProtocol();
    }

    private void back() {
        if (General.sawimActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            General.sawimActivity.recreateActivity();
        else
            General.sawimActivity.getSupportFragmentManager().popBackStack();
    }

    class LoginDialog extends DialogFragment {
        private int type;

        public LoginDialog(final int type) {
            this.type = type;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View dialogLogin = inflater.inflate(R.layout.login, container, false);
            final TextView loginText = (TextView) dialogLogin.findViewById(R.id.acc_login_text);
            final EditText editLogin = (EditText) dialogLogin.findViewById(R.id.Login);
            final EditText editNick = (EditText) dialogLogin.findViewById(R.id.Nick);
            final EditText editPass = (EditText) dialogLogin.findViewById(R.id.Password);
            int protocolIndex = 0;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (type == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            loginText.setText(Profile.protocolIds[protocolIndex]);
            getDialog().setTitle(getText(R.string.acc_add));
            if (type == Profile.PROTOCOL_ICQ) {
                editLogin.setInputType(InputType.TYPE_CLASS_NUMBER);
            } else {
                editLogin.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
            }
            Button buttonOk = (Button) dialogLogin.findViewById(R.id.ButtonOK);
            final int finalProtocolIndex = protocolIndex;
            buttonOk.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    String login = editLogin.getText().toString();
                    String password = editPass.getText().toString();
                    String nick = editNick.getText().toString();
                    Profile account = new Profile();
                    if (1 < Profile.protocolTypes.length) {
                        account.protocolType = Profile.protocolTypes[finalProtocolIndex];
                    }
                    account.userId = login;
                    if (StringConvertor.isEmpty(account.userId)) {
                        return;
                    }
                    account.password = password;
                    account.nick = nick;
                    account.isActive = true;
                    addAccount(Options.getAccountCount() + 1, account);
                    dismiss();
                    back();
                }
            });
            return dialogLogin;
        }
    }
}