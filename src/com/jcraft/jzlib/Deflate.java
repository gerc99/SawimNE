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
public final class Deflate {

    static final private int MAX_MEM_LEVEL = 9;

    static final private int Z_DEFAULT_COMPRESSION = -1;

// # ifdef JZLIB_STD_DEFLATE_BUFS
// //#   //  that is: 128K for windowBits=15  +  128K for memLevel = 8  (default values)
// //#   static final private int MAX_WBITS=15;            // 32K LZ77 window
// //#   static final private int DEF_MEM_LEVEL=8;
// # else
    //  that is: 2K for windowBits=9  +  1K for memLevel = 1    thanks to Taras Zackrepa (ONjA)
    static final public int MAX_WBITS=9;
    static final private int DEF_MEM_LEVEL=1;
// # endif

    static class Config {
        int good_length; // reduce lazy search above this match length
        int max_lazy;    // do not perform lazy search above this match length
        int nice_length; // quit search above this match length
        int max_chain;
        int func;
        Config(int good_length, int max_lazy,
                int nice_length, int max_chain, int func) {
            this.good_length=good_length;
            this.max_lazy=max_lazy;
            this.nice_length=nice_length;
            this.max_chain=max_chain;
            this.func=func;
        }
    }

    static final private int STORED=0;
    static final private int FAST=1;
    static final private int SLOW=2;
    private Config getConfig() {
        switch (level) {
            //                        good  lazy  nice  chain
            case 0: return new Config(0,    0,    0,    0, STORED);
            case 1: return new Config(4,    4,    8,    4, FAST);
            case 2: return new Config(4,    5,   16,    8, FAST);
            case 3: return new Config(4,    6,   32,   32, FAST);
            case 4: return new Config(4,    4,   16,   16, SLOW);
            case 5: return new Config(8,   16,   32,   32, SLOW);
            case 6: return new Config(8,   16,  128,  128, SLOW);
            case 7: return new Config(8,   32,  128,  256, SLOW);
            case 8: return new Config(32, 128,  258, 1024, SLOW);
            case 9: return new Config(32, 258,  258, 4096, SLOW);
        }
        return null;
    }

    // block not completed, need more input or more output
    static final private int NeedMore=0;

    // block flush performed
    static final private int BlockDone=1;

    // finish started, need only more output at next deflate
    static final private int FinishStarted=2;

    // finish done, accept no more input or output
    static final private int FinishDone=3;

    // preset dictionary flag in zlib header
    static final private int PRESET_DICT=0x20;

    static final private int Z_FILTERED=1;
    static final private int Z_HUFFMAN_ONLY=2;
    static final private int Z_DEFAULT_STRATEGY=0;

    static final private int Z_NO_FLUSH=0;
    static final private int Z_PARTIAL_FLUSH=1;
    static final private int Z_SYNC_FLUSH=2;
    static final private int Z_FULL_FLUSH=3;
    static final private int Z_FINISH=4;

    static final private int Z_OK=0;
    static final private int Z_STREAM_END=1;
    static final private int Z_STREAM_ERROR=-2;
    static final private int Z_DATA_ERROR=-3;
    static final private int Z_BUF_ERROR=-5;

    static final private int INIT_STATE=42;
    static final private int BUSY_STATE=113;
    static final private int FINISH_STATE=666;

    // The deflate compression method
    static final private int Z_DEFLATED=8;

    static final private int STORED_BLOCK=0;
    static final private int STATIC_TREES=1;
    static final private int DYN_TREES=2;

    // The three kinds of block type
    static final private byte Z_BINARY=0;
    static final private byte Z_ASCII=1;
    static final private byte Z_UNKNOWN=2;

    static final private int Buf_size=8*2;

    // repeat previous bit length 3-6 times (2 bits of repeat count)
    static final private int REP_3_6=16;

    // repeat a zero length 3-10 times  (3 bits of repeat count)
    static final private int REPZ_3_10=17;

    // repeat a zero length 11-138 times  (7 bits of repeat count)
    static final private int REPZ_11_138=18;

    static final private int MIN_MATCH=3;
    static final private int MAX_MATCH=258;
    static final private int MIN_LOOKAHEAD=(MAX_MATCH+MIN_MATCH+1);

    static final private int MAX_BITS=15;
    static final private int D_CODES=30;
    static final private int BL_CODES=19;
    static final private int LENGTH_CODES=29;
    static final private int LITERALS=256;
    static final private int L_CODES=(LITERALS+1+LENGTH_CODES);
    static final private int HEAP_SIZE=(2*L_CODES+1);

    static final private int END_BLOCK=256;

    ZBuffers strm;         // pointer back to this zlib stream
    int status;           // as the name implies
    byte[] pending_buf;   // output still pending
    int pending_buf_size; // size of pending_buf
    int pending_out;      // next pending byte to output to the stream
    int pending;          // nb of bytes in the pending buffer
    int noheader;         // suppress zlib header and adler32
    int last_flush;       // value of flush param for previous deflate call

    int w_size;           // LZ77 window size (32K by default)
    int w_bits;           // log2(w_size)  (8..16)
    int w_mask;           // w_size - 1

    byte[] window;
    // Sliding window. Input bytes are read into the second half of the window,
    // and move to the first half later to keep a dictionary of at least wSize
    // bytes. With this organization, matches are limited to a distance of
    // wSize-MAX_MATCH bytes, but this ensures that IO is always
    // performed with a length multiple of the block size. Also, it limits
    // the window size to 64K, which is quite useful on MSDOS.
    // To do: use the user input buffer as sliding window.

    int window_size;
    // Actual size of window: 2*wSize, except when the user input buffer
    // is directly used as sliding window.

    short[] prev;
    // Link to older string with same hash index. To limit the size of this
    // array to 64K, this link is maintained only for the last 32K strings.
    // An index in this array is thus a window index modulo 32K.

    short[] head; // Heads of the hash chains or NIL.

    int ins_h;          // hash index of string to be inserted
    int hash_size;      // number of elements in hash table
    int hash_bits;      // log2(hash_size)
    int hash_mask;      // hash_size-1

    // Number of bits by which ins_h must be shifted at each input
    // step. It must be such that after MIN_MATCH steps, the oldest
    // byte no longer takes part in the hash key, that is:
    // hash_shift * MIN_MATCH >= hash_bits
    int hash_shift;

    // Window position at the beginning of the current output block. Gets
    // negative when the window is moved backwards.

    int block_start;

    int match_length;           // length of best match
    int prev_match;             // previous match
    int match_available;        // set if previous match exists
    int strstart;               // start of string to insert
    int match_start;            // start of matching string
    int lookahead;              // number of valid bytes ahead in window

    // Length of the best match at previous step. Matches not greater than this
    // are discarded. This is used in the lazy match evaluation.
    int prev_length;

    // To speed up deflation, hash chains are never searched beyond this
    // length.  A higher limit improves compression ratio but degrades the speed.
    int max_chain_length;

    int compress_func;

    // Attempt to find a better match only when the current match is strictly
    // smaller than this value. This mechanism is used only for compression
    // levels >= 4.
    int max_lazy_match;

    // Insert new strings in the hash table only if the match length is not
    // greater than this length. This saves time but degrades compression.
    // max_insert_length is used only for compression levels <= 3.

    /** compression level (1..9) */
    int level;
    /** favor or force Huffman coding */
    int strategy;

    // Use a faster search when the previous match is longer than this
    int good_match;

    // Stop searching when current match exceeds this
    int nice_match;

    private short[] dyn_ltree;       // literal and length tree
    private short[] dyn_dtree;       // distance tree
    private short[] bl_tree;         // Huffman tree for bit lengths
    private int dyn_l_maxcode;       // literal and length tree
    private int dyn_d_maxcode;       // distance tree
    //private int bl_maxcode;          // Huffman tree for bit lengths

