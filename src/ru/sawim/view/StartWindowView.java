package ru.sawim.view;

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
import protocol.xmpp.XmppRegistration;
import ru.sawim.General;
import ru.sawim.R;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.roster.RosterHelper;

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
        General.returnFromAcc = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.start_window, container, false);
        Button regJidButton = (Button) v.findViewById(R.id.reg_jid);
        regJidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                XmppRegistration xmppRegistration = new XmppRegistration();
                xmppRegistration.setListener(new XmppRegistration.OnAddAccount() {

                    @Override
                    public void addAccount(int num, Profile acc) {
                        addAccount(num, acc);
                    }
                });
                xmppRegistration.init().show();
            }
        });
        Button signInXmppButton = (Button) v.findViewById(R.id.sign_in_jabber);
        signInXmppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LoginDialog(Profile.PROTOCOL_JABBER).show(General.currentActivity.getSupportFragmentManager(), "login");
            }
        });
        Button signIntoOtherNetworksButton = (Button) v.findViewById(R.id.sign_into_other_networks);
        signIntoOtherNetworksButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(General.currentActivity);
                builder.setCancelable(true);
                builder.setTitle(R.string.acc_sel_protocol);
                builder.setItems(Profile.protocolNames, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        new LoginDialog(Profile.protocolTypes[item])
                                .show(General.currentActivity.getSupportFragmentManager(), "login");
                    }
                });
                builder.create().show();
            }
        });

        return v;
    }

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        RosterHelper.getInstance().setCurrentProtocol();
    }

    private void back() {
        General.currentActivity.getSupportFragmentManager().popBackStack();
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            General.currentActivity.getSupportFragmentManager()
                    .findFragmentById(R.id.chat_fragment).getView().setVisibility(View.VISIBLE);
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
            final EditText editLogin = (EditText) dialogLogin.findViewById(R.id.edit_login);
            final TextView serverText = (TextView) dialogLogin.findViewById(R.id.acc_server_text);
            final EditText editServer = (EditText) dialogLogin.findViewById(R.id.edit_server);
            final EditText editNick = (EditText) dialogLogin.findViewById(R.id.edit_nick);
            final EditText editPass = (EditText) dialogLogin.findViewById(R.id.edit_password);
            int protocolIndex = 0;
            final boolean isXmpp = type == Profile.PROTOCOL_JABBER;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (type == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            loginText.setText(Profile.protocolIds[protocolIndex]);
            getDialog().setTitle(getText(R.string.acc_add));
            if (isXmpp) {
                serverText.setVisibility(TextView.VISIBLE);
                editServer.setVisibility(EditText.VISIBLE);
                editServer.setText(General.DEFAULT_SERVER);
            } else {
                serverText.setVisibility(TextView.GONE);
                editServer.setVisibility(EditText.GONE);
            }
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
                    String server = editServer.getText().toString();
                    String password = editPass.getText().toString();
                    String nick = editNick.getText().toString();
                    Profile account = new Profile();
                    if (1 < Profile.protocolTypes.length) {
                        account.protocolType = Profile.protocolTypes[finalProtocolIndex];
                    }
                    if (isXmpp) {
                        if (login.indexOf('@') + 1 > 0) //isServer
                            account.userId = login;
                        else
                            account.userId = login + "@" + server;
                    } else
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