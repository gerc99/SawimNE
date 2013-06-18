



package com.jcraft.jzlib;

final class Adler32 {
    
    
    private static final int BASE = 65521;
    
    private static final int NMAX = 5552;
    
    public static long adler32(long adler, byte[] buf, int index, int len) {
        if (null == buf) return 1L;
        if (0 == len) return adler;
        long s1 = adler & 0xffff;
        long s2 = (adler >> 16) & 0xffff;
        int k;
        
        do {
            k = len < NMAX ? len : NMAX;
            len -= k;
            while (k >= 16) {
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                s1 += buf[index++] & 0xff; s2 += s1;
                k -= 16;
            }
            if (0 != k) {
                do {
                    s1 += buf[index++] & 0xff; s2 += s1;
                } while (--k != 0);
            }
            s1 %= BASE;
            s2 %= BASE;
        } while (0 < len);
        return (s2 << 16) | s1;
    }
}


