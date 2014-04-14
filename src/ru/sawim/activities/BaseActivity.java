package ru.sawim.activities;

import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import ru.sawim.SawimResources;

/**
 * Created by admin on 26.03.14.
 */
public class BaseActivity extends ActionBarActivity {

    private static BaseActivity currentActivity;

    public static BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        currentActivity = this;
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    protected void onResume() {
        currentActivity = this;
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentActivity = null;
    }

    public void resetBar(String title) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setIcon(SawimResources.appIcon);
        actionBar.setTitle(title);
    }

    int oldOrientation;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (oldOrientation != newConfig.orientation) {
            oldOrientation = newConfig.orientation;
            if (configurationChanged != null) {
                configurationChanged.onConfigurationChanged();
            }
        }
    }

    private static OnConfigurationChanged configurationChanged;

    public OnConfigurationChanged getConfigurationChanged() {
        return configurationChanged;
    }

    public void setConfigurationChanged(OnConfigurationChanged configurationChanged) {
        BaseActivity.configurationChanged = null;
        BaseActivity.configurationChanged = configurationChanged;
    }

    public interface OnConfigurationChanged {
        public void onConfigurationChanged();
    }
}
