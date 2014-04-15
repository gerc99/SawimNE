package ru.sawim.modules.crypto;

public final class SHA1 extends ru.sawim.modules.crypto.MessageDigest {
    public static byte[] calculate(byte[] input) {
        return calculate(new SHA1(), input);
    }

    public SHA1() {
        super("SHA-1");
    }
}
