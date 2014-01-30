package ru.sawim.view.preference;

import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentTransaction;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.view.AboutProgramView;
import ru.sawim.view.SawimFragment;
import sawim.OptionsForm;

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

    public static void show() {
        SawimApplication.getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SawimActivity.resetBar();
                if (SawimApplication.getCurrentActivity().getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    SawimApplication.getCurrentActivity().setContentView(R.layout.intercalation_layout);
                MainPreferenceView newFragment = new MainPreferenceView();
                FragmentTransaction transaction = SawimApplication.getCurrentActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, MainPreferenceView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        SawimActivity.resetBar();
        SawimApplication.getActionBar().setDisplayHomeAsUpEnabled(true);
        getActivity().setTitle(R.string.options);
    }

    private void buildList() {
        rootScreen.removeAll();
        PreferenceScreen screen1 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen1.setKey("screen1");
        screen1.setTitle(R.string.options_account);
        //screen.setSummary("Description of screen");
        screen1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(SawimApplication.getCurrentActivity(), AccountsListActivity.class));
                return false;
            }
        });
        rootScreen.addPreference(screen1);
        final PreferenceScreen screen2 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen2.setKey("screen2");
        screen2.setTitle(R.string.options_interface);
        //screen.setSummary("Description of screen");
        screen2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(screen2.getTitle(), OptionsForm.OPTIONS_INTERFACE);
                return false;
            }
        });
        rootScreen.addPreference(screen2);
        final PreferenceScreen screen3 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen3.setKey("screen3");
        screen3.setTitle(R.string.options_signaling);
        //screen.setSummary("Description of screen");
        screen3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(screen3.getTitle(), OptionsForm.OPTIONS_SIGNALING);
                return false;
            }
        });
        rootScreen.addPreference(screen3);
        final PreferenceScreen screen4 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen4.setKey("screen4");
        screen4.setTitle(R.string.antispam);
        //screen.setSummary("Description of screen");
        screen4.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(screen4.getTitle(), OptionsForm.OPTIONS_ANTISPAM);
                return false;
            }
        });
        rootScreen.addPreference(screen4);
        final PreferenceScreen screen5 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen5.setKey("screen5");
        screen5.setTitle(R.string.answerer);
        //screen.setSummary("Description of screen");
        screen5.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(screen5.getTitle(), OptionsForm.OPTIONS_ANSWERER);
                return false;
            }
        });
        rootScreen.addPreference(screen5);
        final PreferenceScreen screen6 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen6.setKey("screen6");
        screen6.setTitle(R.string.options_pro);
        screen6.setSummary(R.string.options_pro);
        screen6.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new OptionsForm().select(screen6.getTitle(), OptionsForm.OPTIONS_PRO);
                return false;
            }
        });
        rootScreen.addPreference(screen6);
        final PreferenceScreen screen7 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen7.setKey("screen7");
        screen7.setTitle(R.string.about_program);
        //screen.setSummary("Description of screen");
        screen7.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutProgramView().show(SawimApplication.getCurrentActivity().getSupportFragmentManager(), AboutProgramView.TAG);
                return false;
            }
        });
        rootScreen.addPreference(screen7);
    }

    public boolean hasBack() {
        SawimFragment preferenceFormView = (SawimFragment) getActivity().getSupportFragmentManager().findFragmentByTag(PreferenceFormView.TAG);
        return preferenceFormView == null ? true : preferenceFormView.hasBack();
    }
}
