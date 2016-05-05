package ru.sawim.modules.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class HMACSHA1 {
    private Mac hmacSHA1;

    public HMACSHA1() {
        try {
            hmacSHA1 = Mac.getInstance("HmacSHA1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("Unknown algorithm: HmacSHA1");
        }
    }

    public void init(byte[] key) {
        try {
            hmacSHA1.init(new SecretKeySpec(key, "HmacSHA1"));
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new RuntimeException("HMACSHA1.init failed [invalid key]");
        }
    }

    public byte[] hmac(byte[] message) {
        return hmacSHA1.doFinal(message);
    }
}
