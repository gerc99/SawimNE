package ru.sawim.activities;

import android.content.res.Configuration;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import ru.sawim.R;
import ru.sawim.SawimApplication;
import ru.sawim.SawimResources;

/**
 * Created by admin on 26.03.14.
 */
public class BaseActivity extends ActionBarActivity {

    private static BaseActivity currentActivity;

    public static BaseActivity getCurrentActivity() {
        return currentActivity;
    }

    public static void setCurrentActivity(BaseActivity a) {
        currentActivity = null;
        currentActivity = a;
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
            if (SawimApplication.getInstance().getConfigurationChanged() != null) {
                SawimApplication.getInstance().getConfigurationChanged().onConfigurationChanged();
            }
        }
    }
}
