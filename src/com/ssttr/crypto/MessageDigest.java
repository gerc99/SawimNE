

package com.ssttr.crypto;

import sawim.comm.Util;


public abstract class MessageDigest {


    public byte digestBits[];


    public boolean digestValid;


    public abstract void init();


    public abstract void update(byte aValue);


    public synchronized void update(short aValue) {
        byte b1, b2;

        b1 = (byte) ((aValue >>> 8) & 0xff);
        b2 = (byte) (aValue & 0xff);
        update(b1);
        update(b2);
    }


    public synchronized void update(int aValue) {
        byte b;

        for (int i = 3; i >= 0; i--) {
            b = (byte) ((aValue >>> (i * 8)) & 0xff);
            update(b);
        }
    }


    public synchronized void update(long aValue) {
        byte b;

        for (int i = 7; i >= 0; i--) {
            b = (byte) ((aValue >>> (i * 8)) & 0xff);
            update(b);
        }
    }


    public synchronized void update(byte input[], int offset, int len) {
        for (int i = 0; i < len; i++) {
            update(input[i + offset]);
        }
    }


    public synchronized void update(byte input[]) {
        update(input, 0, input.length);
    }


    public void update(String input) {
        int i, len;
        short x;

        len = input.length();
        for (i = 0; i < len; i++) {
            x = (short) input.charAt(i);
            update(x);
        }
    }


    public void updateASCII(String input) {
        int i, len;
        byte x;

        len = input.length();
        for (i = 0; i < len; i++) {
            x = (byte) (input.charAt(i) & 0xff);
            update(x);
        }
    }


    public abstract void finish();


    public byte[] getDigestBits() {
        return (digestValid) ? digestBits : null;
    }

    public String getDigestHex() {
        if (!digestValid) return null;
        StringBuffer out = new StringBuffer();
        int len = digestBits.length;
        for (int i = 0; i < len; i++) {
            String hex = Integer.toHexString(((int) digestBits[i]) & 0xFF);
            if (1 == hex.length()) out.append(0);
            out.append(hex);
        }
        return out.toString();
    }

    public String getDigestBase64() {
        return Util.base64encode(digestBits);
    }


    public abstract String getAlg();


}

