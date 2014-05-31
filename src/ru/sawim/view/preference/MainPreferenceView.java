package ru.sawim.view.preference;

import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v4.app.FragmentTransaction;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.activities.BaseActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.modules.Answerer;
import ru.sawim.view.AboutProgramView;
import ru.sawim.view.StartWindowView;

/**
 * Created by admin on 25.01.14.
 */
public class MainPreferenceView extends PreferenceFragment {

    public static final String TAG = "MainPreferenceView";

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);
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

    private void buildList() {
        final BaseActivity activity = (BaseActivity) getActivity();
        getPreferenceScreen().findPreference("options_account").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                StartWindowView newFragment = new StartWindowView();
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.replace(R.id.fragment_container, newFragment, StartWindowView.TAG);
                transaction.addToBackStack(null);
                transaction.commit();
                return false;
            }
        });

        ((ListPreference) getPreferenceScreen().findPreference(Options.OPTION_COLOR_SCHEME)).setEntries(Scheme.getSchemeNames());
        ((ListPreference) getPreferenceScreen().findPreference(Options.OPTION_COLOR_SCHEME)).setEntryValues(Scheme.getSchemeNames());

        final SeekBarPreference fontSeekBarPreference = (SeekBarPreference) getPreferenceScreen().findPreference(Options.OPTION_FONT_SCHEME);
        int level = Options.getInt(Options.OPTION_FONT_SCHEME);
        fontSeekBarPreference.setMax(60);
        fontSeekBarPreference.setDefaultValue(level);
        fontSeekBarPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int value = Integer.parseInt(newValue.toString());
                fontSeekBarPreference.getSeekBar().setProgress(value);
                fontSeekBarPreference.setTitleTextSize(value);
                fontSeekBarPreference.setTitleText(fontSeekBarPreference.getTitle() + "(" + value + ")");
                return true;
            }
        });

        getPreferenceScreen().findPreference("answerer").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Answerer.getInstance().activate();
                return false;
            }
        });

        getPreferenceScreen().findPreference("about_program").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutProgramView().show(activity.getSupportFragmentManager(), AboutProgramView.TAG);
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).resetBar(JLocale.getString(R.string.options));
        ((BaseActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public boolean hasBack() {
        Scheme.setColorScheme(Scheme.getThemeId(Options.getString(Options.OPTION_COLOR_SCHEME)));
        SawimApplication.updateOptions();
        return true;
    }
}