    /** desc for literal tree */
    private final StaticTree l_static_tree =
            new StaticTree(StaticTree.static_ltree, StaticTree.extra_lbits,
            StaticTree.LITERALS + 1, StaticTree.L_CODES, StaticTree.MAX_BITS);

    /** desc for distance tree */
    private final StaticTree d_static_tree =
            new StaticTree(StaticTree.static_dtree, StaticTree.extra_dbits,
            0,  StaticTree.D_CODES, StaticTree.MAX_BITS);

    /** desc for bit length tree */
    private final StaticTree bl_static_tree =
            new StaticTree(null, StaticTree.extra_blbits,
            0, StaticTree.BL_CODES, StaticTree.MAX_BL_BITS);

    // number of codes at each bit length for an optimal tree
    short[] bl_count = new short[MAX_BITS + 1];

    // heap used to build the Huffman trees
    int[] heap = new int[2 * L_CODES + 1];

    int heap_len;               // number of elements in the heap
    int heap_max;               // element of largest frequency
    // The sons of heap[n] are heap[2*n] and heap[2*n+1]. heap[0] is not used.
    // The same heap array is used to build all trees.

    // Depth of each subtree used as tie breaker for trees of equal frequency
    byte[] depth = new byte[2 * L_CODES + 1];

    int l_buf;               // index for literals or lengths */

    // Size of match buffer for literals/lengths.  There are 4 reasons for
    // limiting lit_bufsize to 64K:
    //   - frequencies can be kept in 16 bit counters
    //   - if compression is not successful for the first block, all input
    //     data is still in the window so we can still emit a stored block even
    //     when input comes from standard input.  (This can also be done for
    //     all blocks if lit_bufsize is not greater than 32K.)
    //   - if compression is not successful for a file smaller than 64K, we can
    //     even emit a stored file instead of a stored block (saving 5 bytes).
    //     This is applicable only for zip (not gzip or zlib).
    //   - creating new Huffman trees less frequently may not provide fast
    //     adaptation to changes in the input data statistics. (Take for
    //     example a binary file with poorly compressible code followed by
    //     a highly compressible string table.) Smaller buffer sizes give
    //     fast adaptation but have of course the overhead of transmitting
    //     trees more frequently.
    //   - I can't count above 4
    int lit_bufsize;

    int last_lit;      // running index in l_buf

    // Buffer for distances. To simplify the code, d_buf and l_buf have
    // the same number of elements. To use different lengths, an extra flag
    // array would be necessary.

    int d_buf;         // index of pendig_buf

    int opt_len;        // bit length of current block with optimal trees
    int static_len;     // bit length of current block with static trees
    int matches;        // number of string matches in current block
    int last_eob_len;   // bit length of EOB code for last block

    // Output buffer. bits are inserted starting at the bottom (least
    // significant bits).
    short bi_buf;

    // Number of valid bits in bi_buf.  All bits above the last valid bit
    // are always zero.
    int bi_valid;

    private long adlerHash;

    Deflate(ZBuffers strm) {
        dyn_ltree = new short[HEAP_SIZE*2];
        dyn_dtree = new short[(2*D_CODES+1)*2]; // distance tree
        bl_tree = new short[(2*BL_CODES+1)*2];  // Huffman tree for bit lengths
        this.strm = strm;
    }

    private void lm_init() {
        window_size = 2 * w_size;

        head[hash_size - 1] = 0;
        for (int i = 0; i < hash_size - 1; ++i) {
            head[i] = 0;
        }

        // Set the default configuration parameters:
        Config config = getConfig();
        compress_func    = config.func;
        max_lazy_match   = config.max_lazy;
        good_match       = config.good_length;
        nice_match       = config.nice_length;
        max_chain_length = config.max_chain;

        strstart = 0;
        block_start = 0;
        lookahead = 0;
        match_length = prev_length = MIN_MATCH-1;
        match_available = 0;
        ins_h = 0;
    }

    // Initialize the tree data structures for a new zlib stream.
    void tr_init() {

        bi_buf = 0;
        bi_valid = 0;
        last_eob_len = 8; // enough lookahead for inflate

        // Initialize the first block of the first file:
        init_block();
    }

    void init_block() {
        // Initialize the trees.
        for(int i = 0; i < L_CODES; ++i) dyn_ltree[i*2] = 0;
        for(int i= 0; i < D_CODES; ++i) dyn_dtree[i*2] = 0;
        for(int i= 0; i < BL_CODES; ++i) bl_tree[i*2] = 0;

        dyn_ltree[END_BLOCK*2] = 1;
        opt_len = static_len = 0;
        last_lit = matches = 0;
    }

    /**
     * Restore the heap property by moving down the tree starting at node k,
     * exchanging a node with the smallest of its two sons if necessary, stopping
     * when the heap property is re-established (each father smaller than its
     * two sons).
     *
     * @param tree   the tree to restore
     * @param k      node to move down
     */
    void pqdownheap(short[] tree, int k) {
        int v = heap[k];
        int j = k << 1;  // left son of k
        while (j <= heap_len) {
            // Set j to the smallest of the two sons:
            if (j < heap_len && smaller(tree, heap[j+1], heap[j], depth)) {
                ++j;
            }
            // Exit if v is smaller than both sons
            if(smaller(tree, v, heap[j], depth)) break;

            // Exchange v with the smallest son
            heap[k] = heap[j];
            k = j;
            // And continue down the tree, setting j to the left son of k
            j <<= 1;
        }
        heap[k] = v;
    }

    private boolean smaller(short[] tree, int n, int m, byte[] depth) {
        short tn2 = tree[n * 2];
        short tm2 = tree[m * 2];
        return (tn2 < tm2 || (tn2 == tm2 && depth[n] <= depth[m]));
    }

    /**
     * Scan a literal or distance tree to determine the frequencies of the codes
     * in the bit length tree.
     *
     * @param tree      the tree to be scanned
     * @param max_code  and its largest code of non zero frequency
     */
    void scan_tree(short[] tree, int max_code) {
        int n;                     // iterates over all tree elements
        int prevlen = -1;          // last emitted length
        int curlen;                // length of current code
        int nextlen = tree[0*2+1]; // length of next code
        int count = 0;             // repeat count of the current code
        int max_count = 7;         // max repeat count
        int min_count = 4;         // min repeat count

        if (nextlen == 0) { max_count = 138; min_count = 3; }
        tree[(max_code + 1) * 2 + 1] = (short)0xffff; // guard

        for(n = 0; n <= max_code; ++n) {
            curlen = nextlen;
            nextlen = tree[(n + 1) * 2 + 1];
            if (++count < max_count && curlen == nextlen) {
                continue;
            } else if(count < min_count) {
                bl_tree[curlen * 2] += count;
            } else if(curlen != 0) {
                if(curlen != prevlen) ++bl_tree[curlen << 1];
                ++bl_tree[REP_3_6 * 2];
            } else if(count <= 10) {
                ++bl_tree[REPZ_3_10 * 2];
            } else {
                ++bl_tree[REPZ_11_138 * 2];
            }
            count = 0;
            prevlen = curlen;
            if (0 == nextlen) {
                max_count = 138;
                min_count = 3;
            } else if (curlen == nextlen) {
                max_count = 6;
                min_count = 3;
            } else {
                max_count = 7;
                min_count = 4;
            }
        }
    }

