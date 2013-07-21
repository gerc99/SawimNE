package ru.sawim.activities;

import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import protocol.Profile;
import protocol.jabber.JabberRegistration;
import ru.sawim.R;
import ru.sawim.view.AccountsListView;
import sawim.cl.ContactList;

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
    private static AccountsListActivity instance;

    public static AccountsListActivity getInstance() {
        return instance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.accounts_list_fragment);

        mResultBundle = savedInstanceState;
        mAccountAuthenticatorResponse =
                getIntent().getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE);
        if (mAccountAuthenticatorResponse != null) {
            mAccountAuthenticatorResponse.onRequestContinued();
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
                AccountsListView view = (AccountsListView) getSupportFragmentManager().findFragmentById(R.id.acconts_list_fragment);
                if (view == null) return false;
                view.addAccount();
                return true;

            case R.id.menu_registration_jabber:
                protocol.jabber.JabberRegistration jabberRegistration = new protocol.jabber.JabberRegistration();
                jabberRegistration.setListener(this);
                jabberRegistration.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void addAccount(int num, Profile acc) {
        AccountsListView view = (AccountsListView) getSupportFragmentManager().findFragmentById(R.id.acconts_list_fragment);
        if (view != null)
            view.addAccount(num, acc);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SawimActivity.getInstance().recreateActivity();
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
