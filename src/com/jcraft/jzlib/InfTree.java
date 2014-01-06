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

// #sijapp cond.if modules_ZLIB is "true" #
final class InfTree {

    static final private int MANY=1440;

    public static final int fixed_bl = 9;
    public static final int fixed_bd = 5;

    public static int[] fixed_tl = ArrayLoader.readIntArray("/fixed_tl.zlib");
    public static int[] fixed_td = ArrayLoader.readIntArray("/fixed_td.zlib");

    // Tables for deflate from PKZIP's appnote.txt.
    static final int[] cplens = { // Copy lengths for literal codes 257..285
        3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 15, 17, 19, 23, 27, 31,
        35, 43, 51, 59, 67, 83, 99, 115, 131, 163, 195, 227, 258, 0, 0
    };

    // see note #13 above about 258
    static final int[] cplext = { // Extra bits for literal codes 257..285
        0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2,
        3, 3, 3, 3, 4, 4, 4, 4, 5, 5, 5, 5, 0, 112, 112  // 112==invalid
    };

    static final int[] cpdist = { // Copy offsets for distance codes 0..29
        1, 2, 3, 4, 5, 7, 9, 13, 17, 25, 33, 49, 65, 97, 129, 193,
        257, 385, 513, 769, 1025, 1537, 2049, 3073, 4097, 6145,
        8193, 12289, 16385, 24577
    };

    static final int[] cpdext = { // Extra bits for distance codes
        0, 0, 0, 0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6,
        7, 7, 8, 8, 9, 9, 10, 10, 11, 11,
        12, 12, 13, 13};

    // If BMAX needs to be larger than 16, then h and x[] should be uLong.
    static final int BMAX = 15;         // maximum bit length of any code

    int[] hn = null;  // hufts used in space
    int[] v = null;   // work area for huft_build
    int[] c = null;   // bit length count table
    int[] r = null;   // table entry for structure assignment
    int[] u = null;   // table stack
    int[] x = null;   // bit offsets, then code stack

    private void huft_build(int[] b, // code lengths in bits (all assumed <= BMAX)
            int bindex,
            int n,   // number of codes (assumed <= 288)
            int s,   // number of simple-valued codes (0..s-1)
            int[] d, // list of base values for non-simple codes
            int[] e, // list of extra bits for non-simple codes
            int[] t, // result: starting table
            int[] m, // maximum lookup bits, returns actual
            int[] hp,// space for trees
            int[] hn,// hufts used in space
            int[] v  // working area: values in order of bit length
            ) throws ZError {
        // Given a list of code lengths and a maximum table size, make a set of
        // tables to decode that set of codes.  Return Z_OK on success, Z_BUF_ERROR
        // if the given code set is incomplete (the tables are still built in this
        // case), Z_DATA_ERROR if the input is invalid (an over-subscribed set of
        // lengths).

        int a;                       // counter for codes of length k
        int f;                       // i repeats in table every f entries
        int g;                       // maximum code length
        int h;                       // table level
        int i;                       // counter, current code
        int j;                       // counter
        int k;                       // number of bits in current code
        int l;                       // bits per table (returned in m)
        int mask;                    // (1 << w) - 1, to avoid cc -O bug on HP
        int p;                       // pointer into c[], b[], or v[]
        int q;                       // points to current table
        int w;                       // bits before this table == (l * h)
        int xp;                      // pointer into x
        int y;                       // number of dummy codes added
        int z;                       // number of entries in current table

        // Generate counts for each bit length

        p = 0; i = n;
        do {
            c[b[bindex + p]]++; p++; i--;   // assume all entries <= BMAX
        } while (0 != i);

        if (c[0] == n) {                // null input--all zero length codes
            t[0] = -1;
            m[0] = 0;
            return;
        }

        // Find minimum and maximum length, bound *m by those
        l = m[0];
        for (j = 1; j <= BMAX; ++j) {
            if (0 != c[j]) break;
        }
        k = j;                        // minimum code length
        if (l < j) {
            l = j;
        }
        for (i = BMAX; i != 0; --i) {
            if (0 != c[i]) break;
        }
        g = i;                        // maximum code length
        if (l > i) {
            l = i;
        }
        m[0] = l;

        // Adjust last length count to fill out codes, if needed
        for (y = 1 << j; j < i; j++, y <<= 1){
            if ((y -= c[j]) < 0) {
                throw new ZError(ZError.Z_DATA_ERROR);
            }
        }
        if ((y -= c[i]) < 0) {
            throw new ZError(ZError.Z_DATA_ERROR);
        }
        c[i] += y;

        // Generate starting offsets into the value table for each length
        x[1] = j = 0;
        p = 1;  xp = 2;
        while (--i!=0) {                 // note that i == g from above
            x[xp] = (j += c[p]);
            xp++;
            p++;
        }

        // Make a table of values in order of bit lengths
        i = 0; p = 0;
        do {
            if ((j = b[bindex+p]) != 0) {
                v[x[j]++] = i;
            }
            p++;
        } while (++i < n);
        n = x[g];                     // set n to length of v

        // Generate the Huffman codes and for each, make the table entries
        x[0] = i = 0;                 // first Huffman code is zero
        p = 0;                        // grab values in bit order
        h = -1;                       // no tables yet--level -1
        w = -l;                       // bits decoded == (l * h)
        u[0] = 0;                     // just to keep compilers happy
        q = 0;                        // ditto
        z = 0;                        // ditto

        // go through the bit lengths (k already is bits in shortest code)
        for (; k <= g; k++) {
            a = c[k];
            while (a--!=0) {
                // here i is the Huffman code of length k bits for value *p
                // make tables up to required level
                while (k > w + l) {
                    h++;
                    w += l;                 // previous table always l bits
                    // compute minimum size table less than or equal to l bits
                    z = g - w;
                    z = (z > l) ? l : z;        // table size upper limit
                    if ((f=1<<(j=k-w))>a+1) {     // try a k-w bit table
                        // too few codes for k-w bit table
                        f -= a + 1;               // deduct codes from patterns left
                        xp = k;
                        if (j < z) {
                            while (++j < z) {        // try smaller tables up to z bits
                                if ((f <<= 1) <= c[++xp])
                                    break;              // enough codes to use up j bits
                                f -= c[xp];           // else deduct codes from patterns
                            }
                        }
                    }
                    z = 1 << j;                 // table entries for j-bit table

                    // allocate new table
                    if (hn[0] + z > MANY) {       // (note: doesn't matter for fixed)
                        throw new ZError(ZError.Z_DATA_ERROR); // overflow of MANY
                    }
                    u[h] = q = /*hp+*/ hn[0];   // DEBUG
                    hn[0] += z;

                    // connect to last table, if there is one
                    if(h!=0) {
                        x[h]=i;           // save pattern for backing up
                        r[0]=(byte)j;     // bits in this table
                        r[1]=(byte)l;     // bits to dump before this table
                        j=i>>>(w - l);
                        r[2] = (int)(q - u[h-1] - j);               // offset to this table
                        System.arraycopy(r, 0, hp, (u[h-1]+j)*3, 3); // connect to last table
                    } else {
                        t[0] = q;               // first table is returned result
                    }
                }

                // set up table entry in r
                r[1] = (byte)(k - w);
                if (p >= n){
                    r[0] = 128 + 64;      // out of values--invalid code
                } else if (v[p] < s) {
                    r[0] = (byte)(v[p] < 256 ? 0 : 32 + 64);  // 256 is end-of-block
                    r[2] = v[p++];          // simple code is just the value
                } else {
                    r[0]=(byte)(e[v[p]-s]+16+64); // non-simple--look up in lists
                    r[2]=d[v[p++] - s];
                }

                // fill code-like entries with r
                f = 1 << (k - w);
                for (j = i >>> w; j < z; j += f) {
                    System.arraycopy(r, 0, hp, (q + j) * 3, 3);
                }

                // backwards increment the k-bit code i
                for (j = 1 << (k - 1); (i & j)!=0; j >>>= 1) {
                    i ^= j;
                }
                i ^= j;

                // backup over finished tables
                mask = (1 << w) - 1;      // needed on HP, cc -O bug
                while ((i & mask) != x[h]) {
                    h--;                    // don't need to update q
                    w -= l;
                    mask = (1 << w) - 1;
                }
            }
        }
        // Return Z_BUF_ERROR if we were given an incomplete table
        if (y != 0 && g != 1) {
            ZStream.setMsg("incomplete some tree");
            throw new ZError(ZError.Z_BUF_ERROR);
        }
    }


