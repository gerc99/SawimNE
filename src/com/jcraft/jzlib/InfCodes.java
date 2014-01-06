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
final class InfCodes {

    static final private int[] inflate_mask = {
        0x00000000, 0x00000001, 0x00000003, 0x00000007, 0x0000000f,
        0x0000001f, 0x0000003f, 0x0000007f, 0x000000ff, 0x000001ff,
        0x000003ff, 0x000007ff, 0x00000fff, 0x00001fff, 0x00003fff,
        0x00007fff, 0x0000ffff
    };

    static final private int Z_OK=0;
    static final private int Z_STREAM_END=1;

    // waiting for "i:"=input,
    //             "o:"=output,
    //             "x:"=nothing
    static final private int START=0;  // x: set up for LEN
    static final private int LEN=1;    // i: get length/literal/eob next
    static final private int LENEXT=2; // i: getting length extra (have base)
    static final private int DIST=3;   // i: get distance next
    static final private int DISTEXT=4;// i: getting distance extra
    static final private int COPY=5;   // o: copying bytes in window, waiting for space
    static final private int LIT=6;    // o: got literal, waiting for output space
    static final private int WASH=7;   // o: got eob, possibly still output waiting
    static final private int END=8;    // x: got eob and all data flushed
    //static final private int BADCODE=9;// x: got error

    private int mode;      // current inflate_codes mode

    // mode dependent information
    private int len;

    private int[] tree; // pointer into tree
    private int tree_index = 0;
    private int need;   // bits needed

    private int lit;

    // if EXT or COPY, where and how much
    private int get;              // bits to get for extra
    private int dist;             // distance back to copy from

    private byte lbits;           // ltree bits decoded per branch
    private byte dbits;           // dtree bits decoder per branch
    private int[] ltree;          // literal/length/eob tree
    private int ltree_index;      // literal/length/eob tree
    private int[] dtree;          // distance tree
    private int dtree_index;      // distance tree

    private ZBuffers z;
    InfCodes(ZBuffers z) {
        this.z = z;
    }
    void init(int bl, int bd,
            int[] tl, int tl_index,
            int[] td, int td_index) {
        mode = START;
        lbits = (byte)bl;
        dbits = (byte)bd;
        ltree = tl;
        ltree_index = tl_index;
        dtree = td;
        dtree_index = td_index;
        tree = null;
    }

