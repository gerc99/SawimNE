
package ru.sawim;

import ru.sawim.comm.JLocale;

import java.util.HashMap;

public final class SawimException extends Exception {

    private int errorCode;
    private static HashMap<Integer, Integer> errors = new HashMap<Integer, Integer>();

    static {
        errors.put(180, R.string.error_180);
        errors.put(181, R.string.error_181);
        errors.put(182, R.string.error_182);
        errors.put(183, R.string.error_183);
        errors.put(185, R.string.error_185);
        errors.put(190, R.string.error_190);
        errors.put(191, R.string.error_191);
        errors.put(192, R.string.error_192);
        errors.put(193, R.string.error_193);
        errors.put(194, R.string.error_194);
        errors.put(100, R.string.error_100);
        errors.put(110, R.string.error_110);
        errors.put(111, R.string.error_111);
        errors.put(112, R.string.error_112);
        errors.put(113, R.string.error_113);
        errors.put(114, R.string.error_114);
        errors.put(116, R.string.error_116);
        errors.put(118, R.string.error_118);
        errors.put(120, R.string.error_120);
        errors.put(121, R.string.error_121);
        errors.put(122, R.string.error_122);
        errors.put(123, R.string.error_123);
        errors.put(124, R.string.error_124);
        errors.put(125, R.string.error_125);
        errors.put(126, R.string.error_126);
        errors.put(127, R.string.error_127);
        errors.put(128, R.string.error_128);
        errors.put(130, R.string.error_130);
        errors.put(131, R.string.error_131);
        errors.put(132, R.string.error_132);
        errors.put(134, R.string.error_134);
        errors.put(135, R.string.error_135);
        errors.put(136, R.string.error_136);
        errors.put(137, R.string.error_137);
        errors.put(140, R.string.error_140);
        errors.put(154, R.string.error_154);
        errors.put(155, R.string.error_155);
        errors.put(156, R.string.error_156);
        errors.put(157, R.string.error_157);
        errors.put(158, R.string.error_158);
        errors.put(159, R.string.error_159);
        errors.put(160, R.string.error_160);
        errors.put(161, R.string.error_161);
        errors.put(170, R.string.error_170);
        errors.put(171, R.string.error_171);
        errors.put(172, R.string.error_172);
        errors.put(220, R.string.error_220);
        errors.put(221, R.string.error_221);
    }

    public SawimException(int errCode, int extErrCode) {
        super(JLocale.getString(errors.get(errCode))
                + " (" + errCode + "." + extErrCode + ")");
        this.errorCode = errCode;
    }

    public boolean isReconnectable() {
        return (errorCode < 110 || errorCode > 117)
                && errorCode != 123 && errorCode != 127 && errorCode != 140;
    }
}

