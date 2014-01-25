package ru.sawim.view.preference;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v4.app.FragmentTransaction;
import android.view.inputmethod.InputMethodManager;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.SawimActivity;
import ru.sawim.view.AboutProgramView;
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
        getActivity().setTitle(R.string.options);
        General.actionBar.setDisplayHomeAsUpEnabled(true);
        rootScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(rootScreen);
        buildList();
        getActivity().supportInvalidateOptionsMenu();
    }

    public static void show() {
        General.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SawimActivity.resetBar();
                if (General.currentActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    General.currentActivity.setContentView(R.layout.intercalation_layout);
                MainPreferenceView newFragment = new MainPreferenceView();
                FragmentTransaction transaction = General.currentActivity.getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, MainPreferenceView.TAG);
                transaction.addToBackStack(null);
                transaction.commitAllowingStateLoss();
            }
        });
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
                startActivity(new Intent(General.currentActivity, AccountsListActivity.class));
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
        PreferenceScreen screen6 = getPreferenceManager().createPreferenceScreen(getActivity());
        screen6.setKey("screen6");
        screen6.setTitle(R.string.about_program);
        //screen.setSummary("Description of screen");
        screen6.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutProgramView().show(General.currentActivity.getSupportFragmentManager(), AboutProgramView.TAG);
                return false;
            }
        });
        rootScreen.addPreference(screen6);
    }

    public boolean hasBack() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (General.currentActivity.getSupportFragmentManager()
                        .findFragmentById(R.id.chat_fragment) != null)
                    ((SawimActivity) General.currentActivity).recreateActivity();
                else
                    getFragmentManager().popBackStack();
            }
        });
        return true;
    }
}