    // Construct the Huffman tree for the bit lengths and return the index in
    // bl_order of the last bit length code to send.
    int build_bl_tree() {
        int max_blindex;  // index of last bit length code of non zero freq

        // Determine the bit length frequencies for literal and distance trees
        scan_tree(dyn_ltree, dyn_l_maxcode);
        scan_tree(dyn_dtree, dyn_d_maxcode);

        // Build the bit length tree:
        //bl_maxcode = build_tree(bl_tree, bl_static_tree);
        build_tree(bl_tree, bl_static_tree);

        // opt_len now includes the length of the tree representations, except
        // the lengths of the bit lengths codes and the 5+5+4 bits for the counts.

        // Determine the number of bit length codes to send. The pkzip format
        // requires that at least 4 bit length codes be sent. (appnote.txt says
        // 3 but the actual value used is 4.)
        for (max_blindex = BL_CODES-1; max_blindex >= 3; --max_blindex) {
            if (bl_tree[(StaticTree.bl_order[max_blindex] << 1) + 1] != 0) break;
        }
        // Update opt_len to include the bit length tree and counts
        opt_len += 3*(max_blindex+1) + 5+5+4;

        return max_blindex;
    }


    // Send the header for a block using dynamic Huffman trees: the counts, the
    // lengths of the bit length codes, the literal tree and the distance tree.
    // IN assertion: lcodes >= 257, dcodes >= 1, blcodes >= 4.
    void send_all_trees(int lcodes, int dcodes, int blcodes) {
        send_bits(lcodes - 257, 5); // not +255 as stated in appnote.txt
        send_bits(dcodes - 1,   5);
        send_bits(blcodes - 4,  4); // not -3 as stated in appnote.txt
        for (int rank = 0; rank < blcodes; ++rank) {
            send_bits(bl_tree[(StaticTree.bl_order[rank] << 1) + 1], 3);
        }
        send_tree(dyn_ltree, lcodes - 1); // literal tree
        send_tree(dyn_dtree, dcodes - 1); // distance tree
    }

    /**
     * Send a literal or distance tree in compressed form, using the codes in
     * bl_tree.
     *
     * @param tree      the tree to be sent
     * @param max_code  and its largest code of non zero frequency
     */
    void send_tree(short[] tree, int max_code) {
        int n;                     // iterates over all tree elements
        int prevlen = -1;          // last emitted length
        int curlen;                // length of current code
        int nextlen = tree[0*2+1]; // length of next code
        int count = 0;             // repeat count of the current code
        int max_count = 7;         // max repeat count
        int min_count = 4;         // min repeat count

        if (nextlen == 0){ max_count = 138; min_count = 3; }

        for (n = 0; n <= max_code; ++n) {
            curlen = nextlen; nextlen = tree[((n+1) << 1) + 1];
            if(++count < max_count && curlen == nextlen) {
                continue;
            } else if(count < min_count) {
                do { send_code(curlen, bl_tree); } while (--count != 0);
            } else if(curlen != 0) {
                if(curlen != prevlen) {
                    send_code(curlen, bl_tree); count--;
                }
                send_code(REP_3_6, bl_tree);
                send_bits(count-3, 2);
            } else if(count <= 10) {
                send_code(REPZ_3_10, bl_tree);
                send_bits(count-3, 3);
            } else {
                send_code(REPZ_11_138, bl_tree);
                send_bits(count-11, 7);
            }
            count = 0; prevlen = curlen;
            if(nextlen == 0){
                max_count = 138; min_count = 3;
            } else if(curlen == nextlen){
                max_count = 6; min_count = 3;
            } else{
                max_count = 7; min_count = 4;
            }
        }
    }

    // Output a byte on the stream.
    // IN assertion: there is enough room in pending_buf.
    final void put_byte(byte[] p, int start, int len){
        System.arraycopy(p, start, pending_buf, pending, len);
        pending += len;
    }

    final void put_byte(byte c){
        pending_buf[pending++] = c;
    }
    final void put_short(int w) {
        put_byte((byte)(w/*&0xff*/));
        put_byte((byte)(w >>> 8));
    }
    final void putShortMSB(int b){
        put_byte((byte)(b >> 8));
        put_byte((byte)(b/*&0xff*/));
    }

    final void send_code(int c, short[] tree){
        int c2 = c * 2;
        send_bits((tree[c2] & 0xffff), (tree[c2+1] & 0xffff));
    }

    void send_bits(int value, int length) {
        int len = length;
        if (bi_valid > (int)Buf_size - len) {
            int val = value;
//      bi_buf |= (val << bi_valid);
            bi_buf |= ((val << bi_valid)&0xffff);
            put_short(bi_buf);
            bi_buf = (short)(val >>> (Buf_size - bi_valid));
            bi_valid += len - Buf_size;
        } else {
//      bi_buf |= (value) << bi_valid;
            bi_buf |= (((value) << bi_valid)&0xffff);
            bi_valid += len;
        }
    }

    /**
     * Send one empty static block to give enough lookahead for inflate.
     * This takes 10 bits, of which 7 may remain in the bit buffer.
     * The current inflate code requires 9 bits of lookahead. If the
     * last two codes for the previous block (real code plus EOB) were coded
     * on 5 bits or less, inflate may have only 5+3 bits of lookahead to decode
     * the last real code. In this case we send two empty static blocks instead
     * of one. (There are no problems if the previous block is stored or fixed.)
     * To simplify the code, we assume the worst case of last real code encoded
     * on one bit only.
     */
    void _tr_align() {
        send_bits(STATIC_TREES << 1, 3);
        send_code(END_BLOCK, l_static_tree.static_tree);

        bi_flush();

        // Of the 10 bits for the empty block, we have already sent
        // (10 - bi_valid) bits. The lookahead for the last real code (before
        // the EOB of the previous block) was thus at least one plus the length
        // of the EOB plus what we have just sent of the empty static block.
        if (1 + last_eob_len + 10 - bi_valid < 9) {
            send_bits(STATIC_TREES << 1, 3);
            send_code(END_BLOCK, l_static_tree.static_tree);
            bi_flush();
        }
        last_eob_len = 7;
    }


    /**
     * Save the match info and tally the frequency counts. Return true if
     * the current block must be flushed.
     *
     * @param dist  distance of matched string
     * @param lc    match length-MIN_MATCH or unmatched char (if dist==0)
     */
    boolean _tr_tally(int dist, int lc) {
        pending_buf[d_buf + (last_lit << 1)] = (byte)(dist >>> 8);
        pending_buf[d_buf + (last_lit << 1) + 1] = (byte)dist;

        pending_buf[l_buf + last_lit] = (byte)lc;
        ++last_lit;

        if (dist == 0) {
            // lc is the unmatched char
            ++dyn_ltree[lc << 1];
        } else {
            ++matches;
            // Here, lc is the match length - MIN_MATCH
            --dist;             // dist = match distance - 1
            ++dyn_ltree[(getLength_code(lc) + LITERALS + 1) * 2];
            ++dyn_dtree[d_code(dist) << 1];
        }

        if ((last_lit & 0x1fff) == 0 && level > 2) {
            // Compute an upper bound for the compressed length
            int out_length = last_lit << 3;
            int in_length = strstart - block_start;
            int dcode;
            for (dcode = 0; dcode < D_CODES; ++dcode) {
                out_length += (int)dyn_dtree[dcode*2] *
                        (5L+d_static_tree.extra_bits[dcode]);
            }
            out_length >>>= 3;
            if ((matches < (last_lit/2)) && out_length < in_length/2) return true;
        }

        return (last_lit == lit_bufsize-1);
        // We avoid equality with lit_bufsize because of wraparound at 64K
        // on 16 bit machines and because stored blocks are restricted to
        // 64K-1 bytes.
    }

    /**
     * Mapping from a distance to a distance code. dist is the distance - 1 and
     * must not have side effects. _dist_code[256] and _dist_code[257] are never
     * used.
     */
    private int d_code(int dist) {
        return ((dist) < 256 ? StaticTree._dist_code[dist]
                : StaticTree._dist_code[256 + ((dist) >>> 7)]);
    }

    private byte getLength_code(int lc) {
        return StaticTree._length_code[lc];
    }

