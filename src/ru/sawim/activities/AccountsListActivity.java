package ru.sawim.activities;

import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import protocol.Profile;
import protocol.xmpp.XmppRegistration;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.Scheme;
import ru.sawim.view.AccountsListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:56
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListActivity extends ActionBarActivity implements XmppRegistration.OnAddAccount {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            setContentView(R.layout.intercalation_layout);
        else
            setContentView(General.isManyPane() ? R.layout.main_twopane : R.layout.main);
        if (General.actionBar == null)
            General.actionBar = getSupportActionBar();
        General.actionBar.setDisplayHomeAsUpEnabled(true);
        General.actionBar.setDisplayShowTitleEnabled(true);
        General.actionBar.setDisplayUseLogoEnabled(true);
        General.actionBar.setDisplayShowHomeEnabled(true);
        General.actionBar.setDisplayShowCustomEnabled(false);
        General.actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) return;
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            AccountsListView accountsListView = new AccountsListView();
            accountsListView.setArguments(getIntent().getExtras());
            transaction.add(R.id.fragment_container, accountsListView, AccountsListView.TAG);
            transaction.commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.accounts_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.add:
                AccountsListView view = (AccountsListView) getSupportFragmentManager().findFragmentByTag(AccountsListView.TAG);
                if (view == null) return false;
                view.addAccount();
                return true;

            case R.id.menu_registration_jabber:
                protocol.xmpp.XmppRegistration jabberRegistration = new protocol.xmpp.XmppRegistration();
                jabberRegistration.setListener(this);
                jabberRegistration.init().show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void addAccount(int num, Profile acc) {
        AccountsListView view = (AccountsListView) getSupportFragmentManager().findFragmentByTag(AccountsListView.TAG);
        if (view != null)
            view.addAccount(num, acc);
    }
}
