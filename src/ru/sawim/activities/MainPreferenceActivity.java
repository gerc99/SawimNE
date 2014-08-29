package ru.sawim.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.text.TextUtils;
import ru.sawim.Options;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.Scheme;
import ru.sawim.comm.JLocale;
import ru.sawim.modules.Answerer;
import ru.sawim.view.AboutProgramView;
import ru.sawim.view.preference.SeekBarPreference;

/**
 * Created by admin on 25.01.14.
 */
public class MainPreferenceActivity extends PreferenceActivity {

    public static final String TAG = MainPreferenceActivity.class.getSimpleName();

    private SharedPreferences sharedPreferences;
    private SharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        sharedPreferenceChangeListener = new SharedPreferenceChangeListener();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        getPreferenceScreen().findPreference(getString(R.string.pref_account)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                startActivity(new Intent(MainPreferenceActivity.this, AccountsListActivity.class));
                return false;
            }
        });

        ((ListPreference) getPreferenceScreen().findPreference(JLocale.getString(R.string.pref_color_scheme))).setEntries(Scheme.getSchemeNames());
        ((ListPreference) getPreferenceScreen().findPreference(JLocale.getString(R.string.pref_color_scheme))).setEntryValues(Scheme.getSchemeNames());

        final SeekBarPreference fontSeekBarPreference = (SeekBarPreference) getPreferenceScreen().findPreference(JLocale.getString(R.string.pref_font_scheme));
        int level = Options.getInt(JLocale.getString(R.string.pref_font_scheme));
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

        getPreferenceScreen().findPreference(getString(R.string.pref_answerer)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Answerer.getInstance().activate();
                return false;
            }
        });

        getPreferenceScreen().findPreference(getString(R.string.pref_about_program)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                new AboutProgramView().show(BaseActivity.getCurrentActivity().getSupportFragmentManager(), AboutProgramView.TAG);
                return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this.sharedPreferenceChangeListener);
    }

    private class SharedPreferenceChangeListener implements SharedPreferences.OnSharedPreferenceChangeListener {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = MainPreferenceActivity.this.getPreferenceManager().findPreference(key);
            SawimApplication.updateOptions();
            if (TextUtils.equals(key, getString(R.string.pref_color_scheme))) {
                Scheme.setColorScheme(Scheme.getThemeId(((ListPreference) preference).getValue()));
                Intent intent = getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                finishFromChild(MainPreferenceActivity.this);
                startActivity(intent);
            }
        }
    }
}
