package ru.sawim.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import ru.sawim.ExternalApi;
import ru.sawim.R;
import ru.sawim.Scheme;

/**
 * Created by admin on 26.03.14.
 */
public class BaseActivity extends ActionBarActivity {

    private final static ExternalApi externalApi = new ExternalApi();
    private OnConfigurationChanged configurationChanged;
    private int oldOrientation;

    public void resetBar(String title) {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setTitle(title);
    }

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

    public void recreateActivity() {
        finish();
        startActivity(new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
    }

    public static void setTheme(Activity a) {
        Scheme.init();
        a.setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        Scheme.load(a);
    }

    public static ExternalApi getExternalApi() {
        return externalApi;
    }

    public void setConfigurationChanged(OnConfigurationChanged configurationChanged) {
        this.configurationChanged = configurationChanged;
    }

    public interface OnConfigurationChanged {
        public void onConfigurationChanged();
    }
}
