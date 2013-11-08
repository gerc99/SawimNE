package ru.sawim.activities;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import protocol.Profile;
import protocol.jabber.JabberRegistration;
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
public class AccountsListActivity extends ActionBarActivity implements JabberRegistration.OnAddAccount {
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setTheme(Scheme.isBlack() ? R.style.BaseTheme : R.style.BaseThemeLight);
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            setContentView(R.layout.intercalation_layout);
        else
            setContentView(R.layout.main);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        General.currentActivity = this;
        mResultBundle = savedInstanceState;
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

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
                protocol.jabber.JabberRegistration jabberRegistration = new protocol.jabber.JabberRegistration();
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

    public void finish() {
        if (mAccountAuthenticatorResponse != null) {
            if (mResultBundle != null) {
                mAccountAuthenticatorResponse.onResult(mResultBundle);
            } else {
                mAccountAuthenticatorResponse.onError(AccountManager.ERROR_CODE_CANCELED,
                        "canceled");
            }
            mAccountAuthenticatorResponse = null;
        }
        super.finish();
    }
}
