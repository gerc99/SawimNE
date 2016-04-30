package ru.sawim.ui.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.view.MenuItem;
import ru.sawim.*;
import ru.sawim.ui.fragment.preference.PreferenceScreenBuilder;

/**
 * Created by admin on 25.01.14.
 */
public class MainPreferenceActivity extends PreferenceActivity {

    public static final String TAG = MainPreferenceActivity.class.getSimpleName();

    private SharedPreferences sharedPreferences;
    private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        sharedPreferenceChangeListener = PreferenceScreenBuilder.getSharedPreferenceChangeListener(getPreferenceScreen(), this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

        PreferenceScreenBuilder.build(getPreferenceScreen(), this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
}
