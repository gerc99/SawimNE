package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import protocol.Profile;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.xmpp.Jid;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppChangePassword;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.models.AccountsAdapter;
import ru.sawim.roster.RosterHelper;
import ru.sawim.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:58
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListView extends Fragment {

    public static final String TAG = AccountsListView.class.getSimpleName();
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
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        menu.add(Menu.FIRST, R.id.menu_edit, 0, R.string.edit);
        menu.add(Menu.FIRST, R.id.lift_up, 0, R.string.lift_up);
        menu.add(Menu.FIRST, R.id.put_down, 0, R.string.put_down);
        menu.add(Menu.FIRST, R.id.menu_delete, 0, R.string.delete);
        Profile account = accountsListAdapter.getItem(info.position);
        if (account.protocolType == Profile.PROTOCOL_JABBER) {
            menu.add(Menu.FIRST, R.id.change_password, 0, R.string.change_password);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final int accountID = info.position;
        Profile account = accountsListAdapter.getItem(accountID);
        final String itemName = account.userId;
        final int protocolType = account.protocolType;
        int num = info.position;

        switch (item.getItemId()) {
            case R.id.menu_edit:
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                LoginView loginView = new LoginView();
                loginView.init(protocolType, accountID, true, new LoginView.OnAddListener() {
                    @Override
                    public void onAdd() {
                        Util.hideKeyboard(getActivity());
                        update();
                    }
                });
                transaction.replace(R.id.fragment_container, loginView, loginView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
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
                                        if (p != null)
                                            SawimApplication.getInstance().setStatus(p, StatusInfo.STATUS_OFFLINE, "");
                                        Options.delAccount(accountID);
                                        RosterHelper.getInstance().setCurrentProtocol();
                                        Options.safeSave();
                                        update();
                                    }
                                })
                        .setNegativeButton(android.R.string.no, null)
                        .create().show();
                return true;

            case R.id.change_password:
                final Profile account_ = account;
                LinearLayout linear = new LinearLayout(getActivity());
                final EditText curPassword = new EditText(getActivity());
                final EditText newPassword = new EditText(getActivity());
                final AlertDialog.Builder passwordAlert = new AlertDialog.Builder(getActivity());
                final AlertDialog.Builder warning = new AlertDialog.Builder(getActivity());

                curPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                newPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

                linear.setOrientation(LinearLayout.VERTICAL);
                linear.addView(curPassword);
                linear.addView(newPassword);

                passwordAlert.setTitle(R.string.change_password);
                passwordAlert.setView(linear);

                curPassword.setHint(R.string.current_password);
                newPassword.setHint(R.string.new_password);

                warning.setTitle(R.string.warning);
                warning.setCancelable(true);
                warning.setNegativeButton(android.R.string.cancel, null);
                Protocol p = RosterHelper.getInstance().getProtocol(accountID);

                if (!p.isConnected()) {
                    warning.setMessage(R.string.connect_first);
                    warning.show();
                    return true;
                }

                passwordAlert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        //Do nothing here because we override this button later to change the close behaviour.
                        //However, we still need this because on older versions of Android unless we
                        //pass a handler the button doesn't get instantiated
                    }
                });
                passwordAlert.setNegativeButton(android.R.string.cancel, null);
                final AlertDialog dialog = passwordAlert.create();
                dialog.show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        Protocol p = RosterHelper.getInstance().getProtocol(accountID);
                        String curPasswordStr = curPassword.getText().toString();
                        String newPasswordStr = newPassword.getText().toString();
                        String oldPassword = p.getPassword();

                        if (!curPasswordStr.equals(oldPassword)) {
                           curPassword.setError(getString(R.string.passwords_mismatch));
                        }
                        else if ( newPasswordStr.isEmpty() )  {
                            newPassword.setError(getString(R.string.non_empty));
                        }
                        else {
                            XmppChangePassword changer = new XmppChangePassword();
                            String jid = p.getUserId();
                            String domain = Jid.getDomain(jid);
                            String username = Jid.getNick(jid);
                            String xml = changer.getXml(domain, username, newPasswordStr);
                            changer.sendXml(xml, (Xmpp) RosterHelper.getInstance().getProtocol(accountID));
                            account_.password = newPasswordStr;
                            Options.setAccount(accountID, account_);
                            dialog.dismiss(); // shall we do this?
                            warning.setMessage(R.string.passwords_changed);
                            warning.show();
                        }
                    }
                });
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
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                LoginView loginView = new LoginView();
                loginView.init(Profile.protocolTypes[item], -1, false, new LoginView.OnAddListener() {
                    @Override
                    public void onAdd() {
                        Util.hideKeyboard(getActivity());
                        getActivity().finish();
                    }
                });
                transaction.replace(R.id.fragment_container, loginView, loginView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
        builder.create().show();
    }
}
