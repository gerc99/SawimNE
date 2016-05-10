package ru.sawim.ui.fragment;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import protocol.Profile;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.xmpp.Jid;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppChangePassword;
import protocol.xmpp.XmppRegistration;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.ui.activity.BaseActivity;
import ru.sawim.ui.activity.SawimActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.comm.StringConvertor;
import ru.sawim.roster.RosterHelper;
import ru.sawim.ui.widget.Util;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 29.08.13
 * Time: 19:32
 * To change this template use File | Settings | File Templates.
 */
public class StartWindowFragment extends Fragment {

    public static final String TAG = "StartWindowFragment";

    OnAddListener addListener;
    int accountID = 0;

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

        final boolean isEdit = Options.getAccountCount() > 0;
        final Button buttonChangePass = (Button) v.findViewById(R.id.change_password_btn);
        buttonChangePass.setVisibility(isEdit ? View.VISIBLE : View.GONE);
        buttonChangePass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    return;
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
                            Profile account = Options.getAccount(accountID);
                            account.password = newPasswordStr;
                            Options.setAccount(accountID, account);
                            dialog.dismiss(); // shall we do this?
                            warning.setMessage(R.string.passwords_changed);
                            warning.show();
                        }
                    }
                });
            }
        });
        final EditText editLogin = (EditText) v.findViewById(R.id.edit_login);
        final EditText editPass = (EditText) v.findViewById(R.id.edit_password);
        final Button buttonOk = (Button) v.findViewById(R.id.ButtonOK);
        if (isEdit) {
            final Profile account = Options.getAccount(accountID);
            getActivity().setTitle(R.string.acc_edit);
            editLogin.setText(account.userId.substring(0, account.userId.indexOf('@')));
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
                    back();

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
                        StartWindowFragment.this.addAccount(num, acc);
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
        getActivity().supportInvalidateOptionsMenu();
    }

    public void addAccount(int num, Profile acc) {
        Options.setAccount(num, acc);
        RosterHelper.getInstance().getProtocol().setProfile(Options.getAccount(0));
        RosterHelper.getInstance().loadAccounts();
        RosterHelper.getInstance().updateRoster();
    }

    private void back() {
        if (Options.getAccountCount() > 0) {
        //    getActivity().finish();
        //    return;
        }
        if (SawimApplication.isManyPane())
            ((BaseActivity) getActivity()).recreateActivity();
        else
            getActivity().getSupportFragmentManager().popBackStack();
        Util.hideKeyboard(getActivity());
    }

    public interface OnAddListener {
        void onAdd();
    }
}