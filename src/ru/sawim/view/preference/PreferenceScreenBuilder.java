package ru.sawim.view.preference;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import ru.sawim.*;
import ru.sawim.activities.AccountsListActivity;
import ru.sawim.activities.BaseActivity;
import ru.sawim.activities.EmptyActivity;
import ru.sawim.comm.JLocale;
import ru.sawim.listener.OnCreateListener;
import ru.sawim.modules.Answerer;

/**
 * Created by gerc on 09.01.2015.
 */
public class PreferenceScreenBuilder {

    public static SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceChangeListener(final PreferenceScreen preferenceScreen, final Activity activity) {
        return new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Preference preference = preferenceScreen.findPreference(key);
                SawimApplication.updateOptions();
                if (TextUtils.equals(key, activity.getString(R.string.pref_color_scheme))) {
                    Scheme.setColorScheme(Scheme.getThemeId(((ListPreference) preference).getValue()));
                    Intent intent = activity.getIntent().addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    activity.finishFromChild(activity);
                    activity.startActivity(intent);
                }
            }
        };
    }

    public static void build(PreferenceScreen preferenceScreen, final Activity activity) {
        preferenceScreen.findPreference(activity.getString(R.string.pref_account)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                activity.startActivity(new Intent(activity, AccountsListActivity.class));
                return false;
            }
        });

        ((ListPreference) preferenceScreen.findPreference(JLocale.getString(R.string.pref_color_scheme))).setEntries(Scheme.getSchemeNames());
        ((ListPreference) preferenceScreen.findPreference(JLocale.getString(R.string.pref_color_scheme))).setEntryValues(Scheme.getSchemeNames());

        final SeekBarPreference fontSeekBarPreference = (SeekBarPreference) preferenceScreen.findPreference(JLocale.getString(R.string.pref_font_scheme));
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

        preferenceScreen.findPreference(activity.getString(R.string.pref_answerer)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Container.put(OnCreateListener.class, new OnCreateListener() {
                    @Override
                    public void onCreate(BaseActivity activity) {
                        Answerer.getInstance().activate(activity);
                    }
                });
                activity.startActivity(new Intent(activity, EmptyActivity.class));
                return false;
            }
        });

        preferenceScreen.findPreference(activity.getString(R.string.pref_about_program)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder adb = new AlertDialog.Builder(activity);
                adb.setTitle(R.string.about_program);
                View v = activity.getLayoutInflater().inflate(R.layout.about_program, null);
                LinearLayout aboutLayout = (LinearLayout) v.findViewById(R.id.about_linear);
                TextView about = new TextView(activity);
                about.setTextSize(SawimApplication.getFontSize());
                about.setText(JLocale.getString(R.string.about_program_desc) + "\n" +
                        "PayPal: mazahaka.09@inbox.ru\n" +
                        "WebMoney:\n" +
                        "  R240291120928\n" +
                        "  E299360703034\n" +
                        "  Z158108712712\n" +
                        "Money.Yandex: 410012088026577");
                about.setTypeface(Typeface.DEFAULT_BOLD);
                aboutLayout.addView(about);

                TextView version = new TextView(activity);
                version.setTextSize(SawimApplication.getFontSize());
                version.setText(activity.getString(R.string.version) + ": " + SawimApplication.VERSION);
                version.setTypeface(Typeface.DEFAULT);
                aboutLayout.addView(version);

                TextView author = new TextView(activity);
                author.setTextSize(SawimApplication.getFontSize());
                author.setText(activity.getString(R.string.author) + ": Gerc (Gorbachev Sergey)");
                author.setTypeface(Typeface.DEFAULT);
                aboutLayout.addView(author);

                adb.setView(v);
                adb.show();
                return false;
            }
        });
    }
}
