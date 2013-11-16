


package com.jcraft.jzlib;


final public class ZStream {


    static final private int Z_NO_FLUSH = 0;
    static final private int Z_PARTIAL_FLUSH = 1;
    static final private int Z_SYNC_FLUSH = 2;
    static final private int Z_FULL_FLUSH = 3;
    static final private int Z_FINISH = 4;

    static final private int MAX_MEM_LEVEL = 9;

    static final private int Z_OK = 0;
    static final private int Z_STREAM_END = 1;
    static final private int Z_NEED_DICT = 2;
    static final private int Z_ERRNO = -1;
    static final private int Z_STREAM_ERROR = -2;
    static final private int Z_DATA_ERROR = -3;
    static final private int Z_BUF_ERROR = -5;
    static final private int Z_VERSION_ERROR = -6;


    public static final void setMsg(String msg) {

        sawim.modules.DebugLog.println("zlib: " + msg);

    }

    private static final String[] z_errmsg = {
            "need dictionary",
            "stream end",
            "",
            "file error",
            "stream error",
            "data error",
            "insufficient memory",
            "buffer error",
            "incompatible version",
            ""
    };

    public static final void setDeflateMsg(int msg) {

        sawim.modules.DebugLog.println("zlib: " + z_errmsg[Z_NEED_DICT - msg]);

    }
}


