package protocol;

public final class Profile {

    public String userId = "";
    public String password = "";
    public String nick = "";

    public byte statusIndex = StatusInfo.STATUS_OFFLINE;
    public String statusMessage;

    public byte xstatusIndex = -1;
    public String xstatusTitle;
    public String xstatusDescription;
    public boolean isActive;

    public Profile() {
    }

    public boolean isConnected() {
        return StatusInfo.STATUS_OFFLINE != statusIndex;
    }
}