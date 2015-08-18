package ru.sawim.activities;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import ru.sawim.*;
import ru.sawim.view.preference.PreferenceScreenBuilder;

/**
 * Created by gerc on 09.01.2015.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MainPreferenceActivityNew extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        BaseActivity.setTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new SettingsFragment())
                .commit();
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

    public static class SettingsFragment extends PreferenceFragment {
        private SharedPreferences sharedPreferences;
        private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;

        @Override
        public void onCreate(Bundle paramBundle) {
            super.onCreate(paramBundle);
            addPreferencesFromResource(R.xml.preference);

            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
            sharedPreferenceChangeListener = PreferenceScreenBuilder.getSharedPreferenceChangeListener(getPreferenceScreen(), getActivity());
            sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

            PreferenceScreenBuilder.build(getPreferenceScreen(), getActivity());
        }

        @Override
        public void onResume() {
            super.onResume();
            sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
        }

        @Override
        public void onPause() {
            super.onPause();
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(this.sharedPreferenceChangeListener);
        }
    }
}
