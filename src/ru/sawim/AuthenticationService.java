package ru.sawim;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 16.07.13
 * Time: 19:08
 * To change this template use File | Settings | File Templates.
 */
import android.accounts.*;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.activities.AccountsListActivity;
import sawim.Options;
import sawim.cl.ContactList;

public class AuthenticationService extends Service {

    private static AccountAuthenticatorImpl sAccountAuthenticator = null;

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT)) {
            ret = getAuthenticator().getIBinder();
        }
        return ret;
    }

    private AccountAuthenticatorImpl getAuthenticator() {
        if (sAccountAuthenticator == null) {
            sAccountAuthenticator = new AccountAuthenticatorImpl(this);
        }
        return sAccountAuthenticator;
    }

    private static class AccountAuthenticatorImpl extends AbstractAccountAuthenticator {

        private Context mContext;

        public AccountAuthenticatorImpl(Context context) {
            super(context);
            mContext = context;
        }

        @Override
        public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
            Intent i = new Intent(mContext, AccountsListActivity.class);
            i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            Bundle reply = new Bundle();
            reply.putParcelable(AccountManager.KEY_INTENT, i);
            return reply;
        }

        @Override
        public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account) throws android.accounts.NetworkErrorException {
            String id = account.name;
            Bundle result = super.getAccountRemovalAllowed(response, account);
            result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
            try {
            Log.e("getAccountRemovalAllowed", id+"-"+Options.getAccount(id));
            Protocol p = ContactList.getInstance().getProtocol(Options.getAccount(id));
            if (p != null)
                p.setStatus(StatusInfo.STATUS_OFFLINE, "");
            Options.delAccount(id);
            ContactList.setCurrentProtocol();
            Options.safeSave();
            } catch (Exception e) {
                Log.e("AuthenticationService", e.getMessage());
            }
            return result;
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