    // Send the block data compressed using the given Huffman trees
    void compress_block(short[] ltree, short[] dtree) {
        int  dist;      // distance of matched string
        int lc;         // match length or unmatched char (if dist == 0)
        int lx = 0;     // running index in l_buf
        int code;       // the code to send
        int extra;      // number of extra bits to send

        if (0 != last_lit) {
            do {
                dist = ((pending_buf[d_buf + (lx << 1)]<<8) & 0xff00) |
                        (pending_buf[d_buf + (lx << 1) + 1] & 0xff);
                lc = (pending_buf[l_buf+lx]) & 0xff;
                ++lx;

                if (0 == dist) {
                    send_code(lc, ltree); // send a literal byte
                } else {
                    // Here, lc is the match length - MIN_MATCH
                    code = getLength_code(lc);

                    send_code(code+LITERALS+1, ltree); // send the length code
                    extra = l_static_tree.extra_bits[code];
                    if (extra != 0) {
                        lc -= StaticTree.base_length[code];
                        send_bits(lc, extra);       // send the extra length bits
                    }
                    --dist; // dist is now the match distance - 1
                    code = d_code(dist);

                    send_code(code, dtree);       // send the distance code
                    extra = d_static_tree.extra_bits[code];
                    if (extra != 0) {
                        dist -= StaticTree.base_dist[code];
                        send_bits(dist, extra);   // send the extra distance bits
                    }
                } // literal or match pair ?

                // Check that the overlay between pending_buf and d_buf+l_buf is ok:
            } while (lx < last_lit);
        }

        send_code(END_BLOCK, ltree);
        last_eob_len = ltree[END_BLOCK*2+1];
    }

//    // Set the data type to ASCII or BINARY, using a crude approximation:
//    // binary if more than 20% of the bytes are <= 6 or >= 128, ascii otherwise.
//    // IN assertion: the fields freq of dyn_ltree are set and the total of all
//    // frequencies does not exceed 64K (to fit in an int on 16 bit machines).
//    void set_data_type(){
//        int n = 0;
//        int  ascii_freq = 0;
//        int  bin_freq = 0;
//        while(n<7){ bin_freq += dyn_ltree[n << 1]; ++n;}
//        while(n<128){ ascii_freq += dyn_ltree[n << 1]; ++n;}
//        while(n<LITERALS){ bin_freq += dyn_ltree[n << 1]; ++n;}
//        data_type = bin_freq > (ascii_freq >>> 2) ? Z_BINARY : Z_ASCII;
//    }

    // Flush the bit buffer, keeping at most 7 bits in it.
    void bi_flush() {
        if (bi_valid == 16) {
            put_short(bi_buf);
            bi_buf=0;
            bi_valid=0;
        } else if (bi_valid >= 8) {
            put_byte((byte)bi_buf);
            bi_buf>>>=8;
            bi_valid-=8;
        }
    }

    // Flush the bit buffer and align the output on a byte boundary
    void bi_windup() {
        if (bi_valid > 8) {
            put_short(bi_buf);
        } else if (bi_valid > 0) {
            put_byte((byte)bi_buf);
        }
        bi_buf = 0;
        bi_valid = 0;
    }

    /**
     * Copy a stored block, storing first the length and its
     * one's complement if requested.
     *
     * @param buf     the input data
     * @param len     its length
     * @param header  true if block header must be written
     */
    void copy_block(int buf, int len, boolean header) {
        int index = 0;
        bi_windup();      // align on byte boundary
        last_eob_len = 8; // enough lookahead for inflate

        if (header) {
            put_short((short)len);
            put_short((short)~len);
        }

        //  while(len--!=0) {
        //    put_byte(window[buf+index]);
        //    index++;
        //  }
        put_byte(window, buf, len);
    }

    void flush_block_only(boolean eof){
        _tr_flush_block(block_start >= 0 ? block_start : -1,
                strstart - block_start,
                eof);
        block_start = strstart;
        flush_pending();
    }

    // Copy without compression as much as possible from the input stream, return
    // the current block state.
    // This function does not insert new strings in the dictionary since
    // uncompressible data is probably not useful. This function is used
    // only for the level=0 compression option.
    // NOTE: this function should be optimized to avoid extra copying from
    // window to pending_buf.
    int deflate_stored(int flush) {
        // Stored blocks are limited to 0xffff bytes, pending_buf is limited
        // to pending_buf_size, and each stored block has a 5 byte header:

        int max_block_size = 0xffff;
        int max_start;

        if (max_block_size > pending_buf_size - 5) {
            max_block_size = pending_buf_size - 5;
        }

        // Copy as much as possible from input to output:
        while(true) {
            // Fill the window as much as possible:
            if(lookahead<=1) {
                fill_window();
                if(lookahead==0 && flush==Z_NO_FLUSH) return NeedMore;
                if(lookahead==0) break; // flush the current block
            }

            strstart+=lookahead;
            lookahead=0;

            // Emit a stored block if pending_buf will be full:
            max_start=block_start+max_block_size;
            if(strstart==0|| strstart>=max_start) {
                // strstart == 0 is possible when wraparound on 16-bit machine
                lookahead = (int)(strstart-max_start);
                strstart = (int)max_start;

                flush_block_only(false);
                if(strm.avail_out==0) return NeedMore;

            }

            // Flush if we may have to slide, otherwise block_start may become
            // negative and the data will be gone:
            if(strstart-block_start >= w_size-MIN_LOOKAHEAD) {
                flush_block_only(false);
                if(strm.avail_out==0) return NeedMore;
            }
        }

        flush_block_only(Z_FINISH == flush);
        if(strm.avail_out==0) {
            return (Z_FINISH == flush) ? FinishStarted : NeedMore;
        }

        return (Z_FINISH == flush) ? FinishDone : BlockDone;
    }

    /**
     * Send a stored block
     *
     * @param buf         input block
     * @param stored_len  length of input block
     * @param eof         true if this is the last block for a file
     */
    void _tr_stored_block(int buf, int stored_len, boolean eof) {
        send_bits((STORED_BLOCK<<1)+(eof?1:0), 3);  // send block type
        copy_block(buf, stored_len, true);          // with header
    }

    /**
     * Determine the best encoding for the current block: dynamic trees, static
     * trees or store, and output the encoded block to the zip file.
     *
     * @param buf         input block, or NULL if too old
     * @param stored_len  length of input block
     * @param eof         true if this is the last block for a file
     */
    void _tr_flush_block(int buf, int stored_len, boolean eof) {
        int opt_lenb, static_lenb;// opt_len and static_len in bytes
        int max_blindex = 0;      // index of last bit length code of non zero freq

        // Build the Huffman trees unless a stored block is forced
        if(level > 0) {
//            // Check if the file is ascii or binary
//            if(data_type == Z_UNKNOWN) set_data_type();

            // Construct the literal and distance trees
            dyn_l_maxcode = build_tree(dyn_ltree, l_static_tree);
            dyn_d_maxcode = build_tree(dyn_dtree, d_static_tree);

            // At this point, opt_len and static_len are the total bit lengths of
            // the compressed block data, excluding the tree representations.

            // Build the bit length tree for the above two trees, and get the index
            // in bl_order of the last bit length code to send.
            max_blindex=build_bl_tree();

            // Determine the best encoding. Compute first the block length in bytes
            opt_lenb=(opt_len+3+7)>>>3;
            static_lenb=(static_len+3+7)>>>3;

            if(static_lenb<=opt_lenb) opt_lenb=static_lenb;
        } else {
            opt_lenb=static_lenb=stored_len+5; // force a stored block
        }

        if (stored_len+4<=opt_lenb && buf != -1) {
            // 4: two words for the lengths
            // The test buf != NULL is only necessary if LIT_BUFSIZE > WSIZE.
            // Otherwise we can't have processed more than WSIZE input bytes since
            // the last block flush, because compression would have been
            // successful. If LIT_BUFSIZE <= WSIZE, it is never too late to
            // transform a block into a stored block.
            _tr_stored_block(buf, stored_len, eof);
        } else if (static_lenb == opt_lenb) {
            send_bits((STATIC_TREES << 1) + (eof ? 1 : 0), 3);
            compress_block(l_static_tree.static_tree, d_static_tree.static_tree);
        } else {
            send_bits((DYN_TREES << 1) + (eof ? 1 : 0), 3);
            send_all_trees(dyn_l_maxcode + 1, dyn_d_maxcode + 1, max_blindex + 1);
            compress_block(dyn_ltree, dyn_dtree);
        }

        // The above check is made mod 2^32, for files larger than 512 MB
        // and uLong implemented on 32 bits.

        init_block();

        if (eof) {
            bi_windup();
        }
    }


