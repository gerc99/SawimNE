package ru.sawim.modules.crypto;

public final class MD5 extends MessageDigest {
    public static byte[] calculate(String input) {
        return calculate(new MD5(), input);
    }

    public static byte[] calculate(byte[] input) {
        return calculate(new MD5(), input);
    }

    public MD5() {
        super("MD5");
    }
}

