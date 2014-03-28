package ru.sawim.activities;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import ru.sawim.R;
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
    protected void onResume() {
        super.onResume();
        currentActivity = this;
    }

    public void resetBar() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        getSupportActionBar().setIcon(SawimResources.appIcon);
        getSupportActionBar().setTitle(R.string.app_name);
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
