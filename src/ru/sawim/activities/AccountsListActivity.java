package ru.sawim.activities;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import protocol.Profile;
import protocol.xmpp.XmppRegistration;
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
public class AccountsListActivity extends BaseActivity implements XmppRegistration.OnAddAccount {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
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
                jabberRegistration.init().show(this);
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