    void proc(InfBlocks s) throws ZError {
        int j = 0;              // temporary storage
        int[] t = null;            // temporary pointer
        int tindex = 0;         // temporary pointer
        int e = 0;              // extra bits or operation
        int b = s.bitb;                      // bit buffer
        int k = s.bitk;                      // bits in bit buffer
        int p = z.next_in_index;             // input data pointer
        int n = z.avail_in;                  // bytes available there
        int q = s.write;                     // output window write pointer
        int m = (q < s.read) ? (s.read - q - 1) : (s.end - q); // bytes to end of window or read pointer
        int f = 0;              // pointer to copy strings from
        final byte[] z_next_in = z.next_in;

        // process input and output based on current state
        while (true) {
            switch (mode) {
                // waiting for "i:"=input, "o:"=output, "x:"=nothing
                case START:         // x: set up for LEN
                    if (m >= 258 && n >= 10) {

                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.next_in_index = p;
                        s.write=q;
                        inflate_fast(lbits, dbits,
                                ltree, ltree_index,
                                dtree, dtree_index,
                                s);

                        p = z.next_in_index;
                        n = z.avail_in;
                        b = s.bitb;
                        k = s.bitk;
                        q = s.write;
                        m = q < s.read ? s.read - q - 1 : s.end - q;

                        if (Z_OK != s.result) {
                            if (Z_STREAM_END == s.result) {
                                mode = WASH;
                            } else {
                                throw new ZError(ZError.Z_DATA_ERROR);
                            }
                            break;
                        }
                    }
                    need = lbits;
                    tree = ltree;
                    tree_index = ltree_index;

                    mode = LEN;

                case LEN:           // i: get length/literal/eob next
                    j = need;

                    while (k < j) {
                        if (0 != n) {
                            s.result = Z_OK;

                        } else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    tindex = (tree_index + (b & inflate_mask[j])) * 3;

                    b >>>= (tree[tindex + 1]);
                    k -= (tree[tindex + 1]);

                    e = tree[tindex];

                    if (e == 0) {               // literal
                        lit = tree[tindex + 2];
                        mode = LIT;
                        break;
                    }
                    if ((e & 16) != 0) {          // length
                        get = e & 15;
                        len = tree[tindex + 2];
                        mode = LENEXT;
                        break;
                    }
                    if ((e & 64) == 0) {        // next table
                        need = e;
                        tree_index = tindex / 3 + tree[tindex + 2];
                        break;
                    }
                    if ((e & 32) != 0) {               // end of block
                        mode = WASH;
                        break;
                    }
                    ZStream.setMsg("invalid literal/length code");
                    throw new ZError(ZError.Z_DATA_ERROR);

                case LENEXT:        // i: getting length extra (have base)
                    j = get;

                    while  (k < j) {
                        if (n != 0) {
                            s.result = Z_OK;
                        } else {
                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    len += (b & inflate_mask[j]);

                    b >>= j;
                    k -= j;

                    need = dbits;
                    tree = dtree;
                    tree_index = dtree_index;
                    mode = DIST;

                case DIST:          // i: get distance next
                    j = need;

                    while (k < j) {
                        if (n != 0) {
                            s.result = Z_OK;
                        } else {

                            s.bitb=b;s.bitk=k;
                            z.avail_in=n;
                            z.next_in_index=p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    tindex = (tree_index + (b & inflate_mask[j])) * 3;

                    b >>= tree[tindex + 1];
                    k -= tree[tindex + 1];

                    e = (tree[tindex]);
                    if ((e & 16) != 0) {               // distance
                        get = e & 15;
                        dist = tree[tindex+2];
                        mode = DISTEXT;
                        break;
                    }
                    if ((e & 64) == 0) {        // next table
                        need = e;
                        tree_index = tindex / 3 + tree[tindex + 2];
                        break;
                    }
                    ZStream.setMsg("invalid distance code");
                    throw new ZError(ZError.Z_DATA_ERROR);

                case DISTEXT:       // i: getting distance extra
                    j = get;

                    while (k < j) {
                        if(n != 0) {
                            s.result = Z_OK;
                        } else {

                            s.bitb = b;
                            s.bitk = k;
                            z.avail_in = n;
                            z.next_in_index = p;
                            s.write = q;
                            s.inflate_flush();
                            return;
                        }
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    dist += (b & inflate_mask[j]);

                    b >>= j;
                    k -= j;

                    mode = COPY;

                case COPY:          // o: copying bytes in window, waiting for space
                    f = q - dist;
                    while (f < 0) {     // modulo window size-"while" instead
                        f += s.end;     // of "if" handles invalid distances
                    }
                    while (len != 0) {

                        if (m == 0) {
                            if (q == s.end && s.read != 0) {
                                q = 0;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.write = q;
                                s.inflate_flush();
                                q = s.write;
                                m = q < s.read ? s.read - q - 1 : s.end - q;

                                if (q == s.end && s.read != 0) {
                                    q = 0;
                                    m = q < s.read ? s.read - q - 1 : s.end - q;
                                }

                                if (m == 0) {
                                    s.bitb = b;
                                    s.bitk = k;
                                    z.avail_in = n;
                                    z.next_in_index = p;
                                    s.write = q;
                                    s.inflate_flush();
                                    return;
                                }
                            }
                        }

                        s.window[q++] = s.window[f++];
                        m--;

                        if (f == s.end) {
                            f = 0;
                        }
                        len--;
                    }
                    mode = START;
                    break;
                case LIT:           // o: got literal, waiting for output space
                    if (m == 0) {
                        if (q == s.end && s.read != 0) {
                            q = 0;
                            m = q < s.read ? s.read - q - 1 : s.end - q;
                        }
                        if (m == 0) {
                            s.write = q;
                            s.inflate_flush();
                            q = s.write;
                            m = q < s.read ? s.read - q - 1 : s.end - q;

                            if (q == s.end && s.read != 0) {
                                q = 0;
                                m = q < s.read ? s.read - q - 1 : s.end - q;
                            }
                            if (m == 0) {
                                s.bitb = b;
                                s.bitk = k;
                                z.avail_in = n;
                                z.next_in_index = p;
                                s.write = q;
                                s.inflate_flush();
                                return;
                            }
                        }
                    }
                    s.result = Z_OK;

                    s.window[q++] = (byte)lit;
                    m--;

                    mode = START;
                    break;

                case WASH:           // o: got eob, possibly more output
                    if (k > 7) {        // return unused byte, if any
                        k -= 8;
                        n++;
                        p--;             // can always return one
                    }

                    s.write = q;
                    s.inflate_flush();
                    q = s.write;
                    m = q < s.read ? s.read - q - 1 : s.end - q;

                    if (s.read != s.write) {
                        s.bitb = b;
                        s.bitk = k;
                        z.avail_in = n;
                        z.next_in_index = p;
                        s.write = q;
                        s.inflate_flush();
                        return;
                    }
                    s.result = Z_STREAM_END;
                    mode = END;
                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.next_in_index = p;
                    s.write = q;
                    s.inflate_flush();

                case END:
                    s.result = Z_STREAM_END;
                    return;

                default:
                    throw new ZError(ZError.Z_STREAM_ERROR);
            }
        }
    }

    void free() {
    }

    // Called with number of bytes left to write in window at least 258
    // (the maximum string length) and number of input bytes available
    // at least ten.  The ten bytes are six bytes for the longest length/
    // distance pair plus four bytes for overloading the bit buffer.

    private void inflate_fast(int bl, int bd,
            int[] tl, int tl_index,
            int[] td, int td_index,
            InfBlocks s) throws ZError {
        int t;                // temporary pointer
        int[] tp;             // temporary pointer
        int tp_index;         // temporary pointer
        int e;                // extra bits or operation
        int c;                // bytes to copy
        int d;                // distance back to copy from
        int r;                // copy source pointer

        // load input, output, bit values
        int b = s.bitb;          // bit buffer
        int k = s.bitk;          // bits in bit buffer
        int p = z.next_in_index; // input data pointer
        int n = z.avail_in;      // bytes available there
        int q = s.write;         // output window write pointer
        int m = (q < s.read) ? (s.read - q - 1) : (s.end - q); // bytes to end of window or read pointer

        int tp_index_t_3;     // (tp_index+t)*3

        // initialize masks
        int ml = inflate_mask[bl]; // mask for literal/length tree
        int md = inflate_mask[bd]; // mask for distance tree

        byte[] z_next_in = z.next_in;

        // do until not enough input or output space for fast loop
        do {                          // assume called with m >= 258 && n >= 10
            // get literal/length code
            while (k < 20) {              // max bits for literal/length code
                n--;
                b |= (z_next_in[p++]&0xff) << k;
                k += 8;
            }

            t = b & ml;
            tp = tl;
            tp_index = tl_index;
            tp_index_t_3 = (tp_index + t) * 3;
            if ((e = tp[tp_index_t_3]) == 0){
                b >>= (tp[tp_index_t_3 + 1]);
                k -= (tp[tp_index_t_3 + 1]);

                s.window[q++] = (byte)tp[tp_index_t_3 + 2];
                m--;
                continue;
            }
            while (true) {

                b >>= (tp[tp_index_t_3 + 1]);
                k -= (tp[tp_index_t_3 + 1]);

                if ((e & 16) != 0) {
                    e &= 15;
                    c = tp[tp_index_t_3+2] + ((int)b & inflate_mask[e]);

                    b>>=e; k-=e;

                    // decode distance base of block to copy
                    while (k < 15) {           // max bits for distance code
                        n--;
                        b |= (z_next_in[p++] & 0xff) << k;
                        k += 8;
                    }

                    t = b & md;
                    tp = td;
                    tp_index = td_index;
                    tp_index_t_3 = (tp_index + t) * 3;
                    e = tp[tp_index_t_3];

                    while (true) {

                        b >>= (tp[tp_index_t_3 + 1]);
                        k -= (tp[tp_index_t_3 + 1]);

                        if ((e & 16) != 0) {
                            // get extra bits to add to distance base
                            e &= 15;
                            while (k < e) {         // get extra bits (up to 13)
                                n--;
                                b |= (z_next_in[p++] & 0xff) << k;
                                k += 8;
                            }

                            d = tp[tp_index_t_3 + 2] + (b & inflate_mask[e]);

                            b >>= (e);
                            k -= (e);

                            // do the copy
                            m -= c;
                            if (q >= d) {                // offset before dest
                                //  just copy
                                r=q-d;
                                if(q-r>0 && 2>(q-r)){
                                    s.window[q++]=s.window[r++]; // minimum count is three,
                                    s.window[q++]=s.window[r++]; // so unroll loop a little
                                    c-=2;
                                } else{
                                    System.arraycopy(s.window, r, s.window, q, 2);
                                    q+=2; r+=2; c-=2;
                                }
                            } else {                  // else offset after destination
                                r = q - d;
                                do {
                                    r += s.end;          // force pointer in window
                                } while (r < 0);         // covers invalid distances
                                e = s.end - r;
                                if (c > e) {             // if source crosses,
                                    c -= e;              // wrapped copy
                                    if (q - r > 0 && e > (q - r)) {
                                        do {
                                            s.window[q++] = s.window[r++];
                                        } while (--e!=0);
                                    } else{
                                        System.arraycopy(s.window, r, s.window, q, e);
                                        q += e;
                                        r += e;
                                        e=0;
                                    }
                                    r = 0;                  // copy rest from start of window
                                }

                            }

                            // copy all or what's left
                            if (q - r > 0 && c > (q - r)) {
                                do {
                                    s.window[q++] = s.window[r++];
                                } while(--c != 0);
                            } else {
                                System.arraycopy(s.window, r, s.window, q, c);
                                q += c;
                                r += c;
                                c = 0;
                            }
                            break;
                        } else if ((e & 64) == 0) {
                            t += tp[tp_index_t_3+2];
                            t += (b & inflate_mask[e]);
                            tp_index_t_3 = (tp_index + t) * 3;
                            e = tp[tp_index_t_3];
                        } else {
                            ZStream.setMsg("invalid distance code");
                            throw new ZError(ZError.Z_DATA_ERROR);
                        }
                    }
                    break;
                }

                if ((e & 64) == 0) {
                    t += tp[tp_index_t_3 + 2];
                    t += (b&inflate_mask[e]);
                    tp_index_t_3 = (tp_index + t) * 3;
                    e = tp[tp_index_t_3];
                    if (e == 0) {

                        b >>= (tp[tp_index_t_3 + 1]);
                        k -= (tp[tp_index_t_3 + 1]);

                        s.window[q++] = (byte)tp[tp_index_t_3 + 2];
                        m--;
                        break;
                    }

                } else if ((e & 32) != 0) {

                    c = z.avail_in - n;
                    c = (k >> 3) < c ? k >> 3 : c;
                    n += c;
                    p -= c;
                    k -= c << 3;

                    s.bitb = b;
                    s.bitk = k;
                    z.avail_in = n;
                    z.next_in_index = p;
                    s.write = q;

                    s.result = Z_STREAM_END;
                    return;

                } else {
                    ZStream.setMsg("invalid literal/length code");
                    throw new ZError(ZError.Z_DATA_ERROR);
                }
            }
        } while (m >= 258 && n >= 10);

        // not enough input or output--restore pointers and return
        c = z.avail_in - n;
        c = (k >> 3) < c ? k >> 3 : c;
        n += c;
        p -= c;
        k -= c << 3;

        s.bitb = b;
        s.bitk = k;
        z.avail_in = n;
        z.next_in_index = p;
        s.write = q;

        s.result = Z_OK;
    }
}
// #sijapp cond.end #
