package ru.sawim.service;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import ru.sawim.SawimApplication;

/**
 * Created with IntelliJ IDEA.
 * <preferences/>
 * Date: 28.07.12 1:01
 *
 * @author vladimir
 */
public class SawimServiceConnection implements ServiceConnection {
    private Messenger mService = null;

    public void onServiceConnected(ComponentName className, IBinder service) {
        mService = new Messenger(service);
    }

    public void onServiceDisconnected(ComponentName className) {
        // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
        mService = null;
        SawimApplication.getInstance().isBindService = false;
        SawimApplication.getInstance().quit(true);
    }

    public void send(Message msg) {
        try {
            mService.send(msg);
        } catch (Exception e) {
            // do nothing
        }
    }
}