    /**
     * Construct one Huffman tree and assigns the code bit strings and lengths.
     * Update the total bit length for the current block.
     *
     * IN assertion: the field freq is set for all tree elements.
     * OUT assertions: the fields len and code are set to the optimal bit length
     *      and corresponding code. The length opt_len is updated; static_len is
     *      also updated if stree is not null. The field max_code is set.
     */
    int build_tree(short[] tree, StaticTree stat_desc) {
        final short[] stree = stat_desc.static_tree;
        int elems = stat_desc.elems;
        int n, m;          // iterate over heap elements
        int max_code = -1; // largest code with non zero frequency
        int node;          // new node being created

        // Construct the initial heap, with least frequent element in
        // heap[1]. The sons of heap[n] are heap[2*n] and heap[2*n+1].
        // heap[0] is not used.
        heap_len = 0;
        heap_max = HEAP_SIZE;

        for (n = 0; n < elems; ++n) {
            if(tree[n << 1] != 0) {
                heap[++heap_len] = max_code = n;
                depth[n] = 0;
            } else{
                tree[(n << 1) + 1] = 0;
            }
        }

        // The pkzip format requires that at least one distance code exists,
        // and that at least one bit should be sent even if there is only one
        // possible code. So to avoid special checks later on we force at least
        // two codes of non zero frequency.
        while (heap_len < 2) {
            node = heap[++heap_len] = (max_code < 2 ? ++max_code : 0);
            tree[node << 1] = 1;
            depth[node] = 0;
            --opt_len;
            if (null != stree) {
                static_len -= stree[(node << 1) + 1];
            }
            // node is 0 or 1 so it does not have extra bits
        }

        // The elements heap[heap_len/2+1 .. heap_len] are leaves of the tree,
        // establish sub-heaps of increasing lengths:

        for (n = heap_len / 2; n >= 1; --n) {
            pqdownheap(tree, n);
        }

        // Construct the Huffman tree by repeatedly combining the least two
        // frequent nodes.

        node = elems;                 // next internal node of the tree
        do{
            // n = node of least frequency
            n = heap[1];
            heap[1] = heap[heap_len--];
            pqdownheap(tree, 1);
            m = heap[1];                // m = node of next least frequency

            heap[--heap_max] = n; // keep the nodes sorted by frequency
            heap[--heap_max] = m;

            // Create a new node father of n and m
            tree[node << 1] = (short)(tree[(n << 1)] + tree[m << 1]);
            depth[node] = (byte)(Math.max(depth[n], depth[m]) + 1);
            tree[(n << 1) + 1] = tree[(m << 1) + 1] = (short)node;

            // and insert the new node in the heap
            heap[1] = node++;
            pqdownheap(tree, 1);
        } while (2 <= heap_len);

        heap[--heap_max] = heap[1];

        // At this point, the fields freq and dad are set. We can now
        // generate the bit lengths.

        gen_bitlen(tree, max_code, stat_desc);

        // The field len is now set, we can generate the bit codes
        gen_codes(tree, max_code, bl_count);
        return max_code;
    }

    /**
     * Generate the codes for a given tree and bit counts (which need not be
     * optimal).
     * IN assertion: the array bl_count contains the bit length statistics for
     * the given tree and the field len is set for all tree elements.
     * OUT assertion: the field code is set for all tree elements of non
     * zero code length.
     *
     * @param tree       the tree to decorate
     * @param max_code   largest code with non zero frequency
     * @param bl_count   number of codes at each bit length
     */
    private void gen_codes(short[] tree, int max_code, short[] bl_count) {
        short[] next_code = new short[MAX_BITS+1]; // next code value for each bit length
        short code = 0;            // running code value
        int bits;                  // bit index
        int n;                     // code index

        // The distribution counts are first used to generate the code values
        // without bit reversal.
        for (bits = 1; bits <= MAX_BITS; ++bits) {
            next_code[bits] = code = (short)((code + bl_count[bits - 1]) << 1);
        }

        // Check that the bit counts in bl_count are consistent. The last code
        // must be all ones.
        //Assert (code + bl_count[MAX_BITS]-1 == (1<<MAX_BITS)-1,
        //        "inconsistent bit counts");
        //Tracev((stderr,"\ngen_codes: max_code %d ", max_code));

        for (n = 0;  n <= max_code; ++n) {
            int len = tree[(n << 1) + 1];
            if (len == 0) continue;
            // Now reverse the bits
            tree[n << 1] = bi_reverse(next_code[len]++, len);
        }
    }

    /**
     * Reverse the first len bits of a code, using straightforward code (a faster
     * method would use a table)
     * IN assertion: 1 <= len <= 15
     * the value to invert
     * its bit length
     */
    private short bi_reverse(int code, int len) {
        code  = ((code & 0x5555) << 1 ) | ((code >> 1) & 0x5555);
        code  = ((code & 0x3333) << 2 ) | ((code >> 2) & 0x3333);
        code  = ((code & 0x0F0F) << 4 ) | ((code >> 4) & 0x0F0F);
        code  = ((code & 0x00FF) << 8 ) | ((code >> 8) & 0x00FF);
        return (short)(code >>> (16 - len));
    }

    /**
     * Compute the optimal bit lengths for a tree and update the total bit length
     * for the current block.
     *
     * IN assertion: the fields freq and dad are set, heap[heap_max] and
     *     above are the tree nodes sorted by increasing frequency.
     * OUT assertions: the field len is set to the optimal bit length, the
     *     array bl_count contains the frequencies for each bit length.
     *     The length opt_len is updated; static_len is also updated if stree is
     *     not null.
     */
    void gen_bitlen(short[] tree, int max_code, StaticTree stat_desc) {
        short[] stree = stat_desc.static_tree;
        int[] extra = stat_desc.extra_bits;
        int base = stat_desc.extra_base;
        int max_length = stat_desc.max_length;
        int h;              // heap index
        int n, m;           // iterate over the tree elements
        int bits;           // bit length
        int xbits;          // extra bits
        short f;            // frequency
        int overflow = 0;   // number of elements with bit length too large

        for (bits = 0; bits <= MAX_BITS; ++bits) {
            bl_count[bits] = 0;
        }

        // In a first pass, compute the optimal bit lengths (which may
        // overflow in the case of the bit length tree).
        tree[(heap[heap_max] << 1) + 1] = 0; // root of the heap

        for (h = heap_max + 1; h < HEAP_SIZE; ++h) {
            n = heap[h];
            bits = tree[(tree[(n << 1) + 1] << 1) + 1] + 1;
            if (bits > max_length){
                bits = max_length;
                ++overflow;
            }
            tree[(n << 1) + 1] = (short)bits;
            // We overwrite tree[n*2+1] which is no longer needed

            if (n > max_code) continue;  // not a leaf node

            ++bl_count[bits];
            xbits = 0;
            if (n >= base) xbits = extra[n-base];
            f = tree[n << 1];
            opt_len += f * (bits + xbits);
            if (null != stree) {
                static_len += f * (stree[(n << 1) + 1] + xbits);
            }
        }
        if (overflow == 0) return;

        // This happens for example on obj2 and pic of the Calgary corpus
        // Find the first bit length which could increase:
        do {
            bits = max_length - 1;
            while (0 == bl_count[bits]) {
                --bits;
            }
            --bl_count[bits];      // move one leaf down the tree
            bl_count[bits+1]+=2;   // move one overflow item as its brother
            --bl_count[max_length];
            // The brother of the overflow item also moves one step up,
            // but this does not affect bl_count[max_length]
            overflow -= 2;
        } while (overflow > 0);

        for (bits = max_length; bits != 0; --bits) {
            n = bl_count[bits];
            while (n != 0) {
                m = heap[--h];
                if (m > max_code) continue;
                if (tree[(m << 1) + 1] != bits) {
                    opt_len += ((long)bits - (long)tree[(m << 1) + 1])*(long)tree[m << 1];
                    tree[(m << 1) + 1] = (short)bits;
                }
                --n;
            }
        }
    }



