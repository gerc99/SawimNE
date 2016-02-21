package ru.sawim.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import ru.sawim.R;
import ru.sawim.view.StartWindowView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:56
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(false);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) return;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            StartWindowView newFragment = new StartWindowView();
            newFragment.init(0, null);
            newFragment.setArguments(getIntent().getExtras());
            transaction.add(R.id.fragment_container, newFragment, StartWindowView.TAG);
            transaction.commit();
        }
    }
}