    /**
     *
     * @param c   19 code lengths
     * @param bb  bits tree desired/actual depth
     * @param tb  bits tree result
     * @param hp  space for trees
     */
    void inflate_trees_bits(int[] c, int[] bb, int[] tb, int[] hp) throws ZError {
        initWorkArea(19);
        hn[0] = 0;
        huft_build(c, 0, 19, 19, null, null, tb, bb, hp, hn, v);
        if (0 == bb[0]) {
            ZStream.setMsg("incomplete dynamic bit lengths tree");
            throw new ZError(ZError.Z_DATA_ERROR);
        }
    }

    void inflate_trees_dynamic(int nl,   // number of literal/length codes
            int nd,   // number of distance codes
            int[] c,  // that many (total) code lengths
            int[] bl, // literal desired/actual bit depth
            int[] bd, // distance desired/actual bit depth
            int[] tl, // literal/length tree result
            int[] td, // distance tree result
            int[] hp // space for trees
            ) throws ZError {

        // build literal/length tree
        initWorkArea(288);
        hn[0] = 0;
        huft_build(c, 0, nl, 257, cplens, cplext, tl, bl, hp, hn, v);
        if (0 == bl[0]) {
            ZStream.setMsg("incomplete literal/length tree");
            throw new ZError(ZError.Z_DATA_ERROR);
        }

        // build distance tree
        initWorkArea(288);
        huft_build(c, nl, nd, 0, cpdist, cpdext, td, bd, hp, hn, v);

        if (bd[0] == 0 && nl > 257) {
            ZStream.setMsg("empty distance tree with lengths");
            throw new ZError(ZError.Z_DATA_ERROR);
        }
    }

    private void initWorkArea(int vsize) {
        if (null == hn) {
            hn = new int[1];
            v = new int[vsize];
            c = new int[BMAX + 1];
            r = new int[3];
            u = new int[BMAX];
            x = new int[BMAX + 1];

        } else {
            if (v.length < vsize) {
                v = new int[vsize];
            } else {
                for (int i = 0; i < vsize; ++i) {v[i] = 0;}
            }
            for (int i = 0; i < BMAX + 1; ++i) {c[i] = 0;}
            for (int i = 0; i < 3; ++i) {r[i] = 0;}
            //for (int i = 0; i < BMAX; ++i) {u[i] = 0;}
            System.arraycopy(c, 0, u, 0, BMAX);
            //for (int i = 0; i < BMAX + 1; ++i) {x[i] = 0;}
            System.arraycopy(c, 0, x, 0, BMAX+1);
        }
    }
}
// #sijapp cond.end #
