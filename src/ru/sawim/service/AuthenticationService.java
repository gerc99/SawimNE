package ru.sawim.service;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.07.13
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
import android.accounts.*;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import ru.sawim.SawimApplication;
import ru.sawim.activities.AccountsListActivity;

public class AuthenticationService extends Service {

    private static AccountAuthenticatorImpl sAccountAuthenticator = null;

    static {
        sAccountAuthenticator = new AccountAuthenticatorImpl();
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = getAuthenticator().getIBinder();
        }
        return ret;
    }

    private AccountAuthenticatorImpl getAuthenticator() {
        return sAccountAuthenticator;
    }

    private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {
        public AccountAuthenticatorImpl() {
            super(SawimApplication.getInstance());
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            Intent i = new Intent(SawimApplication.getInstance(), AccountsListActivity.class);
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            Bundle reply = new Bundle();
            reply.putParcelable(AccountManager.KEY_INTENT, i);
            return reply;
        }

        @Override
        public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle hasFeatures(AccountAuthenticatorResponse arg0, Account arg1, String[] arg2) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle updateCredentials(AccountAuthenticatorResponse arg0, Account arg1, String arg2, Bundle arg3) throws NetworkErrorException {
            return null;
        }

        @Override
        public String getAuthTokenLabel(String arg0) {
            return null;
        }

        @Override
        public Bundle confirmCredentials(AccountAuthenticatorResponse arg0, Account arg1, Bundle arg2) throws NetworkErrorException {
            return null;
        }

        @Override
        public Bundle editProperties(AccountAuthenticatorResponse arg0, String arg1) {
            return null;
        }
    }
}