package ru.sawim.modules;

import protocol.Profile;
import protocol.Protocol;
import protocol.StatusInfo;
import ru.sawim.SawimApplication;
import ru.sawim.roster.RosterHelper;

public final class AutoAbsence {

    private static AutoAbsence instance;
    private Protocol protocol;
    private Profile profile;
    private long activityOutTime;
    private boolean absence;
    private boolean isChangeStatus = false;

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
        if (0 < activityOutTime && SawimApplication.isPaused() && SawimApplication.autoAbsenceTime > 0) {
            if (activityOutTime < SawimApplication.getCurrentGmtTime()) {
                activityOutTime = -1;
                away();
            }
        }
    }

    private final void away() {
        if (absence) {
            return;
        }
        Protocol p = RosterHelper.getInstance().getProtocol();
            if (isSupported(p)) {
                Profile pr = new Profile();
                protocol = p;
                profile = pr;
                pr.statusIndex = p.getProfile().statusIndex;
                pr.statusMessage = p.getProfile().statusMessage;
                pr.xstatusIndex = p.getProfile().xstatusIndex;
                pr.xstatusTitle = p.getProfile().xstatusTitle;
                pr.xstatusDescription = p.getProfile().xstatusDescription;

                isChangeStatus = true;
                p.setOnlineStatus(StatusInfo.STATUS_AWAY, pr.statusMessage, false);
                isChangeStatus = false;
            } else {
                protocol = null;
            }
        absence = true;
    }

    public final void online() {
        if (!absence || (null == protocol)) {
            return;
        }
        absence = false;
        if (null != protocol) {
            Profile pr = profile;

            isChangeStatus = true;
            protocol.setOnlineStatus(pr.statusIndex, pr.statusMessage, false);
            isChangeStatus = false;
        }
    }

    public final void userActivity() {
        if (!SawimApplication.isPaused()) {
            activityOutTime = SawimApplication.autoAbsenceTime > 0
                    ? SawimApplication.getCurrentGmtTime() + SawimApplication.autoAbsenceTime
                    : -1;
        }
    }

    public boolean isChangeStatus() {
        return isChangeStatus;
    }
}