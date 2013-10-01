package ru.sawim.activities;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import protocol.Profile;
import protocol.jabber.JabberRegistration;
import ru.sawim.General;
import ru.sawim.R;
import ru.sawim.view.AccountsListView;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.05.13
 * Time: 19:56
 * To change this template use File | Settings | File Templates.
 */
public class AccountsListActivity extends FragmentActivity implements JabberRegistration.OnAddAccount {
    private AccountAuthenticatorResponse mAccountAuthenticatorResponse = null;
    private Bundle mResultBundle = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        if (General.currentActivity.getSupportFragmentManager()
                .findFragmentById(R.id.chat_fragment) != null)
            setContentView(R.layout.intercalation_layout);
        else
            setContentView(R.layout.main);
        General.currentActivity = this;
        mResultBundle = savedInstanceState;
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
        }

        if (findViewById(R.id.fragment_container) != null) {
            if (savedInstanceState != null) return;
            AccountsListView rosterView = new AccountsListView();
            rosterView.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, rosterView, AccountsListView.TAG).commit();
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
