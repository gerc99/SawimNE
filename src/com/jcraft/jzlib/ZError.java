/*
 * ZError.java
 *
 * Created on 5 Март 2011 г., 0:57
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.jcraft.jzlib;

/**
 *
 * @author Vladimir Kryukov
 */
public final class ZError extends Exception {
    public static final int Z_NEED_DICT = 2;
    public static final int Z_STREAM_ERROR = -2;
    public static final int Z_DATA_ERROR = -3;
    public static final int Z_BUF_ERROR = -5;

    public ZError(int code) {
    }
}
