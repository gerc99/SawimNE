package ru.sawim.view;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.*;
import android.widget.*;
import protocol.Protocol;
import sawim.Options;
import sawim.cl.ContactList;
import sawim.comm.StringConvertor;
import protocol.Profile;
import protocol.StatusInfo;
import protocol.jabber.JabberRegistration;
import ru.sawim.R;
import ru.sawim.models.AccountsAdapter;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListView extends Fragment {

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
        ListView accountsList = (ListView) getActivity().findViewById(R.id.AccountsList);
        accountsListAdapter = new AccountsAdapter(getActivity());
        getActivity().setTitle(getActivity().getString(R.string.options_account));
        accountsList.setAdapter(accountsListAdapter);
        registerForContextMenu(accountsList);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        menu.add(Menu.FIRST, R.id.menu_edit, 0, R.string.edit);
        menu.add(Menu.FIRST, R.id.menu_delete, 0, R.string.delete);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Profile account = accountsListAdapter.getItem(info.position);
        final int accountID = (int) accountsListAdapter.getItemId(info.position);
        final String itemName = account.userId;
        final int protocolType = account.protocolType;

        switch (item.getItemId()) {

            case R.id.menu_edit:
                new LoginDialog(protocolType, accountID, true);
                return true;

            case R.id.menu_delete:
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(getString(R.string.acc_delete_confirm) + " " + itemName + "?")
                        .setCancelable(false)
                        .setPositiveButton(android.R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int id) {
                                        Protocol p = ContactList.getInstance().getProtocol(Options.getAccount(accountID));
                                        if (p != null)
                                            p.setStatus(StatusInfo.STATUS_OFFLINE, "");
                                        Options.delAccount(accountID);
                                        setCurrentProtocol();
                                        Options.safeSave();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .create().show();
                return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        setCurrentProtocol();
    }

    public void setCurrentProtocol() {
        ContactList cl = ContactList.getInstance();
        Vector listOfProfiles = new Vector();
        for (int i = 0; i < Options.getAccountCount(); ++i) {
            Profile p = Options.getAccount(i);
            if (p.isActive) {
                listOfProfiles.addElement(p);
            }
        }
        if (listOfProfiles.isEmpty()) {
            Profile p = Options.getAccount(0);
            p.isActive = true;
            listOfProfiles.addElement(p);
        }
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                accountsListAdapter.notifyDataSetChanged();
            }
        });
        cl.addProtocols(listOfProfiles);
        cl.getManager().update();
    }

    public void addAccount() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.acc_sel_protocol);
        builder.setItems(Profile.protocolNames, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int item) {
                new LoginDialog(Profile.protocolTypes[item], -1, false);
            }
        });
        builder.create().show();
    }

    public class LoginDialog {
        private Dialog dialogLogin;
        private boolean isActive;

        public LoginDialog(final int type, final int id, final boolean isEdit) {
            dialogLogin = new Dialog(getActivity());
            dialogLogin.setContentView(R.layout.login);
            final TextView loginText = (TextView) dialogLogin.findViewById(R.id.acc_login_text);
            final EditText editLogin = (EditText) dialogLogin.findViewById(R.id.Login);
            final EditText editNick = (EditText) dialogLogin.findViewById(R.id.Nick);
            final EditText editPass = (EditText) dialogLogin.findViewById(R.id.Password);
            final CheckBox checkAutoConnect = (CheckBox) dialogLogin.findViewById(R.id.auto_connect);
            int protocolIndex = 0;
            for (int i = 0; i < Profile.protocolTypes.length; ++i) {
                if (type == Profile.protocolTypes[i]) {
                    protocolIndex = i;
                    break;
                }
            }
            loginText.setText(Profile.protocolIds[protocolIndex]);
            if (isEdit) {
                final Profile account = Options.getAccount(id);
                dialogLogin.setTitle(getText(R.string.acc_edit));
                editLogin.setText(account.userId);
                editNick.setText(account.nick);
                editPass.setText(account.password);
                //checkAutoConnect.setChecked(account.);
                isActive = (account.isActive);
            } else {
                dialogLogin.setTitle(getText(R.string.acc_add));
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

                    int editAccountNum = Options.getAccountIndex(account);
                    if (Options.getAccountCount() <= editAccountNum) {
                        account.isActive = true;
                    } else {
                        account.isActive = Options.getAccount(editAccountNum).isActive;
                    }

                    if (isEdit) {
                        addAccount(id, account);
                    } else {
                        addAccount(Options.getAccountCount() + 1, account);
                    }
                    dialogLogin.dismiss();
                }

            });
            dialogLogin.show();
        }
    }
}
