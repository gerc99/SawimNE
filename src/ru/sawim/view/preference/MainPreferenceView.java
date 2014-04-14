package ru.sawim.view.preference;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentTransaction;
import protocol.Contact;
import protocol.Protocol;
import protocol.xmpp.AdHoc;
import protocol.xmpp.Jid;
import protocol.xmpp.Xmpp;
import protocol.xmpp.XmppContact;
import ru.sawim.OptionsForm;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.BaseActivity;
import ru.sawim.roster.RosterHelper;
import ru.sawim.util.JLocale;
import ru.sawim.view.AboutProgramView;
import ru.sawim.view.SawimFragment;

/**
 * Created by admin on 25.01.14.
 */
public class MainPreferenceView extends PreferenceFragment {

    public static final String TAG = "MainPreferenceView";
    PreferenceScreen rootScreen;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(rootScreen);
        buildList();
        getActivity().supportInvalidateOptionsMenu();
    }

    public static void show(final BaseActivity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (SawimApplication.isManyPane())
                    activity.setContentView(R.layout.main);
                MainPreferenceView newFragment = new MainPreferenceView();
                FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, MainPreferenceView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(JLocale.getString(R.string.options));
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void buildList() {
        final BaseActivity activity = (BaseActivity) getActivity();
        rootScreen.removeAll();
        PreferenceScreen accountsScreen = getPreferenceManager().createPreferenceScreen(activity);
        accountsScreen.setTitle(R.string.options_account);
        accountsScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(activity, AccountsListActivity.class));
                return false;
            }
        });
        rootScreen.addPreference(accountsScreen);

        final PreferenceScreen optionsNetworkScreen = getPreferenceManager().createPreferenceScreen(activity);
        optionsNetworkScreen.setTitle(R.string.options_network);
        optionsNetworkScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(activity, optionsNetworkScreen.getTitle(), OptionsForm.OPTIONS_NETWORK);
                return false;
            }
        });
        rootScreen.addPreference(optionsNetworkScreen);

        final Protocol protocol = RosterHelper.getInstance().getCurrentProtocol();
        if (protocol != null && protocol.isConnected() && protocol instanceof Xmpp) {
            final PreferenceScreen accountSettingsScreen = getPreferenceManager().createPreferenceScreen(activity);
            accountSettingsScreen.setTitle(R.string.account_settings);
            accountSettingsScreen.setSummary(protocol.getUserId());
            accountSettingsScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    String serverAddress = Jid.getDomain(protocol.getUserId());
                    Contact serverContact = protocol.createTempContact(serverAddress);
                    AdHoc adhoc = new AdHoc((Xmpp) protocol, (XmppContact) serverContact);
                    adhoc.show(activity);
                    return false;
                }
            });
            rootScreen.addPreference(accountSettingsScreen);
        }

        final PreferenceScreen optionsInterfaceScreen = getPreferenceManager().createPreferenceScreen(activity);
        optionsInterfaceScreen.setTitle(R.string.options_interface);
        optionsInterfaceScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(activity, optionsInterfaceScreen.getTitle(), OptionsForm.OPTIONS_INTERFACE);
                return false;
            }
        });
        rootScreen.addPreference(optionsInterfaceScreen);

        final PreferenceScreen optionsSignalingScreen = getPreferenceManager().createPreferenceScreen(activity);
        optionsSignalingScreen.setTitle(R.string.options_signaling);
        optionsSignalingScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(activity, optionsSignalingScreen.getTitle(), OptionsForm.OPTIONS_SIGNALING);
                return false;
            }
        });
        rootScreen.addPreference(optionsSignalingScreen);

        final PreferenceScreen optionsAntispamScreen = getPreferenceManager().createPreferenceScreen(activity);
        optionsAntispamScreen.setTitle(R.string.antispam);
        optionsAntispamScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(activity, optionsAntispamScreen.getTitle(), OptionsForm.OPTIONS_ANTISPAM);
                return false;
            }
        });
        rootScreen.addPreference(optionsAntispamScreen);

        final PreferenceScreen optionsAnswererScreen = getPreferenceManager().createPreferenceScreen(activity);
        optionsAnswererScreen.setTitle(R.string.answerer);
        optionsAnswererScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(activity, optionsAnswererScreen.getTitle(), OptionsForm.OPTIONS_ANSWERER);
                return false;
            }
        });
        rootScreen.addPreference(optionsAnswererScreen);

        final PreferenceScreen optionsProScreen = getPreferenceManager().createPreferenceScreen(activity);
        optionsProScreen.setTitle(R.string.options_pro);
        optionsProScreen.setSummary(R.string.options_pro);
        optionsProScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(activity, optionsProScreen.getTitle(), OptionsForm.OPTIONS_PRO);
                return false;
            }
        });
        rootScreen.addPreference(optionsProScreen);

        final PreferenceScreen aboutProgramScreen = getPreferenceManager().createPreferenceScreen(activity);
        aboutProgramScreen.setTitle(R.string.about_program);
        aboutProgramScreen.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutProgramView().show(activity.getSupportFragmentManager(), AboutProgramView.TAG);
                return false;
            }
        });
        rootScreen.addPreference(aboutProgramScreen);
    }

    public boolean hasBack() {
        SawimFragment preferenceFormView = (SawimFragment) getActivity().getSupportFragmentManager().findFragmentByTag(PreferenceFormView.TAG);
        return preferenceFormView == null || preferenceFormView.hasBack();
    }
}
