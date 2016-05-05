package ru.sawim.modules.crypto;

import ru.sawim.comm.StringConvertor;

import java.security.NoSuchAlgorithmException;

public class MessageDigest {
    private java.security.MessageDigest digest;

    static byte[] calculate(MessageDigest digest, String input) {
        byte data[] = StringConvertor.stringToByteArrayUtf8(input);
        return calculate(digest, data);
    }

    static byte[] calculate(MessageDigest digest, byte input[]) {
        digest.update(input);
        return digest.getDigestBits();
    }

    public MessageDigest(String algorithm) {
        try {
            digest = java.security.MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Unknown algorithm: " + algorithm);
        }
    }

    public void init() {
        digest.reset();
    }

    public void update(byte input) {
        digest.update(input);
    }

    public void update(byte input[]) {
        digest.update(input);
    }

    public void updateASCII(String input) {
        digest.update(StringConvertor.stringToByteArrayUtf8(input));
    }

    public byte[] getDigestBits() {
        return digest.digest();
    }

    public String getDigestHex() {
        byte digestBits[] = digest.digest();
        StringBuilder out = new StringBuilder();
        for (byte digestBit : digestBits) {
            char c = (char) ((digestBit >> 4) & 0xf);
            out.append((c > 9) ? (char) ((c - 10) + 'a') : (char) (c + '0'));
            c = (char) (digestBit & 0xf);
            out.append((c > 9) ? (char) ((c - 10) + 'a') : (char) (c + '0'));
        }
        return out.toString();
    }
}
