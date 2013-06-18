



package com.jcraft.jzlib;

public final class Deflate {

    static final private int MAX_MEM_LEVEL = 9;

    static final private int Z_DEFAULT_COMPRESSION = -1;






    
    static final public int MAX_WBITS=9;
    static final private int DEF_MEM_LEVEL=1;


    static class Config {
        int good_length; 
        int max_lazy;    
        int nice_length; 
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

    
    static final private int NeedMore=0;

    
    static final private int BlockDone=1;

    
    static final private int FinishStarted=2;

    
    static final private int FinishDone=3;

    
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

    
    static final private int Z_DEFLATED=8;

    static final private int STORED_BLOCK=0;
    static final private int STATIC_TREES=1;
    static final private int DYN_TREES=2;

    
    static final private byte Z_BINARY=0;
    static final private byte Z_ASCII=1;
    static final private byte Z_UNKNOWN=2;

    static final private int Buf_size=8*2;

    
    static final private int REP_3_6=16;

    
    static final private int REPZ_3_10=17;

    
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

    ZBuffers strm;         
    int status;           
    byte[] pending_buf;   
    int pending_buf_size; 
    int pending_out;      
    int pending;          
    int noheader;         
    int last_flush;       

    int w_size;           
    int w_bits;           
    int w_mask;           

    byte[] window;
    
    
    
    
    
    
    

    int window_size;
    
    

    short[] prev;
    
    
    

    short[] head; 

    int ins_h;          
    int hash_size;      
    int hash_bits;      
    int hash_mask;      

    
    
    
    
    int hash_shift;

    
    

    int block_start;

    int match_length;           
    int prev_match;             
    int match_available;        
    int strstart;               
    int match_start;            
    int lookahead;              

    
    
    int prev_length;

    
    
    int max_chain_length;

    int compress_func;

    
    
    
    int max_lazy_match;

    
    
    

    
    int level;
    
    int strategy;

    
    int good_match;

    
    int nice_match;

    private short[] dyn_ltree;       
    private short[] dyn_dtree;       
    private short[] bl_tree;         
    private int dyn_l_maxcode;       
    private int dyn_d_maxcode;       
    

    
    private final StaticTree l_static_tree =
            new StaticTree(StaticTree.static_ltree, StaticTree.extra_lbits,
            StaticTree.LITERALS + 1, StaticTree.L_CODES, StaticTree.MAX_BITS);

    
    private final StaticTree d_static_tree =
            new StaticTree(StaticTree.static_dtree, StaticTree.extra_dbits,
            0,  StaticTree.D_CODES, StaticTree.MAX_BITS);

    
    private final StaticTree bl_static_tree =
            new StaticTree(null, StaticTree.extra_blbits,
            0, StaticTree.BL_CODES, StaticTree.MAX_BL_BITS);

    
    short[] bl_count = new short[MAX_BITS + 1];

    
    int[] heap = new int[2 * L_CODES + 1];

    int heap_len;               
    int heap_max;               
    
    

    
    byte[] depth = new byte[2 * L_CODES + 1];

    int l_buf;               

    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    int lit_bufsize;

    int last_lit;      

    
    
    

    int d_buf;         

    int opt_len;        
    int static_len;     
    int matches;        
    int last_eob_len;   

    
    
    short bi_buf;

    
    
    int bi_valid;

    private long adlerHash;

    Deflate(ZBuffers strm) {
        dyn_ltree = new short[HEAP_SIZE*2];
        dyn_dtree = new short[(2*D_CODES+1)*2]; 
        bl_tree = new short[(2*BL_CODES+1)*2];  
        this.strm = strm;
    }