    // Fill the window when the lookahead becomes insufficient.
    // Updates strstart and lookahead.
    //
    // IN assertion: lookahead < MIN_LOOKAHEAD
    // OUT assertions: strstart <= window_size-MIN_LOOKAHEAD
    //    At least one byte has been read, or avail_in == 0; reads are
    //    performed for at least two bytes (required for the zip translate_eol
    //    option -- not supported here).
    void fill_window() {
        int n, m;
        int p;
        int more;    // Amount of free space at the end of the window.

        do{
            more = (window_size-lookahead-strstart);

            // Deal with !@#$% 64K limit:
            if(more==0 && strstart==0 && lookahead==0){
                more = w_size;
            } else if(more==-1) {
                // Very unlikely, but possible on 16 bit machine if strstart == 0
                // and lookahead == 1 (input done one byte at time)
                more--;

                // If the window is almost full and there is insufficient lookahead,
                // move the upper half to the lower one to make room in the upper half.
            } else if (strstart >= w_size+ w_size-MIN_LOOKAHEAD) {
                System.arraycopy(window, w_size, window, 0, w_size);
                match_start-=w_size;
                strstart-=w_size; // we now have strstart >= MAX_DIST
                block_start-=w_size;

                // Slide the hash table (could be avoided with 32 bit values
                // at the expense of memory usage). We slide even when level == 0
                // to keep the hash table consistent if we switch back to level > 0
                // later. (Using level 0 permanently is not an optimal usage of
                // zlib, so we don't care about this pathological case.)

                n = hash_size;
                p=n;
                do {
                    m = (head[--p]&0xffff);
                    head[p]=(m>=w_size ? (short)(m-w_size) : 0);
                } while (--n != 0);

                n = w_size;
                p = n;
                do {
                    m = (prev[--p]&0xffff);
                    prev[p] = (m >= w_size ? (short)(m-w_size) : 0);
                    // If n is not on any hash chain, prev[n] is garbage but
                    // its value will never be used.
                } while (--n!=0);
                more += w_size;
            }

            if (strm.avail_in == 0) return;

            // If there was no sliding:
            //    strstart <= WSIZE+MAX_DIST-1 && lookahead <= MIN_LOOKAHEAD - 1 &&
            //    more == window_size - lookahead - strstart
            // => more >= window_size - (MIN_LOOKAHEAD-1 + WSIZE + MAX_DIST-1)
            // => more >= window_size - 2*WSIZE + 2
            // In the BIG_MEM or MMAP case (not yet supported),
            //   window_size == input_size + MIN_LOOKAHEAD  &&
            //   strstart + s->lookahead <= input_size => more >= MIN_LOOKAHEAD.
            // Otherwise, window_size == 2*WSIZE so more >= 2.
            // If there was sliding, more >= WSIZE. So in all cases, more >= 2.

            n = read_buf(window, strstart + lookahead, more);
            if (0 == noheader) {
                adlerHash = Adler32.adler32(adlerHash, window, strstart + lookahead, n);
            }
            lookahead += n;

            // Initialize the hash value now that we have some input:
            if(lookahead >= MIN_MATCH) {
                ins_h = window[strstart]&0xff;
                ins_h=(((ins_h)<<hash_shift)^(window[strstart+1]&0xff))&hash_mask;
            }
            // If the whole input has less than MIN_MATCH bytes, ins_h is garbage,
            // but this is not important since only literal bytes will be emitted.
        }
        while (lookahead < MIN_LOOKAHEAD && strm.avail_in != 0);
    }

