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
final class StaticTree {
    public static final int MAX_BITS = 15;
    
    public static final int BL_CODES = 19;
    public static final int D_CODES = 30;
    public static final int LITERALS = 256;
    public static final int LENGTH_CODES = 29;
    public static final int L_CODES = (LITERALS + 1 + LENGTH_CODES);
    
    // Bit length codes must not exceed MAX_BL_BITS bits
    static final int MAX_BL_BITS = 7;
    
    // extra bits for each length code
    public static final int[] extra_lbits={
        0,0,0,0,0,0,0,0,1,1,1,1,2,2,2,2,3,3,3,3,4,4,4,4,5,5,5,5,0
    };
    
    // extra bits for each distance code
    public static final int[] extra_dbits={
        0,0,0,0,1,1,2,2,3,3,4,4,5,5,6,6,7,7,8,8,9,9,10,10,11,11,12,12,13,13
    };
    
    // extra bits for each bit length code
    public static final int[] extra_blbits={
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,2,3,7
    };

    public static final short[] static_dtree = {
        0, 5, 16, 5,  8, 5, 24, 5,  4, 5,
        20, 5, 12, 5, 28, 5,  2, 5, 18, 5,
        10, 5, 26, 5,  6, 5, 22, 5, 14, 5,
        30, 5,  1, 5, 17, 5,  9, 5, 25, 5,
        5, 5, 21, 5, 13, 5, 29, 5,  3, 5,
        19, 5, 11, 5, 27, 5,  7, 5, 23, 5
    };
    
    public static final short[] static_ltree = ArrayLoader.readShortArray("/static_ltree.zlib");
    
    final short[] static_tree;     // static tree or null
    final int[] extra_bits;        // extra bits for each code or null
    final int extra_base;          // base index for extra_bits
    final int elems;               // max number of elements in the tree
    final int max_length;          // max bit length for the codes
    
    public StaticTree(short[] static_tree,
            int[] extra_bits,
            int extra_base,
            int elems,
            int max_length
            ) {
        this.static_tree = static_tree;
        this.extra_bits = extra_bits;
        this.extra_base = extra_base;
        this.elems = elems;
        this.max_length = max_length;
    }
    ///////////////////////////////////////////////////////////////////////////
    static final byte[] bl_order={
        16,17,18,0,8,7,9,6,10,5,11,4,12,3,13,2,14,1,15};
    
    
    // The lengths of the bit length codes are sent in order of decreasing
    // probability, to avoid transmitting the lengths for unused bit
    // length codes.
    
    static final int Buf_size = 8 * 2;
    
    // see definition of array dist_code below
    static final int DIST_CODE_LEN = 512;
    
    static byte[] _dist_code = ArrayLoader.readByteArray("/dist_code.zlib");

    static byte[] _length_code = ArrayLoader.readByteArray("/length_code.zlib");
    
    static final int[] base_length = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 20, 24, 28, 32, 40, 48, 56,
        64, 80, 96, 112, 128, 160, 192, 224, 0
    };
    
    static final int[] base_dist = {
        0,   1,      2,     3,     4,    6,     8,    12,    16,     24,
        32,  48,     64,    96,   128,  192,   256,   384,   512,    768,
        1024, 1536,  2048,  3072,  4096,  6144,  8192, 12288, 16384, 24576
    };
}
// #sijapp cond.end #
