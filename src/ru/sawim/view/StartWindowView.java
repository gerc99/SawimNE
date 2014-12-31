package ru.sawim.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import protocol.Profile;
import protocol.StatusInfo;
import protocol.xmpp.XmppRegistration;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.comm.JLocale;
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        SawimApplication.returnFromAcc = true;
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
                        StartWindowView.this.addAccount(num, acc);
                        back();
                        SawimApplication.getInstance().setStatus(RosterHelper.getInstance().getProtocol(acc), StatusInfo.STATUS_ONLINE, "");
                    }
                });
                xmppRegistration.init().show((BaseActivity) getActivity());
            }
        });
        Button signInXmppButton = (Button) v.findViewById(R.id.sign_in_jabber);
        signInXmppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                LoginView loginView = new LoginView();
                loginView.init(Profile.PROTOCOL_JABBER, 0, false, new LoginView.OnAddListener() {
                    @Override
                    public void onAdd() {
                        back();
                    }
                });
                transaction.replace(R.id.fragment_container, loginView, LoginView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
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
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        LoginView loginView = new LoginView();
                        loginView.init(Profile.protocolTypes[item], 0, false, new LoginView.OnAddListener() {
                            @Override
                            public void onAdd() {
                                back();
                            }
                        });
                        transaction.replace(R.id.fragment_container, loginView, LoginView.TAG);
                        transaction.addToBackStack(null);
                        transaction.commitAllowingStateLoss();
                    }
                });
                builder.create().show();
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(JLocale.getString(R.string.app_name));
        if (RosterHelper.getInstance().getProtocolCount() > 0) {
            ((SawimActivity) getActivity()).recreateActivity();
        } else {
            if (SawimApplication.isManyPane())
                getActivity().getSupportFragmentManager().popBackStack();
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
}