    // Compress as much as possible from the input stream, return the current
    // block state.
    // This function does not perform lazy evaluation of matches and inserts
    // new strings in the dictionary only for unmatched strings or for short
    // matches. It is used only for the fast compression options.
    int deflate_fast(int flush) {
//    short hash_head = 0; // head of the hash chain
        int hash_head = 0; // head of the hash chain
        boolean bflush;      // set if current block must be flushed

        while (true) {
            // Make sure that we always have enough lookahead, except
            // at the end of the input file. We need MAX_MATCH bytes
            // for the next match, plus MIN_MATCH bytes to insert the
            // string following the next match.
            if (lookahead < MIN_LOOKAHEAD) {
                fill_window();
                if (lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                    return NeedMore;
                }
                if (lookahead == 0) break; // flush the current block
            }

            // Insert the string window[strstart .. strstart+2] in the
            // dictionary, and set hash_head to the head of the hash chain:
            if (lookahead >= MIN_MATCH) {
                ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;

//	prev[strstart&w_mask]=hash_head=head[ins_h];
                hash_head=(head[ins_h]&0xffff);
                prev[strstart&w_mask]=head[ins_h];
                head[ins_h]=(short)strstart;
            }

            // Find the longest match, discarding those <= prev_length.
            // At this point we have always match_length < MIN_MATCH

            if (0L != hash_head &&
                    ((strstart-hash_head)&0xffff) <= w_size-MIN_LOOKAHEAD
                    ) {
                // To simplify the code, we prevent matches with the string
                // of window index 0 (in particular we have to avoid a match
                // of the string with itself at the start of the input file).
                if (Z_HUFFMAN_ONLY != strategy) {
                    match_length = longest_match(hash_head);
                }
                // longest_match() sets match_start
            }
            if (match_length>=MIN_MATCH) {
                //        check_match(strstart, match_start, match_length);

                bflush = _tr_tally(strstart-match_start, match_length-MIN_MATCH);

                lookahead -= match_length;

                // Insert new strings in the hash table only if the match length
                // is not too large. This saves time but degrades compression.
                if(match_length <= max_lazy_match && lookahead >= MIN_MATCH) {
                    --match_length; // string at strstart already in hash table
                    do {
                        ++strstart;

                        ins_h=((ins_h<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;
//	    prev[strstart&w_mask]=hash_head=head[ins_h];
                        hash_head=(head[ins_h]&0xffff);
                        prev[strstart&w_mask]=head[ins_h];
                        head[ins_h]=(short)strstart;

                        // strstart never exceeds WSIZE-MAX_MATCH, so there are
                        // always MIN_MATCH bytes ahead.
                    } while (--match_length != 0);
                    ++strstart;

                } else {
                    strstart += match_length;
                    match_length = 0;
                    ins_h = window[strstart]&0xff;

                    ins_h=(((ins_h)<<hash_shift)^(window[strstart+1]&0xff))&hash_mask;
                    // If lookahead < MIN_MATCH, ins_h is garbage, but it does not
                    // matter since it will be recomputed at next deflate call.
                }
            } else {
                // No match, output a literal byte

                bflush=_tr_tally(0, window[strstart]&0xff);
                --lookahead;
                ++strstart;
            }

            if (bflush) {
                flush_block_only(false);
                if(0 == strm.avail_out) {
                    return NeedMore;
                }
            }
        }

        flush_block_only(Z_FINISH == flush);
        if (0 == strm.avail_out) {
            return (Z_FINISH == flush) ? FinishStarted : NeedMore;
        }
        return (Z_FINISH == flush) ? FinishDone : BlockDone;
    }

    // Same as above, but achieves better compression. We use a lazy
    // evaluation for matches: a match is finally adopted only if there is
    // no better match at the next window position.
    int deflate_slow(int flush) {
//    short hash_head = 0;    // head of hash chain
        int hash_head = 0;    // head of hash chain
        boolean bflush;         // set if current block must be flushed

        // Process the input block.
        while (true) {
            // Make sure that we always have enough lookahead, except
            // at the end of the input file. We need MAX_MATCH bytes
            // for the next match, plus MIN_MATCH bytes to insert the
            // string following the next match.

            if (lookahead < MIN_LOOKAHEAD) {
                fill_window();
                if(lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                    return NeedMore;
                }
                if(lookahead == 0) break; // flush the current block
            }

            // Insert the string window[strstart .. strstart+2] in the
            // dictionary, and set hash_head to the head of the hash chain:

            if (lookahead >= MIN_MATCH) {
                ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff)) & hash_mask;
//	prev[strstart&w_mask]=hash_head=head[ins_h];
                hash_head=(head[ins_h]&0xffff);
                prev[strstart&w_mask]=head[ins_h];
                head[ins_h]=(short)strstart;
            }

            // Find the longest match, discarding those <= prev_length.
            prev_length = match_length; prev_match = match_start;
            match_length = MIN_MATCH-1;

            if (hash_head != 0 && prev_length < max_lazy_match &&
                    ((strstart-hash_head)&0xffff) <= w_size-MIN_LOOKAHEAD
                    ){
                // To simplify the code, we prevent matches with the string
                // of window index 0 (in particular we have to avoid a match
                // of the string with itself at the start of the input file).

                if (strategy != Z_HUFFMAN_ONLY) {
                    match_length = longest_match(hash_head);
                }
                // longest_match() sets match_start

                if (match_length <= 5 && (strategy == Z_FILTERED ||
                        (match_length == MIN_MATCH &&
                        strstart - match_start > 4096))) {

                    // If prev_match is also MIN_MATCH, match_start is garbage
                    // but we will ignore the current match anyway.
                    match_length = MIN_MATCH-1;
                }
            }

            // If there was a match at the previous step and the current
            // match is not better, output the previous match:
            if (prev_length >= MIN_MATCH && match_length <= prev_length) {
                int max_insert = strstart + lookahead - MIN_MATCH;
                // Do not insert strings in hash table beyond this.

                //          check_match(strstart-1, prev_match, prev_length);

                bflush=_tr_tally(strstart-1-prev_match, prev_length - MIN_MATCH);

                // Insert in hash table all strings up to the end of the match.
                // strstart-1 and strstart are already inserted. If there is not
                // enough lookahead, the last two strings are not inserted in
                // the hash table.
                lookahead -= prev_length-1;
                prev_length -= 2;
                do {
                    if (++strstart <= max_insert) {
                        ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;
                        //prev[strstart&w_mask]=hash_head=head[ins_h];
                        hash_head=(head[ins_h]&0xffff);
                        prev[strstart&w_mask]=head[ins_h];
                        head[ins_h]=(short)strstart;
                    }
                } while (--prev_length != 0);
                match_available = 0;
                match_length = MIN_MATCH-1;
                ++strstart;

                if (bflush) {
                    flush_block_only(false);
                    if(0 == strm.avail_out) {
                        return NeedMore;
                    }
                }

            } else if (0 != match_available) {
                // If there was no match at the previous position, output a
                // single literal. If there was a match but the current match
                // is longer, truncate the previous match to a single literal.

                bflush=_tr_tally(0, window[strstart-1]&0xff);

                if (bflush) {
                    flush_block_only(false);
                }
                ++strstart;
                --lookahead;
                if(0 == strm.avail_out) {
                    return NeedMore;
                }

            } else {
                // There is no previous match to compare with, wait for
                // the next step to decide.

                match_available = 1;
                ++strstart;
                --lookahead;
            }
        }

        if(0 != match_available) {
            bflush = _tr_tally(0, window[strstart-1]&0xff);
            match_available = 0;
        }

        flush_block_only(Z_FINISH == flush);
        if(0 == strm.avail_out) {
            return (Z_FINISH == flush) ? FinishStarted : NeedMore;
        }

        return (Z_FINISH == flush) ? FinishDone : BlockDone;
    }

    int longest_match(int cur_match) {
        int chain_length = max_chain_length; // max hash chain length
        int scan = strstart;                 // current string
        int match;                           // matched string
        int len;                             // length of current match
        int best_len = prev_length;          // best match length so far
        int limit = strstart>(w_size-MIN_LOOKAHEAD) ?
            strstart-(w_size-MIN_LOOKAHEAD) : 0;
        int niceMatch = this.nice_match;

        // Stop when cur_match becomes <= limit. To simplify the code,
        // we prevent matches with the string of window index 0.

        int wmask = w_mask;

        int strend = strstart + MAX_MATCH;
        byte scan_end1 = window[scan+best_len-1];
        byte scan_end = window[scan+best_len];

        // The code is optimized for HASH_BITS >= 8 and MAX_MATCH-2 multiple of 16.
        // It is easy to get rid of this optimization if necessary.

        // Do not waste too much time if we already have a good match:
        if (prev_length >= good_match) {
            chain_length >>= 2;
        }

        // Do not look for matches beyond the end of the input. This is necessary
        // to make deflate deterministic.
        if (niceMatch > lookahead) niceMatch = lookahead;

        do {
            match = cur_match;

            // Skip to next match if the match length cannot increase
            // or if the match length is less than 2:
            if (window[match+best_len]   != scan_end  ||
                    window[match+best_len-1] != scan_end1 ||
                    window[match]       != window[scan]     ||
                    window[++match]     != window[scan+1])      continue;

            // The check at best_len-1 can be removed because it will be made
            // again later. (This heuristic is not always a win.)
            // It is not necessary to compare scan[2] and match[2] since they
            // are always equal when the other bytes match, given that
            // the hash keys are equal and that HASH_BITS >= 8.
            scan += 2; ++match;

            // We check for insufficient lookahead only every 8th comparison;
            // the 256th check will be made at strstart+258.
            do {
            } while (window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    window[++scan] == window[++match] &&
                    scan < strend);

            len = MAX_MATCH - (int)(strend - scan);
            scan = strend - MAX_MATCH;

            if(len>best_len) {
                match_start = cur_match;
                best_len = len;
                if (len >= niceMatch) break;
                scan_end1  = window[scan+best_len-1];
                scan_end   = window[scan+best_len];
            }

        } while ((cur_match = (prev[cur_match & wmask]&0xffff)) > limit
                && --chain_length != 0);

        if (best_len <= lookahead) return best_len;
        return lookahead;
    }

    public int deflateInit(int level, int bits) {
        return deflateInit2(level, Z_DEFLATED, bits, DEF_MEM_LEVEL,
                Z_DEFAULT_STRATEGY);
    }
    private int deflateInit2(int level, int method,  int windowBits,
            int memLevel, int strategy) {
        int noHeader = 0;

        if (level == Z_DEFAULT_COMPRESSION) {
            level = 6;
        }

        if (windowBits < 0) { // undocumented feature: suppress zlib header
            noHeader = 1;
            windowBits = -windowBits;
        }

        if (memLevel < 1 || memLevel > MAX_MEM_LEVEL ||
                method != Z_DEFLATED ||
                windowBits < 9 || windowBits > 15 || level < 0 || level > 9 ||
                strategy < 0 || strategy > Z_HUFFMAN_ONLY) {
            return Z_STREAM_ERROR;
        }

        this.noheader = noHeader;
        w_bits = windowBits;
        w_size = 1 << w_bits;
        w_mask = w_size - 1;

        hash_bits = memLevel + 7;
        hash_size = 1 << hash_bits;
        hash_mask = hash_size - 1;
        hash_shift = ((hash_bits + MIN_MATCH - 1) / MIN_MATCH);

        window = new byte[w_size * 2];
        prev = new short[w_size];
        head = new short[hash_size];

        lit_bufsize = 1 << (memLevel + 6); // 16K elements by default

        // We overlay pending_buf and d_buf+l_buf. This works since the average
        // output size for (length,distance) codes is <= 24 bits.
        pending_buf = new byte[lit_bufsize * 4];
        pending_buf_size = lit_bufsize * 4;

        d_buf = lit_bufsize / 2;
        l_buf = (1 + 2) * lit_bufsize;

        this.level = level;

//System.out.println("level="+level);

        this.strategy = strategy;
//    this.method = (byte)method;

        return deflateReset();
    }

