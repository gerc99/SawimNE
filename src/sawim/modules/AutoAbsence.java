package sawim.modules;

import android.util.Log;
import ru.sawim.SawimApplication;
import ru.sawim.General;
import sawim.Options;
import sawim.cl.ContactList;
import protocol.Profile;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.XStatusInfo;

public final class AutoAbsence {
    public static final AutoAbsence instance = new AutoAbsence();

    public AutoAbsence() {
        absence = false;
        userActivity();
    }

    private Protocol[] protos;
    private Profile[] profiles;
    private long activityOutTime;
    private boolean absence;
    private boolean use = SawimApplication.instance.useAbsence;
    private int time;

    private void doAway() {
        if (absence) {
            return;
        }
        int count = ContactList.getInstance().getManager().getProtocolCount();
        protos = new Protocol[count];
        profiles = new Profile[count];
        for (int i = 0; i < count; ++i) {
            Protocol p = ContactList.getInstance().getManager().getProtocol(i);
            if (isSupported(p)) {
                Profile pr = new Profile();
                protos[i] = p;
                profiles[i] = pr;
                pr.statusIndex = p.getProfile().statusIndex;
                pr.statusMessage = p.getProfile().statusMessage;
                pr.xstatusIndex = p.getProfile().xstatusIndex;
                pr.xstatusTitle = p.getProfile().xstatusTitle;
                pr.xstatusDescription = p.getProfile().xstatusDescription;
                
                if (protos[i] instanceof protocol.mrim.Mrim) {
                    p.getProfile().xstatusIndex = XStatusInfo.XSTATUS_NONE;
                    p.getProfile().xstatusTitle = "";
                    p.getProfile().xstatusDescription = "";
                }
                
                p.setOnlineStatus(StatusInfo.STATUS_AWAY, pr.statusMessage);
            } else {
                protos[i] = null;
            }
        }
        absence = true;
    }
    private boolean isSupported(Protocol p) {
        if ((null == p) || !p.isConnected() || p.getStatusInfo().isAway(p.getProfile().statusIndex)) {
            return false;
        }
        return true;
    }
    private void doRestore() {
        if (!absence || (null == protos) || time == 0) {
            return;
        }
        absence = false;
        for (int i = 0; i < protos.length; ++i) {
            if (null != protos[i]) {
                Profile pr = profiles[i];
                
                if (protos[i] instanceof protocol.mrim.Mrim) {
                    Profile p = protos[i].getProfile();
                    p.xstatusIndex = pr.xstatusIndex;
                    p.xstatusTitle = pr.xstatusTitle;
                    p.xstatusDescription = pr.xstatusDescription;
                }

                protos[i].setOnlineStatus(pr.statusIndex, pr.statusMessage);
            }
        }
    }

    public final void updateTime() {
        time = Options.getInt(Options.OPTION_AA_TIME);
        if (!absence && time > 0) {
            try {
                if (0 < activityOutTime) {
                    if (activityOutTime < General.getCurrentGmtTime()) {
                        doAway();
                        activityOutTime = -1;
                    }
                } else if (General.isPaused()) {
                    away();
                }
            } catch (Exception e) {
            }
        }
    }
    public final void away() {
        if (0 < time && !use) {
            use = false;
            doAway();
        }
    }
    public final void online() {
        if (0 < time && !use) {
            use = true;
            doRestore();
        }
    }

    public final void userActivity() {
        time = Options.getInt(Options.OPTION_AA_TIME);
        try {
            if (!General.isPaused() && time > 0) {
                int init = time * 60;
                if (0 < init) {
                    activityOutTime = General.getCurrentGmtTime() + init;
                } else {
                    activityOutTime = -1;
                }
                if (absence) {
                    doRestore();
                }
            }
        } catch (Exception e) {
        }
    }
}