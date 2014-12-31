package ru.sawim.view;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import protocol.Profile;
import protocol.StatusInfo;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.comm.StringConvertor;
import ru.sawim.roster.RosterHelper;

/**
 * Created by gerc on 22.12.2014.
 */
public class LoginView extends Fragment {

    public static final String TAG = LoginView.class.getSimpleName();

    private int type;
    public int id;
    private boolean isEdit;
    private OnAddListener addListener;

    public void init(final int type, final int id, final boolean isEdit, OnAddListener addListener) {
        this.type = type;
        this.id = id;
        this.isEdit = isEdit;
        this.addListener = addListener;
    }

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        RosterHelper.getInstance().setCurrentProtocol();
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
        final Button buttonOk = (Button) dialogLogin.findViewById(R.id.ButtonOK);
        int protocolIndex = 0;
        final boolean isXmpp = type == Profile.PROTOCOL_JABBER;
        for (int i = 0; i < Profile.protocolTypes.length; ++i) {
            if (type == Profile.protocolTypes[i]) {
                protocolIndex = i;
                break;
            }
        }
        loginText.setText(Profile.protocolIds[protocolIndex]);
        if (isXmpp) {
            serverText.setVisibility(TextView.VISIBLE);
            editServer.setVisibility(EditText.VISIBLE);
        } else {
            serverText.setVisibility(TextView.GONE);
            editServer.setVisibility(EditText.GONE);
        }
        if (isEdit) {
            final Profile account = Options.getAccount(id);
            getActivity().setTitle(R.string.acc_edit);
            if (isXmpp) {
                editLogin.setText(account.userId.substring(0, account.userId.indexOf('@')));
                editServer.setText(account.userId.substring(account.userId.indexOf('@') + 1));
            } else {
                editLogin.setText(account.userId);
            }
            editPass.setText(account.password);
            editNick.setText(account.nick);
            buttonOk.setText(R.string.save);
        } else {
            getActivity().setTitle(R.string.acc_add);
            if (isXmpp) {
                editServer.setText(SawimApplication.DEFAULT_SERVER);
            }
            buttonOk.setText(R.string.acc_add);
        }
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

                if (login.length() > 0 && password.length() > 0) {
                    if (isEdit) {
                        int editAccountNum = Options.getAccountIndex(account);
                        account.isActive = Options.getAccountCount() <= editAccountNum
                                || Options.getAccount(editAccountNum).isActive;
                        addAccount(id, account);
                    } else {
                        account.isActive = true;
                        addAccount(Options.getAccountCount() + 1, account);
                    }
                    SawimApplication.getInstance().setStatus(RosterHelper.getInstance().getProtocol(account), StatusInfo.STATUS_ONLINE, "");
                    getFragmentManager().popBackStack();
                    if (addListener != null) {
                        addListener.onAdd();
                    }
                }
            }
        });
        return dialogLogin;
    }

    public interface OnAddListener {
        void onAdd();
    }
}
