

package com.jcraft.jzlib;


public final class ZError extends Exception {
    public static final int Z_NEED_DICT = 2;
    public static final int Z_STREAM_ERROR = -2;
    public static final int Z_DATA_ERROR = -3;
    public static final int Z_BUF_ERROR = -5;

    public ZError(int code) {
    }
}

