package ru.sawim.view;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import protocol.Profile;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.SawimApplication;
import ru.sawim.R;
import ru.sawim.models.AccountsAdapter;
import sawim.Options;
import sawim.comm.StringConvertor;
import sawim.roster.RosterHelper;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListView extends Fragment {

    public static final String TAG = "AccountsListView";
    private AccountsAdapter accountsListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.accounts_manager, container, false);
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SawimApplication.returnFromAcc = true;
        ListView accountsList = (ListView) getActivity().findViewById(R.id.AccountsList);
        accountsListAdapter = new AccountsAdapter(getActivity());
        getActivity().setTitle(getString(R.string.options_account));
        accountsList.setCacheColorHint(0x00000000);
        accountsList.setAdapter(accountsListAdapter);
        accountsList.setOnCreateContextMenuListener(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.add(Menu.FIRST, R.id.menu_edit, 0, R.string.edit);
        menu.add(Menu.FIRST, R.id.lift_up, 0, R.string.lift_up);
        menu.add(Menu.FIRST, R.id.put_down, 0, R.string.put_down);
        menu.add(Menu.FIRST, R.id.menu_delete, 0, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Profile account = accountsListAdapter.getItem(info.position);
        final int accountID = (int) accountsListAdapter.getItemId(info.position);
        final String itemName = account.userId;
        final int protocolType = account.protocolType;
        int num = info.position;

        switch (item.getItemId()) {
            case R.id.menu_edit:
                new LoginDialog(protocolType, accountID, true).show(getActivity().getSupportFragmentManager(), "login");
                return true;

            case R.id.lift_up:
                if ((0 != num) && (num < Options.getAccountCount())) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num - 1);
                    Options.setAccount(num - 1, up);
                    Options.setAccount(num, down);
                    RosterHelper.getInstance().setCurrentProtocol();
                    update();
                }
                return true;

            case R.id.put_down:
                if (num < Options.getAccountCount() - 1) {
                    Profile up = Options.getAccount(num);
                    Profile down = Options.getAccount(num + 1);
                    Options.setAccount(num, down);
                    Options.setAccount(num + 1, up);
                    RosterHelper.getInstance().setCurrentProtocol();
                    update();
                }
                return true;

            case R.id.menu_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(true);
                builder.setMessage(String.format(getString(R.string.acc_delete_confirm), itemName))
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        Protocol p = RosterHelper.getInstance().getProtocol(Options.getAccount(accountID));
                                        android.accounts.Account acc = new android.accounts.Account(Options.getId(accountID), getString(R.string.app_name));
                                        AccountManager am = AccountManager.get(getActivity());
                                        am.removeAccount(acc, null, null);
                                        if (p != null)
                                            p.setStatus(StatusInfo.STATUS_OFFLINE, "");
                                        Options.delAccount(accountID);
                                        RosterHelper.getInstance().setCurrentProtocol();
                                        Options.safeSave();
                                        update();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .create().show();
                return true;
        }
        return super.onContextItemSelected(item);
    }

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        RosterHelper.getInstance().setCurrentProtocol();
        update();
    }

    public void update() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accountsListAdapter.notifyDataSetChanged();
            }
        });
    }

    public void addAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(true);
        builder.setTitle(R.string.acc_sel_protocol);
        builder.setItems(Profile.protocolNames, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                new LoginDialog(Profile.protocolTypes[item], -1, false).show(getActivity().getSupportFragmentManager(), "login");
            }
        });
        builder.create().show();
    }

    class LoginDialog extends DialogFragment {
        private int type;
        public int id;
        private boolean isEdit;

        public LoginDialog(final int type, final int id, final boolean isEdit) {
            this.type = type;
            this.id = id;
            this.isEdit = isEdit;
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
            if (isXmpp) {
                serverText.setVisibility(TextView.VISIBLE);
                editServer.setVisibility(EditText.VISIBLE);
            } else {
                serverText.setVisibility(TextView.GONE);
                editServer.setVisibility(EditText.GONE);
            }
            if (isEdit) {
                final Profile account = Options.getAccount(id);
                getDialog().setTitle(getText(R.string.acc_edit));
                if (isXmpp) {
                    editLogin.setText(account.userId.substring(0, account.userId.indexOf('@')));
                    editServer.setText(account.userId.substring(account.userId.indexOf('@') + 1));
                } else {
                    editLogin.setText(account.userId);
                }
                editPass.setText(account.password);
                editNick.setText(account.nick);
            } else {
                getDialog().setTitle(getText(R.string.acc_add));
                if (isXmpp) {
                    editServer.setText(SawimApplication.DEFAULT_SERVER);
                }
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

                    int editAccountNum = Options.getAccountIndex(account);
                    account.isActive = Options.getAccountCount() <= editAccountNum || Options.getAccount(editAccountNum).isActive;

                    if (isEdit) {
                        addAccount(id, account);
                    } else {
                        account.isActive = true;
                        addAccount(Options.getAccountCount() + 1, account);
                    }
                    dismiss();
                }
            });
            return dialogLogin;
        }
    }
}
