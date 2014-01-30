package sawim.modules;

import protocol.Profile;
import protocol.Protocol;
import protocol.StatusInfo;
import protocol.XStatusInfo;
import ru.sawim.SawimApplication;
import sawim.roster.RosterHelper;

public final class AutoAbsence {

    private static AutoAbsence instance;
    private Protocol[] protos;
    private Profile[] profiles;
    private long activityOutTime;
    private boolean absence;

    private AutoAbsence() {
        absence = false;
        userActivity();
    }

    public static AutoAbsence getInstance() {
        if (instance == null)
            instance = new AutoAbsence();
        return instance;
    }

    private boolean isSupported(Protocol p) {
        if ((null == p) || !p.isConnected() || p.getStatusInfo().isAway(p.getProfile().statusIndex)) {
            return false;
        }
        return true;
    }

    public final void updateTime() {
        if (SawimApplication.isPaused() && SawimApplication.autoAbsenceTime > 0) {
            if (activityOutTime < SawimApplication.getCurrentGmtTime()) {
                away();
            }
        }
    }

    private final void away() {
        if (absence) {
            return;
        }
        int count = RosterHelper.getInstance().getProtocolCount();
        protos = new Protocol[count];
        profiles = new Profile[count];
        for (int i = 0; i < count; ++i) {
            Protocol p = RosterHelper.getInstance().getProtocol(i);
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

                p.setOnlineStatus(StatusInfo.STATUS_AWAY, pr.statusMessage, false);
            } else {
                protos[i] = null;
            }
        }
        absence = true;
    }

    public final void online() {
        if (!absence || (null == protos)) {
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

                protos[i].setOnlineStatus(pr.statusIndex, pr.statusMessage, false);
            }
        }
    }

    public final void userActivity() {
        if (!SawimApplication.isPaused())
            activityOutTime = SawimApplication.getCurrentGmtTime() + SawimApplication.autoAbsenceTime;
    }
}