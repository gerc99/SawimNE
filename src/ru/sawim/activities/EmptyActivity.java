package ru.sawim.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import ru.sawim.Container;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.listener.OnCreateListener;
import ru.sawim.view.SawimFragment;

public class EmptyActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Container.get(OnCreateListener.class).onCreate(this);
        Container.remove(OnCreateListener.class);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        SawimFragment currentFragment = (SawimFragment) getCurrentFragment();
        if (currentFragment != null && currentFragment.isAdded()) {
            currentFragment.onPrepareOptionsMenu_(menu);
            return true;
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SawimFragment currentFragment = (SawimFragment) getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onOptionsItemSelected_(item);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Fragment getCurrentFragment() {
        int count = getSupportFragmentManager().getBackStackEntryCount();
        return getSupportFragmentManager().getFragments().get(count > 0 ? count - 1 : 0);
    }
}
