package sawim;

import sawim.cl.ContactList;
import sawim.modules.DebugLog;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created with IntelliJ IDEA.
 * User: Gerc
 * Date: 23.06.13
 * Time: 15:41
 * To change this template use File | Settings | File Templates.
 */
public class Updater extends TimerTask {

    private static Updater uiUpdater;
    private static Timer uiTimer;

    public static final int UPDATE_TIME = 250;

    public static void startUIUpdater() {
        if (null != uiTimer) {
            uiTimer.cancel();
        }
        uiTimer = new Timer();
        uiUpdater = new Updater();
        uiTimer.schedule(uiUpdater, 0, UPDATE_TIME);
    }

    public void run() {
        try {
            ContactList.getInstance().timerAction();
        } catch (OutOfMemoryError out) {
            // nothing
        } catch (Exception e) {
            DebugLog.panic("UIUpdater", e);
        }
    }
}
