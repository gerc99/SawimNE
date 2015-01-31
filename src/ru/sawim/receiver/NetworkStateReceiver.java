package ru.sawim.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import protocol.Protocol;
import ru.sawim.roster.RosterHelper;

public class NetworkStateReceiver extends BroadcastReceiver {
    private String previousNetworkType = null;
    private boolean isNetworkAvailable = false;

    private boolean modeNotChanged(String networkType) {
        return (null == previousNetworkType)
                ? (null == networkType)
                : previousNetworkType.equals(networkType);
    }

    public IntentFilter getFilter() {
        return new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE");
    }

    public boolean isNetworkAvailable() {
        return isNetworkAvailable;
    }

    public boolean updateNetworkState(Context context) {
        String networkType = getConnectionType(context);
        if (modeNotChanged(networkType)) return false;
        previousNetworkType = networkType;
        isNetworkAvailable = (null != networkType);
        return true;
    }

    private void resetConnections() {
        int count = RosterHelper.getInstance().getProtocolCount();
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
            p.disconnect(false);
        }
    }

    private void restoreConnections() {
        RosterHelper.getInstance().autoConnect();
    }

    @Override
    public void onReceive(Context context, Intent networkIntent) {
        try {
            if (updateNetworkState(context)) {
                resetConnections();
                if (isNetworkAvailable) {
                    restoreConnections();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String getConnectionType(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            if ((null != activeNetwork) && activeNetwork.isConnected()) {
                return activeNetwork.getTypeName();
            }
            return null;
        } catch (Exception ignored) {
            return "";
        }
    }
}