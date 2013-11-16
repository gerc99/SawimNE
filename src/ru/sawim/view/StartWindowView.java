package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import protocol.Profile;
import protocol.jabber.JabberRegistration;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.AccountsListActivity;
import sawim.Options;
import sawim.OptionsForm;
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
                JabberRegistration jabberRegistration = new JabberRegistration();
                jabberRegistration.setListener(new JabberRegistration.OnAddAccount() {

                    @Override
                    public void addAccount(int num, Profile acc) {
                        addAccount(num, acc);
                    }
                });
                jabberRegistration.init().show();
            }
        });
        Button signInJabberButton = (Button) v.findViewById(R.id.sign_in_jabber);
        signInJabberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new LoginDialog(Profile.PROTOCOL_JABBER).show(getActivity().getSupportFragmentManager(), "login");
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

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        Roster.getInstance().setCurrentProtocol();
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
            final boolean isJabber = type == Profile.PROTOCOL_JABBER;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (type == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            loginText.setText(Profile.protocolIds[protocolIndex]);
            getDialog().setTitle(getText(R.string.acc_add));
            if (isJabber) {
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
                    if (isJabber) {
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

    private static final int MENU_QUIT = 0;

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        menu.add(Menu.NONE, OptionsForm.OPTIONS_ACCOUNT, Menu.NONE, R.string.options_account);
        menu.add(Menu.NONE, OptionsForm.OPTIONS_INTERFACE, Menu.NONE, R.string.options_interface);
        menu.add(Menu.NONE, OptionsForm.OPTIONS_SIGNALING, Menu.NONE, R.string.options_signaling);
        menu.add(Menu.NONE, OptionsForm.OPTIONS_ANTISPAM, Menu.NONE, R.string.antispam);
        menu.add(Menu.NONE, OptionsForm.OPTIONS_ABSENCE, Menu.NONE, R.string.absence);
        menu.add(Menu.NONE, OptionsForm.OPTIONS_ANSWERER, Menu.NONE, R.string.answerer);
        menu.add(Menu.NONE, OptionsForm.OPTIONS_ABOUT, Menu.NONE, R.string.about_program);

        menu.add(Menu.NONE, MENU_QUIT, Menu.NONE, R.string.quit);
        super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case OptionsForm.OPTIONS_ACCOUNT:
                startActivity(new Intent(General.currentActivity, AccountsListActivity.class));
                break;
            case OptionsForm.OPTIONS_INTERFACE:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_INTERFACE);
                break;
            case OptionsForm.OPTIONS_SIGNALING:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_SIGNALING);
                break;
            case OptionsForm.OPTIONS_ANTISPAM:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_ANTISPAM);
                break;
            case OptionsForm.OPTIONS_ABSENCE:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_ABSENCE);
                break;
            case OptionsForm.OPTIONS_ANSWERER:
                new OptionsForm().select(item.getTitle(), OptionsForm.OPTIONS_ANSWERER);
                break;
            case OptionsForm.OPTIONS_ABOUT:
                new AboutProgramView().show(General.currentActivity.getSupportFragmentManager(), AboutProgramView.TAG);
                break;

            case MENU_QUIT:
                General.getInstance().quit();
                SawimApplication.getInstance().quit();
                General.currentActivity.finish();
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}