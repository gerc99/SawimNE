package ru.sawim.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import ru.sawim.roster.RosterHelper;

/**
 * Created by gerc on 03.06.2015.
 */
public class PowerSaveModeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (pm.isPowerSaveMode()) {

            } else {
                RosterHelper.getInstance().autoConnect();
            }
        }
    }
}
