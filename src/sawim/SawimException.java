
package sawim;

import sawim.util.JLocale;

public final class SawimException extends Exception {

    private int errorCode;

    public SawimException(int errCode, int extErrCode) {
        super(JLocale.getString("error_" + errCode)
                + " (" + errCode + "." + extErrCode + ")");
        this.errorCode = errCode;
    }

    public boolean isReconnectable() {
        return (errorCode < 110 || errorCode > 117)
                && errorCode != 123 && errorCode != 127 && errorCode != 140;
    }
}

