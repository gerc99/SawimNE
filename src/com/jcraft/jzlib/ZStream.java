/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/*
Copyright (c) 2000,2001,2002,2003 ymnk, JCraft,Inc. All rights reserved.
 
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 
  1. Redistributions of source code must retain the above copyright notice,
     this list of conditions and the following disclaimer.
 
  2. Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in
     the documentation and/or other materials provided with the distribution.
 
  3. The names of the authors may not be used to endorse or promote products
     derived from this software without specific prior written permission.
 
THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JCRAFT,
INC. OR ANY CONTRIBUTORS TO THIS SOFTWARE BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 * This program is based on zlib-1.1.3, so all credit should go authors
 * Jean-loup Gailly(jloup@gzip.org) and Mark Adler(madler@alumni.caltech.edu)
 * and contributors of zlib.
 */

package com.jcraft.jzlib;

import sawim.modules.DebugLog;

final public class ZStream {

    //static final private int MAX_WBITS=15;        // 32K LZ77 window
    //static final private int DEF_WBITS=MAX_WBITS;

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
        DebugLog.println("zlib: " + msg);
    }

    private static final String[] z_errmsg = {
            "need dictionary",     // Z_NEED_DICT       2
            "stream end",          // Z_STREAM_END      1
            "",                    // Z_OK              0
            "file error",          // Z_ERRNO         (-1)
            "stream error",        // Z_STREAM_ERROR  (-2)
            "data error",          // Z_DATA_ERROR    (-3)
            "insufficient memory", // Z_MEM_ERROR     (-4)
            "buffer error",        // Z_BUF_ERROR     (-5)
            "incompatible version",// Z_VERSION_ERROR (-6)
            ""
    };

    public static final void setDeflateMsg(int msg) {
        DebugLog.println("zlib: " + z_errmsg[Z_NEED_DICT - msg]);
    }
}