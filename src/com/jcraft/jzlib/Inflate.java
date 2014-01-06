/* -*-mode:java; c-basic-offset:2; -*- */
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

// #sijapp cond.if modules_ZLIB is "true" #

final class Inflate {
    
    static final public int MAX_WBITS=15; // 32K LZ77 window
    
    // preset dictionary flag in zlib header
    static final private int PRESET_DICT=0x20;
    
    //static final int Z_FINISH=4;
    
    static final private int Z_DEFLATED=8;
    
    static final private int Z_OK=0;
    static final private int Z_STREAM_END=1;
    static final private int Z_BUF_ERROR=-5;
    
    static final private int METHOD=0;   // waiting for method byte
    static final private int FLAG=1;     // waiting for flag byte
    static final private int DICT4=2;    // four dictionary check bytes to go
    static final private int DICT3=3;    // three dictionary check bytes to go
    static final private int DICT2=4;    // two dictionary check bytes to go
    static final private int DICT1=5;    // one dictionary check byte to go
    static final private int DICT0=6;    // waiting for unused_inflateSetDictionary
    static final private int BLOCKS=7;   // decompressing blocks
    static final private int CHECK4=8;   // four check bytes to go
    static final private int CHECK3=9;   // three check bytes to go
    static final private int CHECK2=10;  // two check bytes to go
    static final private int CHECK1=11;  // one check byte to go
    static final private int DONE=12;    // finished check, done
    
    private int mode;                            // current inflate mode
    
    // mode dependent information
    private int method;        // if FLAGS, method byte
    
    // if CHECK, check values to compare
    private long adlerHash;          // computed check value
    private long need;               // stream check value
    
    // mode independent information
    private int nowrap;           // flag for no wrapper
    private int wbits;            // log2(window size)  (8..15, defaults to 15)
    
    private InfBlocks blocks;     // current inflate_blocks state
    
    public Inflate() {
    }
    private int inflateReset() {
        mode = nowrap != 0 ? BLOCKS : METHOD;
        blocks.reset();
        return Z_OK;
    }
    
    int inflateInit(int w, ZBuffers z) {
        //blocks = null;
        
        // handle undocumented nowrap option (no zlib header or check)
        nowrap = 0;
        if (w < 0) {
            w = - w;
            nowrap = 1;
        }
        
        // set window size
        if (w < 8 || w > 15) {
            return JZlib.Z_STREAM_ERROR;
        }
        wbits = w;
        
        blocks = new InfBlocks(z, 0 == nowrap, 1 << w);
        
        // reset state
        inflateReset();
        return Z_OK;
    }
    
    int getErrCode() {
        return blocks.result;
    }
    void inflate(ZBuffers z) throws ZError {
        if (null == z.next_in) {
            throw new ZError(ZError.Z_STREAM_ERROR);
        }
        
        blocks.result = Z_BUF_ERROR;
        while (true) {
            switch (mode) {
                case METHOD:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    method = z.next_in[z.next_in_index++];
                    if (Z_DEFLATED != (method & 0xf)) {
                        ZStream.setMsg("unknown compression method");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    if ((method >> 4) + 8 > wbits) {
                        ZStream.setMsg("invalid window size");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    mode = FLAG;

                case FLAG:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    int b = z.next_in[z.next_in_index++] & 0xff;
                    
                    if ((((method << 8) + b) % 31) != 0) {
                        ZStream.setMsg("incorrect header check");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    
                    if ((b & PRESET_DICT) == 0) {
                        mode = BLOCKS;
                        break;
                    }
                    mode = DICT4;

                case DICT4:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need = ((z.next_in[z.next_in_index++] & 0xff) << 24) & 0xff000000L;
                    mode = DICT3;

                case DICT3:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff)<<16)&0xff0000L;
                    mode = DICT2;

                case DICT2:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff)<<8)&0xff00L;
                    mode = DICT1;

                case DICT1:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need += (z.next_in[z.next_in_index++] & 0xffL);
                    //dictionaryAdlerHash = need;
                    mode = DICT0;
                    throw new ZError(ZError.Z_NEED_DICT);

                case DICT0:
                    ZStream.setMsg("need dictionary");
                    throw new ZError(ZError.Z_STREAM_ERROR);
                
                case BLOCKS:
                    blocks.proc();
                    if (blocks.result != Z_STREAM_END) {
                        return;
                    }
                    blocks.result = Z_OK;
                    adlerHash = blocks.getAdlerHash();
                    blocks.reset();
                    if (nowrap != 0) {
                        mode = DONE;
                        break;
                    }
                    mode = CHECK4;
                    
                case CHECK4:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need = ((z.next_in[z.next_in_index++] & 0xff) << 24) & 0xff000000L;
                    mode = CHECK3;

                case CHECK3:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff) << 16) & 0xff0000L;
                    mode = CHECK2;

                case CHECK2:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need += ((z.next_in[z.next_in_index++] & 0xff) << 8) & 0xff00L;
                    mode = CHECK1;

                case CHECK1:
                    if (0 == z.avail_in) return;
                    blocks.result = Z_OK;
                    
                    z.avail_in--;
                    need += (z.next_in[z.next_in_index++] & 0xffL);
                    
                    if (adlerHash != need) {
                        ZStream.setMsg("incorrect data check");
                        throw new ZError(ZError.Z_DATA_ERROR);
                    }
                    mode = DONE;

                case DONE:
                    blocks.result = Z_STREAM_END;
                    return;

                default:
                    throw new ZError(ZError.Z_STREAM_ERROR);
            }
        }
    }
}
// #sijapp cond.end #