    int deflateReset() {
//    strm.data_type = Z_UNKNOWN;

        pending = 0;
        pending_out = 0;

        if(noheader < 0) {
            noheader = 0; // was set to -1 by deflate(..., Z_FINISH);
        }
        status = (noheader!=0) ? BUSY_STATE : INIT_STATE;
        adlerHash = Adler32.adler32(0, null, 0, 0);

        last_flush = Z_NO_FLUSH;

        tr_init();
        lm_init();
        return Z_OK;
    }

    int deflateEnd() {
        if(status!=INIT_STATE && status!=BUSY_STATE && status!=FINISH_STATE){
            return Z_STREAM_ERROR;
        }
        // Deallocate in reverse order of allocations:
        pending_buf=null;
        head=null;
        prev=null;
        window=null;
        // free
        // dstate=null;
        return status == BUSY_STATE ? Z_DATA_ERROR : Z_OK;
    }


    int deflate(int flush) {
        int old_flush;

        if (flush > Z_FINISH || flush < 0) {
            return Z_STREAM_ERROR;
        }

        if (strm.next_out == null ||
                (strm.next_in == null && strm.avail_in != 0) ||
                (status == FINISH_STATE && flush != Z_FINISH)) {
            ZStream.setDeflateMsg(Z_STREAM_ERROR);
            return Z_STREAM_ERROR;
        }
        if (0 ==strm.avail_out) {
            ZStream.setDeflateMsg(Z_BUF_ERROR);
            return Z_BUF_ERROR;
        }

        old_flush = last_flush;
        last_flush = flush;

        // Write the zlib header
        if(status == INIT_STATE) {
            int header = (Z_DEFLATED + ((w_bits - 8) << 4)) << 8;
            int level_flags = ((level - 1) & 0xff) >> 1;

            if(level_flags>3) level_flags=3;
            header |= (level_flags<<6);
            if(strstart!=0) header |= PRESET_DICT;
            header+=31-(header % 31);

            status=BUSY_STATE;
            putShortMSB(header);


            // Save the adler32 of the preset dictionary:
            if(0 != strstart) {
                putShortMSB((int)(adlerHash >>> 16));
                putShortMSB((int)(adlerHash & 0xffff));
            }
            adlerHash = Adler32.adler32(0, null, 0, 0);
        }

        // Flush as much pending output as possible
        if(pending != 0) {
            flush_pending();
            if(strm.avail_out == 0) {
                //System.out.println("  avail_out==0");
                // Since avail_out is 0, deflate will be called again with
                // more output space, but possibly with both pending and
                // avail_in equal to zero. There won't be anything to do,
                // but this is not an error situation so make sure we
                // return OK instead of BUF_ERROR at next call of deflate:
                last_flush = -1;
                return Z_OK;
            }

            // Make sure there is something to do and avoid duplicate consecutive
            // flushes. For repeated and useless calls with Z_FINISH, we keep
            // returning Z_STREAM_END instead of Z_BUFF_ERROR.
        } else if ((0 == strm.avail_in) && (flush <= old_flush)
                && (Z_FINISH != flush)) {
            ZStream.setDeflateMsg(Z_BUF_ERROR);
            return Z_BUF_ERROR;
        }

        // User must not provide more input after the first FINISH:
        if ((FINISH_STATE == status) && (0 != strm.avail_in)) {
            ZStream.setDeflateMsg(Z_BUF_ERROR);
            return Z_BUF_ERROR;
        }

        // Start a new block or continue the current one.
        if ((0 != strm.avail_in) || (0 != lookahead) ||
                (Z_NO_FLUSH != flush && FINISH_STATE != status)) {
            int bstate = -1;
            switch (compress_func) {
                case STORED:
                    bstate = deflate_stored(flush);
                    break;
                case FAST:
                    bstate = deflate_fast(flush);
                    break;
                case SLOW:
                    bstate = deflate_slow(flush);
                    break;
                default:
            }

            if (FinishStarted == bstate || FinishDone == bstate) {
                status = FINISH_STATE;
            }
            if (NeedMore == bstate || FinishStarted == bstate) {
                if(strm.avail_out == 0) {
                    last_flush = -1; // avoid BUF_ERROR next call, see above
                }
                return Z_OK;
                // If flush != Z_NO_FLUSH && avail_out == 0, the next call
                // of deflate should use the same flush parameter to make sure
                // that the flush is complete. So we don't have to output an
                // empty block here, this will be done at next call. This also
                // ensures that for a very small output buffer, we emit at most
                // one empty block.
            }

            if (BlockDone == bstate) {
                if (Z_PARTIAL_FLUSH == flush) {
                    _tr_align();
                } else { // FULL_FLUSH or SYNC_FLUSH
                    _tr_stored_block(0, 0, false);
                    // For a full flush, this empty block will be recognized
                    // as a special marker by inflate_sync().
                    if(flush == Z_FULL_FLUSH) {
                        //state.head[s.hash_size-1]=0;
                        for(int i=0; i<hash_size/*-1*/; i++)  // forget history
                            head[i]=0;
                    }
                }
                flush_pending();
                if(0 == strm.avail_out) {
                    last_flush = -1; // avoid BUF_ERROR at next call, see above
                    return Z_OK;
                }
            }
        }

        if(Z_FINISH != flush) return Z_OK;
        if(0 != noheader) return Z_STREAM_END;

        // Write the zlib trailer (adler32)
        putShortMSB((int)(adlerHash>>>16));
        putShortMSB((int)(adlerHash&0xffff));
        flush_pending();

        // If avail_out is zero, the application will call deflate again
        // to flush the rest.
        noheader = -1; // write the trailer only once!
        return (0 != pending) ? Z_OK : Z_STREAM_END;
    }

    // Flush as much pending output as possible. All deflate() output goes
    // through this function so some applications may wish to modify it
    // to avoid allocating a large strm->next_out buffer and copying into it.
    // (See also read_buf()).
    private void flush_pending() {
        int len = pending;

        if (len > strm.avail_out) len = strm.avail_out;
        if (0 == len) return;

        System.arraycopy(pending_buf, pending_out,
                strm.next_out, strm.next_out_index, len);

        strm.next_out_index += len;
        pending_out += len;
        strm.avail_out -= len;
        pending -= len;
        if (0 == pending) {
            pending_out = 0;
        }
    }

    // Read a new buffer from the current input stream, update the adler32
    // and total number of bytes read.  All deflate() input goes through
    // this function so some applications may wish to modify it to avoid
    // allocating a large strm->next_in buffer and copying from it.
    // (See also flush_pending()).
    int read_buf(byte[] buf, int start, int size) {
        int len = strm.avail_in;

        if (len > size) len = size;
        if (0 == len) return 0;

        strm.avail_in -= len;

        System.arraycopy(strm.next_in, strm.next_in_index, buf, start, len);
        strm.next_in_index += len;
        return len;
    }

}
// #sijapp cond.end #