    private void lm_init() {
        window_size = 2 * w_size;

        head[hash_size - 1] = 0;
        for (int i = 0; i < hash_size - 1; ++i) {
            head[i] = 0;
        }

        
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

    
    void tr_init() {

        bi_buf = 0;
        bi_valid = 0;
        last_eob_len = 8; 

        
        init_block();
    }

    void init_block() {
        
        for(int i = 0; i < L_CODES; ++i) dyn_ltree[i*2] = 0;
        for(int i= 0; i < D_CODES; ++i) dyn_dtree[i*2] = 0;
        for(int i= 0; i < BL_CODES; ++i) bl_tree[i*2] = 0;

        dyn_ltree[END_BLOCK*2] = 1;
        opt_len = static_len = 0;
        last_lit = matches = 0;
    }

    
    void pqdownheap(short[] tree, int k) {
        int v = heap[k];
        int j = k << 1;  
        while (j <= heap_len) {
            
            if (j < heap_len && smaller(tree, heap[j+1], heap[j], depth)) {
                ++j;
            }
            
            if(smaller(tree, v, heap[j], depth)) break;

            
            heap[k] = heap[j];
            k = j;
            
            j <<= 1;
        }
        heap[k] = v;
    }

    private boolean smaller(short[] tree, int n, int m, byte[] depth) {
        short tn2 = tree[n * 2];
        short tm2 = tree[m * 2];
        return (tn2 < tm2 || (tn2 == tm2 && depth[n] <= depth[m]));
    }

    
    void scan_tree(short[] tree, int max_code) {
        int n;                     
        int prevlen = -1;          
        int curlen;                
        int nextlen = tree[0*2+1]; 
        int count = 0;             
        int max_count = 7;         
        int min_count = 4;         

        if (nextlen == 0) { max_count = 138; min_count = 3; }
        tree[(max_code + 1) * 2 + 1] = (short)0xffff; 

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

    
    
    int build_bl_tree() {
        int max_blindex;  

        
        scan_tree(dyn_ltree, dyn_l_maxcode);
        scan_tree(dyn_dtree, dyn_d_maxcode);

        
        
        build_tree(bl_tree, bl_static_tree);

        
        

        
        
        
        for (max_blindex = BL_CODES-1; max_blindex >= 3; --max_blindex) {
            if (bl_tree[(StaticTree.bl_order[max_blindex] << 1) + 1] != 0) break;
        }
        
        opt_len += 3*(max_blindex+1) + 5+5+4;

        return max_blindex;
    }


    
    
    
    void send_all_trees(int lcodes, int dcodes, int blcodes) {
        send_bits(lcodes - 257, 5); 
        send_bits(dcodes - 1,   5);
        send_bits(blcodes - 4,  4); 
        for (int rank = 0; rank < blcodes; ++rank) {
            send_bits(bl_tree[(StaticTree.bl_order[rank] << 1) + 1], 3);
        }
        send_tree(dyn_ltree, lcodes - 1); 
        send_tree(dyn_dtree, dcodes - 1); 
    }

    
    void send_tree(short[] tree, int max_code) {
        int n;                     
        int prevlen = -1;          
        int curlen;                
        int nextlen = tree[0*2+1]; 
        int count = 0;             
        int max_count = 7;         
        int min_count = 4;         

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

    
    
    final void put_byte(byte[] p, int start, int len){
        System.arraycopy(p, start, pending_buf, pending, len);
        pending += len;
    }

    final void put_byte(byte c){
        pending_buf[pending++] = c;
    }
    final void put_short(int w) {
        put_byte((byte)(w));
        put_byte((byte)(w >>> 8));
    }
    final void putShortMSB(int b){
        put_byte((byte)(b >> 8));
        put_byte((byte)(b));
    }

    final void send_code(int c, short[] tree){
        int c2 = c * 2;
        send_bits((tree[c2] & 0xffff), (tree[c2+1] & 0xffff));
    }

    void send_bits(int value, int length) {
        int len = length;
        if (bi_valid > (int)Buf_size - len) {
            int val = value;

            bi_buf |= ((val << bi_valid)&0xffff);
            put_short(bi_buf);
            bi_buf = (short)(val >>> (Buf_size - bi_valid));
            bi_valid += len - Buf_size;
        } else {

            bi_buf |= (((value) << bi_valid)&0xffff);
            bi_valid += len;
        }
    }

    
    void _tr_align() {
        send_bits(STATIC_TREES << 1, 3);
        send_code(END_BLOCK, l_static_tree.static_tree);

        bi_flush();

        
        
        
        
        if (1 + last_eob_len + 10 - bi_valid < 9) {
            send_bits(STATIC_TREES << 1, 3);
            send_code(END_BLOCK, l_static_tree.static_tree);
            bi_flush();
        }
        last_eob_len = 7;
    }


    
    boolean _tr_tally(int dist, int lc) {
        pending_buf[d_buf + (last_lit << 1)] = (byte)(dist >>> 8);
        pending_buf[d_buf + (last_lit << 1) + 1] = (byte)dist;

        pending_buf[l_buf + last_lit] = (byte)lc;
        ++last_lit;

        if (dist == 0) {
            
            ++dyn_ltree[lc << 1];
        } else {
            ++matches;
            
            --dist;             
            ++dyn_ltree[(getLength_code(lc) + LITERALS + 1) * 2];
            ++dyn_dtree[d_code(dist) << 1];
        }

        if ((last_lit & 0x1fff) == 0 && level > 2) {
            
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
        
        
        
    }

    
    private int d_code(int dist) {
        return ((dist) < 256 ? StaticTree._dist_code[dist]
                : StaticTree._dist_code[256 + ((dist) >>> 7)]);
    }

    private byte getLength_code(int lc) {
        return StaticTree._length_code[lc];
    }

    
    void compress_block(short[] ltree, short[] dtree) {
        int  dist;      
        int lc;         
        int lx = 0;     
        int code;       
        int extra;      

        if (0 != last_lit) {
            do {
                dist = ((pending_buf[d_buf + (lx << 1)]<<8) & 0xff00) |
                        (pending_buf[d_buf + (lx << 1) + 1] & 0xff);
                lc = (pending_buf[l_buf+lx]) & 0xff;
                ++lx;

                if (0 == dist) {
                    send_code(lc, ltree); 
                } else {
                    
                    code = getLength_code(lc);

                    send_code(code+LITERALS+1, ltree); 
                    extra = l_static_tree.extra_bits[code];
                    if (extra != 0) {
                        lc -= StaticTree.base_length[code];
                        send_bits(lc, extra);       
                    }
                    --dist; 
                    code = d_code(dist);

                    send_code(code, dtree);       
                    extra = d_static_tree.extra_bits[code];
                    if (extra != 0) {
                        dist -= StaticTree.base_dist[code];
                        send_bits(dist, extra);   
                    }
                } 

                
            } while (lx < last_lit);
        }

        send_code(END_BLOCK, ltree);
        last_eob_len = ltree[END_BLOCK*2+1];
    }















    
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

    
    void bi_windup() {
        if (bi_valid > 8) {
            put_short(bi_buf);
        } else if (bi_valid > 0) {
            put_byte((byte)bi_buf);
        }
        bi_buf = 0;
        bi_valid = 0;
    }

    
    void copy_block(int buf, int len, boolean header) {
        int index = 0;
        bi_windup();      
        last_eob_len = 8; 

        if (header) {
            put_short((short)len);
            put_short((short)~len);
        }

        
        
        
        
        put_byte(window, buf, len);
    }

    void flush_block_only(boolean eof){
        _tr_flush_block(block_start >= 0 ? block_start : -1,
                strstart - block_start,
                eof);
        block_start = strstart;
        flush_pending();
    }

    
    
    
    
    
    
    
    int deflate_stored(int flush) {
        
        

        int max_block_size = 0xffff;
        int max_start;

        if (max_block_size > pending_buf_size - 5) {
            max_block_size = pending_buf_size - 5;
        }

        
        while(true) {
            
            if(lookahead<=1) {
                fill_window();
                if(lookahead==0 && flush==Z_NO_FLUSH) return NeedMore;
                if(lookahead==0) break; 
            }

            strstart+=lookahead;
            lookahead=0;

            
            max_start=block_start+max_block_size;
            if(strstart==0|| strstart>=max_start) {
                
                lookahead = (int)(strstart-max_start);
                strstart = (int)max_start;

                flush_block_only(false);
                if(strm.avail_out==0) return NeedMore;

            }

            
            
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

    
    void _tr_stored_block(int buf, int stored_len, boolean eof) {
        send_bits((STORED_BLOCK<<1)+(eof?1:0), 3);  
        copy_block(buf, stored_len, true);          
    }

    
    void _tr_flush_block(int buf, int stored_len, boolean eof) {
        int opt_lenb, static_lenb;
        int max_blindex = 0;      

        
        if(level > 0) {



            
            dyn_l_maxcode = build_tree(dyn_ltree, l_static_tree);
            dyn_d_maxcode = build_tree(dyn_dtree, d_static_tree);

            
            

            
            
            max_blindex=build_bl_tree();

            
            opt_lenb=(opt_len+3+7)>>>3;
            static_lenb=(static_len+3+7)>>>3;

            if(static_lenb<=opt_lenb) opt_lenb=static_lenb;
        } else {
            opt_lenb=static_lenb=stored_len+5; 
        }

        if (stored_len+4<=opt_lenb && buf != -1) {
            
            
            
            
            
            
            _tr_stored_block(buf, stored_len, eof);
        } else if (static_lenb == opt_lenb) {
            send_bits((STATIC_TREES << 1) + (eof ? 1 : 0), 3);
            compress_block(l_static_tree.static_tree, d_static_tree.static_tree);
        } else {
            send_bits((DYN_TREES << 1) + (eof ? 1 : 0), 3);
            send_all_trees(dyn_l_maxcode + 1, dyn_d_maxcode + 1, max_blindex + 1);
            compress_block(dyn_ltree, dyn_dtree);
        }

        
        

        init_block();

        if (eof) {
            bi_windup();
        }
    }


    
    int build_tree(short[] tree, StaticTree stat_desc) {
        final short[] stree = stat_desc.static_tree;
        int elems = stat_desc.elems;
        int n, m;          
        int max_code = -1; 
        int node;          

        
        
        
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

        
        
        
        
        while (heap_len < 2) {
            node = heap[++heap_len] = (max_code < 2 ? ++max_code : 0);
            tree[node << 1] = 1;
            depth[node] = 0;
            --opt_len;
            if (null != stree) {
                static_len -= stree[(node << 1) + 1];
            }
            
        }

        
        

        for (n = heap_len / 2; n >= 1; --n) {
            pqdownheap(tree, n);
        }

        
        

        node = elems;                 
        do{
            
            n = heap[1];
            heap[1] = heap[heap_len--];
            pqdownheap(tree, 1);
            m = heap[1];                

            heap[--heap_max] = n; 
            heap[--heap_max] = m;

            
            tree[node << 1] = (short)(tree[(n << 1)] + tree[m << 1]);
            depth[node] = (byte)(Math.max(depth[n], depth[m]) + 1);
            tree[(n << 1) + 1] = tree[(m << 1) + 1] = (short)node;

            
            heap[1] = node++;
            pqdownheap(tree, 1);
        } while (2 <= heap_len);

        heap[--heap_max] = heap[1];

        
        

        gen_bitlen(tree, max_code, stat_desc);

        
        gen_codes(tree, max_code, bl_count);
        return max_code;
    }

    
    private void gen_codes(short[] tree, int max_code, short[] bl_count) {
        short[] next_code = new short[MAX_BITS+1]; 
        short code = 0;            
        int bits;                  
        int n;                     

        
        
        for (bits = 1; bits <= MAX_BITS; ++bits) {
            next_code[bits] = code = (short)((code + bl_count[bits - 1]) << 1);
        }

        
        
        
        
        

        for (n = 0;  n <= max_code; ++n) {
            int len = tree[(n << 1) + 1];
            if (len == 0) continue;
            
            tree[n << 1] = bi_reverse(next_code[len]++, len);
        }
    }

    
    private short bi_reverse(int code, int len) {
        code  = ((code & 0x5555) << 1 ) | ((code >> 1) & 0x5555);
        code  = ((code & 0x3333) << 2 ) | ((code >> 2) & 0x3333);
        code  = ((code & 0x0F0F) << 4 ) | ((code >> 4) & 0x0F0F);
        code  = ((code & 0x00FF) << 8 ) | ((code >> 8) & 0x00FF);
        return (short)(code >>> (16 - len));
    }

    
    void gen_bitlen(short[] tree, int max_code, StaticTree stat_desc) {
        short[] stree = stat_desc.static_tree;
        int[] extra = stat_desc.extra_bits;
        int base = stat_desc.extra_base;
        int max_length = stat_desc.max_length;
        int h;              
        int n, m;           
        int bits;           
        int xbits;          
        short f;            
        int overflow = 0;   

        for (bits = 0; bits <= MAX_BITS; ++bits) {
            bl_count[bits] = 0;
        }

        
        
        tree[(heap[heap_max] << 1) + 1] = 0; 

        for (h = heap_max + 1; h < HEAP_SIZE; ++h) {
            n = heap[h];
            bits = tree[(tree[(n << 1) + 1] << 1) + 1] + 1;
            if (bits > max_length){
                bits = max_length;
                ++overflow;
            }
            tree[(n << 1) + 1] = (short)bits;
            

            if (n > max_code) continue;  

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

        
        
        do {
            bits = max_length - 1;
            while (0 == bl_count[bits]) {
                --bits;
            }
            --bl_count[bits];      
            bl_count[bits+1]+=2;   
            --bl_count[max_length];
            
            
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



    
    
    
    
    
    
    
    
    void fill_window() {
        int n, m;
        int p;
        int more;    

        do{
            more = (window_size-lookahead-strstart);

            
            if(more==0 && strstart==0 && lookahead==0){
                more = w_size;
            } else if(more==-1) {
                
                
                more--;

                
                
            } else if (strstart >= w_size+ w_size-MIN_LOOKAHEAD) {
                System.arraycopy(window, w_size, window, 0, w_size);
                match_start-=w_size;
                strstart-=w_size; 
                block_start-=w_size;

                
                
                
                
                

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
                    
                    
                } while (--n!=0);
                more += w_size;
            }

            if (strm.avail_in == 0) return;

            
            
            
            
            
            
            
            
            
            

            n = read_buf(window, strstart + lookahead, more);
            if (0 == noheader) {
                adlerHash = Adler32.adler32(adlerHash, window, strstart + lookahead, n);
            }
            lookahead += n;

            
            if(lookahead >= MIN_MATCH) {
                ins_h = window[strstart]&0xff;
                ins_h=(((ins_h)<<hash_shift)^(window[strstart+1]&0xff))&hash_mask;
            }
            
            
        }
        while (lookahead < MIN_LOOKAHEAD && strm.avail_in != 0);
    }

    
    
    
    
    
    int deflate_fast(int flush) {

        int hash_head = 0; 
        boolean bflush;      

        while (true) {
            
            
            
            
            if (lookahead < MIN_LOOKAHEAD) {
                fill_window();
                if (lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                    return NeedMore;
                }
                if (lookahead == 0) break; 
            }

            
            
            if (lookahead >= MIN_MATCH) {
                ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;


                hash_head=(head[ins_h]&0xffff);
                prev[strstart&w_mask]=head[ins_h];
                head[ins_h]=(short)strstart;
            }

            
            

            if (0L != hash_head &&
                    ((strstart-hash_head)&0xffff) <= w_size-MIN_LOOKAHEAD
                    ) {
                
                
                
                if (Z_HUFFMAN_ONLY != strategy) {
                    match_length = longest_match(hash_head);
                }
                
            }
            if (match_length>=MIN_MATCH) {
                

                bflush = _tr_tally(strstart-match_start, match_length-MIN_MATCH);

                lookahead -= match_length;

                
                
                if(match_length <= max_lazy_match && lookahead >= MIN_MATCH) {
                    --match_length; 
                    do {
                        ++strstart;

                        ins_h=((ins_h<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;

                        hash_head=(head[ins_h]&0xffff);
                        prev[strstart&w_mask]=head[ins_h];
                        head[ins_h]=(short)strstart;

                        
                        
                    } while (--match_length != 0);
                    ++strstart;

                } else {
                    strstart += match_length;
                    match_length = 0;
                    ins_h = window[strstart]&0xff;

                    ins_h=(((ins_h)<<hash_shift)^(window[strstart+1]&0xff))&hash_mask;
                    
                    
                }
            } else {
                

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

    
    
    
    int deflate_slow(int flush) {

        int hash_head = 0;    
        boolean bflush;         

        
        while (true) {
            
            
            
            

            if (lookahead < MIN_LOOKAHEAD) {
                fill_window();
                if(lookahead < MIN_LOOKAHEAD && flush == Z_NO_FLUSH) {
                    return NeedMore;
                }
                if(lookahead == 0) break; 
            }

            
            

            if (lookahead >= MIN_MATCH) {
                ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff)) & hash_mask;

                hash_head=(head[ins_h]&0xffff);
                prev[strstart&w_mask]=head[ins_h];
                head[ins_h]=(short)strstart;
            }

            
            prev_length = match_length; prev_match = match_start;
            match_length = MIN_MATCH-1;

            if (hash_head != 0 && prev_length < max_lazy_match &&
                    ((strstart-hash_head)&0xffff) <= w_size-MIN_LOOKAHEAD
                    ){
                
                
                

                if (strategy != Z_HUFFMAN_ONLY) {
                    match_length = longest_match(hash_head);
                }
                

                if (match_length <= 5 && (strategy == Z_FILTERED ||
                        (match_length == MIN_MATCH &&
                        strstart - match_start > 4096))) {

                    
                    
                    match_length = MIN_MATCH-1;
                }
            }

            
            
            if (prev_length >= MIN_MATCH && match_length <= prev_length) {
                int max_insert = strstart + lookahead - MIN_MATCH;
                

                

                bflush=_tr_tally(strstart-1-prev_match, prev_length - MIN_MATCH);

                
                
                
                
                lookahead -= prev_length-1;
                prev_length -= 2;
                do {
                    if (++strstart <= max_insert) {
                        ins_h=(((ins_h)<<hash_shift)^(window[(strstart)+(MIN_MATCH-1)]&0xff))&hash_mask;
                        
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
        int chain_length = max_chain_length; 
        int scan = strstart;                 
        int match;                           
        int len;                             
        int best_len = prev_length;          
        int limit = strstart>(w_size-MIN_LOOKAHEAD) ?
            strstart-(w_size-MIN_LOOKAHEAD) : 0;
        int niceMatch = this.nice_match;

        
        

        int wmask = w_mask;

        int strend = strstart + MAX_MATCH;
        byte scan_end1 = window[scan+best_len-1];
        byte scan_end = window[scan+best_len];

        
        

        
        if (prev_length >= good_match) {
            chain_length >>= 2;
        }

        
        
        if (niceMatch > lookahead) niceMatch = lookahead;

        do {
            match = cur_match;

            
            
            if (window[match+best_len]   != scan_end  ||
                    window[match+best_len-1] != scan_end1 ||
                    window[match]       != window[scan]     ||
                    window[++match]     != window[scan+1])      continue;

            
            
            
            
            
            scan += 2; ++match;

            
            
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

        if (windowBits < 0) { 
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

        lit_bufsize = 1 << (memLevel + 6); 

        
        
        pending_buf = new byte[lit_bufsize * 4];
        pending_buf_size = lit_bufsize * 4;

        d_buf = lit_bufsize / 2;
        l_buf = (1 + 2) * lit_bufsize;

        this.level = level;



        this.strategy = strategy;


        return deflateReset();
    }

    int deflateReset() {


        pending = 0;
        pending_out = 0;

        if(noheader < 0) {
            noheader = 0; 
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
        
        pending_buf=null;
        head=null;
        prev=null;
        window=null;
        
        
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

        
        if(status == INIT_STATE) {
            int header = (Z_DEFLATED + ((w_bits - 8) << 4)) << 8;
            int level_flags = ((level - 1) & 0xff) >> 1;

            if(level_flags>3) level_flags=3;
            header |= (level_flags<<6);
            if(strstart!=0) header |= PRESET_DICT;
            header+=31-(header % 31);

            status=BUSY_STATE;
            putShortMSB(header);


            
            if(0 != strstart) {
                putShortMSB((int)(adlerHash >>> 16));
                putShortMSB((int)(adlerHash & 0xffff));
            }
            adlerHash = Adler32.adler32(0, null, 0, 0);
        }

        
        if(pending != 0) {
            flush_pending();
            if(strm.avail_out == 0) {
                
                
                
                
                
                
                last_flush = -1;
                return Z_OK;
            }

            
            
            
        } else if ((0 == strm.avail_in) && (flush <= old_flush)
                && (Z_FINISH != flush)) {
            ZStream.setDeflateMsg(Z_BUF_ERROR);
            return Z_BUF_ERROR;
        }

        
        if ((FINISH_STATE == status) && (0 != strm.avail_in)) {
            ZStream.setDeflateMsg(Z_BUF_ERROR);
            return Z_BUF_ERROR;
        }

        
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
                    last_flush = -1; 
                }
                return Z_OK;
                
                
                
                
                
                
            }

            if (BlockDone == bstate) {
                if (Z_PARTIAL_FLUSH == flush) {
                    _tr_align();
                } else { 
                    _tr_stored_block(0, 0, false);
                    
                    
                    if(flush == Z_FULL_FLUSH) {
                        
                        for(int i=0; i<hash_size; i++)  
                            head[i]=0;
                    }
                }
                flush_pending();
                if(0 == strm.avail_out) {
                    last_flush = -1; 
                    return Z_OK;
                }
            }
        }

        if(Z_FINISH != flush) return Z_OK;
        if(0 != noheader) return Z_STREAM_END;

        
        putShortMSB((int)(adlerHash>>>16));
        putShortMSB((int)(adlerHash&0xffff));
        flush_pending();

        
        
        noheader = -1; 
        return (0 != pending) ? Z_OK : Z_STREAM_END;
    }

    
    
    
    
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


